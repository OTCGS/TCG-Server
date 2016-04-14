package info.krumholz.tcgservice.interfaces.soap;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.krumholz.tcgservice.Key;
import info.krumholz.tcgservice.data.Transaction;
import info.krumholz.tcgservice.data.Transfer;
import info.krumholz.tcgservice.signing.Signature;
import info.krumholz.tcgservice.signing.Signed;

// TODO: error checking for all translations
public class Translator {

	private static final Logger logger = LoggerFactory.getLogger(Translator.class);
	private static final KeyFactory keyFactory;

	static {
		try {
			keyFactory = KeyFactory.getInstance("RSA");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private static info.krumholz.tcgservice.Transfer transfer(Transfer in) {
		info.krumholz.tcgservice.Transfer out = new info.krumholz.tcgservice.Transfer();
		out.setCardId(in.cardInstanceId.toString());
		out.setCardTransferIndex(in.transferIndex);
		out.setCreator(key(in.creator));
		out.setGiver(key(in.giver));
		out.setPreviousTransactionHash(in.previousTransactionHash);
		out.setRecipient(key(in.recipient));
		return out;
	}

	public static Transfer transfer(info.krumholz.tcgservice.Transfer in) {
		Transfer out = new Transfer(key(in.getGiver()).get(), key(in.getRecipient()).get(),
				UUID.fromString(in.getCardId()), in.getCardTransferIndex(), in.getPreviousTransactionHash(),
				key(in.getCreator()).get());
		return out;
	}

	public static Optional<RSAPublicKey> key(info.krumholz.tcgservice.Key in) {
		RSAPublicKeySpec spec = new RSAPublicKeySpec(new BigInteger(1, in.getModulus()),
				new BigInteger(1, in.getExponent()));
		try {
			return Optional.of((RSAPublicKey) keyFactory.generatePublic(spec));
		} catch (InvalidKeySpecException | ClassCastException e) {
			logger.error("translating key failed with {}" + e.getMessage());
			return Optional.empty();
		}
	}

	public static info.krumholz.tcgservice.Key key(RSAPublicKey rsaKey) {
		Key key = new Key();

		key.setExponent(rsaKey.getPublicExponent().toByteArray());
		key.setModulus(rsaKey.getModulus().toByteArray());

		return key;
	}

	public static Signed<Transaction> transaction(info.krumholz.tcgservice.Transaction in) {
		ArrayList<Transfer> transfers = new ArrayList<>();
		for (info.krumholz.tcgservice.Transfer transfer : in.getTransfers().getTransfer()) {
			transfers.add(transfer(transfer));
		}
		Transaction t = new Transaction(key(in.getA()).get(), key(in.getB()).get(), transfers);
		// TODO: error checking
		Signed<Transaction> out = new Signed<Transaction>(t, new Signature(key(in.getA()).get(), in.getSignatureA()),
				new Signature(key(in.getB()).get(), in.getSignatureB()));
		return out;
	}

	public static info.krumholz.tcgservice.Transaction transaction(Signed<Transaction> in) {
		info.krumholz.tcgservice.Transaction out = new info.krumholz.tcgservice.Transaction();
		out.setA(key(in.signable.a));
		out.setB(key(in.signable.b));
		Signature a = null;
		Signature b = null;
		for (Signature signature : in.signatures) {
			if (Arrays.equals(signature.signee.getEncoded(), in.signable.a.getEncoded())) {
				a = signature;
			}
			if (Arrays.equals(signature.signee.getEncoded(), in.signable.b.getEncoded())) {
				b = signature;
			}
		}
		if (a != null) {
			out.setSignatureA(a.signature);
		}
		if (b != null) {
			out.setSignatureB(b.signature);
		}
		info.krumholz.tcgservice.Transaction.Transfers transfers = new info.krumholz.tcgservice.Transaction.Transfers();
		for (Transfer t : in.signable.transfers) {
			transfers.getTransfer().add(transfer(t));
		}
		out.setTransfers(transfers);
		return out;
	}

}
