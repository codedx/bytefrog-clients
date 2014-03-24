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

import java.util.ArrayList

import scala.collection.JavaConversions.asScalaBuffer

import com.secdec.bytefrog.clients.javafxui.util.SizeFormatter

import javafx.collections.FXCollections
import javafx.scene.chart.XYChart

/** A helper class for presenting data rate over time as a JavaFX chartable series
  *
  * @author robertf
  */
class DataRateMonitor(historyLength: Integer) {
	private var lastReport = 0L
	private val dataList = FXCollections.observableList(new ArrayList[XYChart.Data[Number, Number]](historyLength))
	val dataSeries = new XYChart.Series[Number, Number](dataList)

	for (i <- 0 until historyLength) {
		dataList.add(i, new XYChart.Data[Number, Number](-historyLength + i, 0))
	}

	private[trace] def reportDataRate(rate: Integer, timestamp: Long) {
		// shift everything back by one, and add a new value to the end
		// subtract the change in time from the X values... X @ 0 is the latest number,
		// and then prior values are negative, based on how many milliseconds ago they happened
		val deltaT = timestamp - lastReport

		for (i <- 0 until (historyLength - 1)) {
			dataList(i) setXValue (dataList(i + 1).getXValue.longValue - deltaT)
			dataList(i) setYValue dataList(i + 1).getYValue
			dataList(i) setExtraValue dataList(i + 1).getExtraValue
		}

		dataList(historyLength - 1) setXValue 0
		dataList(historyLength - 1) setYValue rate
		dataList(historyLength - 1) setExtraValue s"$rate events/s"

		lastReport = timestamp
	}

	private[trace] def clear() {
		// clear out anything from a prior trace
		for (i <- 0 until historyLength) {
			dataList(i) setXValue (-historyLength + i)
			dataList(i) setYValue (0)
			dataList(i) setExtraValue null
		}
	}
}