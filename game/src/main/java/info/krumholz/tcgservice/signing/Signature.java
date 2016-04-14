package info.krumholz.tcgservice.signing;

import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;

public class Signature {

	public final RSAPublicKey signee;
	public final byte[] signature;

	public Signature(RSAPublicKey signee, byte[] signature) {
		this.signee = signee;
		this.signature = signature;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(signature);
		result = prime * result + ((signee == null) ? 0 : Arrays.hashCode(signee.getEncoded()));
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
		Signature other = (Signature) obj;
		if (!Arrays.equals(signature, other.signature))
			return false;
		if (signee == null) {
			if (other.signee != null)
				return false;
		} else if (!Arrays.equals(signee.getEncoded(), other.signee.getEncoded()))
			return false;
		return true;
	}
}
