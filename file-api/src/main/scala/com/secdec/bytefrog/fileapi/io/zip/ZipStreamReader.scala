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

package com.secdec.bytefrog.fileapi.io.zip

import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import collection.JavaConversions._

trait ZipEntryHandler {
	def handleZipEntry(entry: ZipEntry, contents: InputStream): Unit
}

class ZipStreamReader(handler: ZipEntryHandler) {
	def read(in: File) = {
		val zipfile = new ZipFile(in)

		try {
			val entries = zipfile.entries
			for (entry <- entries) {
				val content = zipfile.getInputStream(entry)
				val buffered = new BufferedInputStream(content)
				handler.handleZipEntry(entry, buffered)
			}
		} finally {
			zipfile.close
		}
	}
}