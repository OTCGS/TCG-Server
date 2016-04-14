package info.krumholz.dvs;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import info.krumholz.dvs.exceptions.StoreException;

public class TestDistributedValueStoreConstruction {

	private DistributedValueStore dvs;

	@Before
	public void setUp() throws Exception {
		dvs = new DistributedValueStore();
	}

	@Test
	public void storageDirectoryNotExisting_createsStorageDirectory() throws StoreException, IOException {
		Path tempDirectory = Files.createTempDirectory("dvs");
		dvs.setStorageDirectory(tempDirectory.resolve("foo"));
		assertFalse(Files.isDirectory(tempDirectory.resolve("foo")));
		dvs.init();
		assertTrue(Files.isDirectory(tempDirectory.resolve("foo")));
	}

	@Test(expected = StoreException.class)
	public void storageDirectoryIsAFile_throws() throws StoreException, IOException {
		Path tempDirectory = Files.createTempDirectory("dvs");
		Files.createFile(tempDirectory.resolve("foo"));
		dvs.setStorageDirectory(tempDirectory.resolve("foo"));
		dvs.init();
	}

	@Test(expected = StoreException.class)
	public void storingAStringValue_withoutInitialisation_throws() {
		dvs.storeString("Some value");
	}

	@Test(expected = StoreException.class)
	public void storingADoubleValue_withoutInitialisation_throws() {
		dvs.storeDouble(1.0);
	}

	@Test(expected = StoreException.class)
	public void storingALongValue_withoutInitialisation_throws() {
		dvs.storeLong(1L);
	}

	@Test(expected = StoreException.class)
	public void storingAByteArrayValue_withoutInitialisation_throws() {
		dvs.storeByteArray(new byte[] { 1, 2, 3 });
	}

	@Test(expected = StoreException.class)
	public void storingAUUIDValue_withoutInitialisation_throws() {
		dvs.storeUUID(UUID.randomUUID());
	}

	@Test(expected = StoreException.class)
	public void retrievingAUUIDValue_withoutInitialisation_throws() {
		dvs.retrieveUUID(Id.fromData(new byte[] { 1, 2, 3 }));
	}

	@Test(expected = StoreException.class)
	public void retrievingAStringValue_withoutInitialisation_throws() {
		dvs.retrieveString(Id.fromData(new byte[] { 1, 2, 3 }));
	}

	@Test(expected = StoreException.class)
	public void retrievingALongValue_withoutInitialisation_throws() {
		dvs.retrieveLong(Id.fromData(new byte[] { 1, 2, 3 }));
	}

	@Test(expected = StoreException.class)
	public void retrievingADoubleValue_withoutInitialisation_throws() {
		dvs.retrieveDouble(Id.fromData(new byte[] { 1, 2, 3 }));
	}

	@Test(expected = StoreException.class)
	public void retrievingAByteArrayValue_withoutInitialisation_throws() {
		dvs.retrieveBytes(Id.fromData(new byte[] { 1, 2, 3 }));
	}

}
