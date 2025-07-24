# TheFirstTake - AI 기반 패션 큐레이션 채팅 서버

[📋 기획서 보기](https://github.com/SWMTheFirstTake/TheFirstTake-backend/blob/main/docs/2-12.%20The%20First%20Take.pdf)

## 🎯 프로젝트 개요

TheFirstTake는 AI 기반의 개인화된 패션 큐레이션 서비스입니다. 사용자의 스타일 선호도와 상황에 맞는 최적의 패션 추천을 제공하며, 실시간 채팅 인터페이스를 통해 자연스러운 대화형 경험을 제공합니다.

## 🏗️ 시스템 아키텍처

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Web Frontend  │    │  CHAT Server     │    │  External APIs  │
│                 │    │   (Spring Boot)  │    │                 │
│ ┌─────────────┐ │    │ ┌──────────────┐ │    │ ┌─────────────┐ │
│ │ User Input  │ │    │ │Chat Controller│ │    │ │Claude Vision│ │
│ │             │ │    │ │              │ │    │ │    API      │ │
│ │ POST /send  │◄─────┤ │ GET /receive │ │    │ │             │ │
│ │ GET /receive│ │    │ │              │ │    │ └─────────────┘ │
│ └─────────────┘ │    │ └──────────────┘ │    │ ┌─────────────┐ │
└─────────────────┘    │ ┌──────────────┐ │    │ │Product Search│ │
                       │ │Queue Service │ │    │ │    API      │ │
                       │ │              │ │    │ │             │ │
                       │ │ ┌──────────┐ │ │    │ └─────────────┘ │
                       │ │ │Redis Queue│ │ │    └─────────────────┘
                       │ │ └──────────┘ │ │
                       │ └──────────────┘ │
                       │ ┌──────────────┐ │
                       │ │Message Service│ │
                       │ │Room Service  │ │
                       │ └──────────────┘ │
                       └──────────────────┘
                                │
                       ┌──────────────────┐
                       │   Data Storage   │
                       │                  │
                       │ ┌──────────────┐ │
                       │ │ PostgreSQL   │ │
                       │ │- Chat Rooms  │ │
                       │ │- Messages    │ │
                       │ │- Users       │ │
                       │ └──────────────┘ │
                       │ ┌──────────────┐ │
                       │ │    Redis     │ │
                       │ │- Chat Queue  │ │
                       │ │- Prompts     │ │
                       │ └──────────────┘ │
                       │ ┌──────────────┐ │
                       │ │   AWS S3     │ │
                       │ │- Images      │ │
                       │ └──────────────┘ │
                       └──────────────────┘
```

## �� 주요 기능

### 💬 AI 기반 채팅 인터페이스
- **LLM 기반 질의응답**: 자연어로 패션 상담 및 스타일 추천
- **실시간 폴링 방식**: `/chat/receive` API를 통한 실시간 메시지 수신
- **다중 에이전트 시스템**: 
  - 스타일 분석가 (Style Analyst)
  - 트렌드 전문가 (Trend Expert) 
  - 컬러 전문가 (Color Expert)
  - 핏팅 코디네이터 (Fitting Coordinator)

### 🎨 패션 큐레이션 & 추천
- **RAG 기반 지식 검색**: 패션 지식베이스를 활용한 정확한 추천
- **상황별 맞춤 추천**: 소개팅, 면접, 데이트 등 상황별 최적 스타일
- **상품 연동**: 추천 스타일에 맞는 실제 상품 이미지 제공
- **개인화 분석**: 사용자 체형, 피부톤, 선호도 기반 맞춤 추천

### 📸 이미지 처리
- **이미지 업로드**: AWS S3를 통한 안전한 이미지 저장
- **이미지 기반 분석**: 업로드된 이미지를 통한 스타일 분석
- **가상 피팅**: 선택한 옷들의 가상 피팅 시뮬레이션

### 🔐 사용자 관리
- **세션 기반 인증**: 안전한 사용자 세션 관리
- **채팅방 관리**: 개인별 채팅방 생성 및 히스토리 관리
- **사용자 로그**: 상세한 사용자 활동 로그 및 분석

## 🛠️ 기술 스택

### Backend
- **Spring Boot 3.x**: 메인 애플리케이션 프레임워크
- **Spring Security**: 인증 및 보안
- **Spring Data JPA**: 데이터베이스 ORM
- **Spring WebSocket**: 실시간 통신
- **Gradle**: 빌드 도구

### Database & Cache
- **PostgreSQL**: 메인 데이터베이스 (채팅방, 메시지, 사용자 정보)
- **Redis**: 캐시 및 메시지 큐 (채팅 큐, 프롬프트 히스토리)

### Cloud Services
- **AWS S3**: 이미지 파일 저장
- **AWS EC2**: 서버 호스팅

### External APIs
- **Claude Vision API**: AI 이미지 분석 및 큐레이션
- **Product Search API**: 상품 검색 및 이미지 제공

### Development Tools
- **Swagger/OpenAPI**: API 문서화
- **Lombok**: 보일러플레이트 코드 감소
- **Jackson**: JSON 직렬화/역직렬화

## 📡 API 엔드포인트

### 채팅 관련
- `POST /api/chat/send` - 메시지 전송 및 큐 저장
- `GET /api/chat/receive` - AI 응답 메시지 수신 (폴링)
- `GET /api/chat/rooms/history` - 채팅방 히스토리 조회

### 이미지 처리
- `POST /api/chat/upload` - 이미지 파일 업로드 (S3)

### 응답 형식 예시
```json
{
  "status": "success",
  "message": "요청 성공",
  "data": {
    "message": "전문가들의 다양한 의견을 종합해 보았습니다...",
    "order": 1,
    "agent_id": "fitting_coordinator",
    "agent_name": "핏팅 코디네이터",
    "agent_role": "종합적으로 딱 하나의 추천을 해드려요!",
    "product_image_url": "https://example.com/product-image.jpg"
  }
}
```

## 🔄 시스템 흐름

### 1. 메시지 전송 흐름
```
사용자 입력 → POST /chat/send → ChatController → QueueService → Redis Queue
```

### 2. 메시지 수신 흐름
```
GET /chat/receive → ChatController → QueueService → Claude Vision API → 
ProductSearchService → 응답 반환
```

### 3. 이미지 처리 흐름
```
이미지 업로드 → ImageService → AWS S3 → URL 반환
```

## 🗄️ 데이터베이스 스키마

### PostgreSQL
- **chat_rooms**: 채팅방 정보
- **chat_messages**: 메시지 히스토리 (sender_type 포함)
- **users**: 사용자 정보

### Redis
- **chat_queue**: 메시지 큐
- **prompt_history**: 프롬프트 히스토리 (누적 방식)

## 🚀 실행 방법

### Prerequisites
- Java 17+
- PostgreSQL
- Redis
- AWS S3 계정

### 설치 및 실행
```bash
# 1. 저장소 클론
git clone https://github.com/SWMTheFirstTake/TheFirstTake-backend.git

# 2. 프로젝트 디렉토리 이동
cd TheFirstTake-backend/thefirsttake

# 3. 애플리케이션 실행
./gradlew bootRun
```

### 환경 설정
`src/main/resources/application.yaml`에서 다음 설정을 확인하세요:
- 데이터베이스 연결 정보
- Redis 연결 정보
- AWS S3 설정
- 외부 API 엔드포인트

## 📊 모니터링 & 로깅

- **애플리케이션 로그**: Spring Boot 기본 로깅
- **API 호출 로그**: 상품 검색 API 호출 결과
- **에러 추적**: 상세한 에러 로그 및 스택 트레이스

## 🤝 기여하기

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다. 자세한 내용은 `LICENSE` 파일을 참조하세요.

## 📞 문의

프로젝트에 대한 문의사항이 있으시면 이슈를 생성해 주세요.

---

**TheFirstTake** - AI로 만드는 나만의 스타일 🎨✨
