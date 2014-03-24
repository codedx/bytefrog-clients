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

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer
import scala.ref.WeakReference

import com.secdec.bytefrog.clients.javafxui.trace.TraceSegmentEntry
import com.secdec.bytefrog.clients.javafxui.trace.TraceSegmentEntryType
import com.secdec.bytefrog.clients.javafxui.util.JavaFXUtilities
import com.secdec.bytefrog.clients.javafxui.util.TimeFormatter

import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.collections.WeakListChangeListener
import javafx.event.ActionEvent
import javafx.geometry.Bounds
import javafx.geometry.HPos
import javafx.geometry.Insets
import javafx.geometry.VPos
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.Control
import javafx.scene.control.Label
import javafx.scene.control.OverrunStyle
import javafx.scene.control.ScrollPane
import javafx.scene.control.TextField
import javafx.scene.input.MouseEvent
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.shape.Polygon

import reactive.EventSource
import reactive.EventStream

object SegmentGraphics {
	def segmentEntryGraphic(entryType: TraceSegmentEntryType): Node = {
		val graphic = entryType match {
			case TraceSegmentEntryType.SegmentBegin =>
				new Polygon(
					0, 0,
					12, 0,
					6, 10)

			case TraceSegmentEntryType.SegmentEnd =>
				new Polygon(
					6, 0,
					0, 10,
					12, 10)
		}

		graphic.getStyleClass add "trace-segment-graphic"
		graphic
	}
}

/** A custom control that handles displaying a trace segment tree.
  *
  * @author robertf
  */
class SegmentControl extends ScrollPane with JavaFXUtilities {
	def segmentEntries: ObjectProperty[ObservableList[TraceSegmentEntry]] = new SimpleObjectProperty[ObservableList[TraceSegmentEntry]] {
		private var oldListRef = new WeakReference[ObservableList[TraceSegmentEntry]](null)

		override def invalidated() {
			val oldList = oldListRef.get match {
				case Some(oldList) => oldList
				case None => null
			}

			val newList = get

			if (newList != oldList) {
				updateSegmentListObserver(oldList, newList)
				oldListRef = new WeakReference(newList)
			}

			super.invalidated
		}
	}

	private var autoScroll = true
	def getAutoScroll = autoScroll
	def setAutoScroll(scrollToBottom: Boolean) = this.autoScroll = scrollToBottom

	private var readOnly = false
	def getReadOnly = readOnly
	def setReadOnly(readOnly: Boolean) = this.readOnly = readOnly

	private val segmentRows = new ListBuffer[SegmentRecordRow]
	private val segmentMap = new HashMap[TraceSegmentEntry, SegmentRecordRow]
	private val segmentGrid = new GridPane

	viewportBoundsProperty addListener changeListener { bounds: Bounds =>
		segmentGrid setMaxWidth bounds.getWidth
	}

	segmentGrid.heightProperty addListener invalidationListener {
		if (autoScroll)
			setVvalue(getVmax) // keep scrolled to bottom if requested
	}

	segmentGrid setPadding new Insets(3)
	segmentGrid setHgap 3
	segmentGrid setVgap 3

	setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER)
	setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED)
	setContent(segmentGrid)

	getStyleClass add "segment-control"
	getStylesheets add getClass.getResource("segment-control.css").toExternalForm

	private val segmentListChangeListener = listChangeListener { (change: ListChangeListener.Change[_ <: TraceSegmentEntry]) =>
		while (change.next) {
			if (change.wasPermutated()) {
				// sort based on permutation
				for (i <- change.getFrom until change.getTo) {
					val newPos = change.getPermutation(i)
					val temp = segmentRows(newPos)
					segmentRows(newPos) = segmentRows(i)
					segmentRows(i) = temp
				}

				resetRows
			} else if (change.wasUpdated()) {
				throw new NotImplementedError
			} else {
				for (removed <- change.getRemoved) {
					removeSegmentEntry(removed)
				}

				val start = change.getFrom

				for ((added, offset) <- change.getAddedSubList.zipWithIndex) {
					addSegmentEntry(added, Some(start + offset))
				}
			}
		}
	}

	private val weakSegmentListChangeListener = new WeakListChangeListener[TraceSegmentEntry](segmentListChangeListener)

	private def updateSegmentListObserver(oldList: ObservableList[TraceSegmentEntry], newList: ObservableList[TraceSegmentEntry]) {
		// stop listening to the old list updates and remove the old contents
		if (oldList != null) {
			oldList.removeListener(weakSegmentListChangeListener)
			removeAll
		}

		// add new contents and listen to new list for updates
		if (newList != null) {
			addAll(newList)
			newList.addListener(weakSegmentListChangeListener)
		}
	}

	private def addAll(list: ObservableList[TraceSegmentEntry]) {
		for (entry <- list) {
			addSegmentEntry(entry)
		}
	}

	private def removeAll() {
		getChildren.clear
		segmentRows.clear
		segmentMap.clear
	}

	private def addSegmentEntry(segment: TraceSegmentEntry, index: Option[Int] = None) {
		val timeLabel = new Label(TimeFormatter.format(segment.relativeTime, true))
		val labelStack = new StackPane
		val labelHBox = new HBox(4)
		val labelLabel = new Label(segment.label)
		val editButton = new Button("Rename")

		timeLabel setMinWidth Control.USE_PREF_SIZE
		timeLabel setPrefWidth Control.USE_COMPUTED_SIZE
		timeLabel setMaxWidth Control.USE_PREF_SIZE
		timeLabel.getStyleClass add "segment-time"

		labelStack setMinWidth 0
		labelStack setPrefWidth Control.USE_COMPUTED_SIZE
		labelStack setMaxWidth Control.USE_PREF_SIZE
		labelStack setMinHeight 0
		labelStack setPrefHeight Control.USE_COMPUTED_SIZE
		labelStack setMaxHeight Control.USE_PREF_SIZE

		labelHBox setMinWidth 0
		labelHBox setPrefWidth Control.USE_COMPUTED_SIZE
		labelHBox setMaxWidth Control.USE_PREF_SIZE

		labelLabel setMinWidth 0
		labelLabel setPrefWidth Control.USE_COMPUTED_SIZE
		labelLabel setMaxWidth Control.USE_PREF_SIZE
		labelLabel setGraphic SegmentGraphics.segmentEntryGraphic(segment.entryType)
		labelLabel setTextOverrun OverrunStyle.CENTER_ELLIPSIS
		labelLabel.getStyleClass add "segment-name"

		labelHBox.getChildren addAll (labelLabel, editButton)
		labelStack.getChildren add labelHBox

		GridPane.setConstraints(timeLabel, 0, 0, 1, 1, HPos.RIGHT, VPos.TOP)
		GridPane.setConstraints(labelStack, 1, 0, 1, 1, HPos.LEFT, VPos.TOP, Priority.SOMETIMES, Priority.NEVER, new Insets(0, 0, 0, 5 + segment.depth * 15))

		segmentGrid.getChildren addAll (timeLabel, labelStack)

		val newRow = SegmentRecordRow(segment.id, timeLabel, labelHBox, labelStack)
		segmentMap += segment -> newRow

		index match {
			case Some(idx) => segmentRows.insert(idx, newRow)
			case None => segmentRows += newRow
		}

		editButton setVisible false

		{
			var mouseHovers = 0

			labelHBox.addEventHandler(MouseEvent.MOUSE_ENTERED_TARGET, eventHandler { e: MouseEvent =>
				mouseHovers += 1

				if (!readOnly)
					editButton setVisible true
			})

			labelHBox.addEventHandler(MouseEvent.MOUSE_EXITED_TARGET, eventHandler { e: MouseEvent =>
				mouseHovers -= 1

				if (mouseHovers == 0)
					editButton setVisible false
			})
		}

		editButton setOnAction eventHandler { e: ActionEvent =>
			beginRename(newRow, segment.label)
		}

		resetRows
	}

	private def resetRows() {
		for ((row, idx) <- segmentRows.zipWithIndex) {
			GridPane.setRowIndex(row.labelPane, idx)
			GridPane.setRowIndex(row.timeLabel, idx)
		}
	}

	private def removeSegmentEntry(segment: TraceSegmentEntry) {
		for (record <- segmentMap get segment) {
			val row = segmentRows.indexOf(record)
			segmentGrid.getChildren.removeAll(record.timeLabel, record.labelPane)
			segmentRows.remove(row)
			segmentMap.remove(segment)
			resetRows
		}
	}

	private case class SegmentRecordRow(id: Int, timeLabel: Label, labelHBox: HBox, labelPane: StackPane)

	/** Event fired when the user requests a segment rename */
	def segmentRenameEvent: EventStream[(Int, String)] = segmentRenameEventSource
	private val segmentRenameEventSource = new EventSource[(Int, String)]

	private lazy val (renameControl, setupRenameControl) = {
		val hBox = new HBox(4)
		val textBox = new TextField
		val saveButton = new Button("Save")
		val cancelButton = new Button("Cancel")

		hBox setMinWidth 0
		hBox setPrefWidth Control.USE_COMPUTED_SIZE
		hBox setMaxWidth Control.USE_PREF_SIZE

		textBox setMaxWidth Double.MaxValue
		HBox.setHgrow(textBox, Priority.ALWAYS)

		hBox.getChildren addAll (textBox, saveButton, cancelButton)

		saveButton setOnAction eventHandler { e: ActionEvent =>
			for (current <- currentRename)
				segmentRenameEventSource fire current.id -> textBox.getText

			endRename
		}

		saveButton setDefaultButton true

		cancelButton setOnAction eventHandler { e: ActionEvent =>
			endRename
		}

		cancelButton setCancelButton true

		(hBox, textBox setText _)
	}

	private var currentRename: Option[SegmentRecordRow] = None

	private def beginRename(row: SegmentRecordRow, currentLabel: String) {
		endRename

		setupRenameControl(currentLabel)
		row.labelHBox setVisible false
		row.labelPane.getChildren add renameControl
		currentRename = Some(row)
	}

	private def endRename() {
		for (row <- currentRename) {
			row.labelHBox setVisible true
			row.labelPane.getChildren remove renameControl
			currentRename = None
		}
	}
}