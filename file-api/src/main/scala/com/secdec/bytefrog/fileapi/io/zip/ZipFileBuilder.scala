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

import java.io._
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import scala.util.control.Exception

import com.secdec.bytefrog.fileapi.io.FileIOResource
import com.secdec.bytefrog.fileapi.io.IOResource
import com.secdec.bytefrog.fileapi.io.IOUtils._

case class ZipFileBuilderToken(resource: IOResource[InputStream, OutputStream], completionCallback: () => Unit)

/** Helper class for building a ZIP file, where the contents of each entry
  * in the ZIP file are the result of long-running calculations.
  *
  * The general usage of this class is to `vend` several `ZipFileBuilderToken`s
  * via the vend method. A token consists of a `resource` (which has methods
  * for IO) and a `completionCallback`. Whatever thread is processing the
  * resource (generally by writing to its `output`) should call `token.completionCallback()`
  * when it has finished processing.
  *
  * The end result is obtained via callbacks. Use the `getResult` function,
  * providing a callback for a successfully-built result file, and a separate
  * callback for an error that may have occurred while building.
  */
class ZipFileBuilder(file: File) {

	if (file.getParentFile == null) {
		throw new IllegalArgumentException("Cannot use a root file")
	}

	protected val tempFolder = createTempFolder(file)
	private val idCounter = new AtomicInteger(0)
	private val releasedTokens = collection.mutable.Map[Int, Resource]()
	private val completedTokens = collection.mutable.Map[Int, Resource]()
	private val resultWaiters = collection.mutable.ListBuffer[AwaitingResult]()
	private var isBuilt = false

	private type Resource = IOResource[InputStream, OutputStream]
	private case class AwaitingResult(callback: File => Unit, onError: Throwable => Unit)

	protected def createTempFolder(neighbor: File): File = {
		val parent = neighbor.getParentFile

		val availFolder = Iterator.from(0).take(10).map { i =>
			// pick a file named <filename>.tmp#
			new File(parent, neighbor.getName + ".tmp" + i)
		}.find { f =>
			// find the first folder that doesn't yet exist, and can be created
			!f.exists && f.mkdirs
		}.getOrElse {
			// if we ran out of options, throw an IOException
			throw new IOException("Could not create an appropriate temp file")
		}

		availFolder
	}

	protected def nextId = { idCounter.incrementAndGet }

	def vend(filename: String): ZipFileBuilderToken = {
		if (isBuilt) {
			throw new IllegalStateException("Result has already been built. Create a new builder in order to vend new files")
		}
		val tempFile = File.createTempFile("tracefile", ".tmp", tempFolder)
		//tempFile.deleteOnExit()
		val resource = new FileIOResource(tempFile, filename)
		val id = nextId
		val completionCallback = () => completeToken(id)
		val token = ZipFileBuilderToken(resource, completionCallback)
		initializeToken(id, resource)
		token
	}

	protected def initializeToken(id: Int, resource: Resource) = synchronized {
		releasedTokens += id -> resource
	}

	protected def completeToken(id: Int) = synchronized {
		for (file <- releasedTokens remove id) {
			completedTokens += id -> file
		}
		checkCompletion()
	}

	private def buildResult = {
		if (!isBuilt) {
			new FileOutputStream(file) closeAfter { fOut =>
				new ZipOutputStream(fOut) closeAfter { zOut =>
					val bufferLength = 1024 * 10
					val buffer = new Array[Byte](bufferLength)

					for (resource <- completedTokens.values) {
						val zipEntry = new ZipEntry(resource.name)
						zOut.putNextEntry(zipEntry)
						resource.openInput.closeAfter {
							copy(_, zOut, buffer)
						}
						zOut.closeEntry
						resource.delete
					}
				}
			}
			isBuilt = true
			tempFolder.delete()
		}
		file
	}

	def getResult(callback: File => Unit, onError: Throwable => Unit): Unit = {
		val waiter = AwaitingResult(callback, onError)
		resultWaiters += waiter
		checkCompletion()
	}

	def dispose(): Unit = synchronized {
		// dispose of the file in progress

		// clear out any and all tokens
		for (resource <- releasedTokens.values) {
			resource.delete
		}
		releasedTokens.clear

		for (resource <- completedTokens.values) {
			resource.delete
		}
		completedTokens.clear

		isBuilt = true
		tempFolder.delete
	}

	private def checkCompletion(): Unit = synchronized {
		if (releasedTokens.isEmpty && !completedTokens.isEmpty && !resultWaiters.isEmpty) {
			val currentWaiters = resultWaiters.result
			resultWaiters.clear

			Exception.allCatch[File] either { buildResult } match {
				case Left(error) => currentWaiters foreach { _ onError error }
				case Right(result) => currentWaiters foreach { _ callback result }
			}
		}
	}
}
