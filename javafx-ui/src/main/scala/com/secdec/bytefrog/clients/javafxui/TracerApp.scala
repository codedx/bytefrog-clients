package com.secdec.bytefrog.clients.javafxui

import java.io.File
import java.util.Calendar

import com.secdec.bytefrog.clients.common.config._
import com.secdec.bytefrog.hq.config._
import com.secdec.bytefrog.clients.javafxui.trace.TraceManager
import com.secdec.bytefrog.clients.javafxui.util.JavaFXUtilities
import com.secdec.bytefrog.clients.javafxui.util.UserOptions
import com.secdec.bytefrog.clients.javafxui.util.WindowClass
import com.secdec.bytefrog.clients.javafxui.util.WindowState
import com.secdec.bytefrog.clients.javafxui.util.WindowStatePersistance
import com.secdec.bytefrog.clients.javafxui.views.ConfigurationView
import com.secdec.bytefrog.clients.javafxui.views.TraceCompleteView
import com.secdec.bytefrog.clients.javafxui.views.TraceView

import javafx.application.Application
import javafx.scene.image.Image
import javafx.stage.Stage
import javafx.stage.WindowEvent

import reactive.Observing

/** Main JavaFX launcher
  * @author robertf
  */
object TracerApp {
	def main(args: Array[String]) {
		Application.launch(classOf[TracerApp], args: _*)
	}
}

/** The tracer app itself. This ties together the UI views/data models with an underlying
  * trace management wrapper.
  *
  * @author robertf
  */
class TracerApp extends Application with JavaFXUtilities with Observing {
	private var stage: Stage = _

	private var configView: ConfigurationView = _
	private var traceView: TraceView = _
	private var traceCompleteView: TraceCompleteView = _

	private val traceManager = new TraceManager(() => (currentTraceSettings, agentConfig, HQConfiguration(), MonitorConfiguration()))

	def currentTraceSettings = TraceSettings(
		exclusions = (".*" :: Nil),
		inclusions = configView.model.packageIncludes.get)

	def agentConfig = AgentConfiguration(bufferMemoryBudget = UserOptions.agentMemoryBudget.get)

	def start(stage: Stage) {
		this.stage = stage

		stage.setTitle("Code Pulse")

		// set the icon on the stage
		stage.getIcons add new Image(getClass.getResource("app_icon.png").toExternalForm)

		// make sure to shut down the trace manager and persist window state when the window is closed
		stage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, eventHandler { event: WindowEvent =>
			saveWindowState
			traceManager.shutdown
		})

		configView = new ConfigurationView
		traceView = new TraceView
		traceCompleteView = new TraceCompleteView

		// tie configuration view to the user options
		configView.model.agentPort bindBidirectional UserOptions.listenPort
		configView.model.agentMemoryBudget bindBidirectional UserOptions.agentMemoryBudget
		configView.model.traceSaveFolder bindBidirectional UserOptions.savePath

		// resize on demand (when configuration view is the current view)
		for (_ <- configView.controller.requestResizeEvent.filter(_ => activeView == View.Configuration)) {
			// using jfxRun so this gets called later, once whatever caused the resize request is complete
			jfxRun(stage.sizeToScene)
		}

		// reconfigure agent when necessary
		val reconfigurationListener = invalidationListener {
			traceManager.reconfigureAgent(currentTraceSettings, agentConfig)
		}

		configView.model.packageIncludes addListener reconfigurationListener
		UserOptions.agentMemoryBudget addListener reconfigurationListener

		// bind listen port for trace manager
		traceManager.listenPort bindBidirectional UserOptions.listenPort

		// bind trace manager feedback to configuration view
		configView.model.agentConnected bind traceManager.agentConnected
		configView.model.listening bind traceManager.listening
		configView.model.statusMessage bind traceManager.statusMessage

		// bind trace information for trace/trace complete views
		traceView.model.traceName bind configView.model.traceName
		traceView.model.traceSize bind traceManager.eventCountDisplay
		traceView.model.dataRate bind traceManager.dataRateDisplay
		traceView.model.dataRateSeries set traceManager.dataRateMonitor.dataSeries
		traceCompleteView.model.traceName bind configView.model.traceName
		traceCompleteView.model.traceTime bind traceView.model.traceTime
		traceCompleteView.model.traceSize bind traceManager.eventCountDisplay

		// bind trace package information
		traceView.model.tracedPackages bind traceManager.tracedPackages
		traceView.model.ignoredPackages bind traceManager.ignoredPackages
		traceView.model.errorPackages bind traceManager.errorPackages
		traceCompleteView.model.tracedPackages bind traceManager.tracedPackages
		traceCompleteView.model.ignoredPackages bind traceManager.ignoredPackages
		traceCompleteView.model.errorPackages bind traceManager.errorPackages
		traceCompleteView.model.packageSort bindBidirectional traceView.model.packageSort

		// bind trace segment information
		traceView.model.currentSegment bind traceManager.currentTraceSegment
		traceView.model.segments bind traceManager.traceSegmentEntries
		traceCompleteView.model.segments bind traceManager.traceSegmentEntries

		// wire up trace segment events
		traceView.controller.startSegmentEvent += { label =>
			traceManager.startSegment(label.trim match {
				case "" => "Untitled Segment"
				case label => label
			})
		}
		traceView.controller.endSegmentEvent += ((_: Unit) => traceManager.endSegment)

		// wire up trace control events
		traceView.model.tracingControllable bind traceManager.traceControllable
		traceView.controller.applicationExecutionChangeEvent += ((isRunning: Boolean) => traceManager.setAgentPaused(!isRunning))
		traceView.controller.traceCollectionChangeEvent += ((isTracing: Boolean) => traceManager.setTracingSuspended(!isTracing))

		// wire up trace lifetime events
		configView.controller.startTraceEvent += onStartTrace
		traceView.controller.endTraceEvent += ((_: Unit) => onEndTrace)
		traceView.controller.segmentRenameEvent += { case (id, newLabel) => traceManager.renameSegment(id, newLabel) }
		traceManager.traceShuttingDownEvent += ((_: Unit) => traceView.controller.stopTimer)
		traceManager.traceEndEvent += ((_: Unit) => onTraceEnded)
		traceCompleteView.controller.newTraceEvent += ((_: Unit) => startNewTrace)

		startNewTrace

		stage.show
		stage.sizeToScene
	}

	private trait View
	private object View {
		case object Configuration extends View
		case object ActiveTrace extends View
		case object FinishedTrace extends View
	}

	private var activeView: View = _

	private def setView(newView: View) {
		if (activeView != null) {
			activeView match {
				case View.Configuration =>
					// when leaving the configuration view, save the options
					UserOptions.save

				case View.ActiveTrace =>
					// when leaving the active trace view, copy the splitpane's divider position to finished trace
					traceCompleteView.controller setPackageSegmentSplitPosition traceView.controller.packageSegmentSplitPosition

				case View.FinishedTrace =>
					// when leaving the finished trace view, copy the divider position if necessary
					traceCompleteView.controller.packageSegmentSplitPosition match {
						case Some(pos) => traceView.controller setPackageSegmentSplitPosition pos
						case None =>
					}

				case _ =>
			}

			saveWindowState
		}

		newView match {
			case View.Configuration =>
				stage setMinWidth 0
				stage setMinHeight 0

				stage setScene configView.scene
				stage setResizable false

			case View.ActiveTrace =>
				val (minWidth, minHeight) = calculateMinimumSize(traceView.scene.getRoot)

				stage setMinWidth minWidth
				stage setMinHeight minHeight

				stage setScene traceView.scene
				stage setResizable true

			case View.FinishedTrace =>
				val (minWidth, minHeight) = calculateMinimumSize(traceCompleteView.scene.getRoot)

				stage setMinWidth minWidth
				stage setMinHeight minHeight

				stage setScene traceCompleteView.scene
				stage setResizable true
		}

		activeView = newView
		stage.sizeToScene

		loadWindowState
	}

	private def loadWindowState() = {
		val wClass = activeView match {
			case View.Configuration => WindowClass.Configuration
			case View.ActiveTrace | View.FinishedTrace => WindowClass.Trace
		}

		WindowStatePersistance.loadState(wClass) match {
			case Some(state) => wClass.applyToWindow(stage, state)
			case None =>
		}
	}

	private def saveWindowState() {
		val wClass = activeView match {
			case View.Configuration => WindowClass.Configuration
			case View.ActiveTrace | View.FinishedTrace => WindowClass.Trace
		}

		WindowStatePersistance.saveState(wClass, WindowState.fromWindow(stage))
	}

	private def startNewTrace() {
		if (!traceManager.isRunning) {
			traceManager.listenForTrace
			setView(View.Configuration)
		}
	}

	private def onStartTrace(nameAndfiles: (String, File, File)) {
		val (traceName, dumpFile, postProcessedFile) = nameAndfiles
		// tell trace manager to start a trace and transition to the tracing view when a trace start is requested.
		if (!traceManager.isRunning) {
			val dumpOpt = if (java.lang.Boolean.getBoolean(Constants.EnableDumpProperty)) Some(dumpFile) else None

			traceCompleteView.model.traceFile set postProcessedFile.getCanonicalPath
			traceManager.start(TraceOutputSettings(traceName, dumpOpt, postProcessedFile))

			// keep track of trace start time
			traceView.model.traceStartTime = Calendar.getInstance.getTimeInMillis
			traceView.controller.startTimer

			traceView.controller.synchronizeTraceControls(!traceManager.isAgentPaused, !traceManager.isAgentSuspended)

			setView(View.ActiveTrace)
		}
	}

	private def onEndTrace() {
		// tell trace manager to stop the current trace when requested
		traceManager.stop
	}

	private def onTraceEnded() {
		// once we're at the finished trace view, we need to fire off post-processing
		traceView.controller.stopTimer

		// this happens on another thread, so we need to make sure the scene is set on a JavaFX thread
		jfxRun {
			// switch to trace completed view
			setView(View.FinishedTrace)
		}
	}
}