package info.krumholz.tcgservice.interfaces.rest;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import info.krumholz.dvs.DistributedValueStore;
import info.krumholz.dvs.Id;

@Controller
@RequestMapping("/database")
public class DistributedValueStoreController {

	@Autowired
	private DistributedValueStore dvs;

	@RequestMapping(value = "/{idAsString}", method = RequestMethod.DELETE)
	public @ResponseBody ResponseEntity<String> get(@PathVariable String idAsString) {
		try {
			UUID uuid = UUID.fromString(idAsString);
			Id id = dvs.storeUUID(uuid);
			dvs.deleteValue(id);
			return ResponseEntity.ok("success");
		} catch (Exception e) {
			return ResponseEntity.badRequest().body("id not found");
		}
	}
}
