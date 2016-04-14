package info.krumholz.tcgservice.interfaces.soap;

import org.springframework.ws.soap.SoapFaultException;

@SuppressWarnings("serial")
public class WebServiceException extends SoapFaultException {

	public WebServiceException(String message) {
		super(message);
	}

	public WebServiceException(String string, Object... args) {
		super(String.format(string, args));
	}

}
