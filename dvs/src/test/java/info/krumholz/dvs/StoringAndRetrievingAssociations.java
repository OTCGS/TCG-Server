package info.krumholz.dvs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import info.krumholz.dvs.exceptions.StoreException;

public class StoringAndRetrievingAssociations {

	private DistributedValueStore dvs;
	private DistributedAssociationStore das;

	@Before
	public void init() throws IOException, NoSuchAlgorithmException {
		dvs = new DistributedValueStore();
		dvs.setStorageDirectory(Files.createTempDirectory("dvs"));
		dvs.init();
		das = new DistributedAssociationStore();
		das.setStorageDirectory(Files.createTempDirectory("avs"));
	}

	@Test
	public void storingAndRetrieving_withPrimitives_works() {
		Id id1 = dvs.storeString("1");
		Id id2 = dvs.storeString("foo");
		das.storeAssociation("my-association", id1, id2);
		Optional<Association> association = das.retrieveAssociation("my-association", id1);
		assertTrue(association.isPresent());
		assertEquals(association.get().from, id1);
		assertEquals(association.get().to, id2);
	}

	@Test
	public void storingAndRetrieving_withPojos_works() {
		UUID actualUUID = UUID.randomUUID();
		Id id1 = dvs.storeUUID(actualUUID);
		Id id2 = dvs.storeString("foo");
		das.storeAssociation("my-association", id1, id2);
		Optional<Association> association = das.retrieveAssociation("my-association", id1);
		assertTrue(association.isPresent());
		assertEquals(association.get().from, id1);
		assertEquals(association.get().to, id2);
	}

	@Test
	public void retrievingNotExistingAssociation_returnsEmptyOptional() {
		Optional<Association> association = das.retrieveAssociation("my-association",
				Id.fromData(new byte[] { 1, 2, 3 }));
		assertFalse(association.isPresent());
	}

	@Test
	public void storeAssociation_initialAssociationRevision_isOne() {
		Id id1 = dvs.storeString("1");
		Id id2 = dvs.storeString("foo");
		das.storeAssociation("my-association", id1, id2);
		Optional<Association> association = das.retrieveAssociation("my-association", id1);
		assertTrue(association.isPresent());
		assertEquals(1, association.get().revision);
	}

	@Test(expected = StoreException.class)
	public void storeAssociation_withWrongFilesInAssociationDirectory_throws() throws IOException {
		Path storageDirectory = Files.createTempDirectory("temp");
		das.setStorageDirectory(storageDirectory);
		Id id1 = dvs.storeString("1");
		Id id2 = dvs.storeString("foo");

		Files.createFile(storageDirectory.resolve("my-association"));
		das.storeAssociation("my-association", id1, id2);
	}

	@Test(expected = StoreException.class)
	public void storeAssociation_withWrongFilesInAssociationSubDirectory_throws() throws IOException {
		Path storageDirectory = Files.createTempDirectory("temp");
		das.setStorageDirectory(storageDirectory);
		Id id1 = dvs.storeString("1");
		Id id2 = dvs.storeString("foo");

		das.storeAssociation("my-association", id1, id2);
		Path pathToIdFolder = storageDirectory.resolve("my-association").resolve(id1.hashAsString);
		Files.walk(pathToIdFolder).skip(1).forEach((p) -> {
			try {
				Files.delete(p);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		Files.delete(pathToIdFolder);
		Files.createFile(pathToIdFolder);
		das.storeAssociation("my-association", id1, id2);
	}

	@Test(expected = StoreException.class)
	public void retrievingAssociation_FilesInaccesible_throws() throws IOException {
		Path storageDirectory = Files.createTempDirectory("temp");
		das.setStorageDirectory(storageDirectory);
		Id id1 = dvs.storeString("1");
		Id id2 = dvs.storeString("foo");

		String associationName = "my-association";
		das.storeAssociation(associationName, id1, id2);
		Path pathToIdFolder = storageDirectory.resolve(associationName).resolve(id1.hashAsString).resolve("1");
		RandomAccessFile randomAccessFile = new RandomAccessFile(pathToIdFolder.toFile(), "rw");
		try {
			randomAccessFile.getChannel().lock();
			das.retrieveAssociation(associationName, id1);
		} finally {
			randomAccessFile.close();
		}
	}
}
