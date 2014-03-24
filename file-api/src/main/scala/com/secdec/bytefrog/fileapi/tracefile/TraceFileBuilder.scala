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

import java.io.File
import java.util.Properties

import com.secdec.bytefrog.fileapi.io.IOUtils._
import com.secdec.bytefrog.fileapi.io.zip.ZipFileBuilder
import TraceFileBuilder._

object TraceFileBuilder {
	val manifestFilename = "trace.manifest"
}

/** Builder class for Trace files. This is essentially just a ZipFileBuilder
  * but with one exception: A "trace.manifest" file will be automatically
  * generated inside the resulting zip file. The "trace.manifest" filename
  * is reserved, so that if a client attempts to get a builder token via
  * calling `vend("trace.manifest")`, the method will throw an IllegalArgumentException.
  */
class TraceFileBuilder(file: File, manifest: TraceManifest) extends ZipFileBuilder(file) {

	//automatically generate a "trace.manifest" file in the builder
	{
		val mfToken = super.vend(manifestFilename)
		mfToken.resource.openOutput closeAfter { manifest.writeTo }
		mfToken.completionCallback()
	}

	override def vend(filename: String) = filename match {
		case `manifestFilename` => throw new IllegalArgumentException(
			"'%s' is a reserved filename inside trace files".format(manifestFilename))
		case fn => super.vend(fn)
	}
}