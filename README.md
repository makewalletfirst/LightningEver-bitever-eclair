# LightningEver-bitever-eclair (SAVE Branch)

BitEver Lightning Network의 LSP(Lightning Service Provider) 노드입니다.  
Phoenix 기반 LightningEver 앱과 `simple_taproot_phoenix` 채널 타입으로 연결됩니다.

## 핵심 수정 사항 (버그 수정 요약)

### 버그 #1 — NonceGenerator.scala: 키 순서 (비정렬)
`Musig2.generateNonce` 호출 시 `[local, remote]` 순서 유지 (sort 없음).

### 버그 #2 — Commitments.scala: partialSign nonce 순서 ★가장 중요★
`sortedKeys = sort([eclairKey, phoenixKey]) = [phoenix(idx0), eclair(idx1)]`이므로  
`publicNonces = [remoteNonce(phoenix), localNonce(eclair)]` 순서로 전달.

```scala
// RemoteCommit.sign 및 Commitment.sendCommit 내부
remoteCommitTx.partialSign(fundingKey, remoteFundingPubKey, localNonce,
  Seq(remoteNonce, localNonce.publicNonce))  // ✅ [phoenix(0), eclair(1)]
```

### 버그 #3 — Commitments.scala: fromCommitSig nonce 인덱스
`verificationNonce(..., localCommitIndex + 1)` 사용 (RevokeAndAck 전송 시와 동일).

### 버그 #4 — Commitments.scala: RevokeAndAck nonce 인덱스
`verificationNonce(..., localCommitIndex + 2)` 사용 (KMP와 동일).

### 버그 #5 — Transactions.scala: aggregateSigs 순서
`Seq(remoteSig.nonce, localSig.nonce)` = `[phoenix(0), eclair(1)]`.

## 빌드 방법

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./mvnw -DskipTests -pl eclair-core package
```

## 배포 방법

```bash
pkill -f 'fr.acinq.eclair.Boot'; sleep 2
cp eclair-core/target/eclair-core_2.13-0.13.1.jar \
   /root/bitever-eclair-dist/eclair-node-0.13.1-93cc2ab/lib/eclair-core_2.13-0.13.1.jar
cd /root/bitever-eclair-dist/eclair-node-0.13.1-93cc2ab/bin
nohup ./eclair-node.sh /root/.eclair/plugins/channel-funding-plugin-0.13.1.jar \
  >> /root/.eclair/eclair.log 2>&1 &
```

## 주요 수정 파일

- `eclair-core/src/main/scala/fr/acinq/eclair/crypto/NonceGenerator.scala`
- `eclair-core/src/main/scala/fr/acinq/eclair/channel/Commitments.scala`
- `eclair-core/src/main/scala/fr/acinq/eclair/transactions/Transactions.scala`

## ⚠️ 프로덕션 전 필수 작업
`Transactions.scala`의 `checkRemoteSig` bypass 제거 후 실제 검증 코드 복원.

## 전체 구조
`SAVEL_LN.md` 참조 (레포 루트).
