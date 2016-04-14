package info.krumholz.tcgservice.interfaces.rest;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import info.krumholz.tcgservice.data.Ruleset;
import info.krumholz.tcgservice.storage.UUIDPojoStore;
import info.krumholz.tcgservice.utils.CommonlyUsedMethods;

@Controller
@RequestMapping("/api/rulesets")
public class RulesetsController {

	@Autowired
	@Qualifier("rulesetStore")
	private UUIDPojoStore<Ruleset> storage;

	@RequestMapping(method = RequestMethod.GET)
	public @ResponseBody Set<UUID> list() {
		return storage.getKeys();
	}

	@RequestMapping(value = "/new", method = RequestMethod.POST, produces = "application/json")
	public @ResponseBody ResponseEntity<Ruleset> createRuleset(
			@RequestParam(value = "name", required = true) String name,
			@RequestParam(value = "text", required = true) String text) {

		Ruleset ruleset = new Ruleset(UUID.randomUUID(), name, text, 1);
		storage.put(ruleset.id, ruleset);
		return new ResponseEntity<Ruleset>(ruleset, HttpStatus.OK);
	}

	@RequestMapping(value = "/{idAsString}", method = RequestMethod.PUT)
	public @ResponseBody ResponseEntity<Ruleset> update(@PathVariable String idAsString,
			@RequestParam(value = "name", required = true) String name,
			@RequestParam(value = "text", required = true) String text) {

		UUID uuid;
		try {
			uuid = UUID.fromString(idAsString);
		} catch (IllegalArgumentException e) {
			HttpHeaders headers = new HttpHeaders();
			headers.add("failureReason", "UUID could not be parsed");
			return new ResponseEntity<Ruleset>(headers, HttpStatus.BAD_REQUEST);
		}
		Optional<Ruleset> rulesetQuery = storage.get(uuid);
		// TODO: error checking
		Ruleset oldRuleset = rulesetQuery.get();
		Ruleset newRuleset = new Ruleset(uuid, name, text, oldRuleset.revision + 1);
		storage.put(newRuleset.id, newRuleset);
		return ResponseEntity.ok(newRuleset);
	}

	@RequestMapping(value = "/{idAsString}", method = RequestMethod.GET)
	public @ResponseBody ResponseEntity<Ruleset> get(@PathVariable String idAsString) {
		UUID uuid;
		try {
			uuid = UUID.fromString(idAsString);
		} catch (IllegalArgumentException e) {
			HttpHeaders headers = new HttpHeaders();
			headers.add("failureReason", "UUID could not be parsed");
			ResponseEntity<Ruleset> responseEntity = new ResponseEntity<Ruleset>(headers, HttpStatus.BAD_REQUEST);
			return responseEntity;
		}
		Optional<Ruleset> result = storage.get(uuid);
		if (!result.isPresent()) {
			return ResponseEntity.badRequest().body(null);
		}
		return ResponseEntity.ok(result.get());
	}

	@RequestMapping(value = "/methods", method = RequestMethod.GET)
	public @ResponseBody List<String> methods() {
		return CommonlyUsedMethods.methodsWithRequestMapping(this.getClass());
	}
}
