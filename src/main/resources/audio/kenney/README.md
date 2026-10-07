# 퍼즐·전투 효과음 출처

조작음은 짧게, 줄 제거·아이템·전투 결과는 서로 다른 음색으로 구분했다. 소스는 Kenney의 다음 원본 팩이며, 각 ZIP의 `License.txt`에서 Creative Commons Zero(CC0)를 확인했다. 테트리스 상표의 원작 효과음은 사용하지 않았다.

| 공식 팩 | 원본 ZIP SHA-256 | 사용 분야 |
|---|---|---|
| [UI Audio](https://kenney.nl/assets/ui-audio) | `946FC23A63D535D693EB31B2EABB80C8C28D6351E2186B344CEB71B2CB1D5EB6` | 버튼·블록 이동·회전 |
| [Impact Sounds](https://kenney.nl/assets/impact-sounds) | `029D734AF1582474EDF3A694D1B0CEBC97C1C152F2F39FA34D4C2BAFC5DE77F8` | 착지·피격 |
| [Digital Audio](https://kenney.nl/assets/digital-audio) | `24E6CE28B76A6D8C89CFF4D331E0965FF5C3DE8A73C612028E9D363CC64E4F06` | 줄 제거·공격·아이템·승패 |

| 게임 이벤트 | 원본 OGG (`Audio/` 아래) | 포함된 WAV |
|---|---|---|
| 버튼 | UI `click3.ogg` | `button.wav` |
| 블록 이동 | UI `click1.ogg` | `move.wav` |
| 블록 회전 | UI `switch12.ogg` | `rotate.wav` |
| 블록 착지 | Impact `impactGeneric_light_000.ogg` | `drop.wav` |
| 줄 제거 | Digital `threeTone1.ogg` | `line-clear.wav` |
| 공격 | Digital `phaserDown1.ogg` | `attack.wav` |
| 피격 | Impact `impactMetal_heavy_000.ogg` | `hit.wav` |
| 아이템 획득 | Digital `powerUp5.ogg` | `item-acquire.wav` |
| 아이템 사용 | Digital `phaseJump2.ogg` | `item-use.wav` |
| 회복 | Digital `powerUp2.ogg` | `heal.wav` |
| 피버 | Digital `highUp.ogg` | `fever.wav` |
| 승리 | Digital `powerUp1.ogg` | `victory.wav` |
| 패배 | Digital `lowThreeTone.ogg` | `defeat.wav` |

Kenney 원본은 OGG다. 기본 Java 8 `javax.sound.sampled`가 OGG 디코딩을 보장하지 않아 `scripts/prepare-kenney-sfx.py`로 단일 채널 16비트 PCM WAV로 변환해 포함했다. 변환 시 이벤트별 출력 음량과 끝부분의 짧은 페이드를 적용했다. 원본 ZIP은 저장소에 포함하지 않는다. 재생은 `AudioService`의 기존 비동기 풀을 사용하고, 배경음과 효과음 음량·음소거는 별도로 저장한다.

재생성: `python -m pip install soundfile numpy` 후 세 공식 ZIP을 내려받아 `python scripts/prepare-kenney-sfx.py --ui kenney_ui-audio.zip --impact kenney_impact-sounds.zip --digital kenney_digital-audio.zip`. 스크립트는 ZIP SHA-256과 CC0 문구를 확인한 뒤 변환한다. 실제 스피커 청감과 재생 장치 호환성은 사용자 환경에서 별도 확인이 필요하다.
