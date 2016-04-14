package info.krumholz.tcgservice.transactions;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import info.krumholz.dvs.DistributedValueStore;
import info.krumholz.tcgservice.data.Transaction;
import info.krumholz.tcgservice.data.Transfer;
import info.krumholz.tcgservice.signing.IdentityManager;
import info.krumholz.tcgservice.signing.Signed;

public class TransactionManager {

	private static final Logger logger = LoggerFactory.getLogger(TransactionManager.class);

	private static MessageDigest digest;

	static {
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	@Autowired
	IdentityManager identityManager;

	@Autowired
	DistributedValueStore dvs;

	private Map<ComparableByteArray, Signed<Transaction>> transactionMap = new HashMap<>();

	public Optional<Signed<Transaction>> createValue(UUID id, RSAPublicKey owner) {
		Transfer transfer = new Transfer(identityManager.getPublicKey(), owner, id, 0, null,
				identityManager.getPublicKey());
		Transaction transaction = new Transaction(identityManager.getPublicKey(), owner, Arrays.asList(transfer));

		// TODO: save pending values

		Optional<Signed<Transaction>> signedTransaction = identityManager.sign(transaction);
		return signedTransaction;
	}

	public boolean submitTransactions(List<Signed<Transaction>> newTransactions) {
		// check signatures
		for (Signed<Transaction> signedTransaction : newTransactions) {
			boolean areSignaturesCorrect = identityManager.areSignaturesCorrect(signedTransaction);
			if (areSignaturesCorrect == false) {
				logger.warn("Signatures incorrect: " + signedTransaction);
				return false; // signature is incorrect
			}
			boolean isSignatureAPresent = identityManager.isSignaturePresent(signedTransaction.signable.a,
					signedTransaction);
			boolean isSignatureBPresent = identityManager.isSignaturePresent(signedTransaction.signable.b,
					signedTransaction);
			if (!isSignatureAPresent || !isSignatureBPresent) {
				logger.warn("Signature missing " + (isSignatureAPresent ? "B" : "A"));
				return false; // signature is missing
			}
			final Transaction transaction = signedTransaction.signable;
			for (Transfer transfer : transaction.transfers) {
				if (!Arrays.equals(transfer.giver.getEncoded(), transaction.a.getEncoded())
						&& !Arrays.equals(transfer.giver.getEncoded(), transaction.b.getEncoded())) {
					logger.warn("Giver or receiver not part of the transaction");
					return false; // giver or receiver where not part of the
									// transaction
				}
				if (transfer.transferIndex == 0 && transfer.previousTransactionHash == null) { // creation
																								// transaction
					if (!Arrays.equals(transfer.creator.getEncoded(), transfer.giver.getEncoded())) {
						logger.warn("creator not given");
						return false; // creator was not giver
					}
				} else { // non creation transaction
					if (transfer.previousTransactionHash == null || transfer.transferIndex == 0) {
						logger.warn("non creation transaction with null hash or 0 transferindex");
						return false;
					}
				}
			}
		}
		// All new transaction have their required signatures present. Now we
		// check the previous transactions

		// create Working copy
		Map<ComparableByteArray, Signed<Transaction>> transactionMapCopy = new HashMap<>(transactionMap);
		for (Signed<Transaction> transaction : newTransactions) {
			final ComparableByteArray transactionHash = new ComparableByteArray(calculateHash(transaction.signable));
			transactionMapCopy.put(transactionHash, transaction);
		}
		// check each Transaction in context of previousTransactions
		for (Signed<Transaction> signedTransaction : newTransactions) {
			final Transaction transaction = signedTransaction.signable; // check
			// each
			Set<Transfer> outgoingTransfers = getOutgoingTransfers(transaction, transactionMapCopy);
			for (Transfer transfer : transaction.transfers) {
				if (transfer.transferIndex == 0) {
					if (howOftendoesValueExist(transfer.creator, transfer.cardInstanceId, transactionMapCopy) != 1) {
						logger.warn("duplicate value by same creator");
						return false; // duplicate id by same creator
					}
					continue;
				}
				Signed<Transaction> previousTransaction = transactionMapCopy
						.get(new ComparableByteArray(transfer.previousTransactionHash));
				if (previousTransaction == null) {
					logger.warn("previousTransaction missing");
					return false;
				}
				Transfer previousTransfer = null;
				for (Transfer tmpTransfer : previousTransaction.signable.transfers) {
					if (tmpTransfer.cardInstanceId.equals(transfer.cardInstanceId)) {
						previousTransfer = tmpTransfer;
						break;
					}
				}
				if (previousTransfer == null) {
					logger.warn("previousTransfer missing");
					return false; // value should be in previous transaction
				}
				if (previousTransfer.transferIndex + 1 != transfer.transferIndex) {
					logger.warn("previousTransfer not correct transferindex");
					return false; // index should be incremented by one
				}
				if (!previousTransfer.recipient.equals(transfer.giver)) {
					logger.warn("card is not owned by giver");
					return false; // card is not owned by giver
				}
				for (Transfer outgoingTransfer : outgoingTransfers) {
					if (outgoingTransfer.cardInstanceId.equals(transfer.cardInstanceId)) {
						logger.warn("Duplication");
					}
				}
			}
		}
		transactionMap = transactionMapCopy;
		return true;
	}

	private Set<Transfer> getOutgoingTransfers(Transaction transaction,
			Map<ComparableByteArray, Signed<Transaction>> transactionMap) {
		Set<Transfer> outgoingTransfers = new HashSet<>();
		byte[] hash = calculateHash(transaction);
		for (Signed<Transaction> tmpTransaction : transactionMap.values()) {
			for (Transfer tmpTransfer : tmpTransaction.signable.transfers) {
				if (Arrays.equals(tmpTransfer.previousTransactionHash, hash)) {
					outgoingTransfers.add(tmpTransfer);
				}
			}
		}
		return outgoingTransfers;
	}

	private int howOftendoesValueExist(RSAPublicKey creator, UUID valueId,
			Map<ComparableByteArray, Signed<Transaction>> transactionMap) {
		int count = 0;
		Collection<Signed<Transaction>> transactions = transactionMap.values();
		for (Signed<Transaction> transaction : transactions) {
			for (Transfer transfer : transaction.signable.transfers) {
				if (transfer.transferIndex == 0 && Arrays.equals(transfer.creator.getEncoded(), creator.getEncoded())
						&& valueId.equals(transfer.cardInstanceId)) {
					count += 1;
				}
			}
		}
		return count;
	}

	public static byte[] calculateHash(Transaction t) {
		digest.reset();
		digest.update(t.toBytes());
		return digest.digest();
	}

	public Signed<Transaction> getTransaction(byte[] hash) {
		return transactionMap.get(new ComparableByteArray(hash));
	}

	public Set<Signed<Transaction>> getHeads() {
		Set<Signed<Transaction>> result = new HashSet<>(transactionMap.values());
		for (Signed<Transaction> transaction : transactionMap.values()) {
			for (Transfer transfer : transaction.signable.transfers) {
				result.remove(transactionMap.get(new ComparableByteArray(transfer.previousTransactionHash)));
			}
		}
		return result;
	}

	public Set<Signed<Transaction>> list() {
		return new HashSet<Signed<Transaction>>(transactionMap.values());
	}

	public Set<Signed<Transaction>> getCreationTransactions() {
		Set<Signed<Transaction>> result = new HashSet<>();
		for (Signed<Transaction> transaction : transactionMap.values()) {
			for (Transfer transfer : transaction.signable.transfers) {
				if (transfer.transferIndex == 0) {
					result.add(transaction);
				}
			}
		}
		return result;
	}

	class ComparableByteArray {
		public final byte[] data;

		public ComparableByteArray(final byte[] data) {
			this.data = data;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(data);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ComparableByteArray other = (ComparableByteArray) obj;
			if (!Arrays.equals(data, other.data))
				return false;
			return true;
		}
	}

}
