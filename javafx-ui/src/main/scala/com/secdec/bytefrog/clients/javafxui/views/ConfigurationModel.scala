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

import javafx.beans.property.DoubleProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty

/** Data model for the configuration view
  * @author robertf
  */
class ConfigurationModel {
	val traceName: StringProperty = new SimpleStringProperty(this, "traceName", "")
	val packageIncludes: ObjectProperty[List[String]] = new SimpleObjectProperty[List[String]](this, "packageIncludes", List())
	val agentPort: IntegerProperty = new SimpleIntegerProperty(this, "agentPort", 9001)
	val agentMemoryBudget: IntegerProperty = new SimpleIntegerProperty(this, "agentMemoryBudget", 52428800)
	val traceSaveFolder: StringProperty = new SimpleStringProperty(this, "saveFolder", "")

	val agentConnected = new SimpleBooleanProperty(false)
	val listening = new SimpleBooleanProperty(false)
	val statusMessage = new SimpleStringProperty
}