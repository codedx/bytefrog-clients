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

package com.secdec.bytefrog.fileapi.data

import net.liftweb.json._
import Serialization.{ read, write }

case class TraceMarker(key: String, value: String, timestamp: Int)

object TraceMarkerJson {
	implicit val jsonFormats = DefaultFormats
	private case class TraceMarkers(markers: List[TraceMarker])

	def serialize(markers: List[TraceMarker]): String = {
		val obj = TraceMarkers(markers)
		write(obj)
	}

	def deserialize(input: String): List[TraceMarker] = {
		try {
			val obj = read[TraceMarkers](input)
			obj.markers
		} catch {
			//possible json parsing exceptions
			case e: Exception => Nil
		}
	}
}