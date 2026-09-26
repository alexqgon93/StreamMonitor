package com.alexqgon.streammonitor.feature.stream

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexqgon.streammonitor.domain.StreamCoordinatorFactory
import com.alexqgon.streammonitor.domain.StreamLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@HiltViewModel
class StreamViewModel @Inject constructor(
    private val coordinatorFactory: StreamCoordinatorFactory,
) : ViewModel() {
    private val mutableState = MutableStateFlow(StreamUiState())
    val state = mutableState.asStateFlow()

    /**
     * Serialises runs. A new run may only reset state and build its coordinator once the previous
     * one has released the lock, which happens after its flow — and therefore its polling and
     * computation coroutines — has fully torn down. Awaiting the previous [Job] alone is not enough:
     * a third rapid intent can cancel a run while it is still awaiting its own predecessor.
     */
    private val runLock = Mutex()
    private var streamJob: Job? = null

    fun onIntent(intent: StreamIntent) {
        when (intent) {
            StreamIntent.Start,
            StreamIntent.Restart,
            StreamIntent.Retry
            -> startStream()
        }
    }

    private fun startStream() {
        val previousRun = streamJob
        streamJob = viewModelScope.launch {
            previousRun?.cancelAndJoin()
            runLock.withLock {
                val seed = mutableState.value.startingNewRun()
                mutableState.value = seed
                coordinatorFactory.create().snapshots()
                    .runningFold(seed) { previous, snapshot ->
                        if (snapshot.lifecycle == StreamLifecycle.Idle && previous.hasSnapshot) {
                            previous
                        } else {
                            snapshot.toUiState(previous)
                        }
                    }
                    .drop(1)
                    .flowOn(Dispatchers.Default)
                    .collect { mappedState -> mutableState.value = mappedState }
            }
        }
    }
}
