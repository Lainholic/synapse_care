# -*- coding: utf-8 -*-
import sys
from pathlib import Path

# Add azure_worker to path
sys.path.append(r"C:\AI\tools\azure_worker")
import worker

def run_sol_audit():
    print("=== Starting Sol 6.1 (gpt-6.1-sol) Audit for SynapseCare ===")
    settings = worker.load_settings("gpt-6.1-sol")
    client = worker.AzureClient(settings)

    task_prompt = """[디렉터 환님의 작업 지시 — 솔(Sol 6.1) 하네스 정밀 적대적 감사 요청]
"하네스 돌려서 검증해봐"

환님께서 이전 1차 검수(루나 34점 / 딥시크 71점)에서 지적되었던 치명적 결함들을 100% 완치하고, 서비스의 본질(사용자 편의 시트 조절과 스마트 기억) 중심으로 전면 재설계한 최신 버전에 대해 최종 하네스 적대적 감사를 요청하셨습니다.

[완치 및 개선된 핵심 4대 설계]
1. 서비스 본질 확립 (시트 조절 & 스마트 기억):
   - 허리/마사지 곁가지 탈피 -> 사용자별 시트 4대 축(높이, 등받이 각도, 전후 슬라이드, 요추 받침) 정밀 조절 및 소프트웨어 프로필 영구 기억/복원.
   - 자연스러운 일상 편의 발화("불편해", "자세 좀 맞춰줘") 지원.
2. 체압 센서 & 인체공학 융합 지능:
   - "불편해" 발화 시 마법이 아닌, 시트 내장 체압 센서(골반 하중, 허리 밀착도) + 운전자 신장 기반 인체공학 척추 중립 기준값을 대조하여 최적 릴랙스 각도를 과학적으로 산출.
3. 3단계 주행 안전 인터락 (Safety Interlock 100%):
   - 🚗 주행 중 (D): 시트 물리 조작 100% 원천 차단 (UN R79 / ISO 26262 준수, 정차 시 예약).
   - 🚦 신호 대기 (D정차): 체압 맞춤 미세 자세 조절만 가볍게 확인 후 적용 (출발 안전을 위해 신호 대기 중 마사지 절대 금지).
   - 🅿️ 완전 주차/휴게소 휴식 (P단): 오직 차를 안전하게 세워두고 쉴 때만, 운전자가 원할 때 에르고 모션 힐링 마사지 가동.
4. 의료기기법 리스크 원천 차단:
   - 질병 진단/치료가 아닌 안전 운전 편의 웰니스 보조 서비스 명시 및 과장된 효능 수치("28% 경감" 등) 전면 영구 삭제.

[검수 파일 목록]
- C:/AI/synapse_care/기획서_접수양식_완성본.md
- C:/AI/synapse_care/app/src/main/java/com/synapse/care/PleosVehicleController.kt
- C:/AI/synapse_care/app/src/main/java/com/synapse/care/MainActivity.kt
- C:/AI/synapse_care/README.md

위 파일들을 직접 읽고 검증하여, 솔(Sol 6.1)의 냉정하고 객관적인 평가, 점수(100점 만점), 공모전 본선 진출 및 수상 가능성을 판정해 주십시오."""

    files = [
        r"C:\AI\synapse_care\기획서_접수양식_완성본.md",
        r"C:\AI\synapse_care\app\src\main\java\com\synapse\care\PleosVehicleController.kt",
        r"C:\AI\synapse_care\app\src\main\java\com\synapse\care\MainActivity.kt",
        r"C:\AI\synapse_care\README.md"
    ]

    result = worker.review(task_prompt, files, client, max_tokens=10000, max_turns=6)
    print("State:", result.get("state"))
    report = result.get("report", "")
    out_file = Path(r"C:\AI\synapse_care\sol_audit_result.md")
    out_file.write_text(report, encoding="utf-8")
    print(f"Report saved to {out_file} (length: {len(report)})")

if __name__ == "__main__":
    run_sol_audit()
