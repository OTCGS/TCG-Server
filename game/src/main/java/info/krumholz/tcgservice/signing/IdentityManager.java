package info.krumholz.tcgservice.signing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.krumholz.tcgservice.utils.CommonlyUsedMethods;

public class IdentityManager {

	private static final Logger logger = LoggerFactory.getLogger(IdentityManager.class);

	private java.security.Signature signatureFactory;
	private static final String privateKeyFilename = "privkey.txt";
	private static final String publicKeyFilename = "pubkey.txt";
	private KeyPair keyPair;

	@PostConstruct
	public void init() throws NoSuchAlgorithmException {
		signatureFactory = java.security.Signature.getInstance("SHA256withRSA");
		if (!loadIdentity()) {
			logger.info("No identity information found. Creating new identity.");
			createNewIdentity();
		}
	}

	public RSAPublicKey getPublicKey() {
		return (RSAPublicKey) keyPair.getPublic();
	}

	public boolean isSignatureCorrect(Signature signature, byte[] data) {
		try {
			signatureFactory.initVerify(signature.signee);
			signatureFactory.update(data);
			return signatureFactory.verify(signature.signature);
		} catch (InvalidKeyException | SignatureException e) {
			logger.error("Verifying a signature failed with {}" + e.getMessage());
			return false;
		}
	}

	public boolean areSignaturesCorrect(Signed<?> signed) {
		boolean result = true;
		for (Signature signature : signed.signatures) {
			boolean signatureCorrect = isSignatureCorrect(signature, signed.signable.toBytes());
			if (!signatureCorrect) {
				logger.warn("Signature incorrect: " + signed.toString());
			}
			result = result && signatureCorrect;
		}
		return result;
	}

	public boolean isSignaturePresent(RSAPublicKey publicKey, Signed<?> signed) {
		for (info.krumholz.tcgservice.signing.Signature signature : signed.signatures) {
			if (Arrays.equals(signature.signee.getEncoded(), publicKey.getEncoded())) {
				return true;
			}
		}
		return false;
	}

	public boolean isOwnSignaturePresent(Signed<?> signed) {
		return isSignaturePresent(getPublicKey(), signed);
	}

	public <T extends Signable> Optional<Signed<T>> sign(T target) {
		try {
			signatureFactory.initSign(keyPair.getPrivate());
			signatureFactory.update(target.toBytes());
			final byte[] signatureBytes = signatureFactory.sign();
			return Optional.of(new Signed<T>(target, new Signature(getPublicKey(), signatureBytes)));
		} catch (InvalidKeyException | SignatureException e) {
			logger.error("Signing data failed with {}" + e.getMessage());
			return Optional.empty();
		}
	}

	public Optional<byte[]> signData(byte[] data) {
		try {
			signatureFactory.initSign(keyPair.getPrivate());
			signatureFactory.update(data);
			return Optional.of(signatureFactory.sign());
		} catch (InvalidKeyException | SignatureException e) {
			logger.error("Failed to sign data: {}\n", e.getMessage());
			return Optional.empty();
		}
	}

	private void createNewIdentity() {
		Path publicKeyPath = CommonlyUsedMethods.getPath(publicKeyFilename);
		Path privateKeyPath = CommonlyUsedMethods.getPath(privateKeyFilename);
		try {
			KeyPairGenerator instance = KeyPairGenerator.getInstance("RSA");
			instance.initialize(1024);
			keyPair = instance.generateKeyPair();
			byte[] privateKeyBytes = keyPair.getPrivate().getEncoded();
			byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
			Files.write(privateKeyPath, privateKeyBytes);
			Files.write(publicKeyPath, publicKeyBytes);
		} catch (Exception e) {
			logger.error("Failed to create new ServerIdentity: {}\n"
					+ "Has the server permission to write to {} and {} ?\n" + "Is RSA supported on this server?",
					e.getMessage(), privateKeyPath, publicKeyPath);
			throw new RuntimeException(e);
		}
	}

	private boolean loadIdentity() {
		byte[] privateKeyBytes;
		byte[] publicKeyBytes;
		Path publicKeyPath = CommonlyUsedMethods.getPath(publicKeyFilename);
		Path privateKeyPath = CommonlyUsedMethods.getPath(privateKeyFilename);
		try {
			if (!Files.exists(publicKeyPath) || !Files.exists(privateKeyPath)) {
				return false;
			}
			publicKeyBytes = Files.readAllBytes(publicKeyPath);
			privateKeyBytes = Files.readAllBytes(privateKeyPath);
			KeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
			KeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
			PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(privateKeySpec);
			java.security.PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(publicKeySpec);
			keyPair = new KeyPair(publicKey, privateKey);
		} catch (IOException e) {
			return false;
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			logger.error("Failed to load ServerIdentity: {}\n"
					+ "You can try creatign a new key by deleting {} and {}\n" + "Is RSA supported on this server?",
					e.getMessage(), publicKeyPath, privateKeyPath);
			throw new RuntimeException(e);
		}
		return true;
	}
}
