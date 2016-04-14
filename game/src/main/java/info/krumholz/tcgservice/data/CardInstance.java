package info.krumholz.tcgservice.data;

import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

public class CardInstance {

	public UUID id;
	public UUID cardDataId;
	public RSAPublicKey creator;
	
	public CardInstance() {}

	public CardInstance(UUID id, UUID cardDataId, RSAPublicKey creator) {
		this.id = id;
		this.cardDataId = cardDataId;
		this.creator = creator;
	}
}
