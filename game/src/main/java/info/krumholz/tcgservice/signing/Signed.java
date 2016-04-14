package info.krumholz.tcgservice.signing;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.google.common.io.BaseEncoding;

public class Signed<T extends Signable> {

	public final T signable;
	public final Set<Signature> signatures;

	public Signed(T data, Signature signature, Signature... signatures) {
		this.signable = data;
		this.signatures = new HashSet<>();
		this.signatures.add(signature);
		this.signatures.addAll(Arrays.asList(signatures));
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Signature signature : signatures) {
			sb.append(BaseEncoding.base64().encode(signature.signee.getEncoded()));
			sb.append(":\n");
			sb.append(BaseEncoding.base64().encode(signature.signature));
			sb.append("\n");
		}
		return "#Signable{" + signable.toString() + "} with:\n" + sb.toString();
	}
}
