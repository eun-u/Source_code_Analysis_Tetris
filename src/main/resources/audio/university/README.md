# University_Simulation 차용 오디오

`C:\Project\University_Simulation\sound`의 원본 MP3 4개를 Windows Media Transcoder로 PCM WAV로 변환해 Java 8 `javax.sound.sampled`에서 재생할 수 있게 보존한다. 원본 MP3는 이 프로젝트에 복사하지 않았다. 변환 명령은 `scripts/convert-university-sfx.ps1`에 있다. 현재 `AudioService`가 재생하는 것은 `background.wav`뿐이다. 나머지 3개는 기존 자산과 출처 기록을 유지하되, 실제 게임 효과음은 [Kenney CC0 팩](../kenney/README.md)을 사용한다.

| 원본 | 원본 SHA-256 | 변환 WAV SHA-256 |
|---|---|---|
| `click.mp3` | `F8065731BE76B83A80E930A6820AD5ACD1C667CD0F95F21A4C293C92584A41A2` | `146B6B1526FEC865D2A3A5CF8D96084188776D3B2370F63548A72DA32003DF4A` |
| `nextlog.mp3` | `6D3406291215D5C3C404992113790AFE6F529E5FE2BF2FC1EAAE3CC09600462A` | `6C91BDC68829697B5F1A4CD606985599F42E668D84F98EBEF0ECE4D5D953AEF4` |
| `weekSummary.mp3` | `234A990EE382F6FA4D556848787F22A1DB8D01CC6708274E734DD25170C60A72` | `359C6DA0F0F12ED2FFB0957133B0B02F781917CC739C76FA461F82DD93D494C8` |
| `background.mp3` | `759852E8981C827856C561D09E682CC82E3E670EC92CF836F9F88D2667C35503` | `597F1DF55C100E38AEADD69384DC3883BC4A8D918636C83043DC5F11C89B8332` |

`background.wav`는 약 66.7초를 반복 재생한다. `AudioService`는 오디오를 백그라운드에서 초기화하고, 오디오 장치가 없으면 조용히 생략한다. 배경음과 효과음의 음소거/음량은 별도로 저장한다.

변환 스크립트는 Windows PowerShell 5.1/Windows Media Transcoder를 사용한다. 다른 운영체제에서는 이미 저장된 WAV가 바로 재생되며, MP3 디코더를 설치할 필요가 없다. 실제 스피커에서의 청감과 반복 지점의 자연스러움은 사용자 환경에서 확인이 필요하다.
