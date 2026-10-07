# 리소스 안내

실행 리소스는 classpath에 포함되므로 코드에서는 `src/` 경로 대신 classpath 경로로 읽습니다.

- `story/stages.properties`: 9개 전투의 몬스터 정보와 난이도 변수
- `battle/balance.properties`: 공통 전투 수치
- `ai/profiles.properties`: AI 평가 가중치
- `ui/campus-rpg`, `ui/puzzle-rpg`, `ui/characters`, `ui/university`: 화면 이미지와 제작·출처 기록
- `audio/kenney`, `audio/university`: 효과음·배경음과 출처 기록

`assets.properties`와 `images/ui/placeholder.png`는 기존 `AssetManager` 계약·회귀 테스트를 위해 유지합니다. 실제 제품 화면의 새 이미지 목록과 출처는 각 이미지 폴더의 `README.md` 또는 `ASSETS.md`를 확인하세요.
