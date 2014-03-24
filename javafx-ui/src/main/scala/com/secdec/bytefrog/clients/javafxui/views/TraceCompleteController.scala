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

package com.secdec.bytefrog.clients.javafxui.views

import java.awt.Desktop
import java.io.File
import java.net.URL
import java.util.ResourceBundle

import com.secdec.bytefrog.clients.javafxui.controls.SegmentControl
import com.secdec.bytefrog.clients.javafxui.controls.TracePackageDisplayControl
import com.secdec.bytefrog.clients.javafxui.util.JavaFXUtilities

import javafx.beans.binding.Bindings
import javafx.beans.binding.BooleanBinding
import javafx.beans.binding.DoubleBinding
import javafx.beans.binding.StringBinding
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Control
import javafx.scene.control.Label
import javafx.scene.control.SplitPane
import javafx.scene.control.TextField
import javafx.scene.layout.VBox

import reactive.EventSource
import reactive.EventStream

/** JavaFX controller class for the trace complete view
  * @author robertf
  */
class TraceCompleteController extends Initializable with JavaFXUtilities {
	val SortOptionNatural = "Order of Encounter"
	val SortOptionABC = "Alphabetical"

	// these are protected because scala does weird things when they're private
	@FXML protected var traceNameLabel: Label = _
	@FXML protected var traceTimeLabel: Label = _
	@FXML protected var traceSizeLabel: Label = _
	@FXML protected var traceFileField: TextField = _
	@FXML protected var packageSegmentSplitPane: SplitPane = _
	@FXML protected var tracePackagesDisplay: TracePackageDisplayControl = _
	@FXML protected var segmentListVBox: VBox = _
	@FXML protected var segmentList: SegmentControl = _
	@FXML protected var noSegmentLabel: Label = _

	val model = new TraceCompleteModel

	/** Event fired when the user requests a new trace */
	def newTraceEvent: EventStream[Unit] = newTraceEventSource
	private val newTraceEventSource = new EventSource[Unit]

	def initialize(url: URL, resources: ResourceBundle) {
		traceNameLabel.textProperty bind model.traceName
		traceSizeLabel.textProperty bind model.traceSize
		traceFileField.textProperty bind model.traceFile

		// custom binding that formats the time value (in milliseconds)
		val timerBinding = new StringBinding {
			super.bind(model.traceTime)

			def computeValue(): String = {
				val value = model.traceTime.get
				val minutes = (value / 60000).toInt
				val seconds = ((value / 1000) % 60)
				val tenths = ((value / 100) % 10)

				if (minutes > 0)
					f"$minutes:$seconds%02d.$tenths%01d"
				else
					f"$seconds%2d.$tenths%01d"
			}
		}
		traceTimeLabel.textProperty bind timerBinding

		model.packageSort bindBidirectional tracePackagesDisplay.sort
		tracePackagesDisplay.tracedPackages bind model.tracedPackages
		tracePackagesDisplay.ignoredPackages bind model.ignoredPackages
		tracePackagesDisplay.errorPackages bind model.errorPackages

		// don't scroll segment list to bottom automatically
		segmentList setAutoScroll false

		// only show segment list if there are segment entries
		val segmentEntriesVisibleBinding = new BooleanBinding {
			super.bind(model.segments)

			def computeValue() = !model.segments.isEmpty()
		}
		// making the node not managed when not visible will make it not affect layout of other nodes
		segmentList.managedProperty bind segmentList.visibleProperty
		segmentList.visibleProperty bind segmentEntriesVisibleBinding

		// bind segment entries
		segmentList.segmentEntries bind model.segments

		// show the "no segments" label when appropriate
		noSegmentLabel.managedProperty bind noSegmentLabel.visibleProperty
		noSegmentLabel.visibleProperty bind Bindings.not(segmentEntriesVisibleBinding)

		// the segment list's parent layout's max height depends on whether or not we want it resizable
		val segmentVBoxMaxHeightBinding = new DoubleBinding {
			super.bind(segmentEntriesVisibleBinding)

			// if segment entries list is visible, use computed max height (resizable), otherwise, use pref size (not)
			def computeValue() = if (segmentEntriesVisibleBinding.get) Control.USE_COMPUTED_SIZE else Control.USE_PREF_SIZE
		}
		segmentListVBox.maxHeightProperty bind segmentVBoxMaxHeightBinding
	}

	@FXML private def openTraceOutputFolder(buttonEvent: ActionEvent) {
		val folder = new File(model.traceFile.get).getParentFile
		Desktop.getDesktop open folder
	}

	@FXML private def onNewTrace(buttonEvent: ActionEvent) {
		newTraceEventSource.fire()
	}

	def packageSegmentSplitPosition = if (segmentList.isVisible) Some(packageSegmentSplitPane.getDividerPositions()(0)) else None

	def setPackageSegmentSplitPosition(position: Double) = packageSegmentSplitPane.setDividerPosition(0, position)
}