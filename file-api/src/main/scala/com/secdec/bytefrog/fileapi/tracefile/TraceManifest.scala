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

package com.secdec.bytefrog.fileapi.tracefile

import java.io.InputStream
import java.io.OutputStream
import java.util.Properties

case class TraceManifest(name: String, date: Long) {

	/** Write this TraceManifest to the provided `out` stream.
	  * The format is a Java Properties object that contains
	  * the name and date. This method does not close `out` stream.
	  */
	def writeTo(out: OutputStream): Unit = {
		import TraceManifest._

		val props = new Properties
		props.setProperty(manifestPropKey, "true")
		props.setProperty(manifestNameKey, name)
		props.setProperty(manifestDateKey, date.toString)
		props.store(out, "auto-generated trace manifest file")
	}

}

object TraceManifest {
	val manifestPropKey = "cp.trace.file"
	val manifestNameKey = "cp.trace.name"
	val manifestDateKey = "cp.trace.date"

	/** Read a TraceManifest from the provided `in` stream.
	  * The expected format is a Java Properties object that
	  * contains the name and date of the manifest. This
	  * method does not close the `in` stream.
	  */
	def readFrom(in: InputStream): Option[TraceManifest] = {
		val props = new Properties
		props.load(in)

		for {
			_ <- Option { props getProperty manifestPropKey }
			name <- Option { props getProperty manifestNameKey }
			dateString <- Option { props getProperty manifestDateKey }
			date <- try { Some(dateString.toLong) } catch { case e: NumberFormatException => None }
		} yield {
			TraceManifest(name, date)
		}
	}
}