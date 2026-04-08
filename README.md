# Plannie Backend

AI 기반 일정 관리 앱 Plannie의 백엔드 서버
- Express → Spring Boot 리팩토링 프로젝트
- 헥사고날 아키텍처 + OpenAI 연동으로 AI 기능 확장

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.2.0 |
| Architecture | Hexagonal (Ports & Adapters) |
| ORM | Spring Data JPA + QueryDSL |
| Database | MySQL 8.x / H2 (개발용) |
| Cache | Redis |
| AI | OpenAI GPT-4o |
| Resilience | Resilience4j (Circuit Breaker, Retry) |
| Docs | Springdoc OpenAPI (Swagger) |

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
│       └── ai          # OpenAI API 연동
└── common              # 설정, 예외 처리
```

## 실행 방법

### 로컬 환경 (H2 DB)

```bash
# 1. 환경변수 설정 (.env 파일 생성)
echo "OPENAI_API_KEY=your-api-key-here" > .env

# 2. 서버 실행
export $(cat .env | xargs) && ./gradlew bootRun
```

### Docker Compose

```bash
docker-compose up -d
```

## API 문서

서버 실행 후 Swagger UI 접속:
- http://localhost:8080/swagger-ui.html

## 환경 변수

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| `OPENAI_API_KEY` | OpenAI API 키 | - |
| `DB_HOST` | MySQL 호스트 | localhost |
| `DB_PORT` | MySQL 포트 | 3306 |
| `DB_NAME` | 데이터베이스명 | plannie |
| `DB_USERNAME` | DB 사용자명 | - |
| `DB_PASSWORD` | DB 비밀번호 | - |
| `REDIS_HOST` | Redis 호스트 | localhost |
| `REDIS_PORT` | Redis 포트 | 6379 |

## 구현 현황

### 기존 기능 (Express → Spring Boot 리팩토링)

| 기능 | 상태 | 비고 |
|------|------|------|
| 일정 CRUD | ✅ 완료 | 생성/조회/수정/삭제 |
| 반복 일정 | ✅ 완료 | 일별/주별/월별, 개별 수정 |
| 카테고리 관리 | ✅ 완료 | CRUD |
| 일정 충돌 감지 | ✅ 완료 | 시간 겹침 검사 |
| 동시성 제어 | ✅ 완료 | 비관적 락, 낙관적 락 |
| 통계 API + Redis 캐싱 | 🔲 예정 | 월별/카테고리 집계 |
| 알림 스케줄링 | 🔲 예정 | |

### AI 기능 (신규)

| 기능 | 상태 | 비고 |
|------|------|------|
| 자연어 일정 파싱 | ✅ 완료 | GPT-4o, Circuit Breaker 적용 |
| 학습 계획 생성기 | 🚧 진행 중 | 시험/교재 기반 일정 자동 생성 |
| 진도 분석 + AI 피드백 | 🔲 예정 | 주간 리포트 |
| RAG 기반 Q&A | 🔲 예정 (선택) | 교재 업로드, 임베딩 |

### 마무리

| 항목 | 상태 |
|------|------|
| 핵심 테스트 작성 | 🔲 예정 |
| JWT 인증 | 🔲 예정 |
| 문서화 + 아키텍처 다이어그램 | 🔲 예정 |
