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

import java.net.URL
import java.util.Calendar
import java.util.ResourceBundle

import com.secdec.bytefrog.clients.javafxui.controls.SegmentControl
import com.secdec.bytefrog.clients.javafxui.controls.SegmentGraphics
import com.secdec.bytefrog.clients.javafxui.controls.TracePackageDisplayControl
import com.secdec.bytefrog.clients.javafxui.trace.TraceSegmentEntryType
import com.secdec.bytefrog.clients.javafxui.util.JavaFXUtilities
import com.secdec.bytefrog.clients.javafxui.util.TimeFormatter

import javafx.animation.Animation
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.beans.binding.Bindings
import javafx.beans.binding.BooleanBinding
import javafx.beans.binding.StringBinding
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.chart.AreaChart
import javafx.scene.chart.XYChart
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.SplitPane
import javafx.scene.control.TextField
import javafx.scene.control.Toggle
import javafx.scene.control.ToggleButton
import javafx.scene.control.ToggleGroup
import javafx.util.Duration

import reactive.EventSource
import reactive.EventStream

/** JavaFX controller class for the active trace view
  * @author robertf
  */
class TraceController extends Initializable with JavaFXUtilities {
	// these are protected because scala does weird things when they're private
	@FXML protected var traceNameLabel: Label = _
	@FXML protected var traceTimeLabel: Label = _
	@FXML protected var endTraceButton: Button = _
	@FXML protected var applicationExecution: ToggleGroup = _
	@FXML protected var applicationRunningToggle: ToggleButton = _
	@FXML protected var applicationPausedToggle: ToggleButton = _
	@FXML protected var collectTraces: ToggleGroup = _
	@FXML protected var tracingActiveToggle: ToggleButton = _
	@FXML protected var tracingSuspendedToggle: ToggleButton = _
	@FXML protected var traceSizeLabel: Label = _
	@FXML protected var dataRateLabel: Label = _
	@FXML protected var dataRateChart: AreaChart[Number, Number] = _
	@FXML protected var packageSegmentSplitPane: SplitPane = _
	@FXML protected var tracePackagesDisplay: TracePackageDisplayControl = _
	@FXML protected var openSegmentButton: Button = _
	@FXML protected var segmentNameTextField: TextField = _
	@FXML protected var segmentList: SegmentControl = _
	@FXML protected var closeSegmentButton: Button = _

	val model = new TraceModel

	/** Event fired when the user requests that the current trace end */
	def endTraceEvent: EventStream[Unit] = endTraceEventSource
	private val endTraceEventSource = new EventSource[Unit]

	/** Event fired when the user requests that a new segment be started */
	def startSegmentEvent: EventStream[String] = startSegmentEventSource
	private val startSegmentEventSource = new EventSource[String]

	/** Event fired when the user requests that the current segment be closed */
	def endSegmentEvent: EventStream[Unit] = endSegmentEventSource
	private val endSegmentEventSource = new EventSource[Unit]

	/** Event fired when the user requests a change to application execution */
	def applicationExecutionChangeEvent: EventStream[Boolean] = applicationExecutionChangeEventSource
	private val applicationExecutionChangeEventSource = new EventSource[Boolean]

	/** Event fired when the user requests a change to trace collection */
	def traceCollectionChangeEvent: EventStream[Boolean] = traceCollectionChangeEventSource
	private val traceCollectionChangeEventSource = new EventSource[Boolean]

	/** Event fired when the user requests a segment rename */
	def segmentRenameEvent: EventStream[(Int, String)] = segmentList.segmentRenameEvent

	def initialize(url: URL, resources: ResourceBundle) {
		traceNameLabel.textProperty bind model.traceName
		traceSizeLabel.textProperty bind model.traceSize
		dataRateLabel.textProperty bind model.dataRate

		{
			val dataRateSeries = model.dataRateSeries.get
			if (dataRateSeries != null)
				dataRateChart.getData setAll dataRateSeries
		}

		model.dataRateSeries addListener changeListener { newValue: XYChart.Series[Number, Number] =>
			dataRateChart.getData setAll newValue
		}

		// custom binding that formats the time value (in milliseconds)
		val timerBinding = new StringBinding {
			super.bind(model.traceTime)

			def computeValue(): String = TimeFormatter.format(model.traceTime.get)
		}
		traceTimeLabel.textProperty bind timerBinding

		// only enable trace controls when the trace is controllable
		val traceControlDisabled = Bindings.not(model.tracingControllable)
		endTraceButton.disableProperty bind traceControlDisabled
		applicationRunningToggle.disableProperty bind traceControlDisabled
		applicationPausedToggle.disableProperty bind traceControlDisabled
		tracingActiveToggle.disableProperty bind traceControlDisabled
		tracingSuspendedToggle.disableProperty bind traceControlDisabled

		// listen in on trace controls
		applicationExecution.selectedToggleProperty addListener changeListener { (oldValue: Toggle, newValue: Toggle) =>
			if (newValue == null) {
				// don't allow un-selection
				applicationExecution selectToggle oldValue
			} else if (newValue == applicationRunningToggle) {
				applicationExecutionChangeEventSource.fire(true)
			} else if (newValue == applicationPausedToggle) {
				applicationExecutionChangeEventSource.fire(false)
			}
		}

		collectTraces.selectedToggleProperty addListener changeListener { (oldValue: Toggle, newValue: Toggle) =>
			if (newValue == null) {
				// don't allow un-selection
				collectTraces selectToggle oldValue
			} else if (newValue == tracingActiveToggle) {
				traceCollectionChangeEventSource.fire(true)
			} else if (newValue == tracingSuspendedToggle) {
				traceCollectionChangeEventSource.fire(false)
			}
		}

		model.packageSort bindBidirectional tracePackagesDisplay.sort
		tracePackagesDisplay.tracedPackages bind model.tracedPackages
		tracePackagesDisplay.ignoredPackages bind model.ignoredPackages
		tracePackagesDisplay.errorPackages bind model.errorPackages

		// set graphics on the open and close segment buttons
		openSegmentButton setGraphic SegmentGraphics.segmentEntryGraphic(TraceSegmentEntryType.SegmentBegin)
		closeSegmentButton setGraphic SegmentGraphics.segmentEntryGraphic(TraceSegmentEntryType.SegmentEnd)

		// only enable 'open new segment' button when trace is controllable
		openSegmentButton.disableProperty bind traceControlDisabled

		// only enable 'close segment' button when there is an active segment (and trace is controllable)
		val closeSegmentDisableBinding = new BooleanBinding {
			super.bind(traceControlDisabled, model.currentSegment)

			def computeValue() = traceControlDisabled.get || model.currentSegment.get == null
		}
		closeSegmentButton.disableProperty bind closeSegmentDisableBinding

		// binding for text on the close segment button
		val closeSegmentTextBinding = new StringBinding {
			super.bind(model.currentSegment)

			def computeValue() = {
				val currentSegment = model.currentSegment.get

				if (currentSegment != null && !currentSegment.isEmpty)
					s"Close '${model.currentSegment.get}' segment"
				else
					"Close segment"
			}
		}
		closeSegmentButton.textProperty bind closeSegmentTextBinding

		// bind segment entries
		segmentList.segmentEntries bind model.segments
	}

	var timerTimeline: Option[Timeline] = None

	/** Begin actively updating the timer display */
	def startTimer() {
		timerTimeline match {
			case None => {
				// use JavaFX animation to update the trace time every 0.1s
				val timeline = new Timeline(
					new KeyFrame(Duration.seconds(0), eventHandler { actionEvent: ActionEvent =>
						model.traceTime set (Calendar.getInstance.getTimeInMillis - model.traceStartTime)
					}),
					new KeyFrame(Duration.seconds(0.1)))
				timerTimeline = Some(timeline)
				timeline setCycleCount Animation.INDEFINITE
				timeline.play()
			}

			case _ =>
		}
	}

	/** Cease actively updating the timer display */
	def stopTimer() {
		timerTimeline match {
			case Some(timeline) => {
				timeline.stop
				timerTimeline = None
			}

			case _ =>
		}
	}

	/** Synchronize the trace controls with reality */
	def synchronizeTraceControls(isExecuting: Boolean, isTracing: Boolean) {
		applicationExecution selectToggle (if (isExecuting) applicationRunningToggle else applicationPausedToggle)
		collectTraces selectToggle (if (isTracing) tracingActiveToggle else tracingSuspendedToggle)
	}

	def packageSegmentSplitPosition = packageSegmentSplitPane.getDividerPositions()(0)

	def setPackageSegmentSplitPosition(position: Double) = packageSegmentSplitPane.setDividerPosition(0, position)

	@FXML private def onEndTrace(buttonEvent: ActionEvent) {
		endTraceEventSource.fire()
	}

	@FXML private def onOpenNewSegment(buttonEvent: ActionEvent) {
		startSegmentEventSource.fire(segmentNameTextField.getText)
		segmentNameTextField.clear
	}

	@FXML private def onCloseSegment(buttonEvent: ActionEvent) {
		endSegmentEventSource.fire()
	}
}