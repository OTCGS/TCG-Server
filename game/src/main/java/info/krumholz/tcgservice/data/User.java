package info.krumholz.tcgservice.data;

import java.security.interfaces.RSAPublicKey;
import java.util.Date;

public class User {

	public RSAPublicKey publicKey;
	public Date lastSeen;
	public Date lastBoosterRequest;

	public User() {
	}

	public User(RSAPublicKey publicKey, Date lastSeen, Date lastBoosterRequest) {
		this.publicKey = publicKey;
		this.lastSeen = lastSeen;
		this.lastBoosterRequest = lastBoosterRequest;
	}
}
