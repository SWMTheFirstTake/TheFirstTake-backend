# Infrastructure as Code - CloudFormation

이 디렉토리는 TheFirstTake 프로젝트의 AWS 인프라 구성을 정의하는 CloudFormation 템플릿을 포함합니다.

## 📁 파일 구조

```
infra/
├── README.md                    # 이 파일
└── clean-ecs-cluster.yaml      # ECS 클러스터 및 서비스 구성
```

## 🚀 CloudFormation 스택

### clean-ecs-cluster.yaml

**목적**: 테스트 환경용 ECS 클러스터 및 이중 서비스 구성 (ALB Access Logs 포함)

#### 주요 구성 요소

##### 1. ECS 클러스터
- **클러스터명**: `test-ecs-cluster` (기본값)
- **Capacity Providers**: FARGATE, FARGATE_SPOT
- **Container Insights**: 활성화
- **환경**: test

##### 2. 보안 그룹
- **TestECSSecurityGroup**: ECS 태스크용 보안 그룹
  - 포트 8000 (Backend), 6020 (LLM) 허용
- **TestALBSecurityGroup**: ALB용 보안 그룹
  - 포트 80, 443 허용

##### 3. IAM 역할
- **TestECSTaskExecutionRole**: 태스크 실행 역할
  - Secrets Manager 접근 권한
  - SSM Parameter Store 접근 권한
- **TestECSTaskRole**: 태스크 역할
  - S3 버킷 접근 권한

##### 4. 로드 밸런서
- **Application Load Balancer**: 인터넷 연결
- **Access Logs**: S3 버킷 `the-first-take-ecs-log` 아래 `alb-logs/` 프리픽스로 저장
- **Target Groups**:
  - Backend (포트 8000): `/api/*`, `/actuator/*` 경로
  - LLM (포트 6020): `/llm/*` 경로
  - Frontend (포트 80): 기본 경로

##### 5. ECS 서비스
- **Backend Service**: Spring Boot 애플리케이션
  - CPU: 1024, Memory: 2048MB
  - 이미지: ECR 다이제스트 고정(예: `.../thefirsttake-backend@sha256:...`)
- **LLM Service**: LLM 서버
  - CPU: 512, Memory: 1024MB
  - 이미지: ECR 다이제스트 고정(예: `.../thefirsttake-llm@sha256:...`)

##### 6. ECR 리포지토리
- `test-thefirsttake-backend` 자동 생성 (푸시 시 이미지 스캔 활성화)

##### 7. 환경변수 및 시크릿
- **환경변수**: 데이터베이스, Redis, LLM 서버 URL 등
- **시크릿**: 데이터베이스 패스워드, API 키 등 (Secrets Manager에서 관리)

참고:
- LLM 관련 URL은 ALB 경유 경로로 주입(`/llm/api/...`)

## 🔧 배포 방법

### 1. 사전 요구사항
- AWS CLI 설정 완료
- CloudFormation 템플릿에서 참조하는 VPC, 서브넷 존재
- ECR 리포지토리에 이미지 푸시 완료

### 2. 스택 배포
```bash
# 스택 생성
aws cloudformation create-stack \
  --stack-name test-ecs-cluster \
  --template-body file://clean-ecs-cluster.yaml \
  --parameters ParameterKey=ClusterName,ParameterValue=test-ecs-cluster \
               ParameterKey=VpcId,ParameterValue=vpc-0c31aa1692145be5e \
               ParameterKey=SubnetIds,ParameterValue="subnet-05bda01230da1b1b2,subnet-0af9f247125595c2b,subnet-068d9f9b8a76f8497,subnet-0d51f34ac2ba21ff0" \
  --capabilities CAPABILITY_NAMED_IAM

# 스택 업데이트
aws cloudformation update-stack \
  --stack-name test-ecs-cluster \
  --template-body file://clean-ecs-cluster.yaml \
  --parameters ParameterKey=ClusterName,ParameterValue=test-ecs-cluster \
               ParameterKey=VpcId,ParameterValue=vpc-0c31aa1692145be5e \
               ParameterKey=SubnetIds,ParameterValue="subnet-05bda01230da1b1b2,subnet-0af9f247125595c2b,subnet-068d9f9b8a76f8497,subnet-0d51f34ac2ba21ff0" \
  --capabilities CAPABILITY_NAMED_IAM
```

### 3. 배포 확인
```bash
# 스택 상태 확인
aws cloudformation describe-stacks --stack-name test-ecs-cluster

# 출력값 확인 (ALB URL 등)
aws cloudformation describe-stacks --stack-name test-ecs-cluster --query 'Stacks[0].Outputs'
```

주요 출력값(Outputs):
- `LoadBalancerURL`: `http://<ALB-DNS>` 형식의 URL
- `LoadBalancerDNS`: ALB DNS 이름
- `BackendServiceName`, `LLMServiceName`: 서비스명

## 🔍 모니터링

### CloudWatch 로그
- **로그 그룹**: `/ecs/test-ecs-cluster`
- **보존 기간**: 7일
- **로그 스트림**: `backend`, `llm`

### 헬스 체크
- **Backend**: `/actuator/health`
- **LLM**: `/llm/api/health`
- **Frontend**: `/`

프런트엔드 타깃 그룹은 예시로 정적 타깃(IP) 1개가 설정되어 있습니다. 실제 환경에 맞게 대상 등록을 조정하세요.

## 📊 아키텍처

```
Internet → ALB → ECS Services
                ├── Backend Service (Spring Boot)
                └── LLM Service (FastAPI)
```

### 라우팅 규칙
- `/api/*` → Backend Service
- `/actuator/*` → Backend Service  
- `/llm/*` → LLM Service
- `/` → Frontend (정적 파일)

## 🔐 보안

### 시크릿 관리
- **Secrets Manager**: 데이터베이스 패스워드, API 키
- **SSM Parameter Store**: 구성 정보

### 접근 권한
- **최소 권한 원칙**: 각 역할별 필요한 권한만 부여
- **네트워크 보안**: 보안 그룹을 통한 포트 제한

## 🚨 주의사항

1. **VPC 및 서브넷**: 템플릿에서 참조하는 VPC와 서브넷이 미리 존재해야 합니다.
2. **ECR 이미지**: 배포 전에 ECR 리포지토리에 최신 이미지가 푸시되어야 합니다.
3. **시크릿**: Secrets Manager에 필요한 시크릿들이 미리 생성되어야 합니다.
4. **비용**: FARGATE 서비스는 사용량에 따라 비용이 발생합니다.

## 📞 지원

인프라 관련 문의사항이 있으시면 개발팀에 연락해주세요.
