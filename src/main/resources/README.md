# 리소스 계약

실행 시 `AssetManager`는 classpath의 `assets.properties`에서 ID를 해석합니다. JAR 안에서도 같은 방식으로 동작하며 `src/` 파일 경로를 사용하지 않습니다. 현재 모든 ID는 공유 placeholder PNG를 참조합니다. 미등록/누락/잘못된 이미지는 Java2D 대체 이미지와 ID별 1회 경고로 처리합니다. 원본과 표시 크기는 별도로 캐시하며 UI에서 비율을 유지해 축소합니다.

이미지는 `images/background`, `character`, `monster/normal`, `monster/elite`, `monster/boss`, `block`, `item`, `icon`, `effect`, `ui`에 둡니다. 원본 권장 크기: 일반 몬스터 256×256, 엘리트 384×384, 보스/캐릭터 512×512, 아이템/블록 64×64, 아이콘 48×48, 배경 1920×1080. 효과는 투명 PNG/스프라이트시트입니다. 표시 크기마다 원본을 복제하지 않습니다.

`sound/bgm`, `sound/sfx`, `fonts`는 후속 리소스 위치입니다. 실제 BGM/SFX/Theme 동작은 아직 구현하지 않았습니다. SoundManager는 추후 GameEvent를 구독하며 core는 오디오 API를 호출하지 않습니다.
