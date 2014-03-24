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

object ExceptionIdMapJson {
	implicit val jsonFormats = Serialization.formats(NoTypeHints)
	private case class ExceptionId(id: Int, exception: String)
	private case class ExceptionIdList(methods: List[ExceptionId])

	def serialize(segments: collection.Map[Int, String]): String = {
		val obj = ExceptionIdList(segments.map(ExceptionId.tupled).toList)
		write(obj)
	}

	def deserialize(input: String): Map[Int, String] = {
		try {
			val obj = read[ExceptionIdList](input)
			obj.methods.map(m => (m.id, m.exception)).toMap
		} catch {
			//possible json parsing exceptions
			case e: Exception => Map()
		}
	}
}