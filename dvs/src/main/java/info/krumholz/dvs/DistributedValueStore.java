package info.krumholz.dvs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.msgpack.core.ExtendedTypeHeader;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;

import info.krumholz.dvs.exceptions.StoreException;

public class DistributedValueStore {

	private static final Logger logger = LoggerFactory.getLogger(DistributedValueStore.class);

	private ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory());

	@Autowired
	private Path storageDirectory;

	public void setStorageDirectory(Path storageDirectory) {
		this.storageDirectory = storageDirectory;
	}

	@PostConstruct
	public void init() throws IOException, StoreException {
		createIfNotExistsStorageDirectory();
	}

	public void registerModule(Module module) {
		objectMapper.registerModule(module);
	}

	private void createIfNotExistsStorageDirectory() {
		if (!Files.isDirectory(storageDirectory)) {
			logger.warn("Storage directory for values {} does not exist. Creating new directory.",
					storageDirectory.toString());
			try {
				Files.createDirectories(storageDirectory);
			} catch (Exception e) {
				throw new StoreException(e);
			}
		}
	}

	public Id storeUUID(UUID value) {
		return withPacker((packer) -> {
			byte[] bytes = value.toString().getBytes("UTF-8");
			packer.packExtendedTypeHeader(1, bytes.length);
			for (int i = 0; i < bytes.length; i += 1) {
				packer.packByte(bytes[i]);
			}
		});
	}

	public Id storeByteArray(byte[] value) {
		return withPacker((packer) -> {
			packer.packArrayHeader(value.length);
			for (int i = 0; i < value.length; i += 1) {
				packer.packByte(value[i]);
			}
		});
	}

	public Id storeString(String value) {
		return withPacker((packer) -> packer.packString(value));
	}

	public Id storeDouble(double value) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			MessagePacker packer = MessagePack.newDefaultPacker(out);
			packer.packDouble(value).flush();
			byte[] result = out.toByteArray();
			return storeBytes(result);
		} catch (IOException e) {
			throw new StoreException(e);
		}
	}

	public Id storeLong(long value) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			MessagePacker packer = MessagePack.newDefaultPacker(out);
			packer.packLong(value).flush();
			byte[] result = out.toByteArray();
			return storeBytes(result);
		} catch (IOException e) {
			throw new StoreException(e);
		}
	}

	public Id storePojo(Object pojo) {
		try {
			return storeBytes(objectMapper.writeValueAsBytes(pojo));
		} catch (JsonProcessingException e) {
			throw new StoreException(e);
		}
	}

	private Id storeBytes(byte[] data) {
		checkInitialisation();
		Id id = Id.fromData(data);
		Path valueFile = storageDirectory.resolve(id.hashAsString);
		try {
			Path tempFile = Files.createTempFile("dvs", "tempFile");
			Files.write(tempFile, data);
			Files.move(tempFile, valueFile, StandardCopyOption.ATOMIC_MOVE);
			return id;
		} catch (FileAlreadyExistsException e) {
			// nothing to do file already written
			return id;
		} catch (Exception e) {
			logger.error("Value file {} could not be written to", valueFile);
			try {
				Files.delete(valueFile);
				throw new StoreException(e);
			} catch (Exception e1) {
				logger.error("Value file {} could not be deleted after unsuccesful write", valueFile);
				throw new StoreException(e1);
			}
		}
	}

	public void deleteValue(Id id) {
		checkInitialisation();
		Path valueFile = null;
		try {
			valueFile = storageDirectory.resolve(id.hashAsString);
			Files.delete(valueFile);
		} catch (Exception e) {
			logger.warn("Value file {} could not be deleted ", valueFile);
		}
	}

	public Optional<Long> retrieveLong(Id id) {
		checkInitialisation();
		try {
			byte[] data = readFileContent(id);
			MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(data);
			long msg = unpacker.unpackLong();
			return Optional.of(msg);
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	public Optional<String> retrieveString(Id id) {
		checkInitialisation();
		try {
			byte[] data = readFileContent(id);
			MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(data);
			String msg = unpacker.unpackString();
			return Optional.of(msg);
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	public Optional<byte[]> retrieveBytes(Id id) {
		checkInitialisation();
		try {
			byte[] data = readFileContent(id);
			MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(data);
			int length = unpacker.unpackArrayHeader();
			byte[] result = new byte[length];
			for (int i = 0; i < length; i += 1) {
				result[i] = unpacker.unpackByte();
			}
			return Optional.of(result);
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	public Optional<UUID> retrieveUUID(Id id) {
		checkInitialisation();
		try {
			byte[] data = readFileContent(id);
			MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(data);
			ExtendedTypeHeader typeHeader = unpacker.unpackExtendedTypeHeader();
			if (typeHeader.getType() != 1) {
				return Optional.empty();
			}
			byte[] result = new byte[typeHeader.getLength()];
			for (int i = 0; i < typeHeader.getLength(); i += 1) {
				result[i] = unpacker.unpackByte();
			}
			return Optional.of(UUID.fromString(new String(result, Charset.forName("UTF-8"))));
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	public Optional<Double> retrieveDouble(Id id) {
		checkInitialisation();
		try {
			byte[] data = readFileContent(id);
			MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(data);
			return Optional.of(unpacker.unpackDouble());
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	public <T> Optional<T> retrievePojo(Id id, Class<T> class1) {
		checkInitialisation();
		try {
			byte[] data = readFileContent(id);
			return Optional.of(objectMapper.readValue(data, class1));
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	private byte[] readFileContent(Id id) {
		Path path = null;
		try {
			path = storageDirectory.resolve(id.hashAsString);
			byte[] data = Files.readAllBytes(path);
			return data;
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("readFileContent({}) failed with\n{}", path, e.getMessage());
			throw new StoreException(e);
		}
	}

	private void checkInitialisation() {
		if (storageDirectory == null)
			throw new StoreException("Storage directory not set.");
	}

	private interface Consumer<T> {
		public void accept(MessagePacker packer) throws IOException;
	}

	private Id withPacker(Consumer<MessagePacker> packTask) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			MessagePacker packer = MessagePack.newDefaultPacker(out);
			packTask.accept(packer);
			packer.flush();
			byte[] result = out.toByteArray();
			return storeBytes(result);
		} catch (IOException e) {
			throw new StoreException(e);
		}
	}

}