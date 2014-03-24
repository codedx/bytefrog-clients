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

package com.secdec.bytefrog.clients.javafxui.trace

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

import com.secdec.bytefrog.hq.data.TraceSegmentEvent
import com.secdec.bytefrog.hq.agent.AgentState
import com.secdec.bytefrog.hq.config.AgentConfiguration
import com.secdec.bytefrog.hq.config.HQConfiguration
import com.secdec.bytefrog.hq.config.MonitorConfiguration
import com.secdec.bytefrog.clients.common.config.TraceOutputSettings
import com.secdec.bytefrog.hq.config.TraceSettings
import com.secdec.bytefrog.hq.connect.SocketServer
import com.secdec.bytefrog.hq.monitor.DataRouterMonitorData
import com.secdec.bytefrog.hq.monitor.FileSystemMonitorData
import com.secdec.bytefrog.clients.common.trace.FileTraceDataManager
import com.secdec.bytefrog.hq.trace.Trace
import com.secdec.bytefrog.clients.javafxui.util.JavaFXUtilities
import com.secdec.bytefrog.clients.javafxui.util.SizeFormatter

import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.ReadOnlyBooleanWrapper
import javafx.beans.property.ReadOnlyDoubleProperty
import javafx.beans.property.ReadOnlyDoubleWrapper
import javafx.beans.property.ReadOnlyListWrapper
import javafx.beans.property.ReadOnlyStringProperty
import javafx.beans.property.ReadOnlyStringWrapper
import javafx.beans.property.SimpleIntegerProperty
import javafx.collections.FXCollections

import reactive.EventSource
import reactive.EventStream
import reactive.Observing

/** Manages a single trace at a time for the HQ UI.
  * @author robertf
  */
class TraceManager(configurationProvider: () => (TraceSettings, AgentConfiguration, HQConfiguration, MonitorConfiguration)) extends JavaFXUtilities with Observing {
	protected val StatusAwaitingAgent = "Start target application with agent configuration string to enable start trace"
	protected val StatusAgentConnected = "Target application is ready to go, click 'start trace' when ready"
	protected val StatusAgentDisconnected = "Target application lost. Please restart with agent configuration string to enable tracing again"

	/** Read-only property telling whether or not an agent is connected */
	def agentConnected: ReadOnlyBooleanProperty = agentConnectedProperty.getReadOnlyProperty
	protected val agentConnectedProperty = new ReadOnlyBooleanWrapper(false)

	/** Read-only property telling whether or not we are listening for connections */
	def listening: ReadOnlyBooleanProperty = listeningProperty.getReadOnlyProperty
	protected val listeningProperty = new ReadOnlyBooleanWrapper(false)

	/** Read-only property telling whether or not a trace is controllable (can pause/suspend) */
	def traceControllable = traceControllableProperty.getReadOnlyProperty
	protected val traceControllableProperty = new ReadOnlyBooleanWrapper(false)

	/** Read-only property containing the current trace status */
	def statusMessage: ReadOnlyStringProperty = statusMessageProperty.getReadOnlyProperty
	protected val statusMessageProperty = new ReadOnlyStringWrapper(null)

	/** Read-only property containing the current trace size, as a human readable string */
	def eventCountDisplay: ReadOnlyStringProperty = eventCountDisplayProperty.getReadOnlyProperty
	protected val eventCountDisplayProperty = new ReadOnlyStringWrapper(null)

	/** Read-only property containing the current free disk space, as a human readable string */
	def diskSpaceFreeDisplay: ReadOnlyStringProperty = diskSpaceFreeDisplayProperty.getReadOnlyProperty
	protected val diskSpaceFreeDisplayProperty = new ReadOnlyStringWrapper(null)

	/** Read-only property containing the percentage of free disk space, as a double between 0 and 1 */
	def diskPercentFree: ReadOnlyDoubleProperty = diskPercentFreeProperty.getReadOnlyProperty
	protected val diskPercentFreeProperty = new ReadOnlyDoubleWrapper(0)

	/** Read-only property containing the current data rate, as a human readable string */
	def dataRateDisplay: ReadOnlyStringProperty = dataRateDisplayProperty.getReadOnlyProperty
	protected val dataRateDisplayProperty = new ReadOnlyStringWrapper(null)

	// 30 seconds on chart * roughly 2 updates per second = 60 data points to keep
	val dataRateMonitor = new DataRateMonitor(60)

	/** Read-only observable list of traced packages */
	def tracedPackages = tracedPackagesList.getReadOnlyProperty
	private val tracedPackagesList = new ReadOnlyListWrapper[String](FXCollections.observableArrayList[String])

	/** Read-only observable list of ignored packages */
	def ignoredPackages = ignoredPackagesList.getReadOnlyProperty
	private val ignoredPackagesList = new ReadOnlyListWrapper[String](FXCollections.observableArrayList[String])

	/** Read-only observable list of errored packages */
	def errorPackages = errorPackagesList.getReadOnlyProperty
	private val errorPackagesList = new ReadOnlyListWrapper[String](FXCollections.observableArrayList[String])

	/** Read-only property containing the name of the current segment, or null if there is none */
	def currentTraceSegment = segmentManager.currentSegment

	/** Read-only observable list of trace segment entries */
	def traceSegmentEntries = segmentManager.entryList

	/** Event stream for when a trace begins shutting down */
	def traceShuttingDownEvent: EventStream[Unit] = traceShuttingDownEventSource
	protected val traceShuttingDownEventSource = new EventSource[Unit]

	/** Event stream for when a trace ends */
	def traceEndEvent: EventStream[Unit] = traceEndEventSource
	protected val traceEndEventSource = new EventSource[Unit]

	/** The SocketServer we listen for connections on, when one is needed */
	implicit val server = SocketServer.unconfigured
	server.start

	/** Property corresponding with the current listen port */
	val listenPort = new SimpleIntegerProperty

	// when listenPort changes, update the server
	listenPort addListener changeListener { newValue: Number =>
		if (server.isAlive) {
			try {
				server.setPort(newValue.intValue)
				reportStatus(StatusAwaitingAgent)

				if (isListening)
					listeningProperty set (newValue.intValue > 0)
			} catch {
				case _: Exception =>
					reportError(s"cannot listen on port ${newValue}", "try changing the listen port")

					if (isListening)
						listeningProperty set false
			}
		}
	}

	private val traceStream = new EventSource[Trace]

	private var currentTrace: Option[Trace] = None
	private val segmentManager = new TraceSegmentManager
	private var isListening = false
	private var isComplete = false

	private def withTrace[T](body: Trace => T): Option[T] = currentTrace.map(body)

	// setup new traces
	for (trace <- traceStream) jfxRun {
		setupTrace(trace)
	}

	// wire up monitor data for traces
	for (
		data <- for {
			trace <- traceStream
			data <- trace.monitorData
		} yield data
	) jfxRun {
		data match {
			case DataRouterMonitorData(events, eventRate, timestamp) =>
				eventCountDisplayProperty set f"%,d events".format(events)
				dataRateDisplayProperty set f"%,d events/s".format(eventRate)
				dataRateMonitor.reportDataRate(eventRate, timestamp)

			case FileSystemMonitorData(totalSpace, freeSpace) =>
				diskSpaceFreeDisplayProperty set SizeFormatter.format(freeSpace)
				diskPercentFreeProperty set 1 - freeSpace.toDouble / totalSpace

			case _ =>
		}
	}

	// listen to trace states
	for (
		state <- for {
			trace <- traceStream
			state <- trace.agentStateChange
		} yield state
	) {
		state match {
			case AgentState.Initializing | AgentState.Tracing | AgentState.Paused | AgentState.Suspended =>
				if (!traceControllableProperty.get)
					jfxRun(traceControllableProperty set true)

			case AgentState.ShuttingDown =>
				jfxRun(traceControllableProperty set false)
				traceShuttingDownEventSource.fire()

			case _ =>
		}
	}

	// wire up trace errors
	// TODO: why is fatalTraceErrors giving us conditional errors on connections closing during normal shutdown?
	for {
		trace <- traceStream
		error <- trace.errorController.fatalTraceErrors
	} {
		// the trace is as good as dead now... shut it down
		//trace.kill - this should be done by trace itself now...

		jfxRun {
			if (!trace.isStarted) {
				// if trace hasn't been started yet, start listening again
				listenForTrace
			}

			agentConnectedProperty set false
			reportStatus(StatusAgentDisconnected)
		}
	}

	// listen to class transformation events on traces
	for (
		clazz <- for {
			trace <- traceStream
			clazz <- trace.classTransformEvents
		} yield clazz
	) jfxRun {
		val pkg = clazz.split('/').dropRight(1).mkString(".")

		if (!tracedPackages.contains(pkg))
			tracedPackages add pkg
	}
	for (
		clazz <- for {
			trace <- traceStream
			clazz <- trace.classIgnoreEvents
		} yield clazz
	) jfxRun {
		val pkg = clazz.split('/').dropRight(1).mkString(".")

		if (!ignoredPackages.contains(pkg))
			ignoredPackages add pkg
	}
	for (
		clazz <- for {
			trace <- traceStream
			clazz <- trace.classTransformFailEvents
		} yield clazz
	) jfxRun {
		val pkg = clazz.split('/').dropRight(1).mkString(".")

		if (!errorPackages.contains(pkg))
			errorPackages add pkg
	}

	// listen in on trace segments
	for {
		trace <- traceStream
		event <- trace.segmentEvents
	} jfxRun {
		event match {
			case TraceSegmentEvent.SegmentOpened(id, label, startTime, _) =>
				segmentManager.beginSegment(id, startTime - trace.startTime, label)

			case TraceSegmentEvent.SegmentClosed(id, endTime) =>
				segmentManager.endSegment(id, endTime - trace.startTime)

			case TraceSegmentEvent.SegmentRenamed(id, newLabel) =>
				segmentManager.renameSegment(id, newLabel)

			case _: TraceSegmentEvent.AnonSegmentOpened => // no op
			case _: TraceSegmentEvent.SegmentDeleted => // no op
		}
	}

	/** Begin listening for a new trace */
	def listenForTrace() {
		if (!isListening) {
			// setup listening for a trace
			val traceFuture = Trace.getTrace(configurationProvider)
			isListening = true

			currentTrace = None
			reportStatus(StatusAwaitingAgent)
			listeningProperty set true

			// report failure messages
			for {
				failure <- traceFuture.failed
			} jfxRun {
				isListening = false
				failure.printStackTrace
				reportError(failure.getMessage, s"unknown failure, see stack trace")
				listeningProperty set false
				server.shutdown
			}

			// when we get the trace, set it
			for {
				trace <- traceFuture
			} yield {
				isListening = false
				traceStream fire trace
			}
		}
	}

	private def setupTrace(trace: Trace) {
		// now that agent is connected, set up the trace
		currentTrace = Some(trace)
		isComplete = false
		agentConnectedProperty set true
		listeningProperty set false

		try {
			// clear out any display properties from a prior run
			eventCountDisplayProperty set null
			diskSpaceFreeDisplayProperty set "<??>"
			diskPercentFreeProperty set 0
			dataRateDisplayProperty set null
			dataRateMonitor.clear
		} catch {
			case e: Throwable => e.printStackTrace()
		}

		// clear out package lists and segment information from prior trace
		tracedPackages.clear
		ignoredPackages.clear
		errorPackages.clear
		segmentManager.clear

		// update status
		reportStatus(StatusAgentConnected)

		// synchronize internal state
		synchronizeState

		// listen for trace completion
		for {
			reason <- trace.completion
		} yield {
			println(s"Trace over - $reason")

			isComplete = true
			jfxRun { agentConnectedProperty set false }

			if (trace.isStarted) {
				// only fire the trace end event if the trace was started to begin with
				traceEndEventSource.fire()
			}
		}

		//TODO: handle trace failure? will trace.completion ever fail?
	}

	/** Shut down the listener */
	def shutdown() {
		Try(stop)
		server.shutdown
		listeningProperty set false
	}

	/** Whether or not a trace is running */
	def isRunning = !isComplete && (currentTrace match {
		case Some(trace) => trace.isStarted
		case None => false
	})

	/** Reconfigure agent, if connected, with new configurations */
	def reconfigureAgent(traceSettings: TraceSettings, agentConfig: AgentConfiguration) {
		withTrace { trace =>
			if (!trace.isStarted)
				trace.reconfigureAgent(traceSettings, agentConfig)
		}
	}

	/** Start a new segment with the given label */
	def startSegment(label: String) {
		withTrace { _.openSegment(label) }
	}

	def renameSegment(id: Int, newLabel: String) {
		withTrace { _.renameSegment(id, newLabel) }
	}

	/** End the current segment */
	def endSegment() {
		withTrace { _.closeSegment }
	}

	/** Start a new trace with the provided output settings */
	def start(outputSettings: TraceOutputSettings) = currentTrace match {
		case Some(trace) =>
			val dataManager = new FileTraceDataManager(outputSettings)
			trace.start(dataManager)
		case None => reportError("Cannot start trace", "...")
	}

	/** The current agent state, if any */
	def agentState = currentTrace match {
		case Some(trace) => trace.agentState
		case None => throw new IllegalStateException
	}

	/** Synchronize internal state with actual agent state */
	private def synchronizeState {
		isPaused = false
		isSuspended = false

		agentState match {
			case AgentState.Paused => isPaused = true
			case AgentState.Suspended => isSuspended = true
			case _ =>
		}
	}

	def isAgentPaused = isPaused
	def isAgentSuspended = isSuspended

	private var isPaused = false
	def setAgentPaused(isPaused: Boolean) = {
		this.isPaused = isPaused
		sendAgentCommand
	}

	private var isSuspended = false
	def setTracingSuspended(isSuspended: Boolean) {
		this.isSuspended = isSuspended
		sendAgentCommand
	}

	/** Make sure Agent has been told what operating mode to be in */
	private def sendAgentCommand = currentTrace match {
		case Some(trace) =>
			if (traceControllable.get)
				if (isPaused)
					trace.pauseApplication
				else if (isSuspended)
					trace.suspendTracing
				else
					trace.resume

		case None => throw new IllegalStateException
	}

	/** Stop the current trace */
	def stop() {
		withTrace { _.stop }
	}

	protected def reportError(error: String, resolution: String) = statusMessageProperty set s"Error: ${error}, ${resolution}"

	protected def reportStatus(status: String) = statusMessageProperty set status
}