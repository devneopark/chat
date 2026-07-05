# 실시간 채팅 서비스: 백엔드 프로젝트

> [프로젝트 홈](https://github.com/devneopark/chat)

## 0. 개요

실시간 채팅 서비스의 서버 애플리케이션을 구성하는 백엔드 프로젝트입니다.

백엔드는 사용자와 채팅방을 관리하는 HTTP API, 클라이언트와 실시간 메시지를 주고받는 메시징 게이트웨이,
그리고 여러 실행 모듈이 공유하는 도메인 및 shared 라이브러리로 구성합니다.

단일 백엔드 코드베이스 안에서 도메인 경계를 명확히 나누고, 실행 애플리케이션의 책임을 분리합니다.

## 1. 백엔드 구성

### 1.1. Rest API

```text
modules/services/rest-api
```

HTTP 기반 API 서버입니다.

사용자, 채팅방, 인증, 메시지 조회처럼 요청과 응답 중심의 기능을 담당하는 실행 애플리케이션입니다.
무상태를 보장하고 수평적으로 확장 가능한 서버로 유지할 수 있도록 합니다.

### 1.2. Messaging Gateway

```text
modules/services/messaging-gateway
```

클라이언트와의 실시간 메시지 송/수신을 담당하는 실행 애플리케이션입니다.

채팅 메시지 송수신, 서버 내부 이벤트 소비, 클라이언트 연결 프로토콜 처리를 이 모듈을 중심으로 구현합니다.

### 1.3. Domain Libraries

```text
modules/libs/domains/<DOMAIN_NAME>
```

실행 애플리케이션에서 함께 사용하는 도메인 단위 라이브러리입니다.

현재는 `user`, `room` 도메인 영역을 기준으로 모듈을 나누고, 각 도메인 안에서 모델, 이벤트, 참조 타입을 분리합니다.

## 2. 설계 방향

- 실행 가능한 서버 애플리케이션은 `services` 하위에 둡니다.
- 도메인별 코드는 `libs/domains` 하위에 둡니다.
- 여러 도메인에서 함께 쓰는 기반 계약은 `libs/shared` 하위에 둡니다.
- 도메인 모듈은 `model`, `event`, `reference` 역할로 나눕니다.
- Spring Boot 실행 JAR은 실제 서비스 모듈에서만 생성합니다.

## 3. 모듈 구조

```text
modules/
├── libs/
│   ├── domains/
│   │   ├── room/
│   │   │   ├── event/
│   │   │   ├── model/
│   │   │   └── reference/
│   │   └── user/
│   │       ├── event/
│   │       ├── model/
│   │       └── reference/
│   └── shared/
│       ├── domains/
│       │   └── exception/
│       └── kernel/
└── services/
    ├── messaging-gateway/
    └── rest-api/
```

## 4. 기술 스택

- Kotlin: `2.3.21`
- Spring Boot: `4.0.6`
- DB Access: `R2DBC` with coroutine

## 5. 빌드

백엔드 프로젝트 루트에서 전체 프로젝트를 빌드합니다.

```shell
./gradlew build
```

각 모듈 단위로 빌드할 수 있습니다.

```shell
# user 도메인 모델 모듈 빌드
./gradlew :modules:libs:domains:user:model:build

# rest-api 서비스 모듈 빌드
./gradlew :modules:services:rest-api:build
```

## 6. 버전, 의존성, 배포 전략

이 프로젝트는 각 모듈을 독립적인 배포 단위로 다룹니다.

라이브러리 모듈은 다른 모듈에서 Gradle 의존성으로 소비할 수 있어야 하므로 GitHub Packages의 Maven registry에 배포합니다.
실행 모듈은 다른 모듈의 라이브러리 의존성으로 쓰지 않고 독립 실행 가능한 Spring Boot `bootJar` 산출물로 배포하므로
GitHub Release asset으로 배포합니다.

### 6.1. 브랜치 전략

GitFlow를 사용합니다.

```text
develop      SNAPSHOT 개발선
main         stable 릴리즈선
feature/*    기능 개발
release/*    stable 승격 준비
hotfix/*     stable 긴급 수정
```

`feature/*` 브랜치는 `develop`으로 병합하고, `release/*` 브랜치는 `main`으로 병합합니다.
`develop`에서는 `x.y.z-SNAPSHOT` 버전을 사용하고, `main`으로 들어가는 릴리즈 대상 모듈은 `x.y.z` stable 버전을 사용합니다.

### 6.2. 버전 카탈로그와 내부 모듈 의존성

Gradle version catalog인 `gradle/libs.versions.toml`에서는 빌드 플러그인 버전과
각 배포 대상 모듈 자체의 버전을 관리합니다.

```toml
[versions]
shared-kernel = "0.0.1-SNAPSHOT"
shared-domain-exception = "0.0.1-SNAPSHOT"
domain-user-model = "0.0.1-SNAPSHOT"
```

모듈이 다른 내부 모듈을 의존할 때는 `project(":modules:...")` 의존성을 기본으로 사용하지 않습니다.
각 소비 모듈의 빌드 파일에서 이미 GitHub Packages에 배포된 Maven artifact의 좌표와 버전을 명시합니다.
의존성 버전은 해당 소비 모듈이 검증한 버전으로 고정하며, 배포 대상 모듈 자체의 버전과 독립적으로 관리합니다.

```kotlin
dependencies {
    implementation("com.devneopark.chat:shared-kernel:0.0.1-SNAPSHOT")
    implementation("com.devneopark.chat:domain-user-model:0.0.1-SNAPSHOT")
}
```

PR CI와 배포 workflow는 모두 GitHub Packages에 배포된 Maven 좌표만 resolve합니다.
따라서 upstream 모듈을 먼저 배포한 뒤 downstream 모듈의 변경을 검증하고 배포합니다.

### 6.3. 버전 정책

허용하는 버전 형식은 두 가지입니다.

```text
x.y.z-SNAPSHOT
x.y.z
```

`SNAPSHOT`은 변경 가능한 개발 버전입니다.
같은 SNAPSHOT 버전은 재배포할 수 있고, 실행 모듈의 prerelease asset도 교체할 수 있습니다.

stable 버전은 불변 릴리즈입니다.
같은 tag, package version, release, release asset이 이미 존재하면 배포하지 않습니다.
stable 릴리즈가 성공하면 해당 stable로 승격된 모듈의 직전 SNAPSHOT package, prerelease, tag를 정리합니다.

### 6.4. 모듈 좌표

모든 배포 대상 모듈은 고정된 Maven 좌표를 가집니다.

```text
group: com.devneopark.chat
artifact: <module-artifact-name>
version: <module-version>
```

예시:

```text
com.devneopark.chat:shared-kernel:0.0.1-SNAPSHOT
com.devneopark.chat:shared-domain-exception:0.0.1-SNAPSHOT
com.devneopark.chat:domain-user-model:0.0.1-SNAPSHOT
com.devneopark.chat:rest-api:0.0.1-SNAPSHOT
com.devneopark.chat:messaging-gateway:0.0.1-SNAPSHOT
```

### 6.5. Affected 전파

변경된 모듈을 직접 배포 대상으로 삼고, 해당 모듈을 직간접적으로 의존하는 소비 모듈까지 affected set에 포함합니다.

```text
rest-api depends on domain-user-model
domain-user-model depends on shared-kernel

shared-kernel 변경
=> affected: shared-kernel, domain-user-model, rest-api
```

배포는 내부 의존성 그래프를 위상 정렬해 upstream에서 downstream 순서로 진행합니다.
의존성 cycle이 발견되면 workflow를 실패시킵니다.

### 6.6. 배포 대상

라이브러리 모듈:

```text
modules/libs/**
```

- `maven-publish`로 GitHub Packages Maven registry에 배포합니다.
- 다른 모듈은 각 빌드 파일에 선언된 Maven 좌표와 버전으로 이 artifact를 가져옵니다.

실행 모듈:

```text
modules/services/rest-api
modules/services/messaging-gateway
```

- Spring Boot `bootJar`로 실행 가능한 jar를 생성합니다.
- 생성된 bootJar 파일을 GitHub Release asset으로 업로드합니다.
- 실행 모듈의 bootJar는 다른 모듈의 `implementation(...)` 의존성으로 사용하지 않습니다.

### 6.7. 태그 규칙

모듈별 독립 릴리즈를 전제로 태그에는 모듈 이름과 버전을 함께 포함합니다.

```text
shared-kernel-v0.0.1-SNAPSHOT
shared-kernel-v0.0.1
rest-api-v0.0.1-SNAPSHOT
rest-api-v0.0.1
```

SNAPSHOT tag는 mutable 기준점입니다.
stable tag는 immutable 기준점입니다.

### 6.8. 인증과 Secret

로컬 환경에는 GitHub token을 저장하지 않습니다.
GitHub Repository Secrets를 사용합니다.

GitHub Actions 안에서 같은 repository의 tag, release, GitHub Packages를 다루는 작업은
workflow에 명시한 `GITHUB_TOKEN` 권한을 사용합니다.

```yaml
permissions:
  contents: write
  packages: write
```

별도 repository secret은 bot commit, release PR 생성, next SNAPSHOT PR 생성처럼
workflow가 branch/PR을 만들고 후속 workflow trigger가 필요한 자동화에 사용합니다.

필요 secrets:

```text
GH_AUTOMATION_TOKEN
```

`GH_AUTOMATION_TOKEN`에는 private repository 기준으로 다음 권한이 필요합니다.

```text
repo
```

`GH_PACKAGES_USERNAME`은 로컬에서 직접 GitHub Packages에 publish할 때만 사용합니다.
Actions 환경에서는 `github.actor`를 사용합니다.

secret은 필요한 workflow step에만 주입합니다.
`pull_request_target`은 사용하지 않고, fork PR에서는 secret이 필요한 step을 실행하지 않습니다.

### 6.9. 로컬 GitHub Packages 사용

private GitHub Packages는 로컬 개발환경에서도 인증이 필요합니다.
IDE에서 Gradle sync/build가 GitHub Packages artifact를 가져오려면
IDE의 Gradle 실행 환경에 다음 환경변수를 주입해야 합니다.

```bash
GH_PACKAGES_USERNAME=devneopark
GH_AUTOMATION_TOKEN=<read:packages 권한이 있는 GitHub token>
```

루트 Gradle 설정은 다음 순서로 credential을 찾습니다.

```text
username: githubPackagesUsername -> GH_PACKAGES_USERNAME -> GITHUB_ACTOR
token: githubPackagesToken -> GH_AUTOMATION_TOKEN -> GITHUB_TOKEN
```

CLI에서 일시적으로 검증할 때는 다음처럼 실행할 수 있습니다.

```bash
GH_PACKAGES_USERNAME=devneopark GH_AUTOMATION_TOKEN="$(gh auth token)" ./gradlew build
```

### 6.10. 실행 모듈 런타임 버전

실행 모듈은 빌드 시점의 `project.version`을 Spring Boot build info에 주입합니다.
애플리케이션 런타임에서는 `BuildProperties`를 통해 현재 실행 중인 artifact 버전을 확인할 수 있어야 합니다.
