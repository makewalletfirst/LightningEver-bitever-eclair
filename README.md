# LightningEver-bitever-eclair (LSP Server)

BitEver (BEC) L1 체인 전용 라이트닝 네트워크 LSP(Liquidity Service Provider) 서버입니다. 
ACINQ Eclair v0.13.1을 기반으로 비트에버 체인 특성에 맞게 커스텀 패치되었습니다.

## 🚀 주요 구현 사항
- **BitEver 체인 파라미터 적용:** Magic Bytes (`0xe2c3a9fc`), Genesis Hash, Bech32 HRP 등 적용.
- **연결 유지 패치 (Forced Sync):** 비트코인 메인넷과 다른 체인워크 환경에서도 피어 연결이 끊기지 않도록 `PeerConnection` 로직 수정.
- **수수료 정책 최적화:** 비트에버 노드의 초기 데이터 부족으로 인한 수수료 추정 실패를 방지하기 위해 기본 수수료(Default Feerates) 강제 적용.
- **피닉스 앱 호환성:** `ChannelFundingPlugin`을 통한 피닉스 전용 스왑 프로토콜(35017) 지원.

## 🛠 기술 스택
- **언어:** Scala 2.13
- **프레임워크:** Akka, Akka HTTP
- **빌드 도구:** Maven 3.x
- **데이터베이스:** SQLite
- **자바 버전:** OpenJDK 21

## 🏗 빌드 및 실행 방법
### 사전 요구사항
- 비트에버 L1 노드 실행 중 (RPC/ZMQ 활성화)
- Java 21 설치

### 빌드
```bash
mvn clean install -DskipTests
```

### 실행
```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
cd eclair-node/target/eclair-node-0.13.1-9830aa6
java -cp "lib/*" fr.acinq.eclair.Boot <plugin_path>
```

## ⚙️ CI/CD (GitHub Actions)
`.github/workflows/build.yml` 파일을 통해 코드 변경 시 자동으로 Maven 빌드 및 유효성 검사를 수행합니다.
