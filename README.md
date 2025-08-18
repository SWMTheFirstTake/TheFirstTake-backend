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

## 🚀 주요 기능

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
- **상품 연동**: 추천 스타일에 맞는 실제 상품 이미지 제공 (다중 이미지 지원)
- **상품 정보 캐싱**: Redis를 통한 상품 정보 자동 캐싱 및 빠른 조회
- **개인화 분석**: 사용자 체형, 피부톤, 선호도 기반 맞춤 추천

### 📸 이미지 처리
- **이미지 업로드**: AWS S3를 통한 안전한 이미지 저장
- **이미지 기반 분석**: 업로드된 이미지를 통한 스타일 분석
- **가상 피팅**: 선택한 옷들의 가상 피팅 시뮬레이션

### 🔐 사용자 관리
- **세션 기반 인증**: 안전한 사용자 세션 관리
- **채팅방 관리**: 개인별 채팅방 생성 및 히스토리 관리
- **사용자 로그**: 상세한 사용자 활동 로그 및 분석

### 📱 메시지 관리
- **무한 스크롤**: 페이지네이션을 통한 효율적인 메시지 조회
- **메시지 히스토리**: 사용자 업로드 이미지와 AI 추천 상품 이미지 분리 저장
- **실시간 업데이트**: 상품 이미지 URL 실시간 처리 및 저장

## 🛠️ 기술 스택

### Backend
- **Spring Boot 3.x**: 메인 애플리케이션 프레임워크
- **Spring Security**: 인증 및 보안
- **Spring Data JPA**: 데이터베이스 ORM
- **Spring WebSocket**: 실시간 통신
- **Gradle**: 빌드 도구

### Database & Cache
- **PostgreSQL**: 메인 데이터베이스 (채팅방, 메시지, 사용자 정보)
- **Redis**: 캐시 및 메시지 큐 (채팅 큐, 프롬프트 히스토리, 상품 정보 캐시)

### Cloud Services
- **AWS S3**: 이미지 파일 저장
- **AWS EC2**: 서버 호스팅

### External APIs
- **Claude Vision API**: AI 이미지 분석 및 큐레이션
- **Product Search API**: 상품 검색 및 이미지 제공 (리스트 형태 응답)

### Development Tools
- **Swagger/OpenAPI**: API 문서화
- **Lombok**: 보일러플레이트 코드 감소
- **Jackson**: JSON 직렬화/역직렬화

## 📡 API 엔드포인트

### 채팅 관련
- `POST /api/chat/send` - 메시지 전송 및 큐 저장
- `GET /api/chat/receive` - AI 응답 메시지 수신 (폴링)
- `GET /api/chat/rooms/history` - 채팅방 히스토리 조회
- `GET /api/chat/rooms/{roomId}/messages` - 채팅방 메시지 목록 조회 (무한 스크롤)

### 상품 관리
- `GET /api/chat/product/{productId}` - Redis 캐시된 상품 정보 조회

### 이미지 처리
- `POST /api/chat/upload` - 이미지 파일 업로드 (S3)

### 응답 형식 예시

#### AI 응답 (receive API)
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
    "products": [
      {
        "product_url": "https://sw-fashion-image-data.s3.amazonaws.com/TOP/1002/4227290/segment/0_17.jpg",
        "product_id": "4227290"
      },
      {
        "product_url": "https://sw-fashion-image-data.s3.amazonaws.com/BOTTOM/3002/3797063/segment/5_0.jpg",
        "product_id": "3797063"
      }
    ]
  }
}
```

#### 상품 정보 조회 (product API)
```json
{
  "status": "success",
  "message": "요청 성공",
  "data": {
    "product_name": "STRIPE SUNDAY SHIRT [IVORY]",
    "comprehensive_description": "베이지 색상의 세로 스트라이프 패턴이 돋보이는 반팔 셔츠입니다. 라운드넥 칼라와 버튼 여밈으로 심플한 디자인을 갖추고 있으며, 정면에는 패치 포켓이 있어 실용성을 더했습니다.",
    "style_tags": ["캐주얼", "모던", "심플 베이직"],
    "tpo_tags": ["데일리", "여행"]
  }
}
```

#### 메시지 목록 조회 (messages API)
```json
{
  "status": "success",
  "message": "채팅 메시지 목록을 성공적으로 조회했습니다.",
  "data": {
    "messages": [
      {
        "id": 1,
        "content": "내일 소개팅 가는데 입을 옷 추천해줘",
        "image_url": null,
        "message_type": "USER",
        "created_at": "2024-01-15T09:30:00Z",
        "agent_type": null,
        "agent_name": null,
        "product_image_url": null
      },
      {
        "id": 2,
        "content": "소개팅에 어울리는 스타일을 추천해드리겠습니다.",
        "image_url": null,
        "message_type": "STYLE",
        "created_at": "2024-01-15T09:35:00Z",
        "agent_type": "STYLE",
        "agent_name": "스타일 분석가",
        "product_image_url": [
          "https://sw-fashion-image-data.s3.amazonaws.com/TOP/1002/4227290/segment/0_17.jpg",
          "https://sw-fashion-image-data.s3.amazonaws.com/BOTTOM/3002/3797063/segment/5_0.jpg"
        ]
      }
    ],
    "has_more": true,
    "next_cursor": "2024-01-15T09:30:00Z",
    "count": 2
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
ProductSearchService → 상품 정보 Redis 캐싱 → 상품 이미지 URL 추출 → 
products 배열 구성 → 데이터베이스 저장 → 응답 반환
```

### 3. 이미지 처리 흐름
```
이미지 업로드 → ImageService → AWS S3 → URL 반환
```

### 4. 상품 이미지 처리 흐름
```
AI 응답 → ProductSearchService → 외부 검색 API → 상품 정보 Redis 캐싱 → 
이미지 URL & 상품 ID 추출 → products 배열 구성 → 개별 메시지로 저장 → 
사용자에게 통합 객체 형태로 제공
```

### 5. 상품 정보 조회 흐름
```
GET /product/{productId} → ChatController → ProductCacheService → 
Redis 캐시 조회 → 상품 정보 반환
```

## 🗄️ 데이터베이스 스키마

### PostgreSQL
- **chat_rooms**: 채팅방 정보
- **chat_messages**: 메시지 히스토리
  - `sender_type`: 메시지 발신자 타입 (USER, STYLE, TREND, COLOR, FITTING_COORDINATOR, *_PRODUCT)
  - `message`: 메시지 내용
  - `image_url`: 사용자 업로드 이미지 URL
  - `product_image_url`: AI 추천 상품 이미지 URL (개별 저장)
- **users**: 사용자 정보

### Redis
- **chat_queue**: 메시지 큐
- **prompt_history**: 프롬프트 히스토리 (누적 방식)
- **product_id:{product_id}**: 상품 정보 캐시 (상품명, 설명, 태그 등)

## 🔧 최근 업데이트 사항

### v1.3.0 (2024-01-18) - 상품 정보 관리 시스템 구축
- **상품 정보 캐싱 시스템**:
  - Redis를 활용한 상품 정보 자동 캐싱 (`product_id:{id}` 키 형태)
  - AI 응답 시 상품명, 설명, 스타일/TPO 태그 자동 저장
  - 중복 캐싱 방지 로직 구현
- **상품 정보 조회 API 추가**:
  - `GET /api/chat/product/{productId}` 엔드포인트 신규 추가
  - 캐시된 상품 정보 빠른 조회 지원
  - 완전한 Swagger 문서화
- **응답 구조 개선**:
  - 기존: `product_image_url[]`, `product_ids[]` (분리된 배열)
  - 신규: `products[]` (URL과 ID가 묶인 객체 배열)
  - 필드명 표준화: `product_url`, `product_id`
- **데이터베이스 컬럼 확장**:
  - `chat_messages.sender_type` 컬럼 길이 확장 (10자 → 100자)
  - `*_PRODUCT` 접미사 지원으로 상품 메시지 구분 개선

### v1.2.0 (2024-01-15)
- **상품 이미지 처리 개선**: 
  - 외부 API 응답 형식 변경 대응 (단일 URL → 리스트 형태)
  - 각 상품 이미지를 개별 메시지로 저장하여 관리 용이성 향상
  - 사용자에게는 리스트 형태로 제공하여 일관성 유지
- **메시지 저장 구조 최적화**:
  - AI 응답 메시지와 상품 이미지 메시지 분리 저장
  - `_PRODUCT` 접미사를 통한 상품 이미지 메시지 구분
- **API 응답 형식 표준화**:
  - `product_image_url` 필드를 리스트 형태로 통일
  - Swagger 문서 업데이트

### v1.1.0 (2024-01-10)
- **무한 스크롤 기능 추가**: 페이지네이션을 통한 효율적인 메시지 조회
- **메시지 히스토리 개선**: 사용자 업로드 이미지와 AI 추천 이미지 분리 관리

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
- **API 호출 로그**: 상품 검색 API 호출 결과 및 이미지 URL 처리 로그
- **에러 추적**: 상세한 에러 로그 및 스택 트레이스
- **메시지 저장 로그**: AI 응답 및 상품 이미지 저장 상태 추적

## 🔍 주요 기술적 특징

### 메시지 저장 구조
- **사용자 메시지**: 텍스트 + 업로드 이미지 (선택적)
- **AI 응답 메시지**: 텍스트만 저장
- **상품 이미지 메시지**: 각 상품 이미지를 개별 메시지로 저장 (`*_PRODUCT` 타입)

### 상품 정보 관리
- **외부 API 연동**: Product Search API를 통한 실시간 상품 검색
- **Redis 캐싱**: 상품 정보 자동 캐싱으로 빠른 재조회 지원
- **통합 객체 구조**: URL과 ID가 묶인 `products` 배열로 일관성 향상
- **저장 최적화**: 각 상품을 개별 레코드로 저장하여 관리 용이성 향상

### 성능 최적화
- **무한 스크롤**: 페이지네이션을 통한 메모리 효율적인 메시지 조회
- **Redis 큐**: 비동기 메시지 처리로 응답 속도 향상
- **S3 이미지 저장**: CDN을 통한 빠른 이미지 로딩

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
