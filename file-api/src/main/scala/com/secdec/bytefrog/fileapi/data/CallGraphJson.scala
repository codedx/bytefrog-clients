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

object CallGraphJson {
	implicit val jsonFormats = Serialization.formats(NoTypeHints)
	private case class CallGraph(edges: List[CallGraphEdge])

	def serialize(edges: List[CallGraphEdge]): String = {
		val obj = CallGraph(edges)
		write(obj)
	}

	def deserialize(input: String): List[CallGraphEdge] = {
		try {
			val obj = read[CallGraph](input)
			obj.edges
		} catch {
			//possible json parsing exceptions
			case e: Exception => Nil
		}
	}
}