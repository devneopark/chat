# 백엔드 PR 검증 스크립트

이 디렉터리는 GitHub Actions에서만 사용하는 백엔드 검증 스크립트를 보관한다.

## 실행 규칙

스크립트는 일반 Kotlin script 확장자인 `.kts`를 사용한다. 파일명에 `.main`을 붙이지 않는다.
워크플로는 Kotlin 컴파일러 `2.3.21`을 설치한 뒤 스크립트를 실행한다. 이 버전은
`backend/gradle/libs.versions.toml`의 Kotlin 플러그인 버전과 일치시켜야 한다.

```bash
kotlinc -script .github/workflows/scripts/backend/validate-pull-request.kts
kotlinc -script .github/workflows/scripts/backend/calculate-affected-modules.kts
kotlinc -script .github/workflows/scripts/backend/validate-stable-versions.kts
```

실행 위치는 저장소 루트이며, PR 이벤트에서 다음 환경변수를 제공해야 한다.

- `GITHUB_BASE_REF`: PR 대상 브랜치
- `GITHUB_HEAD_REF`: PR 소스 브랜치
- `BASE_SHA`: PR 기준 커밋 SHA
- `HEAD_SHA`: PR 변경 커밋 SHA
- `GITHUB_STEP_SUMMARY`: GitHub Actions 요약 파일 경로
- `GITHUB_TOKEN`: GitHub Packages 및 Releases 조회 토큰
- `GITHUB_REPOSITORY_OWNER`: GitHub Packages 소유자
- `GITHUB_REPOSITORY`: GitHub 저장소 좌표

## 변경 범위 정책

- `feature → develop/backend/<모듈>`: 담당 모듈만 변경할 수 있다.
- `develop/backend/<모듈> → main`: 담당 모듈만 변경할 수 있다.
- 일반 브랜치에서 `main`으로 가는 PR: 저장소 공통 파일, 백엔드 프로젝트 루트 파일, 워크플로 파일만 변경할 수 있다.

main 병합 이후 각 `develop/backend/**` 브랜치의 rebase 및 원격 push는 수동으로 수행한다.

영향 모듈 계산 결과는 `backend/build/affected-modules.tsv`에 기록하며, 직접 변경된
모듈과 해당 모듈을 의존하는 다운스트림 모듈을 포함한다. PR 빌드는 이 결과에 포함된
Gradle 프로젝트만 실행한다.

## 버전 정책

- 영향 모듈의 stable 버전이 GitHub Packages 또는 GitHub Release에 이미 존재하면 PR을 실패시킨다.
- stable 버전이 배포된 내부 모듈을 `-SNAPSHOT`으로 참조하면 PR을 실패시킨다.
- 해당 모듈의 의존성은 즉시 stable 버전으로 교체해야 한다.
