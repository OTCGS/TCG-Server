package info.krumholz.tcgservice.data;

import java.util.UUID;

public class Settings {

	public String serverName;
	public String serverUrl;
	public UUID serverIconId;
	public int boosterSize;

	public Settings() {
	}

	public Settings(String serverName, String serverUrl, UUID serverIconId, int boosterSize) {
		this.serverName = serverName;
		this.serverUrl = serverUrl;
		this.serverIconId = serverIconId;
		this.boosterSize = boosterSize;
	}
}
