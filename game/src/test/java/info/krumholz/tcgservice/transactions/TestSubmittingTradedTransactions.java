package info.krumholz.tcgservice.transactions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import info.krumholz.dvs.DistributedValueStore;
import info.krumholz.tcgservice.data.Transaction;
import info.krumholz.tcgservice.data.Transfer;
import info.krumholz.tcgservice.signing.IdentityManager;
import info.krumholz.tcgservice.signing.Signature;
import info.krumholz.tcgservice.signing.Signed;

public class TestSubmittingTradedTransactions {

	private IdentityManager identityManager;
	private TransactionManager transactionManager;

	@Before
	public void setup() throws NoSuchAlgorithmException, IOException {
		identityManager = Fixture.createIdentityManager();
		DistributedValueStore dvs = Fixture.createDvs();
		transactionManager = Fixture.createTransactionManager(identityManager, dvs);
	}

	@Test
	public void valuesByCTradedBetweenAandB_returnsTrue() {
		final UUID value1 = UUID.randomUUID();
		final UUID value2 = UUID.randomUUID();
		final RSAPublicKey creatorPublicKey = Fixture.publicKeyC;
		final RSAPrivateKey creatorPrivateKey = Fixture.privateKeyC;

		Signed<Transaction> transaction1 = createValue(value1, creatorPrivateKey, creatorPublicKey, Fixture.privateKeyA,
				Fixture.publicKeyA);
		Signed<Transaction> transaction2 = createValue(value2, creatorPrivateKey, creatorPublicKey, Fixture.privateKeyB,
				Fixture.publicKeyB);

		Signed<Transaction> signedTrade = makeTrade(Fixture.keyPairA, Fixture.keyPairB, value1, transaction1, value2,
				transaction2, creatorPublicKey);

		boolean result = transactionManager.submitTransactions(Arrays.asList(transaction1, transaction2, signedTrade));

		assertTrue(result);
	}

	@Test
	public void valuesByCTradedBetweenAandBSubmitedWithoutCreationTransactions_returnsFalse() {
		final UUID value1 = UUID.randomUUID();
		final UUID value2 = UUID.randomUUID();
		final RSAPublicKey creatorPublicKey = Fixture.publicKeyC;
		final RSAPrivateKey creatorPrivateKey = Fixture.privateKeyC;

		Signed<Transaction> transaction1 = createValue(value1, creatorPrivateKey, creatorPublicKey, Fixture.privateKeyA,
				Fixture.publicKeyA);
		Signed<Transaction> transaction2 = createValue(value2, creatorPrivateKey, creatorPublicKey, Fixture.privateKeyB,
				Fixture.publicKeyB);

		Signed<Transaction> signedTrade = makeTrade(Fixture.keyPairA, Fixture.keyPairB, value1, transaction1, value2,
				transaction2, creatorPublicKey);

		boolean result = transactionManager.submitTransactions(Arrays.asList(transaction1, signedTrade));

		assertFalse(result);
	}

	@Test
	public void valuesByCTradedBetweenAandBThatAreNotOwnedByA_returnsFalse() {
		final UUID valueNotOwnedByA = UUID.randomUUID();
		final UUID value1 = valueNotOwnedByA;
		final UUID value2 = valueNotOwnedByA;
		final RSAPublicKey creatorPublicKey = Fixture.publicKeyC;
		final RSAPrivateKey creatorPrivateKey = Fixture.privateKeyC;

		Signed<Transaction> transaction1 = createValue(value1, creatorPrivateKey, creatorPublicKey, Fixture.privateKeyA,
				Fixture.publicKeyA);
		Signed<Transaction> transaction2 = createValue(value2, creatorPrivateKey, creatorPublicKey, Fixture.privateKeyB,
				Fixture.publicKeyB);

		Signed<Transaction> signedTrade = makeTrade(Fixture.keyPairA, Fixture.keyPairB, valueNotOwnedByA, transaction1,
				value2, transaction2, creatorPublicKey);

		boolean result = transactionManager.submitTransactions(Arrays.asList(transaction1, transaction2, signedTrade));

		assertFalse(result);
	}

	@Test
	public void valuesByCTradedBetweenAandBWithSameValueId_returnsFalse() {
		final UUID value1Id = UUID.randomUUID();
		final UUID value2Id = value1Id;
		final RSAPublicKey creatorPublicKey = Fixture.publicKeyC;
		final RSAPrivateKey creatorPrivateKey = Fixture.privateKeyC;

		Signed<Transaction> transaction1 = createValue(value1Id, creatorPrivateKey, creatorPublicKey,
				Fixture.privateKeyA, Fixture.publicKeyA);
		Signed<Transaction> transaction2 = createValue(value2Id, creatorPrivateKey, creatorPublicKey,
				Fixture.privateKeyB, Fixture.publicKeyB);

		Signed<Transaction> signedTrade = makeTrade(Fixture.keyPairA, Fixture.keyPairB, value1Id, transaction1,
				value2Id, transaction2, Fixture.publicKeyC);

		boolean result = transactionManager.submitTransactions(Arrays.asList(transaction1, transaction2, signedTrade));

		assertFalse(result);
	}

	@Test
	public void valuesCreatedBy_C_forB_TradedFrom_A_to_D_returnsFalse() {
		final UUID value1 = UUID.randomUUID();
		final UUID value2 = UUID.randomUUID();
		KeyPair creator = Fixture.keyPairC;

		Signed<Transaction> transaction1 = createValue(value1, creator, Fixture.keyPairA);
		Signed<Transaction> transaction2 = createValue(value2, creator, Fixture.keyPairB);

		Signed<Transaction> signedTradeAandB = makeTrade(Fixture.keyPairA, Fixture.keyPairD, value1, transaction1,
				value2, transaction2, Fixture.publicKeyC);

		boolean result = transactionManager
				.submitTransactions(Arrays.asList(transaction1, transaction2, signedTradeAandB));

		assertFalse(result);
	}

	@Test
	public void valuesBy_C_TradedBetween_A_B_D_DuplicationBy_B_returnsTrue() {
		final UUID value1Id = UUID.randomUUID();
		final UUID value2Id = UUID.randomUUID();
		final UUID value3Id = UUID.randomUUID();
		final RSAPublicKey creatorPublicKey = Fixture.publicKeyC;
		final RSAPrivateKey creatorPrivateKey = Fixture.privateKeyC;

		Signed<Transaction> transaction1 = createValue(value1Id, creatorPrivateKey, creatorPublicKey,
				Fixture.privateKeyA, Fixture.publicKeyA);
		Signed<Transaction> transaction2 = createValue(value2Id, creatorPrivateKey, creatorPublicKey,
				Fixture.privateKeyB, Fixture.publicKeyB);
		Signed<Transaction> transaction3 = createValue(value3Id, creatorPrivateKey, creatorPublicKey,
				Fixture.privateKeyD, Fixture.publicKeyD);

		Signed<Transaction> signedTradeAandB = makeTrade(Fixture.keyPairA, Fixture.keyPairB, value1Id, transaction1,
				value2Id, transaction2, Fixture.publicKeyC);

		Signed<Transaction> signedDuplicationByB = makeTrade(Fixture.keyPairB, Fixture.keyPairD, value2Id, transaction2,
				value3Id, transaction3, Fixture.publicKeyC);

		boolean result = transactionManager.submitTransactions(
				Arrays.asList(transaction1, transaction2, transaction3, signedTradeAandB, signedDuplicationByB));

		assertTrue(result);
	}

	@Test
	public void valuesCreatedBy_C_TradedBetween_A_and_B_WithWrongSignature_returnsFalse() {
		final UUID value1Id = UUID.randomUUID();
		final UUID value2Id = UUID.randomUUID();
		final RSAPublicKey creatorPublicKey = Fixture.publicKeyC;
		final RSAPrivateKey creatorPrivateKey = Fixture.privateKeyC;

		Signed<Transaction> transaction1 = createValue(value1Id, creatorPrivateKey, creatorPublicKey,
				Fixture.privateKeyA, Fixture.publicKeyA);
		Signed<Transaction> transaction2 = createValue(value2Id, creatorPrivateKey, creatorPublicKey,
				Fixture.privateKeyB, Fixture.publicKeyB);

		Transaction trade = new Transaction(Fixture.publicKeyA, Fixture.publicKeyB,
				new Transfer(Fixture.publicKeyA, Fixture.publicKeyB, value1Id, 1,
						TransactionManager.calculateHash(transaction1.signable), creatorPublicKey),
				new Transfer(Fixture.publicKeyB, Fixture.publicKeyA, value2Id, 1,
						TransactionManager.calculateHash(transaction2.signable), creatorPublicKey));

		Signed<Transaction> signedTrade = new Signed<Transaction>(trade,
				new Signature(Fixture.publicKeyA, Fixture.sign(Fixture.privateKeyA, trade.toBytes())),
				new Signature(Fixture.publicKeyB, new byte[] { 1, 2, 3, 4 }));

		boolean result = transactionManager.submitTransactions(Arrays.asList(transaction1, transaction2, signedTrade));

		assertFalse(result);
	}

	@Test
	public void valuesCreatedBy_C_TradedBetween_A_and_B_WithMissingSignature_returnsFalse() {
		final UUID value1Id = UUID.randomUUID();
		final UUID value2Id = UUID.randomUUID();
		final RSAPublicKey creatorPublicKey = Fixture.publicKeyC;
		final RSAPrivateKey creatorPrivateKey = Fixture.privateKeyC;

		Signed<Transaction> transaction1 = createValue(value1Id, creatorPrivateKey, creatorPublicKey,
				Fixture.privateKeyA, Fixture.publicKeyA);
		Signed<Transaction> transaction2 = createValue(value2Id, creatorPrivateKey, creatorPublicKey,
				Fixture.privateKeyB, Fixture.publicKeyB);

		Transaction trade = new Transaction(Fixture.publicKeyA, Fixture.publicKeyB,
				new Transfer(Fixture.publicKeyA, Fixture.publicKeyB, value1Id, 1,
						TransactionManager.calculateHash(transaction1.signable), creatorPublicKey),
				new Transfer(Fixture.publicKeyB, Fixture.publicKeyA, value2Id, 1,
						TransactionManager.calculateHash(transaction2.signable), creatorPublicKey));

		Signed<Transaction> signedTrade = new Signed<Transaction>(trade,
				new Signature(Fixture.publicKeyA, Fixture.sign(Fixture.privateKeyA, trade.toBytes())));

		boolean result = transactionManager.submitTransactions(Arrays.asList(transaction1, transaction2, signedTrade));

		assertFalse(result);
	}

	@Test
	public void valuesCreatedBy_C_TradedBetween_A_and_B_WithWrongTransferIndex_returnsFalse() {
		final UUID value1Id = UUID.randomUUID();
		final UUID value2Id = UUID.randomUUID();
		final RSAPublicKey creatorPublicKey = Fixture.publicKeyC;
		final RSAPrivateKey creatorPrivateKey = Fixture.privateKeyC;

		Signed<Transaction> transaction1 = createValue(value1Id, creatorPrivateKey, creatorPublicKey,
				Fixture.privateKeyA, Fixture.publicKeyA);
		Signed<Transaction> transaction2 = createValue(value2Id, creatorPrivateKey, creatorPublicKey,
				Fixture.privateKeyB, Fixture.publicKeyB);

		Transaction trade = new Transaction(Fixture.publicKeyA, Fixture.publicKeyB,
				new Transfer(Fixture.publicKeyA, Fixture.publicKeyB, value1Id, 1,
						TransactionManager.calculateHash(transaction1.signable), creatorPublicKey),
				new Transfer(Fixture.publicKeyB, Fixture.publicKeyA, value2Id, 2,
						TransactionManager.calculateHash(transaction2.signable), creatorPublicKey));

		Signed<Transaction> signedTrade = new Signed<Transaction>(trade,
				new Signature(Fixture.publicKeyA, Fixture.sign(Fixture.privateKeyA, trade.toBytes())),
				new Signature(Fixture.publicKeyB, Fixture.sign(Fixture.privateKeyB, trade.toBytes())));

		boolean result = transactionManager.submitTransactions(Arrays.asList(transaction1, transaction2, signedTrade));

		assertFalse(result);
	}

	private Signed<Transaction> createValue(UUID valueId, RSAPrivateKey creatorPrivateKey,
			RSAPublicKey creatorPublicKey, RSAPrivateKey recipientPrivateKey, RSAPublicKey recipientPublicKey) {
		Transaction transaction = new Transaction(creatorPublicKey, recipientPublicKey,
				new Transfer(creatorPublicKey, recipientPublicKey, valueId, 0, null, creatorPublicKey));

		Signed<Transaction> signedTransaction = new Signed<Transaction>(transaction,
				new Signature(recipientPublicKey, Fixture.sign(recipientPrivateKey, transaction.toBytes())),
				new Signature(creatorPublicKey, Fixture.sign(creatorPrivateKey, transaction.toBytes())));
		return signedTransaction;
	}

	private Signed<Transaction> createValue(UUID valueId, KeyPair creator, KeyPair recipient) {
		Transaction transaction = new Transaction((RSAPublicKey) creator.getPublic(),
				(RSAPublicKey) recipient.getPublic(), new Transfer((RSAPublicKey) creator.getPublic(),
						(RSAPublicKey) recipient.getPublic(), valueId, 0, null, (RSAPublicKey) creator.getPublic()));

		Signed<Transaction> signedTransaction = new Signed<Transaction>(transaction,
				new Signature((RSAPublicKey) recipient.getPublic(),
						Fixture.sign((RSAPrivateKey) recipient.getPrivate(), transaction.toBytes())),
				new Signature((RSAPublicKey) creator.getPublic(),
						Fixture.sign((RSAPrivateKey) creator.getPrivate(), transaction.toBytes())));
		return signedTransaction;
	}

	private Signed<Transaction> makeTrade(KeyPair a, KeyPair b, UUID valueFromA, Signed<Transaction> previousValueFromA,
			UUID valueFromB, Signed<Transaction> previousValueFromB, RSAPublicKey creator) {
		Transfer previousTransferValueFromA = previousTransfer(previousValueFromA.signable, valueFromA);
		Transaction trade = new Transaction((RSAPublicKey) a.getPublic(), (RSAPublicKey) b.getPublic(),
				new Transfer((RSAPublicKey) a.getPublic(), (RSAPublicKey) b.getPublic(), valueFromA,
						previousTransferValueFromA == null ? 1 : previousTransferValueFromA.transferIndex + 1,
						TransactionManager.calculateHash(previousValueFromA.signable), creator),
				new Transfer((RSAPublicKey) b.getPublic(), (RSAPublicKey) a.getPublic(), valueFromB,
						previousTransfer(previousValueFromB.signable, valueFromB).transferIndex + 1,
						TransactionManager.calculateHash(previousValueFromB.signable), creator));

		Signed<Transaction> signedTrade = new Signed<Transaction>(trade,
				new Signature((RSAPublicKey) a.getPublic(),
						Fixture.sign((RSAPrivateKey) a.getPrivate(), trade.toBytes())),
				new Signature((RSAPublicKey) b.getPublic(),
						Fixture.sign((RSAPrivateKey) b.getPrivate(), trade.toBytes())));

		return signedTrade;
	}

	private Transfer previousTransfer(Transaction previousTransaction, UUID valueId) {
		Transfer previousTransfer = null;
		for (Transfer tmpTransfer : previousTransaction.transfers) {
			if (tmpTransfer.cardInstanceId.equals(valueId)) {
				previousTransfer = tmpTransfer;
				break;
			}
		}
		return previousTransfer;
	}

}
