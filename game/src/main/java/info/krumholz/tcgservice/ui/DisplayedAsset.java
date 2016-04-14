package info.krumholz.tcgservice.ui;

import java.util.UUID;

public class DisplayedAsset {

	public final UUID id;
	public final String contentType;

	public DisplayedAsset(UUID id, String contentType) {
		this.id = id;
		this.contentType = contentType;
	}
}
