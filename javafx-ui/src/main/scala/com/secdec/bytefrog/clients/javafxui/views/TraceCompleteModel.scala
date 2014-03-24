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

import com.secdec.bytefrog.clients.javafxui.trace.TraceSegmentEntry

import javafx.beans.property.DoubleProperty
import javafx.beans.property.ListProperty
import javafx.beans.property.LongProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty

/** Data model for the trace complete view
  * @author robertf
  */
class TraceCompleteModel {
	val traceName: StringProperty = new SimpleStringProperty(this, "traceName", "")
	val traceTime: LongProperty = new SimpleLongProperty(this, "traceTime", 0)
	val traceSize: StringProperty = new SimpleStringProperty(this, "traceSize", "")
	val traceFile: StringProperty = new SimpleStringProperty(this, "traceFile", "")

	val packageSort: StringProperty = new SimpleStringProperty(this, "packageSort", "")
	val tracedPackages: ListProperty[String] = new SimpleListProperty(this, "tracePackages")
	val ignoredPackages: ListProperty[String] = new SimpleListProperty(this, "ignoredPackages")
	val errorPackages: ListProperty[String] = new SimpleListProperty(this, "errorPackages")

	val segments: ListProperty[TraceSegmentEntry] = new SimpleListProperty(this, "segments")
}