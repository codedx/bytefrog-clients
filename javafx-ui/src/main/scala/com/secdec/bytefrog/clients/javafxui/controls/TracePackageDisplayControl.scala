/*
 * bytefrog: a tracing framework for the JVM. For more information
 * see http://code-pulse.com/bytefrog
 *
 * Copyright (C) 2014 Applied Visions - http://securedecisions.avi.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.secdec.bytefrog.clients.javafxui.controls

import com.secdec.bytefrog.clients.javafxui.util.JavaFXUtilities
import com.secdec.bytefrog.clients.javafxui.util.PackageList

import javafx.beans.property.ListProperty
import javafx.beans.property.SimpleListProperty
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.control.ComboBox
import javafx.scene.control.ListView
import javafx.scene.layout.BorderPane

/** A custom control that displays package information, split into three columns:
  * traced packages, ignored packages, and packages that we couldn't instrument.
  *
  * @author robertf
  */
class TracePackageDisplayControl extends BorderPane with JavaFXUtilities {
	protected val SortOptionNatural = "Order of Encounter"
	protected val SortOptionABC = "Alphabetical"

	@FXML private var sortComboBox: ComboBox[String] = _
	@FXML private var tracedPackagesListView: ListView[String] = _
	@FXML private var ignoredPackagesListView: ListView[String] = _
	@FXML private var errorPackagesListView: ListView[String] = _

	/** Traced packages for display */
	val tracedPackages: ListProperty[String] = new SimpleListProperty(this, "tracePackages")
	private val tracedPackagesList = new PackageList(tracedPackages)

	/** Ignored packages for display */
	val ignoredPackages: ListProperty[String] = new SimpleListProperty(this, "ignoredPackages")
	private val ignoredPackagesList = new PackageList(ignoredPackages)

	/** Errored packages for display */
	val errorPackages: ListProperty[String] = new SimpleListProperty(this, "errorPackages")
	private val errorPackagesList = new PackageList(errorPackages)

	private val loader = new FXMLLoader(getClass.getResource("trace-package-display-control.fxml"))
	loader setRoot this
	loader setController this
	loader.load

	@FXML
	private def initialize() {
		sortComboBox.itemsProperty bind new SimpleListProperty(FXCollections.observableArrayList(SortOptionNatural, SortOptionABC))
		sortComboBox.valueProperty addListener changeListener((newSort: String) => setSort(newSort))
		sortComboBox.valueProperty set SortOptionNatural
	}

	private def setSort(newSort: String) = newSort match {
		case SortOptionABC =>
			tracedPackagesListView.itemsProperty bind tracedPackagesList.abcSort
			ignoredPackagesListView.itemsProperty bind ignoredPackagesList.abcSort
			errorPackagesListView.itemsProperty bind errorPackagesList.abcSort

		case SortOptionNatural =>
			tracedPackagesListView.itemsProperty bind tracedPackagesList.naturalSort
			ignoredPackagesListView.itemsProperty bind ignoredPackagesList.naturalSort
			errorPackagesListView.itemsProperty bind errorPackagesList.naturalSort
	}

	/** Current sort order, for binding/synchronization between instances of the control */
	def sort = sortComboBox.valueProperty
}