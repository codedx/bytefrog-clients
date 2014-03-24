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

import javafx.beans.property.BooleanProperty
import javafx.beans.property.DoubleProperty
import javafx.beans.property.ListProperty
import javafx.beans.property.LongProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.scene.chart.XYChart

/** Data model for the active trace view
  * @author robertf
  */
class TraceModel {
	val traceName: StringProperty = new SimpleStringProperty(this, "traceName", "")
	var traceStartTime: Long = _
	val traceTime: LongProperty = new SimpleLongProperty(this, "traceTime", 0)

	val applicationExecuting: BooleanProperty = new SimpleBooleanProperty(this, "applicationExecuting", true)
	val tracingActive: BooleanProperty = new SimpleBooleanProperty(this, "tracingActive", true)
	val tracingControllable: BooleanProperty = new SimpleBooleanProperty(this, "tracingControllable", false)

	val traceSize: StringProperty = new SimpleStringProperty(this, "traceSize", "")
	val dataRate: StringProperty = new SimpleStringProperty(this, "dataRate", "")
	val dataRateSeries: ObjectProperty[XYChart.Series[Number, Number]] = new SimpleObjectProperty[XYChart.Series[Number, Number]](this, "dataRateSeries", null)

	val packageSort: StringProperty = new SimpleStringProperty(this, "packageSort", "")
	val tracedPackages: ListProperty[String] = new SimpleListProperty(this, "tracePackages")
	val ignoredPackages: ListProperty[String] = new SimpleListProperty(this, "ignoredPackages")
	val errorPackages: ListProperty[String] = new SimpleListProperty(this, "errorPackages")

	val currentSegment: StringProperty = new SimpleStringProperty(this, "currentSegment", null)
	val segments: ListProperty[TraceSegmentEntry] = new SimpleListProperty(this, "segments")
}