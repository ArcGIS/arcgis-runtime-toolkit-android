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

package com.esri.arcgisruntime.toolkit.test.sceneview

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.esri.arcgisruntime.mapping.ArcGISScene
import com.esri.arcgisruntime.mapping.Basemap
import com.esri.arcgisruntime.toolkit.extension.logTag
import com.esri.arcgisruntime.toolkit.sceneview.ArcGisArView
import com.esri.arcgisruntime.toolkit.test.R
import kotlinx.android.synthetic.main.activity_arcgisceneview.arcGisArView

class ArcGisSceneViewActivity : AppCompatActivity(), ArcGisArView.OnStateChangedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_arcgisceneview)

        with(ArcGISScene(Basemap.createLightGrayCanvas())) {
            arcGisArView.sceneView.scene = this
        }

        arcGisArView.registerLifecycle(lifecycle)
        arcGisArView.addOnStateChangedListener(this)
    }

    override fun onStateChanged(state: ArcGisArView.ArcGisArViewState) {
        when (state) {
            is ArcGisArView.ArcGisArViewState.Initialized -> {/*no-op */
            }
            is ArcGisArView.ArcGisArViewState.InitializationFailure -> {
                with(getString(R.string.arcgisarview_error, state.exception.message)) {
                    Log.e(logTag, this)
                    Toast.makeText(this@ArcGisSceneViewActivity, this, Toast.LENGTH_SHORT).show()
                }
            }
            is ArcGisArView.ArcGisArViewState.PermissionRequired -> {
                with(getString(R.string.arcgisarview_permission_required, state.permission)) {
                    Log.d(logTag, this)
                    Toast.makeText(this@ArcGisSceneViewActivity, this, Toast.LENGTH_SHORT).show()
                }
            }
            is ArcGisArView.ArcGisArViewState.ArCoreInstallationRequired -> {
                with(getString(R.string.arcgisarview_arcore_install_required)) {
                    Log.d(logTag, this)
                    Toast.makeText(this@ArcGisSceneViewActivity, this, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.arcgissceneview_activity, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_start_tracking -> {
                arcGisArView.startTracking()
                return true
            }
            R.id.action_stop_tracking -> {
                arcGisArView.stopTracking()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        arcGisArView.removeOnStateChangedListener(this)
        super.onDestroy()
    }
}
