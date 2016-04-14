package info.krumholz.tcgservice.data;

import java.util.UUID;

public class Image {

	public UUID id;
	public String name;
	public String contentType;
	public byte[] data;

	public Image() {
	}

	public Image(String name, byte[] data, String contentType) {
		this.name = name;
		this.data = data;
		this.contentType = contentType;
	}
}
