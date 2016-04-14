package info.krumholz.dvs.exceptions;

@SuppressWarnings("serial")
public class StoreException extends RuntimeException {

	public StoreException(Exception e) {
		super(e);
	}

	public StoreException(String message) {
		super(message);
	}

}
