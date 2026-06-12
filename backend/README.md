# 실시간 채팅 서비스: 백엔드 프로젝트

> [프로젝트 홈](https://github.com/devneopark/chat)

## 0. 개요

실시간 채팅 서비스의 서버 애플리케이션을 구성하는 백엔드 프로젝트입니다.

백엔드는 사용자와 채팅방을 관리하는 HTTP API, 클라이언트와 실시간 메시지를 주고받는 메시징 게이트웨이,
그리고 여러 실행 모듈이 공유하는 도메인 라이브러리로 구성합니다.

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
- 도메인 공통 코드는 `libs/domains` 하위에 둡니다.
- 도메인 모듈은 `model`, `event`, `reference` 역할로 나눕니다.
- Spring Boot 실행 JAR은 실제 서비스 모듈에서만 생성합니다.

## 3. 모듈 구조

```text
modules/
├── libs/
│   └── domains/
│       ├── room/
│       │   ├── event/
│       │   ├── model/
│       │   └── reference/
│       └── user/
│           ├── event/
│           ├── model/
│           └── reference/
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

GitHub Releases는 tag 기준의 배포 이력, 릴리즈 노트, 실행 가능한 바이너리 다운로드 지점을 관리하는 용도로 사용합니다.
GitHub Packages는 Maven/Gradle이 `group:artifact:version` 좌표로 resolve할 수 있는 패키지 저장소로 사용합니다.

### 6.1. 버전 카탈로그

모듈 간 의존성 좌표와 버전은 Gradle version catalog인 `gradle/libs.versions.toml`에서 관리합니다.

```toml
[versions]
shared-kernel = "0.1.0"
domain-user-model = "0.1.0"

[libraries]
shared-kernel = { module = "com.devneopark.chat:shared-kernel", version.ref = "shared-kernel" }
domain-user-model = { module = "com.devneopark.chat:domain-user-model", version.ref = "domain-user-model" }
```

모듈이 다른 내부 모듈을 의존할 때는 `project(":modules:...")` 의존성을 기본으로 사용하지 않습니다.
이미 GitHub Packages에 배포된 Maven artifact를 version catalog alias로 참조합니다.

```kotlin
dependencies {
    implementation(libs.shared.kernel)
    implementation(libs.domain.user.model)
}
```

이 원칙은 배포된 artifact 간의 실제 호환성을 빌드 단계에서 검증하기 위한 것입니다.
로컬 개발 편의를 위해 `mavenLocal()` 또는 composite build를 임시로 사용할 수는 있지만,
CI와 release workflow에서는 GitHub Packages에 배포된 좌표를 기준으로 resolve합니다.

### 6.2. 모듈 좌표

모든 배포 대상 모듈은 고정된 Maven 좌표를 가집니다.

```text
group: com.devneopark.chat
artifact: <module-artifact-name>
version: <module-version>
```

artifact 이름은 GitHub Packages의 Maven registry 제약을 고려해 소문자, 숫자, 하이픈만 사용합니다.

예시:

```text
com.devneopark.chat:shared-kernel:0.1.0
com.devneopark.chat:domain-user-model:0.1.0
com.devneopark.chat:domain-user-event:0.1.0
com.devneopark.chat:rest-api:0.1.0
com.devneopark.chat:messaging-gateway:0.1.0
```

### 6.3. 배포 대상

라이브러리 모듈:

```text
modules/libs/**
```

- `maven-publish`로 GitHub Packages Maven registry에 배포합니다.
- 다른 모듈은 version catalog에 선언된 Maven 좌표로 이 artifact를 가져옵니다.
- GitHub Release는 tag와 release note를 남기는 기준점으로 사용할 수 있지만, 주 배포 artifact 저장소는 GitHub Packages입니다.

실행 모듈:

```text
modules/services/rest-api
modules/services/messaging-gateway
```

- Spring Boot `bootJar`로 실행 가능한 jar를 생성합니다.
- 생성된 bootJar 파일을 GitHub Release asset으로 업로드합니다.
- 실행 모듈의 bootJar는 다른 모듈의 `implementation(...)` 의존성으로 사용하지 않습니다.

### 6.4. 태그 규칙

모듈별 독립 릴리즈를 전제로 태그에는 모듈 이름과 버전을 함께 포함합니다.

```text
shared-kernel-v0.1.0
domain-user-model-v0.1.0
domain-user-event-v0.1.0
rest-api-v0.1.0
messaging-gateway-v0.1.0
```

릴리즈 workflow는 태그 이름에서 대상 모듈과 버전을 파싱하고, version catalog 또는 Gradle의 `project.version`과 일치하는지 검증합니다.
태그의 버전과 빌드 설정의 버전이 다르면 배포하지 않습니다.

### 6.5. 실행 모듈 런타임 버전

실행 모듈은 빌드 시점의 `project.version`을 Spring Boot build info에 주입합니다.
애플리케이션 런타임에서는 `BuildProperties`를 통해 현재 실행 중인 artifact 버전을 확인할 수 있어야 합니다.

```kotlin
springBoot {
    buildInfo {
        properties {
            version = project.version.toString()
        }
    }
}
```
