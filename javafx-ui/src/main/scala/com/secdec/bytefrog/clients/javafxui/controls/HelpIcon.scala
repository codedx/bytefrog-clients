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

import javafx.beans.property.SimpleStringProperty
import javafx.fxml.FXMLLoader
import javafx.scene.layout.StackPane

class HelpIcon extends StackPane with JavaFXUtilities {
	private val helpContent = new SimpleStringProperty(this, "helpContent", "")
	def helpContentProperty() = helpContent
	def setHelpContent(content: String) = helpContent set content
	def getHelpContent() = helpContent.get

	private val loader = new FXMLLoader(getClass.getResource("help-icon.fxml"))
	loader setRoot this
	loader setController this
	loader.load

	private var helpTip = new HelpTip(helpContent.get)
	HelpTip.install(this, helpTip)

	helpContent addListener changeListener { newValue: String =>
		HelpTip.uninstall(this)
		helpTip = new HelpTip(newValue)
		HelpTip.install(this, helpTip)
	}
}