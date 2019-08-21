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

package com.esri.arcgisruntime.toolkit.test.ar

import android.graphics.Color
import android.graphics.Point
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import com.esri.arcgisruntime.layers.IntegratedMeshLayer
import com.esri.arcgisruntime.layers.PointCloudLayer
import com.esri.arcgisruntime.location.AndroidLocationDataSource
import com.esri.arcgisruntime.location.LocationDataSource
import com.esri.arcgisruntime.mapping.ArcGISScene
import com.esri.arcgisruntime.mapping.ArcGISTiledElevationSource
import com.esri.arcgisruntime.mapping.Basemap
import com.esri.arcgisruntime.mapping.Surface
import com.esri.arcgisruntime.mapping.view.Camera
import com.esri.arcgisruntime.mapping.view.DefaultSceneViewOnTouchListener
import com.esri.arcgisruntime.mapping.view.TransformationMatrixCameraController
import com.esri.arcgisruntime.portal.Portal
import com.esri.arcgisruntime.portal.PortalItem
import com.esri.arcgisruntime.toolkit.test.R
import kotlinx.android.synthetic.main.activity_ar_arcgissceneview.arcGisArView
import kotlinx.android.synthetic.main.activity_ar_arcgissceneview.calibrationLayout
import kotlinx.android.synthetic.main.activity_ar_arcgissceneview_calibration.calibBtnBigIncAlt
import kotlinx.android.synthetic.main.activity_ar_arcgissceneview_calibration.calibBtnDecAlt
import kotlinx.android.synthetic.main.activity_ar_arcgissceneview_calibration.calibBtnIncAlt
import kotlinx.android.synthetic.main.activity_ar_arcgissceneview_calibration.calibBtnRotLft
import kotlinx.android.synthetic.main.activity_ar_arcgissceneview_calibration.calibBtnRotRht
import kotlinx.android.synthetic.main.activity_ar_arcgissceneview_calibration.calibBtnSurface

/**
 * Activity to show usages of [ArcGISArView].
 *
 * @since 100.6.0
 */
class ArcGISArViewActivity : AppCompatActivity() {

    private val androidLocationDataSource: LocationDataSource
    get() {
        return AndroidLocationDataSource(this)
    }

    /**
     * AR Mode: Full-Scale AR
     * Scene that uses a Streets Basemap.
     *
     * @since 100.6.0
     */
    private fun streetsScene(): () -> ArcGISScene {
        return {
            ArcGISScene(Basemap.createStreets()).apply {
                addElevationSource(this)

                arcGisArView.locationDataSource = androidLocationDataSource
                arcGisArView.translationTransformationFactor = 1.0
            }
        }
    }

    /**
     * AR Mode: Tabletop AR
     * Scene that shows a Point Cloud Layer.
     *
     * @since 100.6.0
     */
    private fun pointCloudScene(): () -> ArcGISScene {
        return {
            ArcGISScene().apply {
                val portal = Portal("http://www.arcgis.com")
                val portalItem = PortalItem(portal, "fc3f4a4919394808830cd11df4631a54")
                val layer = PointCloudLayer(portalItem)
                addElevationSource(this)
                this.operationalLayers.add(layer)

                layer.addDoneLoadingListener {
                    layer.loadError?.let {
                        it.message?.let { errorMessage ->
                            displayErrorMessage(errorMessage)
                        }
                        return@addDoneLoadingListener
                    }

                    val extent = layer.fullExtent

                    if (extent != null) {
                        val center = extent.center
                        val camera = Camera(center, 0.0, 0.0, 0.0)
                        arcGisArView.originCamera = camera
                        arcGisArView.translationTransformationFactor = 2000.0
                    }
                }

                arcGisArView.locationDataSource = null
            }
        }
    }

    /**
     * AR Mode: Tabletop AR
     * Scene that is centered on Yosemite National Park.
     *
     * @since 100.6.0
     */
    private fun yosemiteScene(): () -> ArcGISScene {
        return {
            ArcGISScene().apply {
                addElevationSource(this)

                val layer =
                    IntegratedMeshLayer("https://tiles.arcgis.com/tiles/FQD0rKU8X5sAQfh8/arcgis/rest/services/VRICON_Yosemite_Sample_Integrated_Mesh_scene_layer/SceneServer")
                this.operationalLayers.add(layer)

                layer.addDoneLoadingListener {
                    layer.loadError?.let {
                        it.message?.let { errorMessage ->
                            displayErrorMessage(errorMessage)
                            return@addDoneLoadingListener
                        }
                    }

                    val extent = layer.fullExtent
                    val center = extent.center

                    val elevationSource = baseSurface.elevationSources.first()

                    elevationSource.addDoneLoadingListener {
                        loadError?.let {
                            it.message?.let { errorMessage ->
                                displayErrorMessage(errorMessage)
                            }
                            return@addDoneLoadingListener
                        }

                        val elevationFuture = this.baseSurface.getElevationAsync(center)

                        // when the elevation has loaded
                        elevationFuture.addDoneListener {
                            loadError?.let {
                                it.message?.let { errorMessage ->
                                    displayErrorMessage(errorMessage)
                                }
                                return@addDoneListener
                            }

                            val elevation = elevationFuture.get()
                            val camera = Camera(
                                center.y,
                                center.x,
                                elevation,
                                0.0,
                                0.0,
                                0.0
                            )
                            arcGisArView.originCamera = camera
                            arcGisArView.translationTransformationFactor = 1000.0
                        }
                    }
                }
                arcGisArView.locationDataSource = null
            }
        }
    }

    /**
     * AR Mode: Tabletop AR
     * Scene that is centered on the US-Mexico border.
     *
     * @since 100.6.0
     */
    private fun borderScene(): () -> ArcGISScene {
        return {
            ArcGISScene().apply {
                addElevationSource(this)

                val layer =
                    IntegratedMeshLayer("https://tiles.arcgis.com/tiles/FQD0rKU8X5sAQfh8/arcgis/rest/services/VRICON_SW_US_Sample_Integrated_Mesh_scene_layer/SceneServer")
                this.operationalLayers.add(layer)
                this.addDoneLoadingListener {
                    this.loadError?.let {
                        it.message?.let { errorMessage ->
                            displayErrorMessage(errorMessage)
                            return@addDoneLoadingListener
                        }
                    }

                    val extent = layer.fullExtent
                    val center = extent.center

                    val elevationSource = baseSurface.elevationSources.first()
                    elevationSource.addDoneLoadingListener {
                        loadError?.let {
                            it.message?.let { errorMessage ->
                                displayErrorMessage(errorMessage)
                            }
                            return@addDoneLoadingListener
                        }

                        val elevationFuture = this.baseSurface.getElevationAsync(center)
                        // when the elevation has loaded
                        elevationFuture.addDoneListener {
                            loadError?.let {
                                it.message?.let { errorMessage ->
                                    displayErrorMessage(errorMessage)
                                }
                                return@addDoneListener
                            }

                            val elevation = elevationFuture.get()
                            val camera = Camera(
                                center.y,
                                center.x,
                                elevation,
                                0.0,
                                0.0,
                                0.0
                            )
                            arcGisArView.originCamera = camera
                            arcGisArView.translationTransformationFactor = 1000.0
                        }
                    }
                }
                arcGisArView.locationDataSource = null
            }
        }
    }

    /**
     * AR Mode: Full-Scale AR.
     * Scene that is empty with an elevation source.
     *
     * @since 100.6.0
     */
    private fun emptyScene(): () -> ArcGISScene {
        return {
            ArcGISScene().apply {
                addElevationSource(this)

                arcGisArView.locationDataSource = androidLocationDataSource
                arcGisArView.originCamera = Camera(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
                arcGisArView.translationTransformationFactor = 1.0
            }
        }
    }

    /**
     * AR Mode: Full-Scale AR.
     * Scene that loads a FeatureLayer containing fire hydrant locations.
     *
     * @since 100.6.0
     */
    private fun redlandsFireHydrantsScene(): () -> ArcGISScene {
        return {
            ArcGISScene("http://www.arcgis.com/home/webscene/viewer.html?webscene=d406d82dbc714d5da146d15b024e8d33").apply {
                arcGisArView.locationDataSource = androidLocationDataSource
                arcGisArView.originCamera = Camera(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
                arcGisArView.translationTransformationFactor = 1.0
            }
        }
    }

    private val scenes: Array<SceneInfo> by lazy {
        arrayOf(
            SceneInfo(streetsScene(), getString(R.string.arcgis_ar_view_scene_streets)),
            SceneInfo(pointCloudScene(), getString(R.string.arcgis_ar_view_scene_point_cloud)),
            SceneInfo(yosemiteScene(), getString(R.string.arcgis_ar_view_scene_yosemite)),
            SceneInfo(borderScene(), getString(R.string.arcgis_ar_view_scene_border)),
            SceneInfo(emptyScene(), getString(R.string.arcgis_ar_view_scene_empty)),
            SceneInfo(
                redlandsFireHydrantsScene(),
                getString(R.string.arcgis_ar_view_redlands_fire_hydrants)
            )
        )
    }

    private var currentScene: SceneInfo? = null
        set(value) {
            field = value
            arcGisArView.sceneView.scene = value?.scene?.invoke()
            title = value?.name
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar_arcgissceneview)
        arcGisArView.registerLifecycle(lifecycle)
        currentScene = scenes[0]

        arcGisArView.sceneView.setOnTouchListener(object :
            DefaultSceneViewOnTouchListener(arcGisArView.sceneView) {
            override fun onSingleTapConfirmed(motionEvent: MotionEvent?): Boolean {
                motionEvent?.let {
                    with(Point(motionEvent.x.toInt(), motionEvent.y.toInt())) {
                        if (arcGisArView.locationDataSource != null) {
                            // TODO implement creation of point when implementing screenToLocation
                        } else {
                            arcGisArView.setInitialTransformationMatrix(this)
                        }
                    }
                }
                return false
            }
        })

        calibBtnSurface.setOnClickListener {
            setElevationToSurface()
        }

        calibBtnBigIncAlt.setOnClickListener {
            changeCameraAltitude(10.0)
        }

        calibBtnIncAlt.setOnClickListener {
            changeCameraAltitude(0.1)
        }

        calibBtnRotLft.setOnClickListener {
            rotateCamera(-1.0)
        }

        calibBtnRotRht.setOnClickListener {
            rotateCamera(1.0)
        }

        calibBtnDecAlt.setOnClickListener {
            changeCameraAltitude(-1.0)
        }
    }

    private fun setElevationToSurface() {
        val tmcc = arcGisArView.sceneView.cameraController as TransformationMatrixCameraController
        val camera = tmcc.originCamera
        val point = camera.location
        val elevationFuture = arcGisArView.sceneView.scene.baseSurface.getElevationAsync(point)

        // when the elevation has loaded
        elevationFuture.addDoneListener {
            val elevation = elevationFuture.get()
            val surfacePoint =
                com.esri.arcgisruntime.geometry.Point(
                    point.x,
                    point.y,
                    elevation + 1.8,
                    point.spatialReference
                )
            val surfaceCamera = Camera(
                surfacePoint,
                camera.heading,
                camera.pitch,
                camera.roll
            )
            tmcc.originCamera = surfaceCamera
        }
    }

    private fun changeCameraAltitude(deltaAltitude: Double) {
        val tmcc = arcGisArView.sceneView.cameraController as TransformationMatrixCameraController
        val camera = tmcc.originCamera
        tmcc.originCamera = camera.elevate(deltaAltitude)
    }

    private fun rotateCamera(rotationDelta: Double) {
        val tmcc = arcGisArView.sceneView.cameraController as TransformationMatrixCameraController
        val camera = tmcc.originCamera
        tmcc.originCamera =
            camera.rotateTo(camera.heading + rotationDelta, camera.pitch, camera.roll)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.arcgisarview_menu, menu)
        scenes.forEachIndexed { index, sceneInfo ->
            menu?.add(R.id.arcgisArViewMenuSceneGroup, index, Menu.NONE, sceneInfo.name)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.groupId) {
            R.id.arcgisArViewMenuActionGroup -> {
                handleMenuAction(item.itemId)
                return true
            }
            R.id.arcgisArViewMenuSceneGroup -> {
                selectScene(item.itemId)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun handleMenuAction(itemId: Int) {
        if (itemId == R.id.actionToggleCalibration) {
            toggleCalibration()
        }
    }

    private fun selectScene(itemId: Int) {
        currentScene = scenes[itemId]
    }

    private fun toggleCalibration() {
        calibrationLayout.visibility =
            if (calibrationLayout.visibility == View.GONE) View.VISIBLE else View.GONE
    }

    /**
     * Displays an error message in LogCat and as a Toast.
     *
     * @since 100.6.0
     */
    private fun displayErrorMessage(error: String) {
        Log.e("LOG", error)
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
    }
}

/**
 * Adds an elevation source to the provided [scene].
 *
 * @since 100.6.0
 */
private fun addElevationSource(scene: ArcGISScene) {
    val elevationSource =
        ArcGISTiledElevationSource("https://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer")
    val surface = Surface()
    surface.elevationSources.add(elevationSource)
    surface.name = "baseSurface"
    surface.isEnabled = true
    surface.backgroundGrid.color = Color.TRANSPARENT
    surface.backgroundGrid.gridLineColor = Color.TRANSPARENT
    scene.baseSurface = surface
}

private data class SceneInfo(val scene: () -> ArcGISScene, val name: String)
