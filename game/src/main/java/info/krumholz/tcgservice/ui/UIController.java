package info.krumholz.tcgservice.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import info.krumholz.tcgservice.data.CardData;
import info.krumholz.tcgservice.data.CardInstance;
import info.krumholz.tcgservice.data.Image;
import info.krumholz.tcgservice.data.Ruleset;
import info.krumholz.tcgservice.storage.CardDataStorage;
import info.krumholz.tcgservice.storage.SettingsStorage;
import info.krumholz.tcgservice.storage.UUIDPojoStore;
import info.krumholz.tcgservice.utils.Result;

@Controller
@ControllerAdvice
@RequestMapping("/")
public class UIController {

	@Autowired
	private UUIDPojoStore<Ruleset> rulesetStore;

	@Autowired
	private CardDataStorage cardDataStorage;

	@Autowired
	private UUIDPojoStore<CardInstance> cardInstanceStorage;

	@Autowired
	private UUIDPojoStore<Image> imageStore;

	@Autowired
	private SettingsStorage settingsManager;

	@ModelAttribute("title")
	private String serverName() {
		return settingsManager.getSettings().serverName;
	}

	@ModelAttribute("serverIconLink")
	private String serverIconLink() {
		return "/images/" + settingsManager.getSettings().serverIconId.toString() + "/thumb";
	}

	@RequestMapping(value = "uploadImages", method = RequestMethod.GET)
	public String uploadImages(Model model) {
		return "create/image";
	}

	@RequestMapping(method = RequestMethod.GET)
	public String root(Model model) {
		return "welcome";
	}

	@RequestMapping(value = "edit/card", method = RequestMethod.GET)
	public String editCard(@RequestParam(value = "uuid", required = false) String idAsString, Model model) {
		model.addAttribute("uuid", idAsString);
		return "edit/card";
	}

	@RequestMapping(value = "settings", method = RequestMethod.GET)
	public String settings(Model model) {
		Set<UUID> list = imageStore.getKeys();
		model.addAttribute("imageIds", list);
		return "settings";
	}

	@RequestMapping(value = "view/cards", method = RequestMethod.GET)
	public String viewCards(Model model) {
		Set<CardData> cardDataList = cardDataStorage.list();
		model.addAttribute("cardDataList", new ArrayList<CardData>(cardDataList));
		return "view/cards";
	}

	@RequestMapping(value = "view/images", method = RequestMethod.GET)
	public String viewImages(Model model) {
		Set<UUID> imageIds = imageStore.getKeys();
		model.addAttribute("imageIdList", new ArrayList<UUID>(imageIds));
		return "view/images";
	}

	@RequestMapping(value = "edit/ruleset", method = RequestMethod.GET)
	public String editRuleset(@RequestParam(value = "uuid") String idAsString, Model model) {
		UUID uuid;
		try {
			uuid = UUID.fromString(idAsString);
		} catch (IllegalArgumentException e) {
			HttpHeaders headers = new HttpHeaders();
			headers.add("failureReason", "UUID could not be parsed");
			return "error";
		}
		Optional<Ruleset> rulesetQuery = rulesetStore.get(uuid);
		if (!rulesetQuery.isPresent()) {
			HttpHeaders headers = new HttpHeaders();
			headers.add("failureReason", "Ruleset does not exist");
			return "error";

		}
		Ruleset ruleset = rulesetQuery.get();
		model.addAttribute("uuid", ruleset.id);
		model.addAttribute("name", ruleset.name);
		model.addAttribute("text", ruleset.text);
		return "edit/ruleset";
	}

	@RequestMapping(value = "view/rulesets", method = RequestMethod.GET)
	public String viewRulesets(Model model) {
		Set<Ruleset> list = rulesetStore.getValues();
		model.addAttribute("rulesetList", new ArrayList<Ruleset>(list));
		return "view/rulesets";
	}

	@RequestMapping(value = "viewCard", method = RequestMethod.GET)
	public String viewCard(@RequestParam(value = "uuid") String uuid, Model model) {
		try {
			UUID.fromString(uuid);
		} catch (IllegalArgumentException e) {
			return "error";
		}
		Result<CardData> cardDataList = cardDataStorage.get(UUID.fromString(uuid));
		if (cardDataList.isFailed()) {
			return "error";
		}
		model.addAttribute("card", cardDataList.get());
		return "view/card";
	}

	@RequestMapping(value = "cardCreator", method = RequestMethod.GET)
	public String cardCreator(Model model) throws IOException {

		Set<UUID> list = imageStore.getKeys();
		if (list.isEmpty()) {
			model.addAttribute("error", "noImages");
			return "create/card";
		}

		ArrayList<String> editionNames = new ArrayList<String>(cardDataStorage.listEditionNames());
		if (editionNames.isEmpty()) {
			model.addAttribute("error", "noEditions");
			return "create/card";
		}

		model.addAttribute("editionNames", editionNames);

		model.addAttribute("imageIds", list);

		return "create/card";
	}

	@RequestMapping(value = "serverSettings", method = RequestMethod.GET)
	public String serverSettings() {
		return "serverSettings";
	}

	@RequestMapping(value = "view/all", method = RequestMethod.GET)
	public String assets(Model model) {
		Set<UUID> images = imageStore.getKeys();
		Set<UUID> cardDatas = cardDataStorage.listUUIDs();
		Set<UUID> cardInstances = cardInstanceStorage.getKeys();
		Set<UUID> rulesets = rulesetStore.getKeys();

		Set<DisplayedAsset> assets = new HashSet<DisplayedAsset>();
		for (UUID id : images) {
			assets.add(new DisplayedAsset(id, "image"));
		}
		for (UUID id : cardDatas) {
			assets.add(new DisplayedAsset(id, "card-data"));
		}
		for (UUID id : cardInstances) {
			assets.add(new DisplayedAsset(id, "card-instance"));
		}
		for (UUID id : rulesets) {
			assets.add(new DisplayedAsset(id, "ruleset"));
		}

		model.addAttribute("assetsSelected", "active");
		model.addAttribute("assets", assets);

		return "view/all";
	}

	@RequestMapping(value = "deleteAll", method = RequestMethod.POST)
	public @ResponseBody String deleteAll() {
		// dvs.deleteAll();
		return "success";
	}

	@RequestMapping(value = "createRuleset", method = RequestMethod.GET)
	public String createRuleset() {
		return "create/ruleset";
	}

	@RequestMapping(value = "createRuleset", method = RequestMethod.POST)
	public String createRuleset(@RequestParam(value = "name", required = true) String name,
			@RequestParam(value = "text", required = true) String text, Model model) {
		Ruleset ruleset = new Ruleset(UUID.randomUUID(), name, text, 1);
		rulesetStore.put(ruleset.id, ruleset);
		return "create/ruleset";
	}

}
