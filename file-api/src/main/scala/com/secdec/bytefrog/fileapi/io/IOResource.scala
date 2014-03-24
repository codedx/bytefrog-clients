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

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

trait IOResource[+I, +O] {
	def openInput: I
	def openOutput: O
	def delete: Unit
	def name: String
}

class FileIOResource(file: File, overrideName: Option[String]) extends IOResource[FileInputStream, FileOutputStream] {
	def this(file: File) = this(file, None)
	def this(file: File, overrideName: String) = this(file, Some(overrideName))

	def openInput = new FileInputStream(file)
	def openOutput = new FileOutputStream(file)
	def name = overrideName match {
		case Some(n) => n
		case None => file.getName
	}
	def delete = file.delete()

	override def toString = overrideName match {
		case None => s"FileIOResource($file)"
		case Some(name) => s"FileIOResource($name [$file])"
	}
}