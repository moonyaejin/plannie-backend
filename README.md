# Plannie Backend

> **Express → Spring Boot 리팩토링 프로젝트**
>
> 기존 Express로 구현했던 일정 관리 앱을 Spring Boot + 헥사고날 아키텍처로 전환하고,
> OpenAI GPT-4o 기반 AI 기능을 추가했습니다.
>
> 📎 기존 Express 버전: [moonyaejin/Plannie](https://github.com/moonyaejin/Plannie)

## 리팩토링 배경

Express 버전에서는 계층 분리가 명확하지 않아 기능 추가 시 사이드 이펙트가 잦았습니다.
Spring Boot + 헥사고날 아키텍처로 전환하면서 **도메인 로직 보호**, **포트를 통한 의존성 역전**,
**AI 기능 확장**을 목표로 했습니다.

## 기술 스택

| 분류         | 기술                                             |
| ------------ | ------------------------------------------------ |
| Language     | Java 17                                          |
| Framework    | Spring Boot 3.2                                  |
| Architecture | Hexagonal (Ports & Adapters)                     |
| Auth         | Spring Security + JWT (jjwt 0.12)                |
| ORM          | Spring Data JPA + QueryDSL                       |
| Database     | MySQL 8.x / H2 (개발용)                          |
| Cache        | Redis                                            |
| AI           | OpenAI GPT-4o (WebClient + JSON Schema 프롬프트) |
| Resilience   | Resilience4j (Circuit Breaker, Retry)            |
| Docs         | Springdoc OpenAPI (Swagger)                      |
| Test         | JUnit 5, Mockito, @WebMvcTest                    |

## 아키텍처

**헥사고날 아키텍처 (Ports and Adapters)**

```
com.plannie
├── domain              # 도메인 모델 (순수 자바, 외부 의존성 없음)
├── application         # 유스케이스, 포트 정의
│   ├── port.in        # 인바운드 포트 (유스케이스 인터페이스)
│   └── port.out       # 아웃바운드 포트 (외부 의존성 인터페이스)
├── adapter
│   ├── in.web         # REST 컨트롤러
│   └── out
│       ├── persistence # JPA 구현체
│       └── ai          # OpenAI API 어댑터
├── security            # JWT 필터, JwtProvider
└── config              # SecurityConfig, RedisConfig
```

## 실행 방법

### 로컬 환경 (H2 DB)

```bash
# 1. 환경변수 설정
echo "OPENAI_API_KEY=your-api-key" > .env

# 2. 서버 실행
export $(cat .env | xargs) && ./gradlew bootRun
```

### API 문서

서버 실행 후 Swagger UI: http://localhost:8080/swagger-ui.html

## 환경 변수

| 변수명           | 설명                    | 기본값           |
| ---------------- | ----------------------- | ---------------- |
| `OPENAI_API_KEY` | OpenAI API 키           | -                |
| `JWT_SECRET`     | JWT 서명 키 (32자 이상) | 로컬 기본값 제공 |
| `DB_HOST`        | MySQL 호스트            | localhost        |
| `DB_PORT`        | MySQL 포트              | 3306             |
| `DB_NAME`        | 데이터베이스명          | plannie          |
| `DB_USERNAME`    | DB 사용자명             | -                |
| `DB_PASSWORD`    | DB 비밀번호             | -                |
| `REDIS_HOST`     | Redis 호스트            | localhost        |
| `REDIS_PORT`     | Redis 포트              | 6379             |
| `ADMIN_EMAIL`    | 초기 관리자 계정 이메일 | -  (미설정 시 관리자 계정 생성 안 함) |
| `ADMIN_PASSWORD` | 초기 관리자 계정 비밀번호 | - (미설정 시 관리자 계정 생성 안 함) |
| `ADMIN_NICKNAME` | 초기 관리자 계정 닉네임 | 관리자 |

## 구현 현황

### 인증

| 기능     | 상태    | 비고              |
| -------- | ------- | ----------------- |
| 회원가입 | ✅ 완료 | BCrypt 암호화     |
| 로그인   | ✅ 완료 | JWT 발급 (24시간) |

### 기존 기능 (Express → Spring Boot 리팩토링)

| 기능                  | 상태    | 비고                          |
| --------------------- | ------- | ----------------------------- |
| 일정 CRUD             | ✅ 완료 | 생성/조회/수정/삭제           |
| 반복 일정             | ✅ 완료 | 일별/주별/월별, 개별 수정     |
| 카테고리 관리         | ✅ 완료 | CRUD                          |
| 일정 충돌 감지        | ✅ 완료 | 시간 겹침 검사                |
| 동시성 제어           | ✅ 완료 | 비관적 락 + 낙관적 락         |
| 통계 API + Redis 캐싱 | ✅ 완료 | 월별/카테고리 집계, TTL 1시간 |
| 알림 스케줄링         | 🔲 예정 |                               |

### AI 기능 (신규)

| 기능                | 상태    | 비고                          |
| ------------------- | ------- | ----------------------------- |
| 자연어 일정 파싱    | ✅ 완료 | GPT-4o, Circuit Breaker 적용  |
| 학습 계획 생성기    | ✅ 완료 | 시험/교재 기반 일정 자동 생성 |
| 진도 분석 AI 피드백 | ✅ 완료 | 주간 완료율 기반 리포트       |
| RAG 기반 Q&A        | 🔲 예정 | 교재 업로드, 임베딩           |

### 마무리

| 항목                | 상태    |
| ------------------- | ------- | ---------------------------------- |
| 핵심 테스트 코드    | ✅ 완료 | 단위 테스트 + 슬라이스 테스트 24개 |
| JWT 인증            | ✅ 완료 |                                    |
| 알림 스케줄링       | 🔲 예정 |                                    |
| 아키텍처 다이어그램 | 🔲 예정 |                                    |
