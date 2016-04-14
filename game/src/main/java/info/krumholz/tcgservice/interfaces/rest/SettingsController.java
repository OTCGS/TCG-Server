package info.krumholz.tcgservice.interfaces.rest;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import info.krumholz.tcgservice.data.Settings;
import info.krumholz.tcgservice.storage.SettingsStorage;

@Controller
@RequestMapping("/api/settings")
public class SettingsController {

	private static final String serverNameKey = "serverName";
	private static final String serverIconIdKey = "serverIconId";
	private static final String serverUrlKey = "serverUrl";
	private static final String boosterSize = "boosterSize";

	@Autowired
	private SettingsStorage settingsManager;

	@RequestMapping(value = "/{setting}", method = RequestMethod.GET, headers = "Cache-Control: no-cache, no-store, must-revalidate")
	public @ResponseBody ResponseEntity<?> get(@PathVariable String setting) {
		Settings settings = settingsManager.getSettings();
		switch (setting) {
		case serverNameKey:
			return ResponseEntity.ok(settings.serverName);
		case serverIconIdKey:
			return ResponseEntity.ok(settings.serverIconId);
		case serverUrlKey:
			return ResponseEntity.ok(settings.serverUrl);
		case boosterSize:
			return ResponseEntity.ok(settings.boosterSize);
		default:
			return ResponseEntity.notFound().build();
		}
	}

	@RequestMapping(value = "", method = RequestMethod.PUT)
	public @ResponseBody ResponseEntity<?> setAll(@RequestParam String serverName, @RequestParam String serverUrl,
			@RequestParam String serverIconId, @RequestParam int boosterSize) {
		UUID imageUuid;
		try {
			imageUuid = UUID.fromString(serverIconId);
		} catch (IllegalArgumentException e) {
			HttpHeaders headers = new HttpHeaders();
			headers.add("failureReason", "UUID could not be parsed");
			ResponseEntity<?> ent = new ResponseEntity<String>(headers, HttpStatus.BAD_REQUEST);
			return ent;
		}
		Settings newSettings = new Settings(serverName, serverUrl, imageUuid, boosterSize);
		settingsManager.setSettings(newSettings);
		return ResponseEntity.ok().build();
	}

	@RequestMapping(method = RequestMethod.GET)
	public @ResponseBody ResponseEntity<?> list() {
		Settings settings = settingsManager.getSettings();
		return ResponseEntity.ok(settings);
	}
}
