package com.synapse.care

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PainEventLog(
    val timestamp: String,
    val symptom: String,
    val drivingMinutes: Int,
    val appliedCorrection: String
)

class HealthMemoryStore {

    private val _logs = MutableStateFlow<List<PainEventLog>>(
        listOf(
            PainEventLog(
                timestamp = "2026-10-21 17:40",
                symptom = "요추 피로 및 뻐근함",
                drivingMinutes = 75,
                appliedCorrection = "요추 지지대 +25% 전진 보정"
            ),
            PainEventLog(
                timestamp = "2026-10-20 08:50",
                symptom = "골반 압박감",
                drivingMinutes = 50,
                appliedCorrection = "쿠션 틸트 +4도 조정"
            )
        )
    )
    val logs: StateFlow<List<PainEventLog>> = _logs.asStateFlow()

    fun recordEvent(symptom: String, drivingMinutes: Int, correction: String) {
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA).format(Date())
        val newEntry = PainEventLog(
            timestamp = now,
            symptom = symptom,
            drivingMinutes = drivingMinutes,
            appliedCorrection = correction
        )
        _logs.value = listOf(newEntry) + _logs.value
    }
}
