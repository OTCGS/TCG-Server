package info.krumholz.dvs;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import info.krumholz.dvs.exceptions.StoreException;

public class DistributedAssociationStore {

	private static final Logger logger = LoggerFactory.getLogger(DistributedValueStore.class);

	@Autowired
	private Path associationStorageDirectory;

	public void setStorageDirectory(Path associationStorageDirectory) {
		this.associationStorageDirectory = associationStorageDirectory;
	}

	@PostConstruct
	public void init() {
		try {
			if (!Files.isDirectory(associationStorageDirectory)) {
				logger.warn("Storage directory for associations {} does not exist. Creating new directory.",
						associationStorageDirectory.toString());
				Files.createDirectories(associationStorageDirectory);
			}
		} catch (Exception e) {
			throw new StoreException(e);
		}
	}

	public void storeAssociation(String associationName, Id id1, Id id2) {
		checkInitialisation();
		try {
			Path associationDir = associationStorageDirectory.resolve(associationName);
			try {
				Files.createDirectory(associationDir);
			} catch (FileAlreadyExistsException e) {
				// if files exists. nothing to do
			}
			if (!Files.isDirectory(associationDir)) {
				throw new StoreException("Database corrupted");
			}
			Path valueFolder = associationDir.resolve(id1.hashAsString);
			try {
				Files.createDirectory(valueFolder);
			} catch (FileAlreadyExistsException e) {
				// if files exists. nothing to do
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (!Files.isDirectory(valueFolder)) {
				throw new StoreException("Value folder is not a directory");
			}

			Path valueFile = null;
			boolean successFullCreation = false;
			int revision = 1;
			while (!successFullCreation) {
				try {
					Optional<Path> first = getPathOfLatestRevision(valueFolder);
					if (first.isPresent()) {
						revision = Integer.parseInt(first.get().getFileName().toString());
						revision += 1;
						valueFile = valueFolder.resolve(Integer.toString(revision));
					} else {
						valueFile = valueFolder.resolve("1");
					}
					Files.createFile(valueFile);
					successFullCreation = true;
				} catch (FileAlreadyExistsException e) {
					// we have to retry
				}
			}
			Files.write(valueFile, id2.getHash());
		} catch (IOException e) {
			throw new StoreException(e);
		}
	}

	public Optional<Association> retrieveAssociation(String associationName, Id id, int revision) {
		checkInitialisation();
		Path associationDir = associationStorageDirectory.resolve(associationName);
		if (!Files.isDirectory(associationDir)) {
			logger.debug("Association with name " + associationName + " not found");
			return Optional.empty();
		}

		Path associationFolder = associationDir.resolve(id.toString());
		if (!Files.isDirectory(associationFolder)) {
			logger.debug("Id " + id + " has no association with name " + associationName);
			return Optional.empty();
		}

		Path associationValuePath = associationFolder.resolve("" + revision);

		try {
			byte[] readAllBytes = Files.readAllBytes(associationValuePath);
			return Optional.of(new Association(id, Id.fromHash(readAllBytes),
					Integer.parseInt(associationValuePath.getFileName().toString())));
		} catch (IOException e) {
			logger.error("Association value " + associationValuePath + " could not be read");
			throw new StoreException("Could not read file " + associationValuePath.toString());
		}
	}

	public Optional<Association> retrieveAssociation(String associationName, Id id) {
		checkInitialisation();
		Path associationDir = associationStorageDirectory.resolve(associationName);
		if (!Files.isDirectory(associationDir)) {
			logger.debug("Association with name " + associationName + " not found");
			return Optional.empty();
		}

		Path associationFolder = associationDir.resolve(id.toString());
		if (!Files.isDirectory(associationFolder)) {
			logger.debug("Id " + id + " has no association with name " + associationName);
			return Optional.empty();
		}

		Optional<Path> first = getPathOfLatestRevision(associationFolder);
		if (!first.isPresent()) {
			logger.error("Associationfolder exists and is empty");
			return Optional.empty();
		}
		Path associationValuePath = first.get();

		try {
			byte[] readAllBytes = Files.readAllBytes(associationValuePath);
			return Optional.of(new Association(id, Id.fromHash(readAllBytes),
					Integer.parseInt(associationValuePath.getFileName().toString())));
		} catch (IOException e) {
			logger.error("Association value " + associationValuePath + " could not be read");
			throw new StoreException("Could not read file " + associationValuePath.toString());
		}
	}

	public Set<Id> getIdsWith_ALL_Associations(String... associationNames) {
		ArrayList<Set<Id>> intermediateResult = new ArrayList<>();
		for (String associationName : associationNames) {
			Path associationDir = associationStorageDirectory.resolve(associationName);
			Set<Id> ids = new HashSet<Id>();
			if (!Files.exists(associationDir)) {
				return ids;
			}
			if (!Files.isDirectory(associationDir)) {
				throw new StoreException("AssociationStorage broken. Expected directory at " + associationDir + ".");
			}
			try {
				Files.walk(associationDir, 1).skip(1).forEach((path) -> {
					ids.add(Id.fromHash(path.getFileName().toString()));
				});
			} catch (IOException e) {
				throw new StoreException(e);
			}
			intermediateResult.add(ids);
		}
		if (intermediateResult.isEmpty())
			return new HashSet<>();
		Set<Id> result = intermediateResult.get(0);
		for (int i = 0; i < intermediateResult.size(); i += 1) {
			result.retainAll(intermediateResult.get(i));
		}
		return result;
	}

	private void checkInitialisation() {
		if (associationStorageDirectory == null) {
			throw new StoreException("Storage directory not set.");
		}
	}

	private Optional<Path> getPathOfLatestRevision(Path valueFolder) {
		try {
			Optional<Path> first;
			Stream<Path> sorted = Files.list(valueFolder).sorted((p1, p2) -> {
				return -Integer.parseInt(p1.getFileName().toString()) + Integer.parseInt(p2.getFileName().toString());
			});
			first = sorted.findFirst();
			sorted.close();
			return first;
		} catch (Exception e) {
			throw new StoreException(e);
		}
	}

}
