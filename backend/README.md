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

서비스 기능이 확장되면 도메인 경계와 코드의 역할에 따라 필요한 라이브러리 모듈을 추가합니다.
모든 도메인에 동일한 하위 모듈을 미리 만들지 않고, 독립적인 책임과 실제 구현이 생긴 단위만 모듈로 분리합니다.

## 2. 설계 방향

- 실행 가능한 서버 애플리케이션은 `services` 하위에 둡니다.
- 도메인별 코드는 `libs/domains` 하위에 둡니다.
- 여러 도메인에서 함께 쓰는 기반 계약은 `libs/shared` 하위에 둡니다.
- 도메인 모듈은 필요한 역할에 따라 `model`, `reference`, `service` 등으로 나눕니다.
- Spring Boot 실행 JAR은 실제 서비스 모듈에서만 생성합니다.

## 3. 모듈 구조

```text
modules/
├── libs/
│   ├── domains/
│   │   └── <DOMAIN_NAME>/
│   │       ├── <MODULE_ROLE>/
│   │       └── ...
│   └── shared/
│       └── <SHARED_MODULE>/
└── services/
    └── <SERVICE_NAME>/
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
./gradlew :libs:domain-user-model:build

# rest-api 서비스 모듈 빌드
./gradlew :services:rest-api:build
```

## 6. 버전, 의존성, 배포 전략

각 모듈을 독립적인 배포 단위로 다룹니다.
라이브러리 모듈은 다른 모듈에서 Gradle 의존성으로 소비할 수 있어야 하므로 GitHub Packages의 Maven registry에 배포합니다.
실행 모듈은 다른 모듈의 라이브러리 의존성으로 쓰지 않고 독립 실행 가능한 Spring Boot `bootJar` 산출물로 배포하므로
GitHub Release asset으로 배포합니다.

### 6.1. 배포 채널

backend 모듈 변경이 `develop` 또는 `main`에 push되면 affected 모듈을 다음 채널로 배포합니다.

| 대상 브랜치 | 채널 | 배포 버전 |
| --- | --- | --- |
| `develop` | SNAPSHOT | `x.y.z-SNAPSHOT` |
| `main` | stable | `x.y.z` |

### 6.2. 버전과 내부 의존성

모듈의 base 버전과 내부 artifact 좌표는 각 `build.gradle.kts`에 직접 명시합니다.

```kotlin
version = "0.0.2"

dependencies {
    implementation("com.devneopark.chat.backend:domain-room-reference:0.0.2-SNAPSHOT")
}
```

내부 모듈은 `project(...)`가 아니라 GitHub Packages의 Maven artifact로 소비합니다.
버전은 자동 갱신되지 않으므로 특별한 고정 사유가 없다면 최신 게시 버전을 사용합니다.
호환되지 않는 upstream 변경은 먼저 배포한 뒤 downstream의 의존성과 코드를 갱신합니다.

### 6.3. PR, 버전, affected 모듈

- PR은 실질적인 backend 모듈 변경을 하나만 포함합니다. 기존 모듈의 dependency 선언 변경은 이 제한에서 제외합니다.
- 소스·공개 계약·동작이 바뀌면 base 버전을 올립니다. dependency 좌표만 정렬할 때는 기존 SNAPSHOT 버전을 유지할 수 있습니다.
- SNAPSHOT은 재배포할 수 있지만 stable 버전은 덮어쓸 수 없습니다. stable 배포는 SNAPSHOT 내부 의존성을 허용하지 않습니다.
- 변경 모듈과 이를 직간접적으로 의존하는 모듈을 affected 처리하며, upstream에서 downstream 순서로 검증·배포합니다.

### 6.4. 등록과 배포 대상

`settings.gradle.kts`는 `modules/libs/**`와 `modules/services/**`의 `build.gradle.kts`를 탐색해 모듈을 등록합니다.

| 모듈 | 산출물 | 배포 위치 |
| --- | --- | --- |
| 라이브러리 | Maven artifact | GitHub Packages |
| 실행 모듈 | Spring Boot `bootJar` | GitHub Release asset |

릴리즈 태그는 `backend/<libs|services>/<artifact-id>/v<version>` 형식을 사용합니다.
