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

package com.esri.arcgisruntime.toolkit.sceneview

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import com.esri.arcgisruntime.geometry.Point
import com.esri.arcgisruntime.mapping.view.Camera
import com.esri.arcgisruntime.mapping.view.SceneView
import com.esri.arcgisruntime.toolkit.R
import com.esri.arcgisruntime.toolkit.extension.logTag
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import com.google.ar.sceneform.ArSceneView
import kotlinx.android.synthetic.main.layout_arcgisarview.view._arSceneView
import kotlinx.android.synthetic.main.layout_arcgisarview.view.arcGisSceneView


private const val CAMERA_PERMISSION_CODE = 0
private const val CAMERA_PERMISSION = Manifest.permission.CAMERA

class ArcGisArView : FrameLayout {

    private var installRequested: Boolean = false
    private var session: Session? = null

    val sceneView: SceneView get() = arcGisSceneView
    val arSceneView: ArSceneView get() = _arSceneView

    var camera: Camera
        get() = arcGisSceneView.currentViewpointCamera
        set(value) = arcGisSceneView.setViewpointCamera(value)

    constructor(context: Context) : super(context) {
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initialize()
    }

    private fun initialize() {
        inflateLayout()
        installRequested = false
    }

    private fun inflateLayout() {
        inflate(context, R.layout.layout_arcgisarview, this)
    }

    fun arScreenToLocation(screenPoint: android.graphics.Point): Point {
        return sceneView.screenToLocationAsync(screenPoint).get()
    }

    fun resetTracking() {
        // no-op
    }

    fun resetUsingLocationServices() {
        // no-op
    }

    fun resetUsingSpatialAnchor() {
        // no-op
    }

    fun startTracking() {
        // no-op
    }

    fun stopTracking() {
        // no-op
    }

    fun resume(activity: Activity) {
        if (session == null) {
            var exception: Exception? = null
            var message: String? = null
            try {
                if (ArCoreApk.getInstance().requestInstall(
                        activity,
                        !installRequested
                    ) == ArCoreApk.InstallStatus.INSTALL_REQUESTED
                ) {
                    installRequested = true
                    return
                }

                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!hasCameraPermission(activity)) {
                    requestCameraPermission(activity)
                    return
                }

                // Create the session.
                session = Session(context).apply {
                    val config = Config(this)
                    config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                    this.configure(config)
                    arSceneView.setupSession(this)
                }

            } catch (e: UnavailableArcoreNotInstalledException) {
                message = "Please install ARCore"
                exception = e
            } catch (e: UnavailableUserDeclinedInstallationException) {
                message = "Please install ARCore"
                exception = e
            } catch (e: UnavailableApkTooOldException) {
                message = "Please update ARCore"
                exception = e
            } catch (e: UnavailableSdkTooOldException) {
                message = "Please update this app"
                exception = e
            } catch (e: UnavailableDeviceNotCompatibleException) {
                message = "This device does not support AR"
                exception = e
            } catch (e: Exception) {
                message = "Failed to create AR session"
                exception = e
            }

            if (message != null) {
                Log.e(logTag, "Exception creating session", exception)
                return
            }
        }
        arSceneView.resume()
        arcGisSceneView.resume()
    }

    fun pause() {
        arcGisSceneView.pause()
        arSceneView.pause()
    }

    fun dispose() {
        arcGisSceneView.dispose()
        session = null
    }

    /**
     * Check to see we have the necessary permissions for accessing the camera using the instance of [Activity].
     *
     * @since 100.6.0
     */
    private fun hasCameraPermission(activity: Activity): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            CAMERA_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check to see we have the necessary permissions for the camera using the instance of [Activity], and ask for them
     * if we don't.
     *
     * @since 100.6.0
     */
    private fun requestCameraPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity, arrayOf(CAMERA_PERMISSION), CAMERA_PERMISSION_CODE
        )
    }
}