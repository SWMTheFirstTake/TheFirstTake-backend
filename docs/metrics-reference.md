# 메트릭 참조 가이드 (Prometheus & Grafana용)

이 문서는 `/actuator/prometheus` 엔드포인트를 통해 노출되는 모든 메트릭의 타입과 특성을 설명합니다.

## 메트릭 타입 설명

- **Counter**: 누적 카운터 (증가만 가능, 시간에 따라 증가)
- **Gauge**: 현재 값 (증가/감소 가능, 현재 상태를 나타냄)
- **Timer**: 시간 측정 (Histogram으로 변환, _seconds, _count, _sum, _max, _bucket으로 노출)
- **DistributionSummary**: 분포 측정 (Histogram으로 변환, _bytes, _count, _sum, _max, _bucket으로 노출)

---

## 1. 인증 관련 메트릭

| 메트릭 이름 | 타입 | Prometheus 타입 | 단위 | 설명 |
|------------|------|----------------|------|------|
| `kakao_login_success_total` | Counter | Counter | - | 카카오 로그인 성공 횟수 |
| `kakao_login_failure_total` | Counter | Counter | - | 카카오 로그인 실패 횟수 |
| `logout_total` | Counter | Counter | - | 로그아웃 횟수 |
| `jwt_token_generation_duration` | Timer | Histogram | seconds | JWT 토큰 생성 시간 |
| `api_requests_total` | Counter | Counter | - | 총 API 요청 수 (태그: `type=all`) |

### Timer 메트릭 설명
`jwt_token_generation_duration`는 Timer이므로 Prometheus에서 다음과 같이 노출됩니다:
- `jwt_token_generation_duration_seconds` (Histogram)
- `jwt_token_generation_duration_seconds_count` (총 측정 횟수)
- `jwt_token_generation_duration_seconds_sum` (총 시간 합계)
- `jwt_token_generation_duration_seconds_max` (최대 시간)
- `jwt_token_generation_duration_seconds_bucket` (백분위수 버킷)

---

## 2. SSE (Server-Sent Events) 관련 메트릭

### 2.1 연결 관리

| 메트릭 이름 | 타입 | Prometheus 타입 | 단위 | 설명 |
|------------|------|----------------|------|------|
| `sse_connections_total` | Counter | Counter | - | SSE 연결 총 횟수 (중복: 두 개의 Bean이 동일한 이름 사용) |
| `sse_disconnections_total` | Counter | Counter | - | SSE 연결 해제 총 횟수 |
| `sse_connections_active` | **Gauge** | Gauge | - | **현재 활성 SSE 연결 수** (실시간) |
| `sse_connection_duration` | Timer | Histogram | seconds | SSE 연결 지속 시간 |
| `sse_connection_lifetime` | Timer | Histogram | seconds | SSE 연결 생명주기 (생성부터 완료까지) |
| `sse_connection_creation_duration` | Timer | Histogram | seconds | 커넥션 생성 시간 |
| `sse_connection_timeouts_total` | Counter | Counter | - | 타임아웃 발생 횟수 |
| `sse_connection_errors_total` | Counter | Counter | - | 에러 발생 횟수 |

### 2.2 커넥션 풀

| 메트릭 이름 | 타입 | Prometheus 타입 | 단위 | 설명 |
|------------|------|----------------|------|------|
| `sse_connection_pool_hits_total` | Counter | Counter | - | 커넥션 풀 히트 횟수 (재사용) |
| `sse_connection_pool_misses_total` | Counter | Counter | - | 커넥션 풀 미스 횟수 (새로 생성) |

### 2.3 SSE API 성능

| 메트릭 이름 | 타입 | Prometheus 타입 | 단위 | 설명 |
|------------|------|----------------|------|------|
| `sse_api_total_requests` | Counter | Counter | - | SSE API 요청 총 횟수 |
| `sse_api_total_response_duration` | Timer | Histogram | seconds | SSE API 전체 응답 시간 (시작부터 완료까지) |
| `sse_api_success_total` | Counter | Counter | - | SSE API 성공 횟수 |
| `sse_api_failure_total` | Counter | Counter | - | SSE API 실패 횟수 |

### 2.4 메모리 및 GC

| 메트릭 이름 | 타입 | Prometheus 타입 | 단위 | 설명 |
|------------|------|----------------|------|------|
| `sse_api_memory_usage_bytes` | DistributionSummary | Histogram | bytes | SSE API 처리 중 메모리 사용량 |
| `sse_api_memory_peak_total` | Counter | Counter | - | 메모리 피크 발생 횟수 |
| `sse_api_gc_duration` | Timer | Histogram | seconds | SSE API 처리 중 GC 시간 |
| `sse_connection_memory_usage_bytes` | DistributionSummary | Histogram | bytes | 연결당 메모리 사용량 |

### DistributionSummary 메트릭 설명
`*_bytes` 메트릭은 DistributionSummary이므로 Prometheus에서 다음과 같이 노출됩니다:
- `sse_api_memory_usage_bytes` (Histogram)
- `sse_api_memory_usage_bytes_count` (총 측정 횟수)
- `sse_api_memory_usage_bytes_sum` (총 바이트 합계)
- `sse_api_memory_usage_bytes_max` (최대 바이트)
- `sse_api_memory_usage_bytes_bucket` (백분위수 버킷)

---

## 3. LLM API 관련 메트릭

### 3.1 호출 통계

| 메트릭 이름 | 타입 | Prometheus 타입 | 단위 | 설명 |
|------------|------|----------------|------|------|
| `llm_api_calls_total` | Counter | Counter | - | LLM API 호출 총 횟수 |
| `llm_api_calls_by_expert_total` | Counter | Counter | - | 전문가별 LLM API 호출 횟수 |
| `llm_api_success_total` | Counter | Counter | - | LLM API 호출 성공 횟수 |
| `llm_api_failure_total` | Counter | Counter | - | LLM API 호출 실패 횟수 |
| `llm_api_status_code_total` | Counter | Counter | - | HTTP 상태 코드별 LLM API 호출 횟수 |
| `llm_api_retries_total` | Counter | Counter | - | LLM API 재시도 횟수 |

### 3.2 성능 지표

| 메트릭 이름 | 타입 | Prometheus 타입 | 단위 | 설명 |
|------------|------|----------------|------|------|
| `llm_api_response_duration` | Timer | Histogram | seconds | LLM API 응답 시간 |
| `llm_api_response_size` | DistributionSummary | Histogram | bytes | LLM API 응답 크기 |

---

## 4. 상품 검색 API 관련 메트릭

| 메트릭 이름 | 타입 | Prometheus 타입 | 단위 | 설명 |
|------------|------|----------------|------|------|
| `product_search_api_calls_total` | Counter | Counter | - | 상품 검색 API 호출 총 횟수 |
| `product_search_api_success_total` | Counter | Counter | - | 상품 검색 API 성공 횟수 |
| `product_search_api_failure_total` | Counter | Counter | - | 상품 검색 API 실패 횟수 |
| `product_search_api_response_duration` | Timer | Histogram | seconds | 상품 검색 API 응답 시간 |

---

## 5. 데이터베이스 커넥션 풀 관련 메트릭

**모든 DB 커넥션 풀 메트릭은 Gauge 타입입니다** (실시간 상태를 나타냄)

| 메트릭 이름 | 타입 | Prometheus 타입 | 단위 | 설명 |
|------------|------|----------------|------|------|
| `db_connection_pool_active` | **Gauge** | Gauge | - | **현재 활성 DB 커넥션 수** |
| `db_connection_pool_idle` | **Gauge** | Gauge | - | **현재 유휴 DB 커넥션 수** |
| `db_connection_pool_total` | **Gauge** | Gauge | - | **현재 총 DB 커넥션 수** |
| `db_connection_pool_waiting` | **Gauge** | Gauge | - | **대기 중인 스레드 수** |
| `db_connection_pool_max` | **Gauge** | Gauge | - | **최대 DB 커넥션 수** (설정값) |
| `db_connection_pool_utilization_ratio` | **Gauge** | Gauge | - | **커넥션 풀 사용률** (0.0~1.0) |
| `db_connection_timeout_total` | **Gauge** | Gauge | - | DB 커넥션 타임아웃 총 횟수 (실제로는 Counter처럼 동작) |
| `db_connection_leaks_total` | Counter | Counter | - | DB 커넥션 누수 감지 횟수 |
| `db_connection_acquisition_duration` | Timer | Histogram | seconds | DB 커넥션 획득 시간 |
| `db_transaction_duration` | Timer | Histogram | seconds | DB 트랜잭션 지속 시간 |

---

## 6. JVM 메트릭 (Spring Boot 기본 제공)

`application.yaml`에서 percentiles-histogram이 활성화된 메트릭:

| 메트릭 이름 | 타입 | Prometheus 타입 | 단위 | 설명 |
|------------|------|----------------|------|------|
| `jvm_memory_used` | Gauge | Histogram | bytes | JVM 메모리 사용량 (percentiles-histogram 활성화) |
| `jvm_gc_pause` | Timer | Histogram | seconds | GC 일시 중지 시간 (percentiles-histogram 활성화) |
| `hikaricp_connections_*` | Gauge | Histogram | - | HikariCP 커넥션 풀 메트릭 (percentiles-histogram 활성화) |

### 활성화된 HikariCP 메트릭:
- `hikaricp.connections` (percentiles-histogram)
- `hikaricp.connections.active` (percentiles-histogram)
- `hikaricp.connections.idle` (percentiles-histogram)
- `hikaricp.connections.pending` (percentiles-histogram)

### 활성화된 SSE API 메트릭:
- `sse.api.total.response.duration` (percentiles-histogram)
- `sse.api.memory.usage.bytes` (percentiles-histogram)

---

## 7. 기본 Spring Boot 메트릭 (자동 수집)

| 메트릭 이름 | 타입 | Prometheus 타입 | 설명 |
|------------|------|----------------|------|
| `http_server_requests_seconds` | Timer | Histogram | HTTP 요청 처리 시간 |
| `jvm_memory_*` | Gauge | Gauge | JVM 메모리 사용량 (heap, non-heap 등) |
| `jvm_gc_*` | Timer/Gauge | Histogram/Gauge | GC 관련 메트릭 |
| `jvm_threads_*` | Gauge | Gauge | JVM 스레드 정보 |
| `process_cpu_usage` | Gauge | Gauge | CPU 사용률 |
| `system_cpu_usage` | Gauge | Gauge | 시스템 CPU 사용률 |
| `disk_*` | Gauge | Gauge | 디스크 사용량 |

---

## 8. 메트릭 태그

모든 메트릭에 자동으로 추가되는 태그:
- `application: thefirsttake`
- `service: chat-api`

---

## Grafana 대시보드 작성 팁

### Counter 메트릭 사용법
```promql
# 초당 증가율 계산
rate(kakao_login_success_total[5m])

# 특정 시간 동안의 총 증가량
increase(kakao_login_success_total[1h])
```

### Gauge 메트릭 사용법
```promql
# 현재 값 직접 사용
sse_connections_active

# 평균값 계산
avg_over_time(sse_connections_active[5m])
```

### Timer/Histogram 메트릭 사용법
```promql
# 평균 응답 시간
rate(jwt_token_generation_duration_seconds_sum[5m]) / rate(jwt_token_generation_duration_seconds_count[5m])

# 95th 백분위수 (percentiles-histogram 활성화 시)
histogram_quantile(0.95, rate(jwt_token_generation_duration_seconds_bucket[5m]))

# 99th 백분위수
histogram_quantile(0.99, rate(jwt_token_generation_duration_seconds_bucket[5m]))
```

### DistributionSummary/Histogram 메트릭 사용법
```promql
# 평균 메모리 사용량
rate(sse_api_memory_usage_bytes_sum[5m]) / rate(sse_api_memory_usage_bytes_count[5m])

# 95th 백분위수
histogram_quantile(0.95, rate(sse_api_memory_usage_bytes_bucket[5m]))
```

---

## 주의사항

1. **중복 메트릭 이름**: `sse_connections_total`은 두 개의 Bean에서 동일한 이름으로 등록되어 있습니다 (라인 54, 218). 이는 충돌을 일으킬 수 있으므로 확인이 필요합니다.

2. **Gauge vs Counter**: 
   - `db_connection_timeout_total`은 Gauge로 등록되어 있지만, 이름이 `_total`로 끝나고 설명이 "Total number"로 되어 있어 Counter처럼 사용하려는 의도로 보입니다. 실제로는 Gauge로 동작합니다.

3. **Timer와 DistributionSummary의 차이**:
   - Timer는 시간 측정용 (seconds 단위)
   - DistributionSummary는 크기/크기 측정용 (bytes 등)

4. **Percentiles-Histogram**: 
   - `application.yaml`에서 활성화된 메트릭들은 백분위수 계산을 위한 버킷이 자동으로 생성됩니다.
   - Grafana에서 `histogram_quantile()` 함수를 사용하여 백분위수를 계산할 수 있습니다.

