package fr.acinq.eclair.transactions

import fr.acinq.bitcoin.SigHash
import fr.acinq.bitcoin.scalacompat.Crypto.{PrivateKey, PublicKey, XonlyPublicKey}
import fr.acinq.bitcoin.scalacompat.Musig2.{IndividualNonce, LocalNonce, SecretNonce}
import fr.acinq.bitcoin.scalacompat._
import scodec.bits.ByteVector

case class SwapInProtocol(userPublicKey: PublicKey, serverPublicKey: PublicKey, userRefundKey: PublicKey, refundDelay: Int) {
  val sortedKeys: Seq[PublicKey] = Scripts.sort(Seq(userPublicKey, serverPublicKey))
  private val internalPublicKey: XonlyPublicKey = Musig2.aggregateKeys(sortedKeys)

  private val refundScript: Seq[ScriptElt] = Seq(
    OP_PUSHDATA(userRefundKey.xOnly),
    OP_CHECKSIGVERIFY,
    OP_PUSHDATA(Script.encodeNumber(refundDelay.toLong)),
    OP_CHECKSEQUENCEVERIFY
  )
  private val scriptLeaf = ScriptTree.Leaf(refundScript)
  private val scriptTree: ScriptTree = scriptLeaf

  val pubkeyScript: Seq[ScriptElt] = Script.pay2tr(internalPublicKey, Some(scriptTree))
  val serializedPubkeyScript: ByteVector = Script.write(pubkeyScript)

  def address(chainHash: ByteVector32): String = {
    val prefix = if (chainHash == Block.LivenetGenesisBlock.hash.value) "bc"
    else if (chainHash == Block.Testnet3GenesisBlock.hash.value || chainHash == Block.SignetGenesisBlock.hash.value) "tb"
    else "bcrt"
    fr.acinq.bitcoin.Bech32.encodeWitnessAddress(prefix, 1, Script.write(Script.pay2tr(internalPublicKey, None)).drop(2).toArray)
  }

  def witness(fundingTx: Transaction, index: Int, parentTxOuts: Seq[TxOut], userNonce: IndividualNonce, serverNonce: IndividualNonce, userPartialSig: ByteVector32, serverPartialSig: ByteVector32): Either[Throwable, ScriptWitness] = {
    val publicNonces = Scripts.sortNonces(Seq(userPublicKey -> userNonce, serverPublicKey -> serverNonce))
    val sigs = Seq(userPublicKey -> userPartialSig, serverPublicKey -> serverPartialSig)
      .sortWith { case ((k1, _), (k2, _)) => fr.acinq.bitcoin.scalacompat.LexicographicalOrdering.isLessThan(k1.value, k2.value) }
      .map(_._2)
    Musig2.aggregateTaprootSignatures(sigs, fundingTx, index, parentTxOuts, sortedKeys, publicNonces, Some(scriptTree)).map { aggregateSig =>
      Script.witnessKeyPathPay2tr(aggregateSig)
    }
  }

  def witnessRefund(userSig: ByteVector64): ScriptWitness =
    Script.witnessScriptPathPay2tr(internalPublicKey, scriptLeaf, ScriptWitness(Seq(userSig)), scriptTree)

  def signSwapInputUser(fundingTx: Transaction, index: Int, parentTxOuts: Seq[TxOut], userPrivateKey: PrivateKey, privateNonce: SecretNonce, userNonce: IndividualNonce, serverNonce: IndividualNonce): Either[Throwable, ByteVector32] = {
    scala.util.Try {
      val publicNonces = Scripts.sortNonces(Seq(userPublicKey -> userNonce, serverPublicKey -> serverNonce))
      Musig2.signTaprootInput(userPrivateKey, fundingTx, index, parentTxOuts, sortedKeys, privateNonce, publicNonces, Some(scriptTree))
    }.toEither.flatMap(identity)
  }

  def signSwapInputRefund(fundingTx: Transaction, index: Int, parentTxOuts: Seq[TxOut], userPrivateKey: PrivateKey): ByteVector64 = {
    val leafHash = scriptLeaf.hash()
    Transaction.signInputTaprootScriptPath(userPrivateKey, fundingTx, index, parentTxOuts, SigHash.SIGHASH_DEFAULT, leafHash)
  }

  def signSwapInputServer(fundingTx: Transaction, index: Int, parentTxOuts: Seq[TxOut], serverPrivateKey: PrivateKey, privateNonce: SecretNonce, userNonce: IndividualNonce, serverNonce: IndividualNonce): Either[Throwable, ByteVector32] = {
    scala.util.Try {
      val publicNonces = Scripts.sortNonces(Seq(userPublicKey -> userNonce, serverPublicKey -> serverNonce))
      Musig2.signTaprootInput(serverPrivateKey, fundingTx, index, parentTxOuts, sortedKeys, privateNonce, publicNonces, Some(scriptTree))
    }.toEither.flatMap(identity)
  }

  def generateServerNonce(randomBytes: ByteVector32): LocalNonce = {
    Musig2.generateNonce(randomBytes, Right(serverPublicKey), sortedKeys, Some(scriptLeaf.hash()), None)
  }
}

object SwapInProtocol {
}
