package info.krumholz.tcgservice.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import info.krumholz.tcgservice.data.Image;
import info.krumholz.tcgservice.data.Settings;

public class SettingsStorage {

	private static final Logger logger = LoggerFactory.getLogger(SettingsStorage.class);

	@Autowired
	private UUIDPojoStore<Image> imageStorage;

	@Autowired
	private UUIDPojoStore<Settings> settingsStore;

	private static final String defaultName = "My Server";
	private static final String defaultUrl = "http://localhost:8080";
	private static final int defaultBoosterSize = 5;

	private static final UUID settingsKey = UUID.fromString("43784600-ecc7-49b5-81b8-975e49bb3219");

	private Settings settings;

	@PostConstruct
	public void init() throws IOException {
		if (!loadSettings()) {
			if (!initialiseProperties()) {
				logger.error("Settings couldn't be loaded or initialised.");
				throw new IllegalStateException("SettingsManager couldn't be initialised");
			}
		}

	}

	private boolean initialiseProperties() throws IOException {
		byte[] serverIconData;
		InputStream resource = new ClassPathResource("/ServerIcon.png").getInputStream();
		serverIconData = IOUtils.toByteArray(resource);

		UUID randomUUID = UUID.randomUUID();
		Image image = new Image("Trade splash screen", serverIconData, "image/png");
		imageStorage.put(randomUUID, image);
		Settings settings = new Settings(defaultName, defaultUrl, randomUUID, defaultBoosterSize);

		storeSettings(settings);
		this.settings = settings;
		return true;
	}

	private boolean loadSettings() {
		Optional<Settings> settingsQuery = settingsStore.get(settingsKey);
		if (!settingsQuery.isPresent()) {
			return false;
		}
		this.settings = settingsQuery.get();
		return true;
	}

	public Settings getSettings() {
		return settings;
	}

	public int getRevision() {
		return settingsStore.getRevision(settingsKey).orElseGet(() -> 0);
	}

	private void storeSettings(Settings settings) {
		settingsStore.put(settingsKey, settings);
	}

	public void setSettings(Settings newSettings) {
		storeSettings(newSettings);
		this.settings = newSettings;
	}

}
