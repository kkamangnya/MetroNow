# MetroNow

MetroNow는 선택한 서울 지하철 노선·역을 기준으로 양방향 실시간 도착정보를 앱과 Android 홈 화면 위젯에서 빠르게 확인하는 Kotlin 앱입니다. 서울 열린데이터광장의 `realtimeStationArrival` API를 사용하며, 앱 화면과 위젯은 API DTO가 아닌 도메인 모델만 사용합니다.

## 주요 기능

- 서울 지하철 1~9호선과 노선별 역 검색
- 2호선 내선/외선, 일반 노선 상행/하행 및 실제 진행 방면 안내
- 이름·노선·역·방향으로 구성된 여러 프리셋 저장
- 프리셋별 실시간 도착정보, 도착 상태, 보정된 남은 시간 표시
- 한 위젯에서 양방향 열차·방면·진행 위치를 동시에 표시
- Small/Medium/Large 크기에 대응하는 반투명 Glance 위젯
- `appWidgetId`마다 서로 다른 프리셋 연결
- 위젯 수동 새로고침과 WorkManager 주기 갱신
- API 키 누락, 네트워크 오류, 빈 결과, 오래된 데이터 처리
- 시스템/라이트/다크 앱 테마 및 위젯 표시 옵션
- API 키가 없어도 사용할 수 있는 독립된 Compose Preview 데이터

## 개발 환경

- Android Studio 최신 안정 버전
- JDK 17 이상(Android Studio 내장 JBR 사용 가능)
- Android SDK 36
- minSdk 26 / targetSdk 36
- Gradle Kotlin DSL

Windows PowerShell에서 빌드:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat :app:assembleDebug
```

연결된 에뮬레이터 또는 기기에 설치:

```powershell
.\gradlew.bat :app:installDebug
```

디버그 APK는 `app/build/outputs/apk/debug/app-debug.apk`에 생성됩니다.

## API 키 설정

1. [서울시 지하철 실시간 도착정보 데이터셋](https://data.seoul.go.kr/dataList/OA-12764/F/1/datasetView.do)에서 지하철 Open API 인증키를 신청합니다.
2. MetroNow 첫 실행 온보딩의 `서울시 API 키`에 인증키를 입력합니다.
3. 나중에 변경하려면 `설정 > 실시간 데이터 > 서울시 API 키`를 엽니다.

키는 소스 코드나 `local.properties`에 하드코딩되지 않고 앱의 Preferences DataStore에 저장됩니다. 설정 목록에는 원문 대신 마스킹된 값만 표시됩니다. 현재 방식은 기기 내 일반 앱 저장소이며 별도 암호화 저장소는 아닙니다.

## 서울 지하철 API 연결

MetroNow가 호출하는 공식 API 형식:

```text
http://swopenapi.seoul.go.kr/api/subway/{KEY}/json/realtimeStationArrival/0/20/{역명}
```

사용 필드:

- `subwayId`, `updnLine`: 노선·방향 필터
- `trainLineNm`, `btrainNo`, `btrainSttus`: 행선지·열차 정보
- `barvlDt`: 도착 예정 초
- `arvlMsg2`, `arvlMsg3`, `arvlCd`: 운행 상태와 상대 위치
- `recptnDt`: 데이터 생성 시각과 남은 시간 보정

`MetroRemoteDataSource` 인터페이스가 Retrofit 구현을 감싸므로, 추후 서버 프록시나 다른 키 주입 방식으로 교체할 수 있습니다. 공식 서버가 HTTP 엔드포인트를 제공하므로 cleartext 예외는 `swopenapi.seoul.go.kr` 도메인에만 허용했습니다.

개발 확인에는 서울시의 `sample` 키로 실제 JSON 응답 경로를 점검할 수 있지만, 반환 건수가 제한되어 선택한 노선의 열차가 없을 수 있습니다. 실제 사용에는 발급받은 인증키가 필요합니다.

## 위젯 추가

1. 앱에서 프리셋을 하나 이상 저장합니다.
2. 홈 화면의 `홈 화면에 MetroNow 위젯 추가`를 누르거나 런처의 위젯 선택기에서 MetroNow를 선택합니다.
3. 구성 화면에서 이 위젯에 사용할 프리셋을 고릅니다.
4. 다른 MetroNow 위젯을 추가해 별도의 프리셋을 선택할 수 있습니다.

일부 런처는 고정 직후 구성 화면을 자동으로 열지 않습니다. 이 경우 미설정 MetroNow 위젯을 탭하면 구성 화면이 열립니다. 배치된 위젯을 길게 눌러 `재구성`을 선택하는 방식도 지원합니다.

새로고침 아이콘은 즉시 OneTimeWorkRequest를 예약합니다. 주기 갱신은 Android 배터리 정책을 따르며 WorkManager가 보장하는 최소 주기는 15분입니다. 설정의 1분·5분 값은 앱 사용 중/수동 갱신 의도를 나타내며 백그라운드에서 정확한 간격을 보장하지 않습니다.

## 프로젝트 구조

```text
app/src/main/java/com/takji/metronow/
├── data/
│   ├── local/          # DataStore, 정적 역 카탈로그
│   ├── remote/         # Retrofit API, DTO, 도메인 매퍼
│   └── repository/     # API 추상화와 오류 처리
├── domain/model/       # 노선, 역, 방향, 프리셋, 도착정보, 설정
├── presentation/
│   ├── components/     # 앱 내부 위젯 미리보기
│   ├── home/           # 홈 UI와 ViewModel
│   ├── onboarding/     # 첫 실행 6단계
│   ├── presets/        # 프리셋 목록과 편집
│   ├── settings/       # API·갱신·위젯·테마 설정
│   └── theme/          # MetroNow 색상과 서체 계층
└── widget/             # Glance 위젯, 구성 Activity, WorkManager

app/src/main/assets/seoul_metro_stations.json
```

## 테스트

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

단위 테스트는 API DTO 변환, 2호선 내선/외선 분리, 상대 위치 변환, 음수 도착시간 방지, 키 누락과 API 오류 처리를 검증합니다.

## 알려진 제한사항

- 서울시 API는 GPS 좌표를 제공하지 않으므로 열차 아이콘은 `arvlCd`와 도착 메시지를 구간 상태로 변환한 상대 위치입니다. 정보가 모호하면 텍스트 상태를 우선합니다.
- 공식 API가 제공하지 않는 서울 외 구간은 결과가 비거나 지연될 수 있습니다.
- 정적 역 목록은 1~9호선 선택을 제공하지만, 1·5호선 등 복잡한 지선은 단일 순서 목록으로 단순화되어 방향 설명이 실제 분기와 다를 수 있습니다.
- 런처마다 위젯 크기 계산, 반투명 렌더링, 구성 Activity 자동 실행 방식이 다를 수 있습니다.
- WorkManager는 정확한 실행 시각을 보장하지 않으며 15분보다 짧은 백그라운드 주기 갱신을 지원하지 않습니다.
- API 키는 DataStore에 저장되지만 암호화되지 않습니다. 배포 환경에서는 서버 프록시 또는 별도 보안 저장 전략을 권장합니다.
