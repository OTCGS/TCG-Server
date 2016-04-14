package info.krumholz.tcgservice.transactions;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import info.krumholz.dvs.DistributedValueStore;
import info.krumholz.tcgservice.data.Transaction;
import info.krumholz.tcgservice.data.Transfer;
import info.krumholz.tcgservice.signing.IdentityManager;
import info.krumholz.tcgservice.signing.Signature;
import info.krumholz.tcgservice.signing.Signed;

public class TestSubmittingCreationTransactions {

	private IdentityManager identityManager;
	private TransactionManager transactionManager;

	@Before
	public void init() throws NoSuchAlgorithmException, IOException {
		identityManager = Fixture.createIdentityManager();
		DistributedValueStore dvs = Fixture.createDvs();
		transactionManager = Fixture.createTransactionManager(identityManager, dvs);
	}

	@Test
	public void zeroTransactions_doesNothing() {
		List<Signed<Transaction>> transactions = new ArrayList<>();
		boolean transactionResult = transactionManager.submitTransactions(transactions);
		Assert.assertTrue(transactionResult);
	}

	@Test
	public void valueCreatedByServerAndSignedByA_returnsTrue() {
		// setup
		final UUID valueId = UUID.randomUUID();
		final RSAPublicKey ownerPublicKey = Fixture.publicKeyA;
		final RSAPrivateKey ownerPrivateKey = Fixture.privateKeyA;

		Signed<Transaction> transaction = createValue(valueId, ownerPublicKey);
		Fixture.sign(ownerPublicKey, ownerPrivateKey, transaction);

		// test
		boolean transactionResult = submitTransaction(transaction);

		// verification
		Assert.assertTrue(transactionResult);
	}

	@Test
	public void valueCreatedByCForAAndSignedOnlyByA_returnsFalse() {
		final UUID valueId = UUID.randomUUID();
		final RSAPublicKey creator = Fixture.publicKeyC;
		final RSAPublicKey giver = Fixture.publicKeyC;
		final RSAPublicKey owner = Fixture.publicKeyA;
		final RSAPrivateKey ownerPrivateKey = Fixture.privateKeyA;
		final Transaction transaction = new Transaction(creator, owner,
				new Transfer(giver, owner, valueId, 0, null, creator));

		final byte[] signatureData = Fixture.sign(ownerPrivateKey, transaction.toBytes());
		Signed<Transaction> signedTransaction = new Signed<Transaction>(transaction,
				new Signature(owner, signatureData));

		Assert.assertFalse(submitTransaction(signedTransaction));
	}

	@Test
	public void valueCreatedByCForAAndNotSignedByC_returnsFalse() {
		final UUID valueId = UUID.randomUUID();
		final RSAPublicKey creator = Fixture.publicKeyC;
		final RSAPublicKey giver = Fixture.publicKeyC;
		final RSAPublicKey recipient = Fixture.publicKeyA;
		final RSAPrivateKey recipientPrivateKey = Fixture.privateKeyA;

		Transaction transaction = new Transaction(creator, recipient,
				new Transfer(giver, recipient, valueId, 0, null, creator));

		Signed<Transaction> signedTransaction = new Signed<Transaction>(transaction,
				new Signature(recipient, Fixture.sign(recipientPrivateKey, transaction.toBytes())));

		Assert.assertFalse(submitTransaction(signedTransaction));
	}

	@Test
	public void valueCreatedByAForAAndSignedByA_returnsTrue() {
		final UUID valueId = UUID.randomUUID();
		final RSAPublicKey creator = Fixture.publicKeyA;
		final RSAPublicKey recipient = Fixture.publicKeyA;
		final RSAPrivateKey recipientPrivateKey = Fixture.privateKeyA;
		final RSAPublicKey giver = Fixture.publicKeyA;

		Transaction transaction = new Transaction(creator, recipient,
				new Transfer(giver, recipient, valueId, 0, null, creator));

		Signed<Transaction> signedTransaction = new Signed<Transaction>(transaction,
				new Signature(giver, Fixture.sign(recipientPrivateKey, transaction.toBytes())));

		Assert.assertTrue(submitTransaction(signedTransaction));
	}

	@Test
	public void valueCreatedByCForAWithGiverB_returnsFalse() {
		final UUID valueId = UUID.randomUUID();
		final RSAPublicKey creator = Fixture.publicKeyC;
		final RSAPrivateKey creatorPrivateKey = Fixture.privateKeyC;
		final RSAPublicKey recipient = Fixture.publicKeyA;
		final RSAPublicKey giver = Fixture.publicKeyB;
		final RSAPrivateKey giverPrivateKey = Fixture.privateKeyB;

		Transaction transaction = new Transaction(creator, recipient,
				new Transfer(giver, recipient, valueId, 0, null, creator));

		Signed<Transaction> signedTransaction = new Signed<Transaction>(transaction,
				new Signature(giver, Fixture.sign(giverPrivateKey, transaction.toBytes())),
				new Signature(creator, Fixture.sign(creatorPrivateKey, transaction.toBytes())));

		Assert.assertFalse(submitTransaction(signedTransaction));
	}

	@Test
	public void valueCreatedByCWithWrongTransferIndex_returnsFalse() {
		final UUID valueId = UUID.randomUUID();
		final RSAPublicKey creator = Fixture.publicKeyC;
		final RSAPrivateKey creatorPrivateKey = Fixture.privateKeyC;
		final RSAPublicKey giver = Fixture.publicKeyC;
		final RSAPublicKey recipient = Fixture.publicKeyA;
		final RSAPrivateKey recipientPrivateKey = Fixture.privateKeyA;

		Transaction transaction = new Transaction(creator, recipient,
				new Transfer(giver, recipient, valueId, 1, null, creator));
		Signed<Transaction> signedTransaction = new Signed<Transaction>(transaction,
				new Signature(recipient, Fixture.sign(recipientPrivateKey, transaction.toBytes())),
				new Signature(creator, Fixture.sign(creatorPrivateKey, transaction.toBytes())));

		Assert.assertFalse(submitTransaction(signedTransaction));
	}

	@Test
	public void valueCreatedByCWithWrongPreviousTransactionHash_returnsFalse() {
		final UUID valueId = UUID.randomUUID();
		final RSAPublicKey creator = Fixture.publicKeyC;
		final RSAPrivateKey creatorPrivateKey = Fixture.privateKeyC;
		final RSAPublicKey giver = Fixture.publicKeyC;
		final RSAPublicKey recipient = Fixture.publicKeyA;
		final RSAPrivateKey recipientPrivateKey = Fixture.privateKeyA;

		Transaction transaction = new Transaction(creator, recipient,
				new Transfer(giver, recipient, valueId, 0, new byte[] { 1 }, creator));
		Signed<Transaction> signedTransaction = new Signed<Transaction>(transaction,
				new Signature(recipient, Fixture.sign(recipientPrivateKey, transaction.toBytes())),
				new Signature(creator, Fixture.sign(creatorPrivateKey, transaction.toBytes())));

		Assert.assertFalse(submitTransaction(signedTransaction));
	}

	@Test
	public void valueCreatedByCForAAndSignedWronglyByC_returnsFalse() {
		final UUID valueId = UUID.randomUUID();
		final RSAPublicKey creator = Fixture.publicKeyC;
		final RSAPublicKey giver = Fixture.publicKeyC;
		final RSAPublicKey recipient = Fixture.publicKeyA;
		final RSAPrivateKey recipientPrivateKey = Fixture.privateKeyA;

		Transaction transaction = new Transaction(creator, recipient,
				new Transfer(giver, recipient, valueId, 0, null, creator));

		Signed<Transaction> signedTransaction = new Signed<Transaction>(transaction,
				new Signature(recipient, Fixture.sign(recipientPrivateKey, transaction.toBytes())),
				new Signature(creator, new byte[] { 1, 2, 3, 4, 5, 6 }));

		Assert.assertFalse(submitTransaction(signedTransaction));
	}

	@Test
	public void valueCreatedByCForAAndSignedWronglyByA_returnsFalse() {
		final UUID valueId = UUID.randomUUID();
		final RSAPublicKey creator = Fixture.publicKeyC;
		final RSAPrivateKey creatorPrivateKey = Fixture.privateKeyC;
		final RSAPublicKey giver = Fixture.publicKeyC;
		final RSAPublicKey recipient = Fixture.publicKeyA;

		Transaction transaction = new Transaction(creator, recipient,
				new Transfer(giver, recipient, valueId, 0, null, creator));

		Signed<Transaction> signedTransaction = new Signed<Transaction>(transaction,
				new Signature(creator, Fixture.sign(creatorPrivateKey, transaction.toBytes())),
				new Signature(recipient, new byte[] { 1, 2, 3, 4, 5, 6 }));

		Assert.assertFalse(submitTransaction(signedTransaction));
	}

	private Signed<Transaction> createValue(UUID id, RSAPublicKey owner) {

		Optional<Signed<Transaction>> signedTransaction = transactionManager.createValue(id, owner);
		Assert.assertTrue(signedTransaction.isPresent());
		return signedTransaction.get();
	}

	private boolean submitTransaction(Signed<Transaction> signedTransaction) {
		List<Signed<Transaction>> transactions = new ArrayList<>();
		transactions.add(signedTransaction);
		return transactionManager.submitTransactions(transactions);
	}

}
