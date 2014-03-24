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

import java.util.Collections

import scala.collection.JavaConversions.asScalaBuffer

import javafx.beans.property.ReadOnlyListWrapper
import javafx.beans.property.ReadOnlyProperty
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList

/** A list wrapper for package lists that maintains a copy in chronological
  * order as well as a copy in alphabetical sort order. This facilitates
  * fast switching/binding between the two in the UI.
  *
  * @author robertf
  */
class PackageList(packageList: ObservableList[String]) extends JavaFXUtilities {
	protected val naturalSortList = new ReadOnlyListWrapper[String](packageList)
	protected val abcSortList = new ReadOnlyListWrapper[String](FXCollections.observableArrayList[String])

	def naturalSort: ReadOnlyProperty[ObservableList[String]] = naturalSortList.getReadOnlyProperty
	def abcSort: ReadOnlyProperty[ObservableList[String]] = abcSortList.getReadOnlyProperty

	packageList addListener listChangeListener { change: ListChangeListener.Change[_ <: String] =>
		while (change.next) {
			if (change.wasPermutated || change.wasUpdated)
				throw new NotImplementedError

			for (removed <- change.getRemoved) {
				val itemLocation = Collections.binarySearch(abcSortList, removed)

				if (itemLocation >= 0)
					abcSortList.remove(itemLocation)
			}

			for (added <- change.getAddedSubList) {
				val insertLocation = -(Collections.binarySearch(abcSortList, added)) - 1

				if (insertLocation >= 0)
					abcSortList.add(insertLocation, added)
			}
		}
	}
}