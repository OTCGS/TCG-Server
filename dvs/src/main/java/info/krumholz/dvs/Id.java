package info.krumholz.dvs;

import java.util.Arrays;
import java.util.Base64;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

public class Id {

	private final byte[] hash;
	public final String hashAsString;

	static Id fromHash(String hashAsString) {
		byte[] hash = Base64.getUrlDecoder().decode(hashAsString);
		return new Id(hash);
	}

	static Id fromHash(byte[] hash) {
		return new Id(hash);
	}

	static Id fromData(byte[] data) {
		final HashFunction hf = Hashing.sha256();
		byte[] hash = hf.newHasher().putBytes(data).hash().asBytes();
		return new Id(hash);
	}

	protected Id(byte[] hash) {
		this.hash = hash;
		hashAsString = Base64.getUrlEncoder().encodeToString(hash);
	}

	@Override
	public String toString() {
		return hashAsString;
	}

	@Override
	public int hashCode() {
		int result = Arrays.hashCode(hash);
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
		Id other = (Id) obj;
		if (!Arrays.equals(hash, other.hash))
			return false;
		return true;
	}
	
	public byte[] getHash() {
		return Arrays.copyOf(hash, hash.length);
	}

}
