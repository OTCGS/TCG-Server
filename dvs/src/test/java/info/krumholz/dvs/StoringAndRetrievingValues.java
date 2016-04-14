package info.krumholz.dvs;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import info.krumholz.dvs.exceptions.StoreException;

public class StoringAndRetrievingValues {

	private DistributedValueStore dvs;

	@Before
	public void createDistributedValueStore() throws IOException, NoSuchAlgorithmException {
		dvs = new DistributedValueStore();
		dvs.setStorageDirectory(Files.createTempDirectory("dvs"));
		dvs.init();
	}

	static class Pojo {
		public final int someInteger = new Random().nextInt();
		public final String someString = UUID.randomUUID().toString();
	}

	@Test
	public void storeValue_withPojo_storesTheValue() {
		Pojo expected = new Pojo();
		Id id = dvs.storePojo(expected);
		Optional<Pojo> actual = dvs.retrievePojo(id, Pojo.class);
		assertEquals(expected.someInteger, actual.get().someInteger);
		assertEquals(expected.someString, actual.get().someString);
	}

	@Test
	public void storeValue_withLong_storesTheValue() throws IOException {
		long expected = 1L;
		Id id = dvs.storeLong(expected);
		Optional<Long> actual = dvs.retrieveLong(id);
		assertEquals(expected, actual.get().longValue());
	}

	@Test
	public void storeValue_withString_storesTheValue() throws IOException {
		String expected = UUID.randomUUID().toString();
		Id id = dvs.storeString(expected);
		Optional<String> actual = dvs.retrieveString(id);
		assertEquals(expected, actual.get());
	}

	@Test
	public void storeValue_withUUID_storesTheValue() throws IOException {
		UUID expected = UUID.randomUUID();
		Id id = dvs.storeUUID(expected);
		Optional<UUID> actual = dvs.retrieveUUID(id);
		assertEquals(expected, actual.get());
	}

	@Test
	public void storeValue_withDouble_storesTheValue() throws IOException {
		double expected = new Random().nextDouble();
		Id id = dvs.storeDouble(expected);
		Optional<Double> actual = dvs.retrieveDouble(id);
		assertTrue(expected == actual.get().doubleValue());
	}

	@Test
	public void storeValue_withByteArray_storesTheValue() throws IOException {
		Random random = new Random();
		byte[] expected = new byte[400];
		random.nextBytes(expected);
		Id id = dvs.storeByteArray(expected);
		Optional<byte[]> actual = dvs.retrieveBytes(id);
		assertArrayEquals(expected, actual.get());
	}

	@Test
	public void retrievingALong_ThatDoesNotExist_returnsOptionalEmpty() {
		Id id = Id.fromData(new byte[] { 1, 2, 3 });
		Optional<Long> retrieveLong = dvs.retrieveLong(id);
		assertFalse(retrieveLong.isPresent());
	}

	@Test(expected = StoreException.class)
	public void storingAValue_ImpossibleToWriteFile_throws() throws StoreException, IOException {
		Path tempDirectory = Files.createTempDirectory("dvs");
		dvs.setStorageDirectory(tempDirectory.resolve("foo"));
		dvs.init();
		Id storeValue = dvs.storeString("hello");
		String hashAsString = storeValue.hashAsString;
		Files.delete(tempDirectory.resolve("foo").resolve(hashAsString));
		Files.createDirectory(tempDirectory.resolve("foo").resolve(hashAsString));
		dvs.storeString("hello");
	}

	@Test
	public void retrievingValue_thatDoesntExist_returnsEmptyOptional() {
		Optional<UUID> idQuery = dvs.retrieveUUID(Id.fromData(new byte[] { 1 }));
		assertFalse(idQuery.isPresent());
	}

	@Test
	public void storingTheSameValue_returnsTheSameId() {
		Random random = new Random();
		byte[] value = new byte[400];
		random.nextBytes(value);
		Id id1 = dvs.storeByteArray(value);
		Id id2 = dvs.storeByteArray(value);
		assertEquals(id1, id2);
	}
}