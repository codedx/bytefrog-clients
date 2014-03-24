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

package com.secdec.bytefrog.fileapi.io

import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream

import scala.annotation.tailrec
import scala.language.implicitConversions

object IOUtils {

	class CloseableWithCloseAfter[R <: Closeable](resource: R) {
		def closeAfter[T](body: R => T): T = try {
			body(resource)
		} finally {
			resource.close
		}
	}

	implicit def closeableWithCloseAfter[R <: Closeable](resource: R) =
		new CloseableWithCloseAfter(resource)

	def copy(from: InputStream, to: OutputStream, buffer: Array[Byte] = new Array[Byte](1024)): Unit = {
		val len = buffer.length
		@tailrec def c: Unit = {
			val n = from.read(buffer, 0, len)
			if (n >= 0) {
				to.write(buffer, 0, n)
				c
			}
		}
		c
	}
}