package com.synapse.care

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class VehicleDriveMode {
    DRIVING,          // 🚗 주행 중 (물리 시트 일체 조작 무시/차단)
    TEMPORARY_STOP,   // 🚦 신호 대기 (D정차: 체압 맞춤 자세 미세 조절만 가능)
    PARKED_REST       // 🅿️ 완전 주차 휴식 (P단: 에르고 모션 마사지 가능)
}

data class SeatPressureDistribution(
    val pelvisPressureKpa: Float = 4.2f,   // 골반 하중
    val lumbarContactRatio: Float = 0.55f, // 허리 밀착도
    val upperBackPressureKpa: Float = 2.8f
)

data class UserSeatProfile(
    val profileName: String = "윤두환 (운전자 #1)",
    val driverHeightCm: Int = 175,
    val seatHeightMm: Int = 45,         // 의자 높이 (0 ~ 100 mm)
    val reclineAngleDeg: Float = 102f,  // 등받이 각도 (90도 ~ 125도)
    val slidePositionMm: Int = 60,      // 전후 거리 (0 ~ 100 mm)
    val lumbarLevel: Int = 35           // 허리 받침 (0% ~ 100%)
)

data class VehicleSeatState(
    val selectedProfileName: String = "윤두환 (운전자 #1)",
    val currentProfile: UserSeatProfile = UserSeatProfile(),
    val availableProfileNames: List<String> = listOf("윤두환 (운전자 #1)", "배우자 (운전자 #2)", "게스트 (운전자 #3)"),
    val pressure: SeatPressureDistribution = SeatPressureDistribution(),
    val driveMode: VehicleDriveMode = VehicleDriveMode.PARKED_REST,
    val isErgoMotionActive: Boolean = false,
    val pendingAdjustmentPrompt: String? = null,
    val systemStatusNotice: String = "P단 완전 주차 상태 · 프로필 맞춤 시트 대기"
)

sealed class SeatActionResponse {
    data class Success(val message: String) : SeatActionResponse()
    data class BlockedDuringDriving(val warning: String) : SeatActionResponse()
    data class ConfirmationRequired(val question: String, val plannedAdjustment: String) : SeatActionResponse()
}

/**
 * 시냅스케어 (SynapseCare) 핵심 컨트롤러
 * 1. 프로필 선택 & 저장: 사용자가 원하는 프로필을 직접 선택하고 자유롭게 저장/불러오기
 * 2. 주행 중 철칙: 주행 중 시트 조작은 복잡한 예약 없이 '그냥 무시/차단'
 * 3. 마사지 안전: 오직 P단 완전 주차에서만 가능하며, P단에서 벗어나면 무조건 즉시 강제 종료
 * 4. 체압 웰니스: 정차 상태에서 "불편해" 발화 시 체압 센서 분석 후 승인 하 미세 조절
 */
class PleosVehicleController {

    private val _seatState = MutableStateFlow(VehicleSeatState())
    val seatState: StateFlow<VehicleSeatState> = _seatState.asStateFlow()

    // 다중 사용자 프로필 맵
    private val profilesMap = mutableMapOf<String, UserSeatProfile>(
        "윤두환 (운전자 #1)" to UserSeatProfile("윤두환 (운전자 #1)", 175, 45, 102f, 60, 35),
        "배우자 (운전자 #2)" to UserSeatProfile("배우자 (운전자 #2)", 163, 60, 98f, 40, 20),
        "게스트 (운전자 #3)" to UserSeatProfile("게스트 (운전자 #3)", 170, 50, 100f, 50, 25)
    )

    /**
     * 주행 모드 전환 (P단 이탈 시 마사지 무조건 즉시 종료)
     */
    fun setDriveMode(mode: VehicleDriveMode) {
        val wasActive = _seatState.value.isErgoMotionActive
        // 핵심 안전 가드: P단이 아니면 마사지는 무조건 false!
        val stillActive = (mode == VehicleDriveMode.PARKED_REST) && wasActive

        val notice = when (mode) {
            VehicleDriveMode.DRIVING -> "주행 중 (D) · 안전 인터락 가동 (시트 조작 원천 차단)"
            VehicleDriveMode.TEMPORARY_STOP -> "신호 대기 (D정차) · 체압 맞춤 자세 미세 조절 가능"
            VehicleDriveMode.PARKED_REST -> "P단 완전 주차/휴식 · 에르고 모션 힐링 마사지 가능"
        }

        _seatState.value = _seatState.value.copy(
            driveMode = mode,
            isErgoMotionActive = stillActive,
            pendingAdjustmentPrompt = if (mode == VehicleDriveMode.DRIVING) null else _seatState.value.pendingAdjustmentPrompt,
            systemStatusNotice = notice
        )
    }

    /**
     * 사용자가 프로필을 직접 선택하여 전환
     */
    fun selectProfile(profileName: String): SeatActionResponse {
        if (_seatState.value.driveMode == VehicleDriveMode.DRIVING) {
            return SeatActionResponse.BlockedDuringDriving("주행 중에는 안전을 위해 프로필을 변경할 수 없습니다.")
        }
        val target = profilesMap[profileName] ?: return SeatActionResponse.BlockedDuringDriving("프로필을 찾을 수 없습니다.")
        _seatState.value = _seatState.value.copy(
            selectedProfileName = profileName,
            currentProfile = target,
            systemStatusNotice = "'${profileName}' 님의 맞춤 시트 포지션 적용 완료"
        )
        return SeatActionResponse.Success("'${profileName}' 님의 시트 세팅으로 조절되었습니다.")
    }

    /**
     * 현재 시트 설정을 선택된 프로필에 저장
     */
    fun saveCurrentProfile(height: Int, recline: Float, slide: Int, lumbar: Int) {
        val currentName = _seatState.value.selectedProfileName
        val existing = profilesMap[currentName] ?: UserSeatProfile(profileName = currentName)
        val updated = existing.copy(
            seatHeightMm = height,
            reclineAngleDeg = recline,
            slidePositionMm = slide,
            lumbarLevel = lumbar
        )
        profilesMap[currentName] = updated
        _seatState.value = _seatState.value.copy(
            currentProfile = updated,
            systemStatusNotice = "'${currentName}' 님의 시트 세팅이 저장되었습니다."
        )
    }

    /**
     * "불편해 / 자세 좀 맞춰줘" 음성 발화 처리
     * - 주행 중: 그냥 무시/차단
     * - 정차 중: 체압 센서 분석 후 승인 카드 노출
     */
    fun handleDiscomfortVoice(): SeatActionResponse {
        // 주행 중일 때는 복잡한 예약 없이 깔끔하게 차단!
        if (_seatState.value.driveMode == VehicleDriveMode.DRIVING) {
            return SeatActionResponse.BlockedDuringDriving("주행 중에는 안전을 위해 시트 조작이 불가능합니다.")
        }

        val p = _seatState.value.pressure
        val cur = _seatState.value.currentProfile

        val optimalLumbar = if (p.lumbarContactRatio < 0.6f) (cur.lumbarLevel + 15).coerceAtMost(75) else cur.lumbarLevel
        val optimalRecline = if (p.pelvisPressureKpa > 3.8f) (cur.reclineAngleDeg + 2.0f).coerceAtMost(115f) else cur.reclineAngleDeg
        val optimalHeight = if (cur.driverHeightCm > 170) (cur.seatHeightMm - 5).coerceAtLeast(20) else cur.seatHeightMm

        val planText = "체압 분석: 등받이 %.1f°, 요추 받침 %d%%, 높이 %dmm 추천".format(optimalRecline, optimalLumbar, optimalHeight)

        _seatState.value = _seatState.value.copy(
            pendingAdjustmentPrompt = planText,
            systemStatusNotice = "체압 분석 완료 · 사용자 승인 대기 중"
        )

        return SeatActionResponse.ConfirmationRequired("체압 분석에 맞춰 시트를 편안한 자세로 조정할까요?", planText)
    }

    /**
     * 사용자가 "응, 맞춰줘" 승인 시 미세 조절 적용
     */
    fun applyAdjustment(): SeatActionResponse {
        if (_seatState.value.driveMode == VehicleDriveMode.DRIVING) {
            return SeatActionResponse.BlockedDuringDriving("차량이 움직여 조절이 중단되었습니다.")
        }
        val cur = _seatState.value.currentProfile
        val updated = cur.copy(
            reclineAngleDeg = (cur.reclineAngleDeg + 2.0f).coerceAtMost(115f),
            lumbarLevel = (cur.lumbarLevel + 15).coerceAtMost(75)
        )
        _seatState.value = _seatState.value.copy(
            currentProfile = updated,
            pendingAdjustmentPrompt = null,
            systemStatusNotice = "체압 맞춤 최적 시트 포지션 적용 완료"
        )
        return SeatActionResponse.Success("가장 편안한 맞춤 시트 포지션으로 조절되었습니다.")
    }

    /**
     * 에르고 모션 힐링 마사지: 오직 P단 완전 주차 상태에서만 허용
     */
    fun startRestingMassage(): SeatActionResponse {
        if (_seatState.value.driveMode != VehicleDriveMode.PARKED_REST) {
            return SeatActionResponse.BlockedDuringDriving("마사지는 출발 위험이 없는 P단 완전 주차 상태에서만 가능합니다.")
        }
        _seatState.value = _seatState.value.copy(
            isErgoMotionActive = true,
            systemStatusNotice = "P단 완전 주차 휴식 · 에르고 모션 릴랙스 마사지 작동 중"
        )
        return SeatActionResponse.Success("에르고 모션 릴랙스 마사지를 시작합니다. 편안하게 휴식하세요.")
    }

    fun stopRestingMassage() {
        _seatState.value = _seatState.value.copy(
            isErgoMotionActive = false,
            systemStatusNotice = "에르고 모션 마사지 종료"
        )
    }
}
