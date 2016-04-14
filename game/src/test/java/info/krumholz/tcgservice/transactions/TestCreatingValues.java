package info.krumholz.tcgservice.transactions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.util.Optional;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import info.krumholz.dvs.DistributedValueStore;
import info.krumholz.tcgservice.data.Transaction;
import info.krumholz.tcgservice.signing.IdentityManager;
import info.krumholz.tcgservice.signing.Signed;

// TODO: test that pending transactions are saved to disk
public class TestCreatingValues {

	private TransactionManager transactionManager;
	private IdentityManager identityManager;

	@Before
	public void init() throws IOException, NoSuchAlgorithmException {
		identityManager = Fixture.createIdentityManager();
		DistributedValueStore dvs = Fixture.createDvs();
		transactionManager = Fixture.createTransactionManager(identityManager, dvs);
	}

	@Test
	public void createValue_withLegalInput_createsTransferWithCreatorSetToServerPublicKey() {
		// test
		UUID randomUUID = UUID.randomUUID();
		Signed<Transaction> signedTransaction = createValue(randomUUID, Fixture.publicKeyA);

		// verification
		Assert.assertArrayEquals(identityManager.getPublicKey().getEncoded(),
				signedTransaction.signable.transfers.get(0).creator.getEncoded());
	}

	@Test
	public void createValue_withLegalInput_createsTransferWithTransferIndexZero() {
		// test
		UUID randomUUID = UUID.randomUUID();
		Signed<Transaction> signedTransaction = createValue(randomUUID, Fixture.publicKeyA);

		// verification
		Assert.assertEquals(0, signedTransaction.signable.transfers.get(0).transferIndex);
	}

	@Test
	public void createValue_withLegalInput_createsTransferWithPreviousTransactionHashNull() {
		// test
		UUID randomUUID = UUID.randomUUID();
		Signed<Transaction> signedTransaction = createValue(randomUUID, Fixture.publicKeyA);

		// verification
		Assert.assertEquals(null, signedTransaction.signable.transfers.get(0).previousTransactionHash);
	}

	@Test
	public void createValue_withLegalInput_createsTransferWithGiverSetToServerPublicKey() {
		// test
		UUID randomUUID = UUID.randomUUID();
		Signed<Transaction> signedTransaction = createValue(randomUUID, Fixture.publicKeyA);

		// verification
		Assert.assertArrayEquals(identityManager.getPublicKey().getEncoded(),
				signedTransaction.signable.transfers.get(0).giver.getEncoded());
	}

	@Test
	public void createValue_withLegalInput_createsTransferWithRecipientSetToGivenOwner() {
		// test
		UUID randomUUID = UUID.randomUUID();
		Signed<Transaction> signedTransaction = createValue(randomUUID, Fixture.publicKeyA);

		// verification
		Assert.assertArrayEquals(Fixture.publicKeyA.getEncoded(),
				signedTransaction.signable.transfers.get(0).recipient.getEncoded());
	}

	@Test
	public void createValue_withLegalInput_createsTransactionWithOneTransfer() {
		// test
		UUID randomUUID = UUID.randomUUID();
		Signed<Transaction> signedTransaction = createValue(randomUUID, Fixture.publicKeyA);

		// verification
		Assert.assertEquals(1, signedTransaction.signable.transfers.size());
	}

	@Test
	public void createValue_withLegalInput_setsValueIdToGivenUuid() {
		// test
		UUID randomUUID = UUID.randomUUID();
		Signed<Transaction> signedTransaction = createValue(randomUUID, Fixture.publicKeyA);

		// verification
		Assert.assertEquals(randomUUID, signedTransaction.signable.transfers.get(0).cardInstanceId);
	}

	@Test
	public void createValue_withLegalInput_setsAToServersPublicKey() {
		// test
		Signed<Transaction> signedTransaction = createValue(UUID.randomUUID(), Fixture.publicKeyA);

		// verification
		Assert.assertArrayEquals(identityManager.getPublicKey().getEncoded(),
				signedTransaction.signable.a.getEncoded());
	}

	@Test
	public void createValue_withLegalInput_setsBToGivenOwner() {
		// test
		Signed<Transaction> signedTransaction = createValue(UUID.randomUUID(), Fixture.publicKeyA);

		// verification
		Assert.assertArrayEquals(Fixture.publicKeyA.getEncoded(), signedTransaction.signable.b.getEncoded());
	}

	@Test
	public void createValue_withLegalInput_signsWithServerKey() {
		// test
		Signed<Transaction> signedTransaction = createValue(UUID.randomUUID(), Fixture.publicKeyA);

		// verification
		assertTrue(identityManager.isOwnSignaturePresent(signedTransaction));
		assertEquals(1, signedTransaction.signatures.size());
		assertTrue(identityManager.areSignaturesCorrect(signedTransaction));
	}

	private Signed<Transaction> createValue(UUID id, RSAPublicKey owner) {

		Optional<Signed<Transaction>> signedTransaction = transactionManager.createValue(id, owner);
		assertTrue(signedTransaction.isPresent());
		return signedTransaction.get();
	}
}
