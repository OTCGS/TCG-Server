import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;

import com.google.common.io.BaseEncoding;

public class PrivKeyMaker {

	public static void main(String[] args) throws NoSuchAlgorithmException {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		KeyPair generateKeyPair = generator.generateKeyPair();

		System.out.println(BaseEncoding.base16().encode(generateKeyPair.getPrivate().getEncoded()));
		System.out.println(BaseEncoding.base16().encode(generateKeyPair.getPublic().getEncoded()));
		System.out.println(BaseEncoding.base16().encode(((RSAPublicKey)generateKeyPair.getPublic()).getModulus().toByteArray()));
		System.out.println(BaseEncoding.base16().encode(((RSAPublicKey)generateKeyPair.getPublic()).getPublicExponent().toByteArray()));
	}

}
