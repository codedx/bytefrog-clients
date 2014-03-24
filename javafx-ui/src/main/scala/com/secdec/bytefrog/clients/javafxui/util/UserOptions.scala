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

package com.secdec.bytefrog.clients.javafxui.util

import scala.util.Try

import com.secdec.bytefrog.hq.util.NumberUnits.intToFileSizes

import javafx.beans.property.IntegerProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty

/** Handles persisting user options (mainly the advanced options pane)
  *
  * @author robertf
  */
object UserOptions extends PropertiesFile {
	protected val PropertiesFileName = "useroptions.properties"
	protected val PropertiesComment = "Code Pulse User Options"

	val DefaultListenPort = 9001
	val DefaultAgentMemoryBudget = 50.megabytes.toInt
	val DefaultSavePath = ""

	val listenPort: IntegerProperty = new SimpleIntegerProperty(this, "listenPort", Try(properties.getProperty("listenPort").toInt).getOrElse(DefaultListenPort))
	val agentMemoryBudget: IntegerProperty = new SimpleIntegerProperty(this, "memoryBudget", Try(properties.getProperty("agentMemoryBudget").toInt).getOrElse(DefaultAgentMemoryBudget))
	val savePath: StringProperty = new SimpleStringProperty(this, "savePath", properties.getProperty("savePath", DefaultSavePath))

	def save() {
		properties.setProperty("listenPort", listenPort.get.toString)
		properties.setProperty("agentMemoryBudget", agentMemoryBudget.get.toString)
		properties.setProperty("savePath", savePath.get)

		saveProperties
	}
}