package com.synapse.care

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    private val vehicleController = PleosVehicleController()
    private val memoryStore = HealthMemoryStore()
    private lateinit var ttsManager: TtsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ttsManager = TtsManager(this)

        setContent {
            SynapseCareTheme {
                DashboardScreen(
                    vehicleController = vehicleController,
                    memoryStore = memoryStore,
                    onVoiceCommand = { phrase ->
                        handleVoiceCommand(phrase)
                    },
                    onSelectProfile = { name ->
                        val res = vehicleController.selectProfile(name)
                        if (res is SeatActionResponse.Success) {
                            ttsManager.speak(res.message)
                        } else if (res is SeatActionResponse.BlockedDuringDriving) {
                            ttsManager.speak(res.warning)
                        }
                    },
                    onConfirmAdjustment = {
                        val res = vehicleController.applyAdjustment()
                        if (res is SeatActionResponse.Success) {
                            ttsManager.speak(res.message)
                            memoryStore.recordEvent(
                                symptom = "체압 분석 맞춤 조절",
                                drivingMinutes = 30,
                                correction = "신호 대기 중 체압 맞춤 자세 최적화 완료"
                            )
                        } else if (res is SeatActionResponse.BlockedDuringDriving) {
                            ttsManager.speak(res.warning)
                        }
                    },
                    onToggleMassage = {
                        val s = vehicleController.seatState.value
                        if (s.isErgoMotionActive) {
                            vehicleController.stopRestingMassage()
                            ttsManager.speak("마사지를 종료합니다.")
                        } else {
                            val res = vehicleController.startRestingMassage()
                            if (res is SeatActionResponse.Success) {
                                ttsManager.speak(res.message)
                                memoryStore.recordEvent(
                                    symptom = "사용자 휴식 마사지 요청",
                                    drivingMinutes = 0,
                                    correction = "P단 완전 주차 릴랙스 마사지 작동"
                                )
                            } else if (res is SeatActionResponse.BlockedDuringDriving) {
                                ttsManager.speak(res.warning)
                            }
                        }
                    },
                    onDriveModeChange = { mode ->
                        vehicleController.setDriveMode(mode)
                    }
                )
            }
        }
    }

    private fun handleVoiceCommand(phrase: String) {
        when {
            phrase.contains("불편") || phrase.contains("자세") -> {
                when (val res = vehicleController.handleDiscomfortVoice()) {
                    is SeatActionResponse.BlockedDuringDriving -> {
                        // 주행 중일 때는 그냥 안전 차단 안내 후 무시!
                        ttsManager.speak(res.warning)
                    }
                    is SeatActionResponse.ConfirmationRequired -> {
                        ttsManager.speak(res.question)
                    }
                    is SeatActionResponse.Success -> {
                        ttsManager.speak(res.message)
                    }
                }
            }
            phrase.contains("내 세팅") || phrase.contains("불러와") -> {
                val current = vehicleController.seatState.value.selectedProfileName
                val res = vehicleController.selectProfile(current)
                if (res is SeatActionResponse.Success) {
                    ttsManager.speak(res.message)
                } else if (res is SeatActionResponse.BlockedDuringDriving) {
                    ttsManager.speak(res.warning)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
    }
}

@Composable
fun SynapseCareTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF090D16),
            surface = Color(0xFF111827),
            primary = Color(0xFF00E5FF),
            secondary = Color(0xFF10B981)
        ),
        content = content
    )
}

@Composable
fun DashboardScreen(
    vehicleController: PleosVehicleController,
    memoryStore: HealthMemoryStore,
    onVoiceCommand: (String) -> Unit,
    onSelectProfile: (String) -> Unit,
    onConfirmAdjustment: () -> Unit,
    onToggleMassage: () -> Unit,
    onDriveModeChange: (VehicleDriveMode) -> Unit
) {
    val seatState by vehicleController.seatState.collectAsState()
    val logs by memoryStore.logs.collectAsState()
    val profile = seatState.currentProfile
    val pressure = seatState.pressure
    val driveMode = seatState.driveMode

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // [좌측]: 사용자별 시트 조절 & 프로필 선택 메모리 (Core)
        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "시냅스케어 (SynapseCare)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "사용자 편의 맞춤 좌석 조절 & 프로필 메모리",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))

                // 사용자 프로필 직접 선택기
                Text(text = "👤 사용자 프로필 선택:", fontSize = 11.sp, color = Color.LightGray)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    seatState.availableProfileNames.forEach { pName ->
                        val isSelected = pName == seatState.selectedProfileName
                        Button(
                            onClick = { onSelectProfile(pName) },
                            modifier = Modifier.weight(1f).height(34.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF1F2937)
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(1.dp)
                        ) {
                            Text(
                                text = pName.split(" ")[0],
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.Black else Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                SeatMetricRow("의자 높이 (Height)", "${profile.seatHeightMm} mm")
                SeatMetricRow("등받이 각도 (Recline)", "%.1f °".format(profile.reclineAngleDeg))
                SeatMetricRow("전후 거리 (Slide)", "${profile.slidePositionMm} mm")
                SeatMetricRow("허리 받침 (Lumbar)", "${profile.lumbarLevel} %")

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        vehicleController.saveCurrentProfile(
                            profile.seatHeightMm,
                            profile.reclineAngleDeg,
                            profile.slidePositionMm,
                            profile.lumbarLevel
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("💾 현재 세팅을 '${seatState.selectedProfileName.split(" ")[0]}'에 기억하기", fontSize = 11.sp, color = Color.White)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .padding(8.dp)
            ) {
                Text(
                    text = "▶ ${seatState.systemStatusNotice}",
                    fontSize = 11.sp,
                    color = if (driveMode == VehicleDriveMode.DRIVING) Color(0xFFF87171) else Color(0xFF38BDF8)
                )
            }
        }

        // [중앙]: 주행 안전 인터락 & 스마트 편의 음성 인터랙션
        Column(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "차량 주행 상태 & 음성 편의 케어",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "주행 중 100% 락 · 정차 시 미세 조절 · P단 주차 시 힐링 마사지",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))

                // 주행 모드 선택 버튼
                Text(text = "차량 주행 상태 (시뮬레이터):", fontSize = 11.sp, color = Color.LightGray)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { onDriveModeChange(VehicleDriveMode.DRIVING) },
                        modifier = Modifier.weight(1f).height(38.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (driveMode == VehicleDriveMode.DRIVING) Color(0xFFDC2626) else Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(2.dp)
                    ) {
                        Text("🚗 주행(D)", fontSize = 10.sp)
                    }
                    Button(
                        onClick = { onDriveModeChange(VehicleDriveMode.TEMPORARY_STOP) },
                        modifier = Modifier.weight(1.2f).height(38.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (driveMode == VehicleDriveMode.TEMPORARY_STOP) Color(0xFF0284C7) else Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(2.dp)
                    ) {
                        Text("🚦 신호대기(D정차)", fontSize = 10.sp)
                    }
                    Button(
                        onClick = { onDriveModeChange(VehicleDriveMode.PARKED_REST) },
                        modifier = Modifier.weight(1.3f).height(38.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (driveMode == VehicleDriveMode.PARKED_REST) Color(0xFF059669) else Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(2.dp)
                    ) {
                        Text("🅿️ 주차휴식(P단)", fontSize = 10.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 음성 발화 명령 버튼
                Button(
                    onClick = { onVoiceCommand("불편해") },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("🗣️ \"불편해 / 자세 좀 맞춰줘\" (음성 발화)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 에르고 모션 마사지: 오직 완전 주차(P단) 상태에서만 활성화!
                val isParked = driveMode == VehicleDriveMode.PARKED_REST
                Button(
                    onClick = onToggleMassage,
                    enabled = isParked,
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (seatState.isErgoMotionActive) Color(0xFF7C3AED) else Color(0xFF334155),
                        disabledContainerColor = Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (!isParked) "💆 마사지는 P단 완전 주차 휴식 시에만 가능"
                        else if (seatState.isErgoMotionActive) "💆 릴랙스 마사지 끄기"
                        else "💆 휴식 에르고 모션 마사지 켜기 (사용자 선택)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isParked) Color.White else Color.Gray
                    )
                }

                // 정차 시 승인 안내 카드 (F-02 결함 완치)
                if (driveMode != VehicleDriveMode.DRIVING && seatState.pendingAdjustmentPrompt != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "✨ 체압 분석 최적 각도 산출 완료",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6EE7B7)
                            )
                            Text(
                                text = seatState.pendingAdjustmentPrompt ?: "",
                                fontSize = 10.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = onConfirmAdjustment,
                                    modifier = Modifier.weight(1f).height(32.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                ) {
                                    Text("응, 지금 맞춰줘", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { onDriveModeChange(driveMode) },
                                    modifier = Modifier.weight(1f).height(32.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF374151))
                                ) {
                                    Text("다음에", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                } else if (driveMode == VehicleDriveMode.DRIVING) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF450A0A)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "🛡️ 주행 안전 잠금: 주행 중에는 조작이 원천 차단됩니다. (정차 후 음성으로 말씀해 주세요)",
                            fontSize = 10.sp,
                            color = Color(0xFFFCA5A5),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }

            Text(
                text = "※ 본 앱은 안전 주행 환경을 보조하는 운전자 편의 웰니스 시스템입니다.",
                fontSize = 10.sp,
                color = Color.DarkGray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        // [우측]: 체압 센서 상태 & 시트 조절 로그
        Column(
            modifier = Modifier
                .weight(1.0f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(18.dp)
        ) {
            Text(
                text = "체압 센서 & 조절 로그",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1F2937))
                    .padding(8.dp)
            ) {
                Column {
                    Text(text = "압력 센서 매트 실시간 감지:", fontSize = 11.sp, color = Color.LightGray)
                    Text(text = "• 골반 하중: %.1f kPa".format(pressure.pelvisPressureKpa), fontSize = 10.sp, color = Color(0xFF38BDF8))
                    Text(text = "• 허리 밀착도: %d %%".format((pressure.lumbarContactRatio * 100).toInt()), fontSize = 10.sp, color = Color(0xFF34D399))
                    Text(text = "• 등받이 지지: %.1f kPa".format(pressure.upperBackPressureKpa), fontSize = 10.sp, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { log ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B))
                            .padding(8.dp)
                    ) {
                        Text(text = log.symptom, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = log.appliedCorrection, fontSize = 10.sp, color = Color(0xFF38BDF8))
                        Text(text = log.timestamp, fontSize = 9.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun SeatMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = Color(0xFF94A3B8))
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
