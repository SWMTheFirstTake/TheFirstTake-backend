# TheFirstTake - AI 기반 패션 큐레이션 채팅 서버

[📋 기획서 보기](https://docs.google.com/document/d/1wXRMFZXSbN6UP7M42D2i-N0BNuzQg8nuugPhjUqb67Y/edit?usp=sharing)

## 🎯 프로젝트 개요

TheFirstTake는 AI 기반의 개인화된 패션 큐레이션 서비스입니다. 사용자의 스타일 선호도와 상황에 맞는 최적의 패션 추천을 제공하며, 실시간 채팅 인터페이스를 통해 자연스러운 대화형 경험을 제공합니다.

## 🏗️ 시스템 아키텍처

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Web Frontend  │    │  CHAT Server     │    │  External APIs  │
│                 │    │   (Spring Boot)  │    │                 │
│ ┌─────────────┐ │    │ ┌──────────────┐ │    │ ┌─────────────┐ │
│ │ Chat Input  │ │    │ │Chat Controller│ │    │ │Claude Vision│ │
│ │ POST /send  │◄─────┤ │ GET /receive │ │    │ │    API      │ │
│ │ GET /receive│ │    │ │              │ │    │ │             │ │
│ └─────────────┘ │    │ └──────────────┘ │    │ └─────────────┘ │
│ ┌─────────────┐ │    │ ┌──────────────┐ │    │ ┌─────────────┐ │
│ │Fitting Input│ │    │ │Fitting       │ │    │ │Product Search│ │
│ │POST /fitting│◄─────┤ │Controller    │ │    │ │    API      │ │
│ │GET /status  │ │    │ │GET /result   │ │    │ │             │ │
│ └─────────────┘ │    │ └──────────────┘ │    │ └─────────────┘ │
└─────────────────┘    │ ┌──────────────┐ │    │ ┌─────────────┐ │
                       │ │Queue Service │ │    │ │  FitRoom    │ │
                       │ │              │ │    │ │    API      │ │
                       │ │ ┌──────────┐ │ │    │ │(Virtual     │ │
                       │ │ │Redis Queue│ │ │    │ │ Fitting)    │ │
                       │ │ └──────────┘ │ │    │ └─────────────┘ │
                       │ └──────────────┘ │    └─────────────────┘
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
                       │ │- Product Cache│ │
                       │ └──────────────┘ │
                       │ ┌──────────────┐ │
                       │ │   AWS S3     │ │
                       │ │- Images      │ │
                       │ │- Fitting     │ │
                       │ │  Results     │ │
                       │ └──────────────┘ │
                       └──────────────────┘
```

## 🚀 주요 기능

### 💬 AI 기반 채팅 인터페이스
- **LLM 기반 질의응답**: 자연어로 패션 상담 및 스타일 추천
- **실시간 메시지 수신**: 
  - **폴링 방식**: 기존 `/chat/receive` API를 통한 상태 조회  
  - **스트림 방식**: `/api/chat/rooms/{roomId}/messages/stream` API를 통한 실시간 메시지 수신
- **다중 에이전트 시스템**: 
  - 스타일 분석가 (Style Analyst)
  - 트렌드 전문가 (Trend Expert) 
  - 컬러 전문가 (Color Expert)
  - 핏팅 코디네이터 (Fitting Coordinator)
- **SSE 지원**: 실시간으로 AI 응답을 클라이언트에 전송하여 사용자 경험 향상

### 🎨 패션 큐레이션 & 추천
- **RAG 기반 지식 검색**: 패션 지식베이스를 활용한 정확한 추천
- **상황별 맞춤 추천**: 소개팅, 면접, 데이트 등 상황별 최적 스타일
- **상품 연동**: 추천 스타일에 맞는 실제 상품 이미지 제공 (다중 이미지 지원)
- **상품 정보 캐싱**: Redis를 통한 상품 정보 자동 캐싱 및 빠른 조회
- **개인화 분석**: 사용자 체형, 피부톤, 선호도 기반 맞춤 추천

### 📸 이미지 처리 & 가상 피팅
- **이미지 업로드**: AWS S3를 통한 안전한 이미지 저장
- **이미지 기반 분석**: 업로드된 이미지를 통한 스타일 분석
- **가상 피팅 시뮬레이션**: AI 기반 가상 피팅 기술로 실제 착용 모습 시뮬레이션
- **피팅 결과 제공**: 가상 피팅 완료 후 결과 이미지 및 상태 정보 제공
- **실시간 피팅 처리**: 비동기 작업을 통한 효율적인 가상 피팅 처리

### 🔐 사용자 관리 & 인증
- **카카오 OAuth 로그인**: HttpOnly 쿠키 기반 보안 인증
- **JWT 토큰 관리**: 안전한 토큰 생성 및 검증
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
- **FitRoom API**: AI 기반 가상 피팅 시뮬레이션 서비스

### Development Tools
- **Swagger/OpenAPI**: API 문서화
- **Lombok**: 보일러플레이트 코드 감소
- **Jackson**: JSON 직렬화/역직렬화

## 📡 API 엔드포인트

### 채팅 관련
- `POST /api/chat/send` - 메시지 전송 및 큐 저장
- `GET /api/chat/receive` - AI 응답 메시지 수신 (폴링)
- `GET /api/chat/rooms/{roomId}/messages/stream` - **스트림 API: 특정 방에서 실시간 AI 응답 수신**
- `GET /api/chat/rooms/messages/stream` - **스트림 API: 자동 방 생성 및 실시간 AI 응답 수신**
- `GET /api/chat/rooms/history` - 채팅방 히스토리 조회
- `GET /api/chat/rooms/{roomId}/messages` - 채팅방 메시지 목록 조회 (무한 스크롤)

### 상품 관리
- `GET /api/chat/product/{productId}` - Redis 캐시된 상품 정보 조회

### 이미지 처리 & 가상 피팅
- `POST /api/chat/upload` - 이미지 파일 업로드 (S3)
- `POST /api/fitting/start` - 가상 피팅 시작 (비동기 처리)
- `GET /api/fitting/status/{taskId}` - 가상 피팅 상태 조회
- `GET /api/fitting/result/{taskId}` - 가상 피팅 결과 조회

### 인증 관련
- `GET /api/auth/kakao/callback` - 카카오 로그인 콜백 처리
- `GET /api/auth/me` - 현재 사용자 정보 조회
- `POST /api/auth/logout` - 로그아웃

## 🎨 프론트엔드 개발자 가이드

### 카카오 로그인 구현 방법

#### 1. 로그인 버튼 구현
```html
<button onclick="handleKakaoLogin()">카카오로 로그인</button>
```

```javascript
function handleKakaoLogin() {
    const kakaoAuthURL = 'https://kauth.kakao.com/oauth/authorize?' +
        'client_id=YOUR_KAKAO_CLIENT_ID&' +
        'redirect_uri=https://the-second-take.com/api/auth/kakao/callback&' +
        'response_type=code';
    
    window.location.href = kakaoAuthURL;
}
```

#### 2. 사용자 정보 조회
```javascript
async function getCurrentUser() {
    try {
        const response = await fetch('/api/auth/me', {
            method: 'GET',
            credentials: 'include', // 쿠키 포함 필수!
            headers: {
                'Content-Type': 'application/json'
            }
        });

        const data = await response.json();
        
        if (response.ok && data.status === 'success') {
            const user = data.data;
            console.log('사용자 ID:', user.userId);
            console.log('닉네임:', user.nickname);
            return user;
        } else {
            // 로그인되지 않은 사용자
            return null;
        }
    } catch (error) {
        console.error('사용자 정보 조회 실패:', error);
        return null;
    }
}
```

#### 3. 로그아웃 구현 (완전한 로그아웃)
```javascript
async function logout() {
    try {
        // 1. 서버 측 로그아웃 (JWT 쿠키 삭제)
        const response = await fetch('/api/auth/logout', {
            method: 'POST',
            credentials: 'include', // 쿠키 포함 필수!
            headers: {
                'Content-Type': 'application/json'
            }
        });

        // 2. 카카오 로그아웃 (카카오 측 세션도 삭제)
        if (window.Kakao && window.Kakao.Auth) {
            window.Kakao.Auth.logout();
        }
        
        if (response.ok) {
            alert('로그아웃되었습니다.');
            window.location.href = '/login';
        }
    } catch (error) {
        console.error('로그아웃 실패:', error);
    }
}
```

**카카오 SDK 추가 (HTML에 포함):**
```html
<script src="https://developers.kakao.com/sdk/js/kakao.js"></script>
<script>
    Kakao.init('YOUR_KAKAO_CLIENT_ID');
</script>
```

#### 4. 페이지 로드 시 사용자 상태 확인
```javascript
window.onload = async function() {
    const user = await getCurrentUser();
    
    if (user) {
        // 로그인된 사용자
        document.getElementById('user-info').innerHTML = `
            <p>안녕하세요, ${user.nickname}님!</p>
            <button onclick="logout()">로그아웃</button>
        `;
    } else {
        // 로그인되지 않은 사용자
        document.getElementById('login-section').style.display = 'block';
    }
};
```

### ⚠️ 중요 사항

1. **credentials: 'include' 필수**: 모든 API 호출 시 쿠키를 포함해야 합니다.
2. **HTTPS 환경**: 프로덕션에서는 반드시 HTTPS를 사용해야 합니다.
3. **에러 처리**: 네트워크 오류와 인증 오류를 구분하여 처리하세요.
4. **토큰 만료**: JWT 토큰은 7일 후 자동 만료됩니다.

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


#### 스트림 API 응답 (rooms/{roomId}/messages/stream, rooms/messages/stream)
**응답 형식**: `text/event-stream` (Server-Sent Events)

**이벤트 타입별 메시지**:

1. **연결 성공** (`connect` 이벤트):
```
event: connect
data: {
  "status": "success",
  "message": "요청 성공",
  "data": {
    "message": "SSE 연결 성공",
    "type": "connect",
    "timestamp": 1757045016039
  }
}
```

2. **방 정보** (`room` 이벤트) - 자동 방 생성 API에서만:
```
event: room
data: {
  "status": "success",
  "message": "요청 성공",
  "data": {
    "room_id": 259,
    "type": "room",
    "timestamp": 1757045016039
  }
}
```

3. **AI 응답 스트리밍** (`content` 이벤트):
```
event: content
data: {
  "status": "success",
  "message": "요청 성공",
  "data": {
    "agent_id": "style_analyst",
    "agent_name": "스타일 분석가",
    "message": "소개팅에 어울리는 스타일을 분석해보겠습니다...",
    "type": "content",
    "timestamp": 1757045028619
  }
}
```

4. **완료 및 상품 추천** (`complete` 이벤트):
```
event: complete
data: {
  "status": "success",
  "message": "요청 성공",
  "data": {
    "agent_id": "style_analyst",
    "agent_name": "스타일 분석가",
    "message": "브라운 린넨 반팔 셔츠에 그레이 와이드 슬랙스가 잘 어울려...",
    "products": [
      {
        "product_url": "https://sw-fashion-image-data.s3.amazonaws.com/TOP/1002/4989731/segment/4989731_seg_001.jpg",
        "product_id": "4989731"
      },
      {
        "product_url": "https://sw-fashion-image-data.s3.amazonaws.com/BOTTOM/3002/4557903/segment/4557903_seg_003.jpg",
        "product_id": "4557903"
      }
    ]
  }
}
```

5. **에러** (`error` 이벤트):
```
event: error
data: {
  "status": "fail",
  "message": "스트림 처리 오류: [에러 메시지]",
  "data": null
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

#### 가상 피팅 시작 (fitting/start API)
```json
{
  "status": "success",
  "message": "가상 피팅이 시작되었습니다.",
  "data": {
    "task_id": "fitting-task-12345",
    "status": "PROCESSING",
    "estimated_time": "30-60초"
  }
}
```

#### 가상 피팅 상태 조회 (fitting/status API)
```json
{
  "status": "success",
  "message": "가상 피팅 상태를 조회했습니다.",
  "data": {
    "task_id": "fitting-task-12345",
    "status": "COMPLETED",
    "progress": 100,
    "created_at": "2024-01-15T10:00:00Z",
    "completed_at": "2024-01-15T10:01:30Z"
  }
}
```

#### 가상 피팅 결과 조회 (fitting/result API)
```json
{
  "status": "success",
  "message": "가상 피팅 결과를 성공적으로 조회했습니다.",
  "data": {
    "task_id": "fitting-task-12345",
    "status": "COMPLETED",
    "result_image_url": "https://fitroom-results.s3.amazonaws.com/results/fitting-task-12345.jpg",
    "input_images": {
      "person_image": "https://user-uploads.s3.amazonaws.com/person-123.jpg",
      "clothing_images": [
        "https://sw-fashion-image-data.s3.amazonaws.com/TOP/1002/4227290/segment/0_17.jpg"
      ]
    }
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

### 6. 가상 피팅 처리 흐름
```
POST /fitting/start → SimpleFittingController → FitRoomApiClient → 
FitRoom API 호출 → Task ID 반환 → 비동기 처리 시작

GET /fitting/status/{taskId} → SimpleFittingController → FitRoomApiClient → 
FitRoom API 상태 조회 → 처리 상태 반환

GET /fitting/result/{taskId} → SimpleFittingController → FitRoomApiClient → 
FitRoom API 결과 조회 → 피팅 결과 이미지 URL 반환
```

### 7. 스트림 API 처리 흐름
```
GET /rooms/{roomId}/messages/stream → ChatController → 
세션 기반 사용자 생성/조회 → 사용자 메시지 DB 저장 → 
외부 AI 스트림 API 호출 → 실시간 응답 스트리밍 → 
상품 검색 → 상품 정보 Redis 캐싱 → AI 응답 DB 저장 → 
SSE 이벤트 전송 (connect, content, complete, error)
```

### 8. 자동 방 생성 스트림 API 처리 흐름
```
GET /rooms/messages/stream → ChatController → 
세션 기반 사용자 생성/조회 → 채팅방 자동 생성 → 
방 정보 SSE 전송 (room 이벤트) → 사용자 메시지 DB 저장 → 
외부 AI 스트림 API 호출 → 실시간 응답 스트리밍 → 
상품 검색 → 상품 정보 Redis 캐싱 → AI 응답 DB 저장 → 
SSE 이벤트 전송 (connect, content, complete, error)
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

### v1.9.0 (2024-01-28) - SSE 커넥션 모니터링 및 최적화 시스템 구축
- **SSE 커넥션 풀 최적화를 위한 모니터링 시스템**:
  - SSE 연결 생명주기 추적 및 메트릭 수집
  - 연결당 메모리 사용량 측정 및 분석
  - 커넥션 풀 효율성 지표 수집 (히트율, 미스율)
  - 연결 생성/종료/타임아웃/에러 상세 추적
- **새로운 SSE 커넥션 메트릭**:
  - `sse_connections_total`: SSE 연결 총 생성 횟수
  - `sse_connections_active`: 현재 활성 SSE 연결 수
  - `sse_connection_lifetime`: SSE 연결 생명주기 시간
  - `sse_connection_memory_usage_bytes`: 연결당 메모리 사용량
  - `sse_connection_pool_hits_total`: 커넥션 풀 히트 횟수
  - `sse_connection_pool_misses_total`: 커넥션 풀 미스 횟수
  - `sse_connection_creation_duration`: 커넥션 생성 시간
  - `sse_connection_timeouts_total`: 타임아웃 발생 횟수
  - `sse_connection_errors_total`: 에러 발생 횟수
- **메모리 모니터링 강화**:
  - SSE API 처리 중 실시간 메모리 사용량 측정
  - 메모리 피크 발생 시 자동 경고 로그
  - JVM 힙 메모리와 SSE 연결 수 상관관계 분석
  - GC 발생 빈도와 SSE 연결 패턴 연관성 추적
- **프로메테우스/그라파나 연동**:
  - SSE 커넥션 최적화 전용 알림 규칙 설정
  - 메모리 사용률 임계값 기반 자동 알림
  - 커넥션 풀 효율성 모니터링 대시보드
  - 부하 테스트 시나리오별 성능 지표 수집

### v1.8.0 (2024-01-27) - 카카오 OAuth 로그인 시스템 구축
- **카카오 OAuth 로그인 구현**:
  - HttpOnly 쿠키 기반 보안 인증 시스템
  - JWT 토큰을 통한 사용자 인증 및 세션 관리
  - XSS 및 CSRF 공격 방지 보안 강화
- **새로운 인증 API 엔드포인트**:
  - `GET /api/auth/kakao/callback` - 카카오 로그인 콜백 처리
  - `GET /api/auth/me` - 현재 사용자 정보 조회
  - `POST /api/auth/logout` - 로그아웃
- **보안 기능**:
  - HttpOnly, Secure, SameSite 쿠키 설정
  - JWT 토큰 검증 및 사용자 정보 추출
  - 카카오 API 연동을 통한 안전한 사용자 인증
- **테스트 도구**:
  - `kakao-login-test.html` - 카카오 로그인 기능 테스트 페이지
  - 실시간 사용자 정보 조회 및 로그아웃 기능

### v1.7.0 (2024-01-26) - 스트림 API 및 실시간 채팅 시스템 구축
- **스트림 API 시스템 구축**:
  - `GET /api/chat/rooms/{roomId}/messages/stream` - 특정 방에서 실시간 AI 응답 수신
  - `GET /api/chat/rooms/messages/stream` - 자동 방 생성 및 실시간 AI 응답 수신
  - CommonResponse 형식으로 모든 스트림 이벤트 표준화
  - 사용자 메시지와 AI 응답을 PostgreSQL에 자동 저장
- **실시간 스트리밍 기능**:
  - `content` 이벤트: AI 응답 실시간 스트리밍
  - `complete` 이벤트: 최종 완료 및 상품 추천
  - `room` 이벤트: 방 정보 전송 (자동 방 생성 시)
  - `error` 이벤트: 에러 처리
- **다중 전문가 시스템**:
  - 스타일 분석가, 컬러 전문가, 핏팅 코디네이터 동시 실행
  - 각 전문가별 개별 응답 및 상품 추천
  - 전문가별 이름 및 역할 정보 포함
- **클라이언트 테스트 도구**:
  - `chat-multi-expert-test.html` - 다중 전문가 스트림 테스트
  - `sse-real-test.html`, `sse-test.html` - SSE 기능 테스트
  - 실시간 메시지 수신 및 상품 이미지 표시 기능

### v1.6.0 (2024-01-25) - 채팅 SSE 기반 실시간 메시지 수신 추가
- **채팅 SSE(Server-Sent Events) 지원**:
  - 실시간으로 AI 응답 메시지를 클라이언트에 전송
  - 폴링 방식 대비 사용자 경험 향상 및 서버 부하 감소
  - 이벤트 기반 메시지 전송으로 즉각적인 응답 제공
- **새로운 스트림 API 엔드포인트**:
  - `GET /api/chat/rooms/{roomId}/messages/stream` - 특정 방에서 실시간 AI 응답 수신
  - `GET /api/chat/rooms/messages/stream` - 자동 방 생성 및 실시간 AI 응답 수신
- **클라이언트 테스트 도구**:
  - `chat-sse-test.html` - 채팅 SSE 기능을 테스트할 수 있는 웹 인터페이스 제공
  - 실시간 메시지 수신 및 상품 이미지 표시 기능

### v1.4.0 (2024-01-20) - 가상 피팅 기능 추가
- **가상 피팅 시스템 구축**:
  - FitRoom API 연동을 통한 AI 기반 가상 피팅 서비스
  - 비동기 처리를 통한 효율적인 피팅 작업 관리
  - 실시간 상태 조회 및 결과 확인 기능
- **새로운 API 엔드포인트**:
  - `POST /api/fitting/start` - 가상 피팅 시작
  - `GET /api/fitting/status/{taskId}` - 피팅 상태 조회  
  - `GET /api/fitting/result/{taskId}` - 피팅 결과 조회
- **환경 설정 강화**:
  - `FITROOM_API_KEY` 환경변수 추가로 보안 강화
  - CI/CD 파이프라인에 가상 피팅 관련 환경변수 통합

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
- 카카오 개발자 계정

## 🔐 카카오 로그인 설정

### 1. 카카오 개발자 콘솔 설정

1. **애플리케이션 등록**
   - https://developers.kakao.com 접속
   - 카카오 계정으로 로그인
   - "내 애플리케이션" → "애플리케이션 추가하기"
   - 앱 이름 입력 (예: "TheSecondTake") → 저장

2. **앱 키 확인**
   - 생성된 앱 클릭
   - "앱 키" 탭에서 REST API 키 복사
   - "보안" 탭에서 Client Secret 생성 → 복사

3. **플랫폼 및 리다이렉트 URI 설정**
   - "제품 설정" → "카카오 로그인" 활성화
   - "Redirect URI" 등록:
     - 개발: `https://the-second-take.com/api/auth/kakao/callback`
     - 운영: `https://the-second-take.com/api/auth/kakao/callback`

### 2. GitHub Actions Secrets 설정

GitHub 저장소의 Settings → Secrets and variables → Actions에서 다음 시크릿을 추가하세요:

```
KAKAO_CLIENT_ID=your_kakao_rest_api_key
KAKAO_CLIENT_SECRET=your_kakao_client_secret
KAKAO_REDIRECT_URI=https://the-second-take.com/api/auth/kakao/callback
JWT_SECRET=your_jwt_secret_key_min_256_bits
```

### 3. 로컬 개발 환경 설정

로컬에서 테스트할 때는 다음 환경변수를 설정하세요:

```bash
export KAKAO_CLIENT_ID=your_kakao_rest_api_key
export KAKAO_CLIENT_SECRET=your_kakao_client_secret
export KAKAO_REDIRECT_URI=http://localhost:8000/api/auth/kakao/callback
export JWT_SECRET=your_jwt_secret_key_min_256_bits
```

### 4. 테스트

1. 애플리케이션 실행 후 `http://localhost:8000/kakao-login-test.html` 접속
2. "카카오로 로그인" 버튼 클릭
3. 카카오 로그인 진행
4. 성공 시 사용자 정보 확인

### 5. API 문서 확인

- **Swagger UI**: `http://localhost:8000/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8000/v3/api-docs`
- 인증 관련 API는 "인증 관리" 태그에서 확인 가능

### 6. 모니터링 설정 (프로메테우스 + 그라파나)

#### 로컬 모니터링 환경 구축
```bash
# 1. 모니터링 스택 실행
docker-compose -f docker-compose.monitoring.yml up -d

# 2. Spring Boot 애플리케이션 실행 (별도 터미널)
cd thefirsttake
./gradlew bootRun
```

#### 접속 URL
- **프로메테우스**: http://localhost:9090
- **그라파나**: http://localhost:3000 (admin/admin)
- **메트릭 엔드포인트**: http://localhost:8000/actuator/prometheus

#### 주요 메트릭

**인증 관련:**
- `kakao_login_success_total`: 카카오 로그인 성공 횟수
- `kakao_login_failure_total`: 카카오 로그인 실패 횟수
- `logout_total`: 로그아웃 횟수
- `jwt_token_generation_duration`: JWT 토큰 생성 시간

**채팅 SSE 관련:**
- `sse_connections_total`: SSE 연결 총 횟수
- `sse_disconnections_total`: SSE 연결 해제 총 횟수
- `sse_connection_duration`: SSE 연결 지속 시간
- `sse_api_total_response_duration`: SSE API 전체 응답 시간
- `sse_api_memory_usage_bytes`: SSE API 메모리 사용량
- `sse_api_memory_peak_total`: 메모리 피크 발생 횟수
- `sse_connections_active`: 현재 활성 SSE 연결 수
- `sse_connection_lifetime`: SSE 연결 생명주기 시간
- `sse_connection_memory_usage_bytes`: 연결당 메모리 사용량
- `sse_connection_pool_hits_total`: 커넥션 풀 히트 횟수
- `sse_connection_pool_misses_total`: 커넥션 풀 미스 횟수
- `sse_connection_creation_duration`: 커넥션 생성 시간
- `sse_connection_timeouts_total`: 타임아웃 발생 횟수
- `sse_connection_errors_total`: 에러 발생 횟수
- `llm_api_calls_total`: 외부 LLM API 호출 총 횟수
- `llm_api_success_total`: LLM API 호출 성공 횟수
- `llm_api_failure_total`: LLM API 호출 실패 횟수
- `llm_api_response_duration`: LLM API 응답 시간
- `product_search_api_calls_total`: 상품 검색 API 호출 총 횟수
- `product_search_api_success_total`: 상품 검색 API 성공 횟수
- `product_search_api_failure_total`: 상품 검색 API 실패 횟수
- `product_search_api_response_duration`: 상품 검색 API 응답 시간

**기본 메트릭:**
- `http_server_requests_seconds`: HTTP 요청 응답 시간
- `jvm_memory_used_bytes`: JVM 메모리 사용량
- `jvm_gc_pause_seconds`: GC 일시정지 시간

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
- LLM 서버 설정 (`llm.server.host`, `llm.server.port`)

**필수 환경변수:**
- `LLM_SERVER_HOST`: LLM 서버 호스트 주소
- `LLM_SERVER_PORT`: LLM 서버 포트 번호
- `POSTGRES_ENDPOINT`: PostgreSQL 서버 주소
- `POSTGRES_USER`: PostgreSQL 사용자명
- `POSTGRES_PASSWORD`: PostgreSQL 비밀번호
- `REDIS_ENDPOINT`: Redis 서버 주소
- `AWS_ACCESS_KEY`: AWS 액세스 키
- `AWS_SECRET_KEY`: AWS 시크릿 키
- `FITROOM_API_KEY`: FitRoom API 키
- `KAKAO_CLIENT_ID`: 카카오 개발자 콘솔 REST API 키
- `KAKAO_CLIENT_SECRET`: 카카오 개발자 콘솔 Client Secret
- `KAKAO_REDIRECT_URI`: 카카오 로그인 리다이렉트 URI
- `JWT_SECRET`: JWT 토큰 서명용 시크릿 키 (최소 256비트)

## 📊 모니터링 & 로깅

- **애플리케이션 로그**: Spring Boot 기본 로깅
- **API 호출 로그**: 상품 검색 API 호출 결과 및 이미지 URL 처리 로그
- **에러 추적**: 상세한 에러 로그 및 스택 트레이스
- **메시지 저장 로그**: AI 응답 및 상품 이미지 저장 상태 추적
- **SSE 커넥션 모니터링**: 실시간 연결 상태 및 메모리 사용량 추적
- **성능 메트릭**: Prometheus 기반 상세 성능 지표 수집
- **알림 시스템**: 메모리 사용률 및 커넥션 풀 효율성 기반 자동 알림

### 🔍 SSE 커넥션 최적화 모니터링

#### 핵심 지표
- **연결당 메모리 사용량**: SSE 연결 1개당 평균 메모리 소비량
- **커넥션 풀 효율성**: 히트율/미스율을 통한 연결 재사용 패턴 분석
- **메모리 피크 추적**: 80% 이상 메모리 사용 시 자동 경고
- **GC 패턴 분석**: SSE 연결과 가비지 컬렉션 발생 빈도 상관관계

#### 부하 테스트 시나리오
```bash
# SSE 연결 100개 생성하여 메모리 영향도 측정
for i in {1..100}; do
  curl -N "http://localhost:8000/api/chat/rooms/$i/messages/stream?user_input=테스트" &
done

# 30초 후 메트릭 확인
curl -s "http://localhost:9090/api/v1/query?query=sse_connections_active"
curl -s "http://localhost:9090/api/v1/query?query=jvm_memory_used_bytes{area=\"heap\"}"
```

#### 최적화 목표
- **연결당 메모리**: 0.5MB → 0.1MB (80% 감소)
- **최대 동시 연결**: 1,000개 → 10,000개 (10배 증가)
- **커넥션 생성 시간**: 200ms → 50ms (75% 감소)
- **커넥션 풀 히트율**: 0% → 80% (80% 증가)

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
- **SSE 커넥션 모니터링**: 실시간 연결 상태 추적으로 메모리 효율성 향상
- **메트릭 기반 최적화**: Prometheus 지표를 통한 데이터 기반 성능 개선

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
