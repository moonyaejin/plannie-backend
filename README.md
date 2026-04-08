# Plannie Backend

AI 챗봇 기반 일정 관리 앱 Plannie의 백엔드 서버

## 기술 스택

- Java 17
- Spring Boot 3.2.0
- Spring Data JPA + QueryDSL
- MySQL 8.x / H2 (개발용)
- Redis
- OpenAI API
- Resilience4j

## 아키텍처

**헥사고날 아키텍처 (Ports and Adapters)**

```
com.plannie
├── domain              # 도메인 모델 (순수 자바)
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
# 1. 환경변수 설정
cp .env.example .env  # 또는 직접 .env 생성
# .env에 OPENAI_API_KEY 입력

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
| `DB_HOST` | MySQL 호스트 | localhost |
| `DB_PORT` | MySQL 포트 | 3306 |
| `DB_NAME` | 데이터베이스명 | plannie |
| `DB_USERNAME` | DB 사용자명 | - |
| `DB_PASSWORD` | DB 비밀번호 | - |
| `REDIS_HOST` | Redis 호스트 | localhost |
| `REDIS_PORT` | Redis 포트 | 6379 |
| `OPENAI_API_KEY` | OpenAI API 키 | - |

## 주요 기능

- [x] 일정 CRUD
- [x] 반복 일정
- [x] 카테고리/태그
- [x] 일정 충돌 감지
- [x] 동시성 제어 (비관적 락)
- [x] 자연어 일정 파싱 (OpenAI GPT-4o)
- [ ] 알림 스케줄링
- [ ] JWT 인증
- [ ] 통계 API
