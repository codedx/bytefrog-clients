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

import java.io.File
import java.net.InetAddress
import java.net.URL
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.ResourceBundle

import scala.collection.JavaConversions.seqAsJavaList

import com.secdec.bytefrog.clients.javafxui.util.AgentConfiguration
import com.secdec.bytefrog.clients.javafxui.util.InputValidator
import com.secdec.bytefrog.clients.javafxui.util.JavaFXUtilities
import com.secdec.bytefrog.clients.javafxui.util.PackageListParser
import com.secdec.bytefrog.hq.util.NumberUnits.intToFileSizes

import javafx.beans.binding.Bindings
import javafx.beans.binding.BooleanBinding
import javafx.beans.binding.StringBinding
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.SimpleStringProperty
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.control.TitledPane
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.layout.HBox
import javafx.stage.DirectoryChooser
import javafx.util.converter.NumberStringConverter

import reactive.EventSource
import reactive.EventStream

/** JavaFX controller class for configuration view
  * @author robertf
  */
class ConfigurationController extends Initializable with JavaFXUtilities {
	// these are protected because scala does weird things when they're private
	@FXML protected var traceNameField: TextField = _
	@FXML protected var agentConfigurationField: TextField = _
	@FXML protected var includePackagesArea: TextArea = _
	@FXML protected var advancedOptionsPane: TitledPane = _
	@FXML protected var agentPortField: TextField = _
	@FXML protected var agentListeningLabel: Label = _
	@FXML protected var memoryBudgetField: TextField = _
	@FXML protected var memoryBudgetUnitBox: ComboBox[String] = _
	@FXML protected var traceSaveFolder: TextField = _
	@FXML protected var statusBar: HBox = _
	@FXML protected var startTraceButton: Button = _
	@FXML protected var statusLabel: Label = _

	val model = new ConfigurationModel

	/** binding for agent configuration string. Also used by `copyAgentConfigurationToClipboard` */
	private val agentConfigurationStringBinding = new StringBinding {
		val thisHost = InetAddress.getLocalHost.getHostAddress

		super.bind(model.agentPort)

		def computeValue() = AgentConfiguration(thisHost, model.agentPort.get)
	}

	/** Event fired when the user requests a trace start */
	def startTraceEvent: EventStream[(String, File, File)] = startTraceEventSource
	private val startTraceEventSource = new EventSource[(String, File, File)]

	/** Event fired when this view is requesting a re-computation of its size */
	def requestResizeEvent: EventStream[Unit] = requestResizeEventSource
	private val requestResizeEventSource = new EventSource[Unit]

	def initialize(url: URL, resources: ResourceBundle) {
		// basic info
		traceNameField.textProperty bindBidirectional model.traceName
		agentPortField.textProperty.bindBidirectional(model.agentPort, new NumberStringConverter(new DecimalFormat("0")))
		traceSaveFolder.textProperty bindBidirectional model.traceSaveFolder

		// setup memory budget
		memoryBudgetUnitBox.getItems.setAll(memoryBudgetConverter.Units)
		memoryBudgetConverter.memoryBudgetBytes bindBidirectional model.agentMemoryBudget
		memoryBudgetField.textProperty.bindBidirectional(memoryBudgetConverter.memoryBudgetValue, new NumberStringConverter(new DecimalFormat("0")))
		memoryBudgetUnitBox.valueProperty bindBidirectional memoryBudgetConverter.memoryBudgetUnit

		// agent configuration display
		agentConfigurationField.textProperty bind agentConfigurationStringBinding

		// whether or not we're listening
		agentListeningLabel.visibleProperty bind Bindings.and(model.listening, Bindings.not(model.agentConnected))

		// user can only change port *before* agent connects
		agentPortField.disableProperty bind model.agentConnected

		// set up package parsing
		val packageParser = new PackageListParser(includePackagesArea)
		model.packageIncludes bind packageParser.packageList

		// set up validation
		val traceNameValid = InputValidator.validFileName(traceNameField).valid
		val packageIncludesValid = packageParser.valid
		val agentPortFieldValid = InputValidator.port(agentPortField).valid
		val memoryBudgetValid = InputValidator.numeric(memoryBudgetField).valid
		val tracePathValid = InputValidator.folder(traceSaveFolder).valid

		// only enable start trace button when all validation passes and we're listening
		val startTraceEnabled = new BooleanBinding {
			super.bind(model.agentConnected, traceNameValid, packageIncludesValid, agentPortFieldValid, memoryBudgetValid, tracePathValid)

			def computeValue(): Boolean = {
				model.agentConnected.get && traceNameValid.get && packageIncludesValid.get && agentPortFieldValid.get && memoryBudgetValid.get && tracePathValid.get
			}
		}

		def updateStatusBarClass(isError: Boolean) {
			if (isError)
				statusBar.getStyleClass add "error"
			else
				statusBar.getStyleClass remove "error"
		}

		startTraceEnabled addListener changeListener { enabled: java.lang.Boolean => updateStatusBarClass(!enabled) }
		updateStatusBarClass(!startTraceEnabled.get)

		startTraceButton.disableProperty bind Bindings.not(startTraceEnabled)
		statusLabel.textProperty bind model.statusMessage

		advancedOptionsPane.expandedProperty addListener invalidationListener {
			requestResizeEventSource.fire()
		}
	}

	@FXML private def onStartTrace(buttonEvent: ActionEvent) {
		val traceName = model.traceName.get
		val filenameBase = s"$traceName ${new SimpleDateFormat("yyyy-MM-dd hh.mm").format(new Date)}"
		val saveFolder = new File(model.traceSaveFolder.get)
		val filename = new File(saveFolder, s"$filenameBase.trace")
		val postFilename = new File(saveFolder, s"$filenameBase.cptrace")
		startTraceEventSource.fire(traceName, filename, postFilename)
	}

	@FXML private def browseForSave(buttonEvent: ActionEvent) {
		// show a directory chooser for the user to pick a folder
		val chooser = new DirectoryChooser
		chooser setTitle "Save traces to"

		val initialDir = new File(traceSaveFolder.getText)
		if (initialDir.exists)
			chooser setInitialDirectory initialDir

		val dir = chooser showDialog buttonEvent.getSource.asInstanceOf[Button].getScene.getWindow

		if (dir != null)
			traceSaveFolder.setText(dir.getCanonicalPath)
	}

	@FXML private def copyAgentConfigurationToClipboard(buttonEvent: ActionEvent) {
		val content = new ClipboardContent
		content putString agentConfigurationStringBinding.get
		Clipboard.getSystemClipboard setContent content
	}

	/** A helper object to convert between byte value and a value with units */
	object memoryBudgetConverter {
		val Units = List("KB", "MB", "GB")

		val memoryBudgetBytes = new SimpleLongProperty
		val memoryBudgetValue = new SimpleIntegerProperty
		val memoryBudgetUnit = new SimpleStringProperty

		var newMemoryBudgetBytes: Option[Number] = None
		var newMemoryBudgetValue: Option[Number] = None
		var newMemoryBudgetUnit: Option[String] = None

		memoryBudgetBytes addListener changeListener { newValue: Number =>
			// filter against infinite update loops
			if (newMemoryBudgetBytes.map(_ != newValue).getOrElse(true)) {
				var reducedValue = newValue.intValue
				var reductions = 0

				// we start at KB, so make sure the loop runs at least once
				do {
					reducedValue = reducedValue / 1024
					reductions = reductions + 1
				} while (reducedValue > 1024 && reductions < Units.length)

				newMemoryBudgetValue = Some(reducedValue)
				memoryBudgetValue set reducedValue

				val newUnit = Units(reductions - 1)
				newMemoryBudgetUnit = Some(newUnit)
				memoryBudgetUnit set newUnit
			}

			newMemoryBudgetBytes = None
		}

		memoryBudgetValue addListener changeListener { newValue: Number =>
			// filter against infinite update loops
			if (newMemoryBudgetValue.map(_ != newValue).getOrElse(true)) {
				val newBytes = memoryBudgetUnitBox.valueProperty.get match {
					case "KB" => newValue.intValue.kilobytes
					case "MB" => newValue.intValue.megabytes
					case "GB" => newValue.intValue.gigabytes
				}

				newMemoryBudgetBytes = Some(newBytes)
				memoryBudgetBytes set newBytes
			}

			newMemoryBudgetValue = None
		}

		memoryBudgetUnit addListener changeListener { newValue: String =>
			// filter against infinite update loops
			if (newMemoryBudgetUnit.map(_ != newValue).getOrElse(true)) {
				val newBytes = newValue match {
					case "KB" => memoryBudgetValue.get.kilobytes
					case "MB" => memoryBudgetValue.get.megabytes
					case "GB" => memoryBudgetValue.get.gigabytes
				}

				newMemoryBudgetBytes = Some(newBytes)
				memoryBudgetBytes set newBytes
			}

			newMemoryBudgetUnit = None
		}
	}
}