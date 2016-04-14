package info.krumholz.tcgservice.interfaces.rest;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.PostConstruct;

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

import info.krumholz.tcgservice.data.CardInstance;
import info.krumholz.tcgservice.signing.IdentityManager;
import info.krumholz.tcgservice.storage.UUIDPojoStore;
import info.krumholz.tcgservice.utils.CommonlyUsedMethods;

@Controller
@RequestMapping("/cardInstances")
public class CardInstanceController {

	@Autowired
	private IdentityManager identityManager;

	@Autowired
	private UUIDPojoStore<CardInstance> cardInstanceStorage;

	@PostConstruct
	public void init() {
	}

	@RequestMapping(method = RequestMethod.GET)
	public @ResponseBody Set<UUID> list() {
		return cardInstanceStorage.getKeys();
	}

	@RequestMapping(value = "/new", method = RequestMethod.POST, produces = "application/json")
	public @ResponseBody ResponseEntity<CardInstance> create(
			@RequestParam(value = "cardDataId", required = true) String cardDataIdAsString) {

		UUID uuid;
		try {
			uuid = UUID.fromString(cardDataIdAsString);
		} catch (IllegalArgumentException e) {
			HttpHeaders header = new HttpHeaders();
			header.add("failureReason", "UUID could not be parsed");
			return new ResponseEntity<CardInstance>(header, HttpStatus.BAD_REQUEST);
		}

		CardInstance cardInstance = new CardInstance(UUID.randomUUID(), uuid, identityManager.getPublicKey());
		cardInstanceStorage.put(cardInstance.id, cardInstance);

		return new ResponseEntity<CardInstance>(cardInstance, HttpStatus.OK);
	}

	@RequestMapping(value = "/{idAsString}", method = RequestMethod.GET)
	public @ResponseBody ResponseEntity<CardInstance> get(@PathVariable String idAsString) {
		UUID uuid;
		try {
			uuid = UUID.fromString(idAsString);
		} catch (IllegalArgumentException e) {
			HttpHeaders headers = new HttpHeaders();
			headers.add("failureReason", "UUID could not be parsed");
			ResponseEntity<CardInstance> responseEntity = new ResponseEntity<CardInstance>(headers,
					HttpStatus.BAD_REQUEST);
			return responseEntity;
		}
		Optional<CardInstance> cardInstance = cardInstanceStorage.get(uuid);
		if (!cardInstance.isPresent()) {
			HttpHeaders headers = new HttpHeaders();
			headers.add("failureReason", "Card instance not found");
			ResponseEntity<CardInstance> responseEntity = new ResponseEntity<CardInstance>(headers,
					HttpStatus.BAD_REQUEST);
			return responseEntity;
		}

		return new ResponseEntity<CardInstance>(cardInstance.get(), HttpStatus.OK);
	}

	@RequestMapping(value = "/methods", method = RequestMethod.GET)
	public @ResponseBody List<String> methods() {
		return CommonlyUsedMethods.methodsWithRequestMapping(this.getClass());
	}
}
