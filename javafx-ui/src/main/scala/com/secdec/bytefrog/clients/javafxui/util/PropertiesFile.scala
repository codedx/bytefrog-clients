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

package com.secdec.bytefrog.clients.javafxui.util

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.Properties

import com.secdec.bytefrog.clients.common.util.ApplicationData
import com.secdec.bytefrog.clients.javafxui.Constants

/** Helper trait for classes that deal with properties files.
  * Handles loading and saving (on a best-effort basis - will not throw).
  *
  * @author robertf
  */
trait PropertiesFile {
	protected val PropertiesFileName: String
	protected val PropertiesComment: String

	private lazy val propertiesFile = new File(ApplicationData.getApplicationDataFolder(Constants.CompanyName, Constants.ApplicationName, Constants.ApplicationShortName), PropertiesFileName)
	private var saveEnabled = false
	protected lazy val properties = {
		val properties = new Properties

		try {
			if (propertiesFile.exists)
				properties.load(new FileInputStream(propertiesFile))
			saveEnabled = true
		} catch {
			case e: IOException => // if we can't load, just leave the properties blank
		}

		properties
	}

	protected def saveProperties() {
		try {
			val parent = propertiesFile.getParentFile
			if (!parent.exists)
				parent.mkdirs()

			properties.store(new FileOutputStream(propertiesFile), PropertiesComment)
		} catch {
			case e: IOException => // ignore failure saving
		}
	}
}