# 시냅스케어 (SynapseCare)
> **"운전자별 최적 시트 포지션(높이·각도·거리·허리받침)을 소프트웨어로 맞춤 기억하고, 주행 안전 락과 정차 상태별(신호대기 자세조절 / 완전주차 릴랙스 마사지) 맞춤 편의를 제공하는 SDV 인카 웰니스 시스템"**

2026 현대자동차그룹 차량용 앱 개발 공모전 출품작  
- **참가자:** 윤두환
- **앱/프로젝트명:** 시냅스케어 (SynapseCare)
- **공모 분야:** 모빌리티 라이프 케어

---

## 1. 프로젝트 개요

**시냅스케어(SynapseCare)**는 하드웨어 버튼 1번/2번에 머물러 있던 메모리 시트의 한계를 넘어, **현대자동차그룹 플레오스 커넥트(Pleos Connect / AAOS) 기반으로 운전자별 최적의 시트 포지션을 정밀하게 기억하고 케어하는 차량용 네이티브 편의 시스템**입니다.

- **사용자별 스마트 메모리 시트 (Core):** 운전자 프로필별 의자 높이(Height), 등받이 각도(Recline), 전후 거리(Slide), 허리 받침(Lumbar)을 소프트웨어로 맞춤 기억 및 원터치 복원.
- **주행 안전 인터락 (Safety Interlock 100%):** 주행 중(Speed > 0) 어떠한 물리 시트 조작도 원천 차단(Lock). 주행 중 "불편해" 발화 시 정차 예약 큐 등록.
- **신호 대기 (잠깐 정차):** 시트 내장 체압 센서(골반 하중, 허리 밀착도)를 분석하여 출발에 방해되지 않는 선에서 가장 편안한 척추 각도로 미세 보정 (출발 안전을 위해 신호 대기 중 마사지 금지).
- **완전 주차/휴식 (P단):** 고속도로 쉼터, 휴게소, 목적지 도착 후 안전하게 차를 세워두고 쉴 때, 운전자가 원할 때 켜서 피로를 푸는 **에르고 모션(Ergo Motion) 공압 스트레칭 마사지** 지원.

---

## 2. 실행 방법

### 방법 A. 릴리즈 APK 직접 설치 (추천)
저장소의 `apk/` 디렉토리에 포함된 릴리즈 빌드를 안드로이드 기기 또는 차량 에뮬레이터에 설치합니다.

```bash
# adb를 통한 설치
adb install -r apk/윤두환_시냅스케어_release.apk
```

### 방법 B. 소스 코드 빌드
Android Studio에서 프로젝트를 열어 빌드 및 실행합니다.

```bash
# Gradle CLI를 통한 빌드
./gradlew assembleRelease
```

---

## 3. 테스트 환경 및 필요한 설정

* **타깃 플랫폼:** Pleos Connect OS (Android Automotive OS 기반) / Android 13+ (API 33 이상 권장)
* **테스트 권장 환경:**
  * **에뮬레이터:** Android Studio Automotive OS 에뮬레이터 (1024x768 또는 1920x1080 Landscape AVN 해상도)
  * **실기 기기:** Android 13 이상의 안드로이드 태블릿 또는 스마트폰 (가로 모드 지원)
* **필요 권한 (Permissions):**
  * `RECORD_AUDIO` (음성 인텐트 감지용, Gleo AI 환경 외 단독 테스트 시)
  * `VIRTUAL_VEHICLE_CONTROL` (Pleos Vehicle SDK 시트 제어 인터페이스)
* **SDK 구성:**
  * Kotlin 1.9+ / Jetpack Compose for Car
  * Pleos Connect SDK (Vehicle SDK, Gleo AI & TTS SDK)

---

## 4. APK 파일 위치

본 저장소 내 정규 제출 경로에 빌드된 실행 파일이 포함되어 있습니다.

* **APK 상대 경로:** [`./apk/윤두환_시냅스케어_release.apk`](./apk/윤두환_시냅스케어_release.apk)
* **파일명 규격:** `윤두환_시냅스케어_release.apk` (공모전 공식 파일명 명명 규칙 준수, 용량 6.49MB)
