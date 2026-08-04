<div align="center">

<img src="https://raw.githubusercontent.com/Likelion-YeungNam-Univ/14th-ISIX-was/main/docs/banner-closr-white.svg" width="100%" />

</div>

<br />

## 👥 팀원 소개

<div align="center">

<table>
  <tr>
    <td align="center" width="120"><a href="https://github.com/knayoung0"><img src="https://github.com/knayoung0.png" width="100"/><br/>구나영</a></td>
    <td align="center" width="120"><a href="https://github.com/copepb"><img src="https://github.com/copepb.png" width="100"/><br/>김민호</a></td>
    <td align="center" width="120"><a href="https://github.com/ryudayeong"><img src="https://github.com/ryudayeong.png" width="100"/><br/>류다영</a></td>
    <td align="center" width="120"><a href="https://github.com/hyeonseo-sung"><img src="https://github.com/hyeonseo-sung.png" width="100"/><br/>성현서</a></td>
    <td align="center" width="120"><a href="https://github.com/ckrhkdwls"><img src="https://github.com/ckrhkdwls.png" width="100"/><br/>차광진</a></td>
    <td align="center" width="120"><a href="https://github.com/user070917"><img src="https://github.com/user070917.png" width="100"/><br/>황연준</a></td>
  </tr>
</table>

</div>

## 🎯 프로젝트 소개

> **내 몸 위의 3D 가상 아틀리에**

한 장의 사진으로 3D 아바타를 생성하고, 물리 시뮬레이션 기반으로 의류 사이즈를 추천하는 가상 피팅 플랫폼입니다.

기존 가상 피팅은 대부분 2D 이미지 합성으로, 옷과 몸의 공간 관계를 계산하지 않습니다.
**CLOSER는 합성이 아니라 물리 연산을 합니다.**

<br />

## ✨ 핵심 기능

| | 기능 | 내용 |
|:---:|---|---|
| **①** | **신체 비율 판단** | 전신 사진 1장 + 키·몸무게로 3D 아바타 생성, 12개 부위 치수 자동 계측 |
| **②** | **체형 유형 진단** | 역삼각형·직사각형·모래시계형 등 체형 타입 판정 |
| **③** | **가상 피팅** | 아바타에 의류 착용 · 360° 회전 · 핏 히트맵 · 사이즈 추천 |
| **④** | **피팅 저장** | 아바타와 피팅 결과를 계정에 저장, 재방문 시 복원 |

<br />

## 🏗 AI 아키텍처

**[사전 계산 · GPU]** 표준 마네킹 → 패턴 18개 → 체형 12구간 드레이핑 → GLB 216개

**[실시간 · CPU]** 사진 → β → 3D 메시 → 치수 12개 → 최근접 GLB 조회

의류 시뮬레이션은 1벌당 30초~2분 소요 → 런타임 실행 불가.
의류 6종 × 사이즈 3 × 체형 12구간 = **216개 사전 계산**, 실시간엔 최근접 결과 조회.

<br />

## 🚀 로컬 실행

```bash
# 1. 레포 클론
git clone https://github.com/Likelion-YeungNam-Univ/14th-ISIX-ai.git
git clone https://github.com/Likelion-YeungNam-Univ/14th-ISIX-was.git
git clone https://github.com/Likelion-YeungNam-Univ/14th-ISIX-web.git

# 2. AI 서버 (Python 3.9+)
cd 14th-ISIX-ai
python -m venv venv && source venv/bin/activate
pip install -c constraints.txt -r requirements.txt   # -c 필수
cp .env.example .env
uvicorn app.main:app --reload --port 8000

# 3. BE 서버 (Java 17+, Gradle)
cd 14th-ISIX-was && ./gradlew bootRun

# 4. FE
cd 14th-ISIX-web && npm install && npm run dev
```

> **AI 서버 두 가지 주의**
> - `pip install` 에 `-c constraints.txt` 를 빼면 numpy가 2.x로 올라가 mediapipe가 동작하지 않습니다.
> - SMPL-X 모델은 재배포 금지 라이선스라 저장소에 없습니다.
>   [직접 다운로드](https://smpl-x.is.tue.mpg.de) 후 `assets/models/smplx/SMPLX_FEMALE.npz` 에 배치하세요.

<br />

---

## ⚙️ 이 저장소 — 백엔드 (Spring Boot)

3D 가상 피팅 플랫폼 API 서버입니다.

멋쟁이사자처럼 해커톤 ISIX 2026

---

## 시스템 구조

AI 파이프라인은 Python 으로 작성되어 있어 Spring 에서 직접 호출할 수 없습니다.
별도 FastAPI 서버로 분리하고 HTTP 로 통신합니다.

```
[React]  ──HTTP──→  [Spring Boot]  ──HTTP──→  [FastAPI · AI]
                          │                    사진 → 3D 아바타
                          ├── PostgreSQL       12부위 치수 계측
                          └── Cloudflare R2
```

| 서버 | 역할 |
|---|---|
| Spring Boot | 회원 · 인증, 의류, 피팅, 사이즈 추천, DB |
| FastAPI (AI) | 사진 → 3D 아바타 → 치수 12개 |

---

## 실행 방법

### 1. 환경변수

```bash
cp .env.example .env
```

JWT 시크릿 생성:

```bash
openssl rand -base64 32
```

### 2. 실행

```bash
./gradlew bootRun
```

| | |
|---|---|
| API 문서 | http://localhost:8080/docs |
| H2 콘솔 | http://localhost:8080/h2-console |

H2 콘솔 접속 정보는 `application.yml` 의 datasource 설정과 동일합니다.

### 3. AI 서버 함께 실행

아바타 생성 기능을 쓰려면 AI 서버도 켜져 있어야 합니다.

```bash
# 별도 터미널
cd ../14th-ISIX-ai
uvicorn app.main:app --reload --port 8000
```

---

## 폴더 구조

```
src/main/java/com/closer/
├─ CloserApplication.java
├─ global/
│  ├─ config/            Web(CORS), JPA, Swagger, RestClient
│  ├─ exception/         ErrorCode, CustomException, GlobalExceptionHandler
│  ├─ security/          JWT Provider, Filter
│  └─ common/            ApiResponse, BaseTimeEntity
├─ domain/
│  ├─ user/              회원 · 인증
│  ├─ avatar/            아바타 생성 · 조회
│  ├─ garment/           의류 정보
│  └─ fitting/           피팅 결과 · 사이즈 추천
├─ infra/
│  ├─ ai/                AI 서버 클라이언트
│  └─ storage/           R2 · CDN
└─ api/                  Swagger 명세 인터페이스
```

각 도메인은 `controller` / `service` / `repository` / `entity` / `dto` 로 나눕니다.

---

## 응답 형식

프론트와의 약속입니다. 모든 응답을 `ApiResponse<T>` 로 감쌉니다.

**성공**

```json
{ "success": true, "data": { }, "error": null }
```

**실패**

```json
{
  "success": false,
  "data": null,
  "error": { "code": "AVATAR_NOT_FOUND", "message": "아바타를 찾을 수 없습니다" }
}
```

---

## 스웨거 명세

**컨트롤러에 스웨거 어노테이션을 직접 작성하지 않습니다.**
`api` 패키지에 인터페이스를 만들어 명세를 작성하고, 컨트롤러가 이를 구현합니다.

```java
// api/AvatarApi.java
@Tag(name = "Avatar")
public interface AvatarApi {
    @Operation(summary = "아바타 생성 요청")
    ResponseEntity<ApiResponse<ResponseJobDto>> create(RequestAvatarDto dto);
}

// domain/avatar/controller/AvatarController.java
@RestController
public class AvatarController implements AvatarApi { ... }
```

컨트롤러가 어노테이션으로 지저분해지지 않고, 명세를 한곳에서 관리할 수 있습니다.

---

## 아바타 생성은 비동기

처리에 최대 30초가 걸립니다. 동기 응답 시 게이트웨이 타임아웃이 발생합니다.

```
POST /api/v1/avatars        → 202 + jobId 즉시 반환
GET  /api/v1/avatars/{job}  → 프론트가 2초 간격 폴링
```

배포 후에 드러나는 문제이므로 설계 단계에서 확정합니다.

---

## 체형 매칭 주의사항

`body_grid.json` 의 구간 대표 치수로 사이즈를 계산하면 안 됩니다.

**키 양자화 오차가 최대 7.4cm** 로 허용오차의 두 배에 가깝습니다.
격자는 "보여주는 몸"이지 "재는 몸"이 아닙니다.

```
✅ 사이즈 추천 · 핏 리포트  →  사용자별 measurements
❌ 구간 대표 치수
```

에러가 나지 않고 조용히 틀리는 부분이라 특히 주의합니다.

---

## 개인정보

**원본 사진을 저장하지 않습니다.**

전신 사진은 민감정보입니다. AI 서버가 체형 파라미터 추출 후 즉시 폐기하고,
DB 에는 숫자(β 10개 + 치수 12개)만 저장합니다.

`avatars` 테이블에 사진 컬럼 자체를 만들지 않습니다.

---

## 주의사항

**`.env` 와 `application-local.yml` 을 커밋하지 않습니다.**
저장소가 Public 이므로 시크릿이 한 번 올라가면 히스토리에 영구히 남습니다.
`git rm` 으로는 지워지지 않고, 노출 시 키를 즉시 재발급해야 합니다.

**배포 환경에서 `ddl-auto: create` 를 사용하지 않습니다.**
데이터가 전부 삭제됩니다.

**AI 서버 타임아웃을 60초 이상으로 둡니다.**
기본값(보통 5~10초)이면 정상 요청도 실패합니다.

---

## 라이선스

MIT License. `LICENSE` 참고.

AI 파트(`14th-ISIX-ai`)가 사용하는 SMPL-X 모델은 **비상업 학술 라이선스**로
별도 조건이 적용됩니다. 상업적 이용 검토 시 해당 저장소의 라이선스 항목을 확인하세요.

<br />

<div align="center">

<img src="https://raw.githubusercontent.com/Likelion-YeungNam-Univ/14th-ISIX-was/main/docs/footer.svg" width="100%" />

</div>
