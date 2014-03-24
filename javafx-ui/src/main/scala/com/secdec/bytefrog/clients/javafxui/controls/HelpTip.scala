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

package com.secdec.bytefrog.clients.javafxui.controls

import scala.collection.JavaConversions.asScalaSet
import scala.io.Source

import com.secdec.bytefrog.clients.javafxui.util.JavaFXUtilities

import javafx.animation.Animation
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.collections.ListChangeListener
import javafx.concurrent.Worker.State
import javafx.geometry.HPos
import javafx.geometry.VPos
import javafx.scene.Node
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Region
import javafx.scene.web.WebView
import javafx.stage.Popup
import javafx.util.Duration

/** A custom tooltip implementation that works around undesired limitations of the
  * JavaFX tooltip class - namely, the hardcoded delay on showing and time restriction
  * on how long it is visible.
  *
  * Rather than try and override private implementation details of the existing tooltip
  * (which supposedly will be made public in Java 8, introducing breaking changes), this
  * class simply reimplements the popup. Since we'd just be re-styling the tooltip to
  * look how we want and simply displaying a webview with it, the actual popup window itself
  * is quite simple.
  *
  * @author robertf
  */
class HelpTip(content: String) extends Popup {
	val tipView = new HelpTipView
	tipView setWidth 500
	tipView setContent content
	getContent add tipView

	setAutoFix(true)
	setAutoHide(true)
	setConsumeAutoHidingEvents(false)

	// add CSS info
	tipView.getStyleClass add "help-popup"
	tipView.getStylesheets add getClass.getResource("help-tip.css").toExternalForm

	/** A webview wrapper that auto-sizes height based on content.
	  * Width must be statically set.
	  *
	  * Based on the implementation discussed at
	  * http://java-no-makanaikata.blogspot.com/2012/10/javafx-webview-size-trick.html
	  *
	  * @author robertf
	  */
	protected class HelpTipView extends Region with JavaFXUtilities {
		private val webView = new WebView
		private val engine = webView.getEngine

		webView.getStyleClass add "web-view"

		// keep width sized appropriately
		widthProperty addListener changeListener { newWidth: Number =>
			webView setPrefWidth newWidth.doubleValue
			adjustHeight
		}

		// adjust height when content changes
		engine.getLoadWorker.stateProperty addListener changeListener { newState: State =>
			if (newState == State.SUCCEEDED)
				adjustHeight
		}

		// keep scrollbar hidden
		// http://stackoverflow.com/questions/11206942/how-to-hide-scrollbars-in-the-javafx-webview
		webView.getChildrenUnmodifiable addListener listChangeListener { change: ListChangeListener.Change[_ <: Node] =>
			for (scrollbar <- webView lookupAll ".scroll-bar") {
				scrollbar setVisible false
				scrollbar setManaged false
			}
		}

		getChildren add webView

		override protected def layoutChildren() {
			val insets = getInsets
			layoutInArea(webView, insets.getLeft, insets.getRight, getWidth - insets.getLeft - insets.getRight, getHeight - insets.getTop - insets.getBottom, 0, HPos.CENTER, VPos.CENTER)
		}

		private lazy val helpTipCSS = {
			val cssFile = Source.fromURL(getClass.getResource("help-tip-contents.css"))
			val cssContents = cssFile.getLines mkString "\n"
			cssFile.close

			s"<style type=\042text/css\042>\n$cssContents\n</style>"
		}
		private def wrapContent(content: String) = s"<html><head>$helpTipCSS</head><body><div id=\042content\042>$content</div></body></html>"

		def setContent(content: String) = engine loadContent wrapContent(content)

		override def setWidth(width: Double) {
			super.setWidth(width)
		}

		protected def adjustHeight() {
			// this works better when queued in the javafx thread, so the webengine can finish its work
			jfxRun {
				if (engine.executeScript("document.getElementById('content')") != null) {
					val width = engine.executeScript("document.getElementById('content').offsetWidth").asInstanceOf[Integer]
					// TODO: what should we expect width to be? hint: it isn't getWidth - horizontal insets, unfortunately.
					if (width > 10) {
						val height = engine.executeScript("document.getElementById('content').offsetHeight").asInstanceOf[Integer]
						webView setPrefHeight height.toDouble
					} else {
						// when the webview isn't wide enough, don't set an absurd height, to prevent flicker
						webView setPrefHeight 0
					}
				}
			}
		}
	}
}

/** Helpers for wiring up helptips to their control nodes.
  *
  * The behavior here is that, upon installing the help tip to its control node, we listen
  * for mouse enter and exit events. When the mouse enters the control node, the helptip
  * is shown, and when the mouse exits, the helptip is hidden.
  */
object HelpTip extends JavaFXUtilities {
	private val HelpTipPropertyKey = "com.secdec.bytefrog.clients.javafxui.controls.HelpTip"
	private val ShowDelay = Duration millis 100
	private val HideDelay = Duration millis 50

	// The last known mouse coordinates (screen-space)
	private var mouseX: Double = 0
	private var mouseY: Double = 0

	// The last known node that the mouse was hovering over
	private var hoveredNode: Option[Node] = None

	// The tip that is waiting to be shown
	private var activatedTip: Option[HelpTip] = None

	// The tip that is visible right now
	private var visibleTip: Option[HelpTip] = None

	private val showTimeline = {
		val timeline = new Timeline
		timeline.getKeyFrames add new KeyFrame(ShowDelay)
		timeline setOnFinished eventHandler { _ =>
			// show the help tip
			for {
				node <- hoveredNode
				tip <- activatedTip
			} {
				// if for some reason a tip is already visible, hide it
				for {
					oldTip <- visibleTip
				} oldTip.hide

				val (displaySX, displaySY) = tipDisplayLocation
				tip.show(node, displaySX, displaySY)

				visibleTip = activatedTip
				activatedTip = None
				hoveredNode = None
			}
		}
		timeline
	}

	private val hideTimeline = {
		val timeline = new Timeline
		timeline.getKeyFrames add new KeyFrame(HideDelay)
		timeline setOnFinished eventHandler { _ =>
			// hide the help tip
			for {
				tip <- visibleTip
			} {
				tip.hide
				activatedTip = None
				visibleTip = None
				hoveredNode = None
			}
		}
		timeline
	}

	// when the mouse enters a help node, start the timeline to show the popup
	private val mouseEnterHandler = eventHandler { event: MouseEvent =>
		mouseX = event.getScreenX
		mouseY = event.getScreenY

		val node = event.getSource.asInstanceOf[Node]
		hoveredNode = Some(node)
		val helpTip = node.getProperties.get(HelpTipPropertyKey).asInstanceOf[HelpTip]
		if (helpTip != null) {
			// start the timer to show the tip
			activatedTip = Some(helpTip)
			showTimeline.stop
			showTimeline.playFromStart
		}
	}

	// when the mouse enters a help popup, cancel any pending hide timeline events
	private val mouseEnterPopupHandler = eventHandler[MouseEvent] { _ =>
		if (hideTimeline.getStatus == Animation.Status.RUNNING) {
			hideTimeline.stop
		}
	}

	// when the mouse moves within the help node, reposition the popup
	private val mouseMoveHandler = eventHandler { event: MouseEvent =>
		mouseX = event.getScreenX
		mouseY = event.getScreenY

		if (showTimeline.getStatus == Animation.Status.RUNNING) {
			showTimeline.stop
			showTimeline.playFromStart
		} else {
			updateVisibleTip
		}
	}

	// when the mouse exits a help node, cancel pending show timelines if necessary
	// otherwise, IF the mouse has actually moved, begin the timeline for hiding the popup
	private val mouseExitHandler = eventHandler { event: MouseEvent =>
		if (showTimeline.getStatus == Animation.Status.RUNNING) {
			showTimeline.stop
		} else if (mouseX != event.getScreenX || mouseY != event.getScreenY) {
			hideTimeline.playFromStart
		}
	}

	// when the mouse exits a help popup, unconditionally start the timeline for hiding
	private val mouseExitPopupHandler = eventHandler[MouseEvent] { _ =>
		hideTimeline.playFromStart
	}

	private def tipDisplayLocation: (Double, Double) = (mouseX + 30, mouseY)

	private def updateVisibleTip() {
		for {
			tip <- visibleTip if (tip.isShowing)
		} {
			val (displaySX, displaySY) = tipDisplayLocation
			tip.show(tip.getOwnerWindow, displaySX, displaySY)
		}
	}

	/** Install `tip` to be shown when hovering over `node`.  */
	def install(node: Node, tip: HelpTip) {
		if (node != null) {
			node.addEventHandler(MouseEvent.MOUSE_ENTERED, mouseEnterHandler)
			node.addEventHandler(MouseEvent.MOUSE_MOVED, mouseMoveHandler)
			node.addEventHandler(MouseEvent.MOUSE_EXITED, mouseExitHandler)
			node.getProperties.put(HelpTipPropertyKey, tip)

			// TODO: handle one tip being reused several times properly here
			tip.tipView.addEventHandler(MouseEvent.MOUSE_ENTERED, mouseEnterPopupHandler)
			tip.tipView.addEventHandler(MouseEvent.MOUSE_EXITED, mouseExitPopupHandler)
		}
	}

	/** Uninstall the previously installed tip from `node`. */
	def uninstall(node: Node) {
		if (node != null) {
			node.removeEventHandler(MouseEvent.MOUSE_ENTERED, mouseEnterHandler)
			node.removeEventHandler(MouseEvent.MOUSE_MOVED, mouseMoveHandler)
			node.removeEventHandler(MouseEvent.MOUSE_EXITED, mouseExitHandler)

			val oldTip = node.getProperties.get(HelpTipPropertyKey).asInstanceOf[HelpTip]
			if (oldTip != null) {
				oldTip.hide
				node.getProperties remove HelpTipPropertyKey
			}
		}
	}
}