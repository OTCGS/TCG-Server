package info.krumholz.tcgservice.interfaces.soap;

import java.io.IOException;

@SuppressWarnings("serial")
public class SerialisationException extends RuntimeException {

	public SerialisationException() {
		super();
	}

	public SerialisationException(IOException e) {
		super(e);
	}

}
