package info.krumholz.tcgservice.data;

import java.security.interfaces.RSAPublicKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CardData {

	public UUID id;
	public RSAPublicKey creator;
	public String name;
	public String edition;
	public int revision;
	public UUID imageId;
	public Map<String, String> values;

	public CardData() {
	}

	public CardData(UUID id, RSAPublicKey creator, String name, String edition, int revision, UUID imageId,
			Map<String, String> values) {
		this.id = id;
		this.creator = creator;
		this.name = name;
		this.edition = edition;
		this.revision = revision;
		this.imageId = imageId;
		this.values = values == null ? new HashMap<String, String>() : Collections.unmodifiableMap(values);
	}
}