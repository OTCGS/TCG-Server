package info.krumholz.tcgservice.interfaces.soap;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import org.springframework.ws.soap.server.endpoint.SoapFaultDefinition;
import org.springframework.ws.soap.server.endpoint.SoapFaultMappingExceptionResolver;

public class ExceptionHandler extends SoapFaultMappingExceptionResolver {

	@Override
	protected SoapFaultDefinition getFaultDefinition(Object endpoint, Exception ex) {
		ex.printStackTrace();
		SoapFaultDefinition faultDefinition = new SoapFaultDefinition();
		faultDefinition.setFaultCode(SoapFaultDefinition.SERVER);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PrintStream tmp = new PrintStream(out);
		ex.printStackTrace(tmp);
		tmp.flush();
		try {
			faultDefinition.setFaultStringOrReason(out.toString("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return faultDefinition;
	}

}
