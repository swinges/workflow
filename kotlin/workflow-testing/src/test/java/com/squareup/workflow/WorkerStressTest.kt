package com.squareup.workflow

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.flow.reduce
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class WorkerStressTest {

  @UseExperimental(ExperimentalCoroutinesApi::class, FlowPreview::class)
  @Test fun `multiple subscriptions to single channel when closed`() {
    val channel = Channel<Unit>()
    val workers = List(100) { channel.asWorker() }
    val finishedWorkers = List(100) {
      channel.asWorker()
          .transform { it.onCompletion { emit(Unit) } }
    }
    val action = action<Nothing, Unit> { setOutput(Unit) }
    val workflow = Workflow.stateless<Unit, Unit, Unit> {
      // Run lots of workers that will all see the same close event.
      workers.forEachIndexed { i, worker ->
        runningWorker(worker, key = i.toString()) {
          fail("Expected non-finishing worker $i not to emit.")
        }
      }
      finishedWorkers.forEachIndexed { i, worker ->
        runningWorker(worker, key = "finished $i") { action }
      }
    }

    runBlocking {
      val outputs = launchWorkflowIn(this, workflow, flowOf(Unit)) {
        // Collect to a channel immediately so that we don't miss any outputs.
        it.outputs.produceIn(this)
      }

      // This should just work, and the test will finish, but this is broken by
      // https://github.com/Kotlin/kotlinx.coroutines/issues/1584 and will crash instead if
      // receiveOrClosed is used.
      channel.close()

      outputs.consumeAsFlow()
          .take(100)
          .collect()

      // Cancel the runtime so the test can finish.
      coroutineContext.cancelChildren()
    }
  }

  @UseExperimental(ExperimentalCoroutinesApi::class, FlowPreview::class)
  @Test fun `multiple subscriptions to single channel when emits`() {
    val channel = ConflatedBroadcastChannel(Unit)
    val workers = List(100) { channel.asWorker() }
    val action = action<Nothing, Int> { setOutput(1) }
    val workflow = Workflow.stateless<Unit, Int, Unit> {
      // Run lots of workers that will all see the same conflated channel value.
      workers.forEachIndexed { i, worker ->
        runningWorker(worker, key = i.toString()) { action }
      }
    }

    runBlocking {
      val outputs = launchWorkflowIn(this, workflow, flowOf(Unit)) {
        it.outputs.produceIn(this)
      }
      val sum = outputs.consumeAsFlow()
          .take(100)
          .reduce { sum, value -> sum + value }
      assertEquals(100, sum)

      // Cancel the runtime so the test can finish.
      coroutineContext.cancelChildren()
    }
  }
}
