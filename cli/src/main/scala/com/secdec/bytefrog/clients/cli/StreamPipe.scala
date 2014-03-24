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

package com.secdec.bytefrog.clients.cli

import java.io.InputStream
import java.io.OutputStream

class StreamPipe(input: InputStream, output: OutputStream) extends Runnable {
	val buffer = new Array[Byte](2048)
	var numRead: Int = 0

	def run = {
		numRead = input.read(buffer)
		while (numRead > 0) {
			output.write(buffer, 0, numRead)
			numRead = input.read(buffer)
		}
		input.close
	}
}