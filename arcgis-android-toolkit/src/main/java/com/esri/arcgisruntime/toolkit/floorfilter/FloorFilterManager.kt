/*
 * Copyright 2021 Esri
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

package com.esri.arcgisruntime.toolkit.floorfilter

import com.esri.arcgisruntime.ArcGISRuntimeException
import com.esri.arcgisruntime.geometry.Envelope
import com.esri.arcgisruntime.loadable.LoadStatus
import com.esri.arcgisruntime.mapping.GeoModel
import com.esri.arcgisruntime.mapping.Viewpoint
import com.esri.arcgisruntime.mapping.floor.FloorFacility
import com.esri.arcgisruntime.mapping.floor.FloorLevel
import com.esri.arcgisruntime.mapping.floor.FloorManager
import com.esri.arcgisruntime.mapping.floor.FloorSite
import com.esri.arcgisruntime.mapping.view.GeoView

/**
 * Manages the sharing of information between the [FloorFilterView] and the [SiteFacilityView].
 * Responsible for loading the [FloorManager], filtering the [GeoModel] based on the selected
 * [FloorLevel], and zooming to the selected [FloorSite] and [FloorFacility].
 *
 * @since 100.13.0
 */
internal class FloorFilterManager {

    /**
     * The list of [FloorLevel]s from the [FloorManager].
     *
     * @since 100.13.0
     */
    private val levels: List<FloorLevel>
        get() {
            return floorManager?.levels ?: emptyList()
        }

    /**
     * The list of [FloorSite]s from the [FloorManager].
     *
     * @since 100.13.0
     */
    val sites: List<FloorSite>
        get() {
            return floorManager?.sites ?: emptyList()
        }

    /**
     * The list of [FloorFacility]s from the [FloorManager].
     *
     * @since 100.13.0
     */
    val facilities: List<FloorFacility>
        get() {
            return floorManager?.facilities ?: emptyList()
        }

    /**
     * The [FloorManager] from the attached [GeoModel].
     *
     * @since 100.13.0
     */
    var floorManager: FloorManager? = null

    /**
     * Invoked when the selected facility changes
     *
     * @since 100.13.0
     */
    var onFacilityChangeListener: ((FloorFacility?) -> Unit)? = null

    /**
     * Invoked when the selected level changes
     *
     * @since 100.13.0
     */
    var onLevelChangeListener: ((FloorLevel?) -> Unit)? = null

    /**
     * The selected [FloorSite]'s site ID.
     *
     * @since 100.13.0
     */
    private var _selectedSiteId: String? = null
    var selectedSiteId: String?
        get() {
            return _selectedSiteId
        }
        set(value) {
            _selectedSiteId = value
            selectedFacilityId = null
            zoomToSite()
        }

    /**
     * The selected [FloorFacility]'s facility ID.
     *
     * @since 100.13.0
     */
    private var _selectedFacilityId: String? = null
    var selectedFacilityId: String?
        get() {
            return _selectedFacilityId
        }
        set(value) {
            _selectedFacilityId = value
            if (_selectedFacilityId != null) {
                _selectedSiteId = getSelectedFacility()?.site?.siteId
            }
            selectedLevelId = getDefaultLevelIdForFacility(value)
            zoomToFacility()
            onFacilityChangeListener?.invoke(getSelectedFacility())
        }

    /**
     * The selected [FloorLevel]'s level ID.
     *
     * @since 100.13.0
     */
    private var _selectedLevelId: String? = null
    var selectedLevelId: String?
        get() {
            return _selectedLevelId
        }
        set(value) {
            if (_selectedLevelId != value) {
                _selectedLevelId = value
                if (_selectedLevelId != null) {
                    val selectedLevelsFacility = getSelectedLevel()?.facility
                    _selectedFacilityId = selectedLevelsFacility?.facilityId
                    _selectedSiteId = selectedLevelsFacility?.site?.siteId
                }
                filterMap()
            }
            onLevelChangeListener?.invoke(getSelectedLevel())
        }

    /**
     * The [GeoView] attached to the [FloorFilterView].
     *
     * @since 100.13.0
     */
    var geoView: GeoView? = null
        private set


    /**
     * Returns true if the [level] is selected.
     *
     * @since 100.13.0
     */
    fun isLevelSelected(level: FloorLevel?): Boolean {
        return level != null && selectedLevelId == level.levelId
    }

    /**
     * Returns true if the [facility] is selected.
     *
     * @since 100.13.0
     */
    fun isFacilitySelected(facility: FloorFacility?): Boolean {
        return facility != null && selectedFacilityId == facility.facilityId
    }

    /**
     * Returns true if the [site] is selected.
     *
     * @since 100.13.0
     */
    fun isSiteSelected(site: FloorSite?): Boolean {
        return site != null && selectedSiteId == site.siteId
    }

    /**
     * Returns the selected [FloorLevel] or null if no [FloorLevel] is selected.
     *
     * @since 100.13.0
     */
    fun getSelectedLevel(): FloorLevel? {
        return levels.firstOrNull { isLevelSelected(it) }
    }

    /**
     * Returns the selected [FloorFacility] or null if no [FloorFacility] is selected.
     *
     * @since 100.13.0
     */
    fun getSelectedFacility(): FloorFacility? {
        return facilities.firstOrNull { isFacilitySelected(it) }
    }

    /**
     * Returns the selected [FloorSite] or null if no [FloorSite] is selected.
     *
     * @since 100.13.0
     */
    fun getSelectedSite(): FloorSite? {
        return sites.firstOrNull { isSiteSelected(it) }
    }

    /**
     * Loads the [FloorManager] of the [GeoView] attached to the [FloorFilterView]
     *
     * @since 100.13.0
     */
    fun setupFloorManager(geoView: GeoView, map: GeoModel, setupDone: ((LoadStatus, ArcGISRuntimeException?) -> Unit)) {
        this.geoView = geoView

        loadMap(map) {
            if (map.loadStatus == LoadStatus.LOADED) {
                floorManager = map.floorManager

                if (floorManager == null) {
                    // The map is not configured to be floor aware.
                    setupDone.invoke(LoadStatus.LOADED, null)
                } else {
                    val doneLoadingListener: Runnable = object: Runnable {
                        override fun run() {
                            floorManager?.removeDoneLoadingListener(this)

                            if (floorManager?.loadStatus == LoadStatus.FAILED_TO_LOAD) {
                                setupDone.invoke(LoadStatus.FAILED_TO_LOAD, floorManager?.loadError)
                            } else {
                                // Do this to make sure the UI gets set correctly if the selected level id was set
                                // before the floor manager loaded.
                                val temp = _selectedLevelId
                                _selectedLevelId = null
                                selectedLevelId = temp
                                filterMap()

                                setupDone.invoke(LoadStatus.LOADED, null)
                            }
                        }
                    }
                    floorManager?.addDoneLoadingListener(doneLoadingListener)
                    floorManager?.loadAsync()
                }
            } else {
                // The map load failed.
                setupDone.invoke(LoadStatus.FAILED_TO_LOAD, map.loadError)
            }
        }
    }

    /**
     * Loads the [GeoModel] of the [GeoView] attached to the [FloorFilterView]
     *
     * @since 100.13.0
     */
    private fun loadMap(map: GeoModel, doneLoading: (() -> Unit)) {
        if (map.loadStatus == LoadStatus.LOADED) {
            doneLoading.invoke()
            return
        }

        val doneLoadingListener: Runnable = object: Runnable {
            override fun run() {
                map.removeDoneLoadingListener(this)
                doneLoading.invoke()
            }
        }
        map.addDoneLoadingListener(doneLoadingListener)
        map.loadAsync()
    }

    /**
     * Removes the attached [GeoView].
     *
     * @since 100.13.0
     */
    fun clearGeoView() {
        clearMapFilter()
        this.geoView = null
        this.floorManager = null
    }

    /**
     * Zooms to the selected [FloorFacility]. If no [FloorFacility] is selected, it will zoom to
     * the selected [FloorSite].
     *
     * @since 100.13.0
     */
    internal fun zoomToSelection() {
        if (!selectedFacilityId.isNullOrBlank()) {
            zoomToFacility()
        } else if (!selectedSiteId.isNullOrBlank()) {
            zoomToSite()
        }
    }

    /**
     * Zooms to the selected [FloorSite].
     *
     * @since 100.13.0
     */
    private fun zoomToSite() {
        zoomToExtent(geoView, getSelectedSite()?.geometry?.extent)
    }

    /**
     * Zooms to the selected [FloorFacility].
     *
     * @since 100.13.0
     */
    private fun zoomToFacility() {
        zoomToExtent(geoView, getSelectedFacility()?.geometry?.extent)
    }

    /**
     * Zooms the [geoView] to the [envelope].
     *
     * @since 100.13.0
     */
    private fun zoomToExtent(geoView: GeoView?, envelope: Envelope?, bufferFactor: Double = 1.25) {
        if (geoView != null && envelope != null && !envelope.isEmpty) {
            val envelopeWithBuffer = Envelope(envelope.center, envelope.width * bufferFactor, envelope.height * bufferFactor)
            if (!envelopeWithBuffer.isEmpty) {
                geoView.setViewpointAsync(Viewpoint(envelopeWithBuffer), 0.5f)
            }
        }
    }

    /**
     * Filters the attached [GeoModel] to the selected [FloorLevel]. If no [FloorLevel] is
     * selected, clears the floor filter from the selected [GeoModel].
     *
     * @since 100.13.0
     */
    private fun filterMap() {
        // Set levels that match the selected level's vertical order to visible
        val selectedLevel = getSelectedLevel()
        if (selectedLevel == null) {
            clearMapFilter()
        } else {
            floorManager?.levels?.forEach {
                it?.isVisible = it?.verticalOrder == selectedLevel.verticalOrder
            }
        }
    }

    /**
     * Clears the floor filter from the attached [GeoModel].
     *
     * @since 100.13.0
     */
    private fun clearMapFilter() {
        floorManager?.levels?.forEach {
            it?.isVisible = true
        }
    }

    /**
     * Returns the level ID of the [FloorLevel] with [FloorLevel.getVerticalOrder] of 0. If no
     * [FloorLevel] has [FloorLevel.getVerticalOrder] of 0, it will return the level ID of the
     * [FloorLevel] with the lowest [FloorLevel.getVerticalOrder].
     *
     * @since 100.13.0
     */
    private fun getDefaultLevelIdForFacility(facilityId: String?): String? {
        val candidateLevels = levels.filter { it.facility?.facilityId == facilityId }
        return (candidateLevels.firstOrNull{ it.verticalOrder == 0 } ?: getLowestLevel(candidateLevels))?.levelId
    }

    /**
     * Returns the [FloorLevel] with the lowest[FloorLevel.getVerticalOrder].
     *
     * @since 100.13.0
     */
    private fun getLowestLevel(levels: List<FloorLevel>): FloorLevel? {
        var lowestLevel: FloorLevel? = null
        levels.forEach {
            if (it.verticalOrder != Int.MIN_VALUE && it.verticalOrder != Int.MAX_VALUE) {
                val lowestVerticalOrder = lowestLevel?.verticalOrder
                if (lowestVerticalOrder == null || lowestVerticalOrder > it.verticalOrder) {
                    lowestLevel = it
                }
            }
        }
        return lowestLevel
    }
}
