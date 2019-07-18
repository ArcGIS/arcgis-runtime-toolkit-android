/*
 * Copyright 2019 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.esri.arcgisruntime.toolkit.ar

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.arch.lifecycle.DefaultLifecycleObserver
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.util.Log
import android.view.OrientationEventListener
import android.view.WindowManager
import android.widget.FrameLayout
import com.esri.arcgisruntime.mapping.ArcGISScene
import com.esri.arcgisruntime.mapping.view.AtmosphereEffect
import com.esri.arcgisruntime.mapping.view.Camera
import com.esri.arcgisruntime.mapping.view.DeviceOrientation
import com.esri.arcgisruntime.mapping.view.SceneView
import com.esri.arcgisruntime.mapping.view.SpaceEffect
import com.esri.arcgisruntime.mapping.view.TransformationMatrix
import com.esri.arcgisruntime.mapping.view.TransformationMatrixCameraController
import com.esri.arcgisruntime.toolkit.R
import com.esri.arcgisruntime.toolkit.extension.logTag
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Scene
import kotlinx.android.synthetic.main.layout_arcgisarview.view._arSceneView
import kotlinx.android.synthetic.main.layout_arcgisarview.view.arcGisSceneView

private const val CAMERA_PERMISSION_CODE = 0
private const val CAMERA_PERMISSION = Manifest.permission.CAMERA
private const val DEFAULT_TRANSLATION_TRANSFORMATION_FACTOR = 1.0

/**
 * This view simplifies the task of configuring a [SceneView] to be used for Augmented Reality experiences by calculating
 * the optimal [TransformationMatrix] to be set on the [Camera] of the [SceneView] by using the translation and quaternion
 * factors provided by the camera used in [ArSceneView].
 *
 * @since 100.6.0
 */
final class ArcGISArView : FrameLayout, DefaultLifecycleObserver, Scene.OnUpdateListener {

    /**
     * A Boolean defining whether a request for ARCore has been made. Used when requesting installation of ARCore.
     *
     * @since 100.6.0
     */
    private var arCoreInstallRequested: Boolean = false

    /**
     * Defines whether the background of the [SceneView] is transparent or not. Enabling transparency allows for the
     * [ArSceneView] to be visible underneath the SceneView.
     *
     * @since 100.6.0
     */
    private var renderVideoFeed: Boolean = true

    /**
     * Initial [TransformationMatrix] used by [cameraController].
     *
     * @since 100.6.0
     */
    private val initialTransformationMatrix = TransformationMatrix()

    /**
     * The camera controller used to control the camera that is used in [arcGisSceneView].
     *
     * @since 100.6.0
     */
    private var cameraController = TransformationMatrixCameraController()

    /**
     * A list of [OnStateChangedListener] used to notify when the state of this view has changed.
     *
     * @since 100.6.0
     */
    private val onStateChangedListeners: MutableList<OnStateChangedListener> = ArrayList()

    /**
     * Device Orientation to be used when setting Field of View.
     *
     * @since 100.6.0
     */
    private var deviceOrientation: DeviceOrientation? = null

    /**
     * Instance of WindowManager used to determine device orientation. Lazy delegated to prevent multiple calls to
     * [Context.getSystemService].
     *
     * @since 100.6.0
     */
    private val windowManager: WindowManager? by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    /**
     * Convenience value used to retrieve device rotation from WindowManager.
     *
     * @since 100.6.0
     */
    private val windowOrientation: Int?
        get() {
            return windowManager?.defaultDisplay?.rotation
        }

    /**
     * Event listener to listen for orientation changes. We are ignoring the orientation value supplied by it as it doesn't
     * reflect the orientation of the Window at all times. Instead we are making a call to the WindowManager to retrieve
     * the orientation it reports.
     *
     * @since 100.6.0
     */
    private val orientationEventListener by lazy {
        object : OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL) {
            override fun onOrientationChanged(orientation: Int) {
                this@ArcGISArView.deviceOrientation = when (windowOrientation) {
                    0 -> DeviceOrientation.PORTRAIT
                    1 -> DeviceOrientation.LANDSCAPE_RIGHT
                    2 -> DeviceOrientation.REVERSE_PORTRAIT
                    3 -> DeviceOrientation.LANDSCAPE_LEFT
                    else -> throw IllegalStateException("Unknown DeviceOrientation")
                }
            }
        }
    }

    /**
     * ArcGIS SceneView used to render the data from an [ArcGISScene].
     *
     * @since 100.6.0
     */
    val sceneView: SceneView get() = arcGisSceneView

    /**
     * A SurfaceView that integrates with ARCore and renders a scene.
     *
     * @since 100.6.0
     */
    val arSceneView: ArSceneView get() = _arSceneView

    /**
     * A Camera that defines the origin of the Camera used as the viewpoint for the [SceneView]. Setting this property
     * sets the origin camera of the [TransformationMatrixCameraController].
     *
     * @since 100.6.0
     */
    var originCamera: Camera? = null
        set(value) {
            field = value
            cameraController.originCamera = value
        }

    /**
     * This allows the "flyover" and the "table top" experience by augmenting the translation inside the
     * TransformationMatrix. Meaning that if the user moves 1 meter in real life, they could be moving faster in the
     * digital model, dependant on the value used. The default value is 1.0.
     *
     * @since 100.6.0
     */
    var translationTransformationFactor: Double = DEFAULT_TRANSLATION_TRANSFORMATION_FACTOR
        set(value) {
            field = value
            cameraController.translationFactor = value
        }

    /**
     * Represents the current status of this View. When this property set, notifies any [OnStateChangedListener] currently
     * added to this View.
     *
     * @since 100.6.0
     */
    var initializationStatus: ArcGISArViewState = ArcGISArViewState.NOT_INITIALIZED
        private set(value) {
            field = value
            onStateChangedListeners.forEach {
                it.onStateChanged(value)
            }
        }

    /**
     * Exposes an [Exception] should it occur when using this view.
     *
     * @since 100.6.0
     */
    var error: Exception? = null

    /**
     * Constructor used when instantiating this View directly to attach it to another view programmatically.
     *
     * @since 100.6.0
     */
    constructor(context: Context, renderVideoFeed: Boolean) : super(context) {
        this.renderVideoFeed = renderVideoFeed
        initialize()
    }

    /**
     * Constructor used when defining this view in an XML layout.
     *
     * @since 100.6.0
     */
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.ArcGISArView,
            0, 0
        ).apply {
            try {
                renderVideoFeed = getBoolean(R.styleable.ArcGISArView_renderVideoFeed, true)
            } finally {
                recycle()
            }
        }
        initialize()
    }

    /**
     * Initialize this View by inflating the layout containing the [SceneView] and [ArSceneView].
     *
     * @since 100.6.0
     */
    private fun initialize() {
        inflate(context, R.layout.layout_arcgisarview, this)
        sceneView.isManualRenderingEnabled = true
        sceneView.cameraController = cameraController
        originCamera = sceneView.currentViewpointCamera
        if (renderVideoFeed) {
            sceneView.spaceEffect = SpaceEffect.TRANSPARENT
            sceneView.atmosphereEffect = AtmosphereEffect.NONE
        }
        orientationEventListener.enable()
    }

    /**
     * Begins AR session. Should not be used in conjunction with [registerLifecycle] as when using [registerLifecycle] the
     * lifecycle of this View is maintained by the LifecycleOwner.
     *
     * @since 100.6.0
     */
    fun startTracking() {
        beginSession()
    }

    /**
     * Pauses AR session. Should not be used in conjunction with [registerLifecycle] as when using [registerLifecycle] the
     * lifecycle of this View is maintained by the LifecycleOwner.
     *
     * @since 100.6.0
     */
    fun stopTracking() {
        arSceneView.pause()
    }

    /**
     * Add a [listener] to be notified of changes to the [ArcGISArViewState].
     *
     * @since 100.6.0
     */
    fun addOnStateChangedListener(listener: OnStateChangedListener) {
        onStateChangedListeners.add(listener)
    }

    /**
     * Remove a [listener] that was previously added.
     *
     * @since 100.6.0
     */
    fun removeOnStateChangedListener(listener: OnStateChangedListener) {
        onStateChangedListeners.remove(listener)
    }

    /**
     * Register this View as a [DefaultLifecycleObserver] to the provided [lifecycle].
     *
     * @since 100.6.0
     */
    fun registerLifecycle(lifecycle: Lifecycle) {
        lifecycle.addObserver(this)
    }

    /**
     * The function invoked for the [Lifecycle.Event.ON_RESUME] lifecycle event.
     *
     * @since 100.6.0
     */
    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        beginSession()
    }

    /**
     * Checks the following prerequisites required for the use of ARCore:
     * - Checks for permissions required to use ARCore.
     * - Checks for an installation of ARCore.
     *
     * If prerequisites are met, the ARCore session is created and started, provided there are no exceptions. If there are
     * any exceptions related to permissions, ARCore installation or the beginning of an ARCore session, an exception is
     * caught, the [error] property set and the listeners are notified of an initialization failure. Otherwise,
     * listeners are notified that this view has initialized.
     *
     * This function currently assumes that the [Context] of this view is an instance of [Activity] to ensure that we can
     * request permissions. This may not always be the case and the handling of permission are under review.
     *
     * @since 100.6.0
     */
    @SuppressLint("MissingPermission") // suppressed as function returns if permission hasn't been granted
    private fun beginSession() {
        initializationStatus = ArcGISArViewState.INITIALIZING

        try {
            (context as? Activity)?.let {
                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                // when the permission is requested and the user responds to the request from the OS this is executed again
                // during onResume()
                if (!hasPermission(CAMERA_PERMISSION)) {
                    requestPermission(
                        it,
                        CAMERA_PERMISSION,
                        CAMERA_PERMISSION_CODE
                    )
                    return
                }

                // when the installation is requested and the user responds to the request from the OS this is executed again
                // during onResume()
                if (ArCoreApk.getInstance().requestInstall(
                        it,
                        !arCoreInstallRequested
                    ) == ArCoreApk.InstallStatus.INSTALL_REQUESTED
                ) {
                    arCoreInstallRequested = true
                    return
                }
            }

            // Create the session.
            Session(context).apply {
                val config = Config(this)
                config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                config.focusMode = Config.FocusMode.AUTO
                this.configure(config)
                arSceneView.setupSession(this)
            }

            arSceneView.scene.addOnUpdateListener(this)
        } catch (e: Exception) {
            error = e
        }

        if (error != null) {
            Log.e(logTag, error!!.message)
            initializationStatus = ArcGISArViewState.INITIALIZATION_FAILURE
        } else {
            arSceneView.resume()
            sceneView.resume()
            initializationStatus = ArcGISArViewState.INITIALIZED
        }
    }

    /**
     * Callback that is invoked once per frame immediately before the [Scene] is updated, with the provided [frameTime]
     * which provides time information for the current frame.
     *
     * @since 100.6.0
     */
    override fun onUpdate(frameTime: FrameTime?) {
        arSceneView.arFrame?.camera?.let { arCamera ->
            if (arCamera.trackingState == TrackingState.TRACKING) {
                TransformationMatrix(
                    arCamera.displayOrientedPose.rotationQuaternion.map {
                        it.toDouble()
                    }.toDoubleArray(),
                    arCamera.displayOrientedPose.translation.map {
                        it.toDouble()
                    }.toDoubleArray()
                ).let { arCoreTransMatrix ->
                    cameraController.transformationMatrix =
                        initialTransformationMatrix.addTransformation(arCoreTransMatrix)
                }
            }
            arCamera.imageIntrinsics.let {
                sceneView.setFieldOfViewFromLensIntrinsics(
                    it.focalLength[0],
                    it.focalLength[1],
                    it.principalPoint[0],
                    it.principalPoint[1],
                    it.imageDimensions[0].toFloat(),
                    it.imageDimensions[1].toFloat(),
                    if (deviceOrientation != null) deviceOrientation else DeviceOrientation.PORTRAIT
                )
            }
            if (sceneView.isManualRenderingEnabled) {
                sceneView.renderFrame()
            }
        }
    }

    /**
     * The function invoked for the [Lifecycle.Event.ON_PAUSE] lifecycle event.
     *
     * @since 100.6.0
     */
    override fun onPause(owner: LifecycleOwner) {
        arSceneView.pause()
        sceneView.pause()
        super.onPause(owner)
    }

    /**
     * The function invoked for the [Lifecycle.Event.ON_DESTROY] lifecycle event.
     * [ArSceneView] may have an issue with a leak and an error may appear in the LogCat:
     * https://github.com/google-ar/sceneform-android-sdk/issues/538
     *
     * @since 100.6.0
     */
    override fun onDestroy(owner: LifecycleOwner) {
        arSceneView.destroy()
        // disposing of SceneView causes the render surface, which is shared with ArSceneView, to become invalid and
        // rendering of the camera fails.
        // TODO https://devtopia.esri.com/runtime/java/issues/1170
        // sceneView.dispose()
        super.onDestroy(owner)
    }

    /**
     * Check to see we have been granted the necessary [permission] using the current [Context].
     *
     * @since 100.6.0
     */
    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request a [permission] using the provided [activity] and [permissionCode].
     *
     * @since 100.6.0
     */
    private fun requestPermission(activity: Activity, permission: String, permissionCode: Int) {
        ActivityCompat.requestPermissions(activity, arrayOf(permission), permissionCode)
    }

    /**
     * An interface that allows a user to receive updates of the state of [ArcGISArView].
     *
     * @since 100.6.0
     */
    interface OnStateChangedListener {
        /**
         * Called when the state of [ArcGISArView] changes using an appropriate [state] of type [ArcGISArViewState].
         *
         * @since 100.6.0
         */
        fun onStateChanged(state: ArcGISArViewState)
    }

    /**
     * A class representing the available states of [ArcGISArView].
     *
     * @since 100.6.0
     */
    enum class ArcGISArViewState {
        /**
         * Used to indicate that the [ArcGISArView] has not yet begun initializing.
         *
         * @since 100.6.0
         */
        NOT_INITIALIZED,

        /**
         * Used to indicate that the [ArcGISArView] is initializing.
         *
         * @since 100.6.0
         */
        INITIALIZING,

        /**
         * Used to indicate that the [ArcGISArView] has initialized correctly, an ARCore [Session] has begun
         * and the [SceneView] has resumed.
         *
         * @since 100.6.0
         */
        INITIALIZED,

        /**
         * Used to indicate that an [Exception] has occurred during initialization.
         *
         * @since 100.6.0
         */
        INITIALIZATION_FAILURE
    }
}
