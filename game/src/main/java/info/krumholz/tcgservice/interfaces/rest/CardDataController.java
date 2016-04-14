package info.krumholz.tcgservice.interfaces.rest;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import info.krumholz.tcgservice.data.CardData;
import info.krumholz.tcgservice.storage.CardDataStorage;
import info.krumholz.tcgservice.utils.Result;
import info.krumholz.tcgservice.utils.CommonlyUsedMethods;

@Controller
@RequestMapping("/cardDatas")
public class CardDataController {

	@Autowired
	private CardDataStorage cardDataStorage;

	@RequestMapping(method = RequestMethod.GET)
	public @ResponseBody Set<UUID> list() {
		return cardDataStorage.listUUIDs();
	}

	@RequestMapping(value = "/editions", method = RequestMethod.GET, produces = "application/json")
	public @ResponseBody ResponseEntity<Set<String>> editions() {
		return ResponseEntity.ok(cardDataStorage.listEditionNames());
	}

	@SuppressWarnings("unchecked")
	@RequestMapping(value = "", method = RequestMethod.POST, produces = "application/json")
	public @ResponseBody ResponseEntity<CardData> createCard(@RequestParam(value = "name", required = true) String name,
			@RequestParam(value = "edition", required = true) String edition,
			@RequestParam(value = "imageId", required = true) String imageIdAsString,
			@RequestParam(value = "values", required = false) String values) {

		ObjectMapper o = new ObjectMapper();
		final Map<String, String> readValue;
		if (values == null) {
			readValue = new HashMap<String, String>();
		} else {
			try {
				readValue = (Map<String, String>) o.readValue(values, Map.class);
			} catch (IOException e) {
				HttpHeaders header = new HttpHeaders();
				header.put("failureMessage", Arrays.asList(new String[] { e.getMessage() }));
				ResponseEntity<CardData> ent = new ResponseEntity<CardData>(header, HttpStatus.BAD_REQUEST);
				return ent;
			}
		}
		UUID imageId;
		try {
			imageId = UUID.fromString(imageIdAsString);
		} catch (IllegalArgumentException e) {
			HttpHeaders header = new HttpHeaders();
			header.add("failureReason", "Given uuid is not a legal uuid");
			return new ResponseEntity<CardData>(header, HttpStatus.BAD_REQUEST);
		}
		Result<UUID> newCardData = cardDataStorage.create(name, edition, imageId, readValue);
		if (newCardData.isFailed()) {
			HttpHeaders header = new HttpHeaders();
			header.add("failureReason", newCardData.getFailureReason());
			return new ResponseEntity<CardData>(header, HttpStatus.BAD_REQUEST);
		}
		Result<CardData> result = cardDataStorage.get(newCardData.get());
		if (result.isFailed()) {
			HttpHeaders header = new HttpHeaders();
			header.add("failureReason", result.getFailureReason());
			return new ResponseEntity<CardData>(header, HttpStatus.BAD_REQUEST);
		}

		return new ResponseEntity<CardData>(result.get(), HttpStatus.OK);
	}

	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/{idAsString}", method = RequestMethod.PUT)
	public @ResponseBody ResponseEntity<CardData> update(@PathVariable String idAsString,
			@RequestParam(value = "name", required = true) String name,
			@RequestParam(value = "edition", required = true) String edition,
			@RequestParam(value = "imageId", required = true) String imageId,
			@RequestParam(value = "values", required = false) String valuesAsString) {
		UUID uuid;
		try {
			uuid = UUID.fromString(idAsString);
		} catch (IllegalArgumentException e) {
			HttpHeaders headers = new HttpHeaders();
			headers.add("failureReason", "UUID could not be parsed");
			ResponseEntity<CardData> responseEntity = new ResponseEntity<CardData>(headers, HttpStatus.BAD_REQUEST);
			return responseEntity;
		}
		ObjectMapper o = new ObjectMapper();
		final Map<String, String> values;
		if (valuesAsString == null) {
			values = new HashMap<String, String>();
		} else {
			try {
				values = (Map<String, String>) o.readValue(valuesAsString, Map.class);
			} catch (IOException e) {
				HttpHeaders header = new HttpHeaders();
				header.put("failureMessage", Arrays.asList(new String[] { e.getMessage() }));
				ResponseEntity<CardData> ent = new ResponseEntity<CardData>(header, HttpStatus.BAD_REQUEST);
				return ent;
			}
		}
		Result<CardData> cardData = cardDataStorage.update(uuid, name, edition, UUID.fromString(imageId), values);
		if (cardData.isFailed()) {
			HttpHeaders headers = new HttpHeaders();
			headers.add("failureReason", cardData.getFailureReason());
			ResponseEntity<CardData> responseEntity = new ResponseEntity<CardData>(headers, HttpStatus.BAD_REQUEST);
			return responseEntity;
		}

		return new ResponseEntity<CardData>(cardData.get(), HttpStatus.OK);
	}

	@RequestMapping(value = "/{idAsString}/{revision}", method = RequestMethod.GET)
	public @ResponseBody ResponseEntity<CardData> getRevision(@PathVariable String idAsString,
			@PathVariable int revision) {
		UUID uuid;
		try {
			uuid = UUID.fromString(idAsString);
		} catch (IllegalArgumentException e) {
			HttpHeaders headers = new HttpHeaders();
			headers.add("failureReason", "UUID could not be parsed");
			ResponseEntity<CardData> responseEntity = new ResponseEntity<CardData>(headers, HttpStatus.BAD_REQUEST);
			return responseEntity;
		}
		Result<CardData> cardData = cardDataStorage.get(uuid, revision);
		if (cardData.isFailed()) {
			HttpHeaders headers = new HttpHeaders();
			headers.add("failureReason", cardData.getFailureReason());
			ResponseEntity<CardData> responseEntity = new ResponseEntity<CardData>(headers, HttpStatus.BAD_REQUEST);
			return responseEntity;
		}

		return new ResponseEntity<CardData>(cardData.get(), HttpStatus.OK);
	}

	@RequestMapping(value = "/{idAsString}", method = RequestMethod.GET)
	public @ResponseBody ResponseEntity<CardData> get(@PathVariable String idAsString) {
		UUID uuid;
		try {
			uuid = UUID.fromString(idAsString);
		} catch (IllegalArgumentException e) {
			HttpHeaders headers = new HttpHeaders();
			headers.add("failureReason", "UUID could not be parsed");
			ResponseEntity<CardData> responseEntity = new ResponseEntity<CardData>(headers, HttpStatus.BAD_REQUEST);
			return responseEntity;
		}
		Result<CardData> cardData = cardDataStorage.get(uuid);
		if (cardData.isFailed()) {
			HttpHeaders headers = new HttpHeaders();
			headers.add("failureReason", cardData.getFailureReason());
			ResponseEntity<CardData> responseEntity = new ResponseEntity<CardData>(headers, HttpStatus.BAD_REQUEST);
			return responseEntity;
		}

		return new ResponseEntity<CardData>(cardData.get(), HttpStatus.OK);
	}

	@RequestMapping(value = "/methods", method = RequestMethod.GET)
	public @ResponseBody List<String> methods() {
		return CommonlyUsedMethods.methodsWithRequestMapping(this.getClass());
	}
}
