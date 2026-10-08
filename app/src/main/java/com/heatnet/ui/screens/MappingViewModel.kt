package com.heatnet.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heatnet.data.db.SessionRepository
import com.heatnet.data.model.Metric
import com.heatnet.data.model.Point
import com.heatnet.data.model.Reading
import com.heatnet.data.model.Session
import com.heatnet.measurement.model.ConnectionType
import com.heatnet.measurement.model.MeasurementStage
import com.heatnet.measurement.network.ConnectionSnapshot
import com.heatnet.ui.geometry.Geometry
import com.heatnet.ui.measure.MeasurementSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** F3 to F5: tap a position, press Measure, watch the dots and heatmap update. */
class MappingViewModel(
    private val sessionId: Long,
    private val repository: SessionRepository,
    private val connectionState: StateFlow<ConnectionSnapshot>,
    private val source: () -> MeasurementSource,
) : ViewModel() {
    var session by mutableStateOf<Session?>(null)
        private set
    var notFound by mutableStateOf(false)
        private set

    val readings: StateFlow<List<Reading>> =
        repository.observeReadings(sessionId).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    var metric by mutableStateOf(Metric.DOWNLOAD)
        private set

    /** The tapped spot (metres) that the next Measure will be saved at. */
    var position by mutableStateOf<Point?>(null)
        private set

    /** Non-null while a measurement is running. */
    var stage by mutableStateOf<MeasurementStage?>(null)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var selectedReadingId by mutableStateOf<Long?>(null)

    /** Set when the phone moved to a different connection type than the session's (PRD 5 edge case). */
    var switchedTo by mutableStateOf<ConnectionType?>(null)
        private set
    private var warningDismissed = false

    private var job: Job? = null

    val measuring get() = stage != null

    init {
        viewModelScope.launch {
            val loaded = repository.load(sessionId)
            if (loaded == null) {
                notFound = true
            } else {
                session = loaded.session
                onConnectionChanged(connectionState.value)
            }
        }
        viewModelScope.launch {
            connectionState.collect { snapshot -> onConnectionChanged(snapshot) }
        }
    }

    private fun onConnectionChanged(snapshot: ConnectionSnapshot) {
        val type = session?.connectionType ?: return
        val current = snapshot.connectionType
        when {
            source().isFake || !snapshot.hasTransportObservation || current == null -> Unit
            current == type -> {
                switchedTo = null
                warningDismissed = false
            }
            !warningDismissed -> switchedTo = current
        }
    }

    fun dismissSwitchWarning() {
        warningDismissed = true
        switchedTo = null
    }

    fun selectMetric(m: Metric) {
        metric = m
    }

    fun onMapTap(p: Point) {
        val s = session ?: return
        if (measuring) return
        if (Geometry.contains(s.outline, p)) {
            position = p
            message = null
        } else {
            message = "Tap inside the room outline."
        }
    }

    fun measure() {
        val s = session ?: return
        val p = position ?: return
        if (measuring) return
        stage = MeasurementStage.VERIFYING_NETWORK
        message = null
        job = viewModelScope.launch {
            try {
                val measurementSource = source()
                val result = measurementSource.measure(s.connectionType, p.x, p.y) { stage = it.stage }
                if (MeasureRules.shouldSave(result)) {
                    repository.addReading(s.id, p.x, p.y, result)
                    position = null
                }
                message = MeasureRules.message(result, s.connectionType)
            } catch (e: CancellationException) {
                message = "Measurement cancelled. Nothing was saved."
                throw e
            } catch (e: Exception) {
                message = "Something went wrong during the test. Nothing was saved."
            } finally {
                stage = null
            }
        }
    }

    fun cancelMeasure() {
        job?.cancel()
    }

    fun deleteReading(id: Long) {
        selectedReadingId = null
        viewModelScope.launch { repository.deleteReading(id) }
    }

    fun clearMessage() {
        message = null
    }
}
