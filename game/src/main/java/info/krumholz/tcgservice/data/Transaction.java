package info.krumholz.tcgservice.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.io.BaseEncoding;
import com.google.common.primitives.UnsignedLongs;

import info.krumholz.tcgservice.signing.Signable;

public class Transaction implements Signable {

	public final RSAPublicKey a;
	public final RSAPublicKey b;
	public final List<Transfer> transfers;

	public Transaction(RSAPublicKey a, RSAPublicKey b, List<Transfer> transfers) {
		this.a = a;
		this.b = b;
		this.transfers = transfers;
	}

	public Transaction(RSAPublicKey a, RSAPublicKey b, Transfer transfer, Transfer... transfers) {
		this.a = a;
		this.b = b;
		ArrayList<Transfer> totalTransfers = new ArrayList<>();
		totalTransfers.add(transfer);
		totalTransfers.addAll(Arrays.asList(transfers));
		this.transfers = totalTransfers;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((a == null) ? 0 : Arrays.hashCode(a.getEncoded()));
		result = prime * result + ((b == null) ? 0 : Arrays.hashCode(b.getEncoded()));
		result = prime * result + ((transfers == null) ? 0 : transfers.hashCode());
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
		Transaction other = (Transaction) obj;
		if (a == null) {
			if (other.a != null)
				return false;
		} else if (!Arrays.equals(a.getEncoded(), other.a.getEncoded()))
			return false;
		if (b == null) {
			if (other.b != null)
				return false;
		} else if (!Arrays.equals(b.getEncoded(), other.b.getEncoded()))
			return false;
		if (transfers == null) {
			if (other.transfers != null)
				return false;
		} else if (!transfers.equals(other.transfers))
			return false;
		return true;
	}

	@Override
	public byte[] toBytes() {
		try {
			ByteArrayOutputStream result = new ByteArrayOutputStream();
			// publicKey a
			BigInteger modulus = a.getModulus();
			BigInteger publicExponent = a.getPublicExponent();
			result.write(modulus.toByteArray());
			result.write(publicExponent.toByteArray());

			// publicKey b
			modulus = ((RSAPublicKey) b).getModulus();
			publicExponent = ((RSAPublicKey) b).getPublicExponent();
			result.write(modulus.toByteArray());
			result.write(publicExponent.toByteArray());

			ByteBuffer tmpInt = ByteBuffer.allocate(4);
			ByteBuffer tmpUUID = ByteBuffer.allocate(16);
			ArrayList<Transfer> sorted = new ArrayList<Transfer>(transfers);
			sorted.sort((o1, o2) -> {
				int mostSignificantBitsComparisonResult = UnsignedLongs.compare(o1.cardInstanceId.getMostSignificantBits(),
						o2.cardInstanceId.getMostSignificantBits());
				if (mostSignificantBitsComparisonResult != 0) {
					return mostSignificantBitsComparisonResult;
				}
				return UnsignedLongs.compare(o1.cardInstanceId.getLeastSignificantBits(),
						o2.cardInstanceId.getLeastSignificantBits());
			});
			for (Transfer transfer : sorted) {
				// card id
				tmpUUID.clear();
				tmpUUID.putLong(transfer.cardInstanceId.getMostSignificantBits());
				tmpUUID.putLong(transfer.cardInstanceId.getLeastSignificantBits());
				result.write(tmpUUID.array());

				// card creator
				modulus = ((RSAPublicKey) transfer.creator).getModulus();
				publicExponent = ((RSAPublicKey) transfer.creator).getPublicExponent();
				result.write(modulus.toByteArray());
				result.write(publicExponent.toByteArray());

				// card transfer index
				tmpInt.clear();
				tmpInt.putInt(transfer.transferIndex);
				result.write(tmpInt.array());

				// publicKey giver
				modulus = ((RSAPublicKey) transfer.giver).getModulus();
				publicExponent = ((RSAPublicKey) transfer.giver).getPublicExponent();
				result.write(modulus.toByteArray());
				result.write(publicExponent.toByteArray());

				// publicKey recipient
				modulus = ((RSAPublicKey) transfer.recipient).getModulus();
				publicExponent = ((RSAPublicKey) transfer.recipient).getPublicExponent();
				result.write(modulus.toByteArray());
				result.write(publicExponent.toByteArray());

				if (transfer.previousTransactionHash != null) {
					result.write(transfer.previousTransactionHash);
				}
			}
			return result.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString() {
		return "#Transaction{\nFrom:\n" + BaseEncoding.base64().encode(a.getEncoded()) + "\nTo:\n"
				+ BaseEncoding.base64().encode(b.getEncoded()) + "\nNumber transfers: " + transfers.size() + "\n}";
	}

}
