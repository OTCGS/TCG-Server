package info.krumholz.tcgservice.data;

import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.UUID;

public class Transfer {

	public final RSAPublicKey giver;
	public final RSAPublicKey recipient;
	public final UUID cardInstanceId;
	public final int transferIndex;
	public final byte[] previousTransactionHash;
	public final RSAPublicKey creator;

	public Transfer(RSAPublicKey giver, RSAPublicKey recipient, UUID valueId, int transferIndex,
			byte[] previousTransactionHash, RSAPublicKey valueCreator) {
		this.giver = giver;
		this.recipient = recipient;
		this.cardInstanceId = valueId;
		this.transferIndex = transferIndex;
		this.previousTransactionHash = previousTransactionHash;
		this.creator = valueCreator;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((creator == null) ? 0 : Arrays.hashCode(creator.getEncoded()));
		result = prime * result + ((giver == null) ? 0 : Arrays.hashCode(giver.getEncoded()));
		result = prime * result + Arrays.hashCode(previousTransactionHash);
		result = prime * result + ((recipient == null) ? 0 : Arrays.hashCode(recipient.getEncoded()));
		result = prime * result + transferIndex;
		result = prime * result + ((cardInstanceId == null) ? 0 : cardInstanceId.hashCode());
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
		Transfer other = (Transfer) obj;
		if (creator == null) {
			if (other.creator != null)
				return false;
		} else if (!Arrays.equals(creator.getEncoded(), other.creator.getEncoded()))
			return false;
		if (giver == null) {
			if (other.giver != null)
				return false;
		} else if (!Arrays.equals(giver.getEncoded(), other.giver.getEncoded()))
			return false;
		if (!Arrays.equals(previousTransactionHash, other.previousTransactionHash))
			return false;
		if (recipient == null) {
			if (other.recipient != null)
				return false;
		} else if (!Arrays.equals(recipient.getEncoded(), other.recipient.getEncoded()))
			return false;
		if (transferIndex != other.transferIndex)
			return false;
		if (cardInstanceId == null) {
			if (other.cardInstanceId != null)
				return false;
		} else if (!cardInstanceId.equals(other.cardInstanceId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return String.format("#Transfer[%s -> %s, %s]", giver, recipient, cardInstanceId);
	}
}
