package info.krumholz.tcgservice.interfaces.soap;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.UUID;

import info.krumholz.tcgservice.ServerIdentity;

public class Serializer {

	public static void write(DataOutputStream out, Object o) {
		if (o instanceof ServerIdentity) {
			writeBytes(out, (ServerIdentity) o);
		} else {
			throw new SerialisationException();
		}
	}

	private static void writeBytes(DataOutputStream writer, ServerIdentity serverIdentity) {
		try {
			writer.write(serverIdentity.getName().getBytes(Charset.forName("UTF-8")));
			writer.write(serverIdentity.getKey().getModulus());
			writer.write(serverIdentity.getKey().getExponent());
			String icon = serverIdentity.getIcon();
			if (icon != null) {
				UUID imageId = UUID.fromString(icon);
				writer.writeLong(imageId.getMostSignificantBits());
				writer.writeLong(imageId.getLeastSignificantBits());
			}
			writer.write(serverIdentity.getUri().getBytes(Charset.forName("UTF-8")));
			writer.writeInt(serverIdentity.getRevision());
			writer.flush();
		} catch (IOException e) {
			throw new SerialisationException(e);
		}

	}

}
