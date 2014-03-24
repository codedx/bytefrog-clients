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

import javafx.application.Platform
import javafx.beans.InvalidationListener
import javafx.beans.Observable
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.ListChangeListener
import javafx.event.Event
import javafx.event.EventHandler
import javafx.geometry.Orientation
import javafx.scene.Node

/** A trait containing helper methods for JavaFX
  *
  * @author robertf
  */
trait JavaFXUtilities {
	/** Posts the provided delegate for running on a JavaFX thread */
	def jfxRun(delegate: => Unit) {
		Platform.runLater(new Runnable {
			def run() = delegate
		})
	}

	/** Creates an invalidation listener with the provided delegate */
	def invalidationListener(delegate: => Unit): InvalidationListener = invalidationListener(observable => delegate)

	/** Creates an invalidation listener with the provided delegate */
	def invalidationListener(delegate: Observable => Unit): InvalidationListener = new InvalidationListener {
		def invalidated(observable: Observable) = delegate(observable)
	}

	/** Creates a change listener with the provided delegate */
	def changeListener[T](delegate: T => Unit): ChangeListener[T] = new ChangeListener[T] {
		def changed(observable: ObservableValue[_ <: T], oldValue: T, newValue: T) = delegate(newValue)
	}

	/** Creates a change listener with the provided delegate */
	def changeListener[T](delegate: (T, T) => Unit): ChangeListener[T] = new ChangeListener[T] {
		def changed(observable: ObservableValue[_ <: T], oldValue: T, newValue: T) = delegate(oldValue, newValue)
	}

	/** Creates a change listener with the provided delegate */
	def changeListener[T](delegate: (ObservableValue[_ <: T], T, T) => Unit): ChangeListener[T] = new ChangeListener[T] {
		def changed(observable: ObservableValue[_ <: T], oldValue: T, newValue: T) = delegate(observable, oldValue, newValue)
	}

	/** Creates a list change listener with the provided delegate */
	def listChangeListener[T](delegate: ListChangeListener.Change[_ <: T] => Unit): ListChangeListener[T] = new ListChangeListener[T] {
		def onChanged(change: ListChangeListener.Change[_ <: T]) = delegate(change)
	}

	/** Creates an event handler with the provided delegate */
	def eventHandler[T <: Event](delegate: T => Unit) = new EventHandler[T] {
		def handle(event: T) = delegate(event)
	}

	/** Calculates the minimum size for a node, taking bias into account properly */
	def calculateMinimumSize(node: Node): (Double, Double) = node.getContentBias match {
		case Orientation.VERTICAL =>
			val minHeight = node minHeight -1
			(node minWidth minHeight, minHeight)

		case Orientation.HORIZONTAL =>
			val minWidth = node minWidth -1
			(minWidth, node minHeight minWidth)

		case _ => (node minWidth -1, node minHeight -1)
	}
}