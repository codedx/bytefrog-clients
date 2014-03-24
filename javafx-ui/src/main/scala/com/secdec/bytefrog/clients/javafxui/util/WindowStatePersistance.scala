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

import scala.util.Try

import com.secdec.bytefrog.clients.common.util.ApplicationData
import com.secdec.bytefrog.clients.javafxui.Constants

import javafx.stage.Screen
import javafx.stage.Window

sealed trait WindowClass {
	def name: String
	def applyToWindow(window: Window, state: WindowState)
}

object WindowClass {
	case object Configuration extends WindowClass {
		val name = "configuration"

		def applyToWindow(window: Window, state: WindowState) {
			val destScreens = Screen.getScreensForRectangle(state.xPos, state.yPos, 1, 1)

			if (!destScreens.isEmpty()) {
				window setX state.xPos
				window setY state.yPos
			}
		}
	}

	case object Trace extends WindowClass {
		val name = "trace"

		def applyToWindow(window: Window, state: WindowState) {
			val destScreens = Screen.getScreensForRectangle(state.xPos, state.yPos, state.width, state.height)

			// this could be a more intelligent check (make sure our bounds fall totally within the screens given)...
			if (!destScreens.isEmpty()) {
				window setX state.xPos
				window setY state.yPos
				window setWidth state.width
				window setHeight state.height
			}
		}
	}
}

case class WindowState(xPos: Double, yPos: Double, width: Double, height: Double)
object WindowState {
	def fromWindow(window: Window) = WindowState(window.getX, window.getY, window.getWidth, window.getHeight)
}

/** Handles persisting window state (size, position, etc).
  *
  * @author robertf
  */
object WindowStatePersistance extends PropertiesFile {
	protected val PropertiesFileName = "windowstate.properties"
	protected val PropertiesComment = "Code Pulse Window Position/Location Persistance"

	def saveState(windowClass: WindowClass, windowState: WindowState) {
		val className = windowClass.name
		properties.setProperty(s"$className.xPos", windowState.xPos.toString)
		properties.setProperty(s"$className.yPos", windowState.yPos.toString)
		properties.setProperty(s"$className.width", windowState.width.toString)
		properties.setProperty(s"$className.height", windowState.height.toString)
		saveProperties
	}

	def loadState(windowClass: WindowClass): Option[WindowState] = {
		val className = windowClass.name
		var state: Option[WindowState] = None

		for {
			xPos <- Try(properties.getProperty(s"$className.xPos").toDouble)
			yPos <- Try(properties.getProperty(s"$className.yPos").toDouble)
			width <- Try(properties.getProperty(s"$className.width").toDouble)
			height <- Try(properties.getProperty(s"$className.height").toDouble)
		} yield {
			state = Some(WindowState(xPos, yPos, width, height))
		}

		state
	}
}