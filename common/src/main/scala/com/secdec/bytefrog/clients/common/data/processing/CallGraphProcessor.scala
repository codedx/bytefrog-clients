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

package com.secdec.bytefrog.clients.common.data.processing.processors

import scala.collection.mutable

import com.secdec.bytefrog.fileapi.data.CallGraphEdge
import com.secdec.bytefrog.fileapi.tracefile.TraceFile
import com.secdec.bytefrog.fileapi.tracefile.TraceFileBuilder
import com.secdec.bytefrog.hq.data.processing.DataProcessor
import com.secdec.bytefrog.hq.protocol.DataMessageContent

object CallGraphProcessor {
	def apply(output: TraceFileBuilder) = new CallGraphProcessor(output)
}

/** The CallGraphProcessor, based on the trace data stream, maintains a call stack for each thread, and
  * in turn produces a call graph for the execution of the program.
  *
  * @author robertf
  */
class CallGraphProcessor(output: TraceFileBuilder) extends DataProcessor {
	private val callStacks = mutable.Map[Int, mutable.Stack[Int]]()
	private val graphEdges = mutable.Map[(Int, Int), Long]() withDefaultValue 0
	private val writer = TraceFile.callGraphEntry.writer(output)

	// watch messages that affect callstack
	def processMessage(message: DataMessageContent) = message match {
		case DataMessageContent.MethodEntry(callee, _, thread) => handleMethodEntry(callee, thread)
		case DataMessageContent.MethodExit(method, _, _, thread) => handleMethodExit(method, thread)
		case DataMessageContent.ExceptionBubble(_, method, _, thread) => handleExceptionBubble(method, thread)
		case _ =>
	}

	def processDataBreak {
		// when a data break happens, our call stacks go out the window
		callStacks.clear
	}

	def finishProcessing() {
		// notice there are no assertions about the callstacks being empty - this is not a valid assertion to
		// make. for example, if a thread calls `System.exit` (which never returns, under normal
		// circumstances), it will be locked deeper in a callstack, and therefore the stack will not clear.

		// write out
		//val writer = TraceFile.callGraphEntry.writer(output)
		writer write graphEdges.map(edge => CallGraphEdge(edge._1._1, edge._1._2, edge._2)).toList
		writer.finish
	}

	def cleanup() {
		// nothing to do
	}

	/** Returns the callstack for a thread, constructing an empty one if necessary. */
	private def getCallStack(thread: Int) = callStacks.getOrElseUpdate(thread, mutable.Stack[Int]())

	private def popMethod(method: Int, thread: Int) {
		// we cannot assume anything, even here. if there is a data break, we'll be at some unknown point in
		// the callstack - at some point it will be empty as we're popping above that point. we can continue
		// on just fine, we just can't make any assumptions about our depth in the stack.

		val stack = getCallStack(thread)

		if (!stack.isEmpty) {
			val popped = stack.pop
			assert(popped == method)
		}
	}

	private def handleMethodEntry(callee: Int, thread: Int) {
		val stack = getCallStack(thread)

		// add a call to the graph edge between caller and callee, if the stack isn't empty
		for (caller <- stack.headOption) graphEdges(caller, callee) += 1

		// push the new method on the stack
		stack.push(callee)
	}

	private def handleMethodExit(method: Int, thread: Int) {
		// we could track if the thread just bubbled an exception, and if so, use that as a detection
		// mechanism to tell us if this method caught the exception that's been bubbling, if this is deemed
		// to be useful info
		popMethod(method, thread)
	}

	private def handleExceptionBubble(method: Int, thread: Int) {
		// exception was unhandled by method and bubbled up the stack
		popMethod(method, thread)
	}
}