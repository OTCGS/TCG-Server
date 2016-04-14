package info.krumholz.tcgservice.storage;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import info.krumholz.tcgservice.data.CardData;
import info.krumholz.tcgservice.data.Image;
import info.krumholz.tcgservice.signing.IdentityManager;
import info.krumholz.tcgservice.utils.Result;

public class CardDataStorage {

	@Autowired
	private IdentityManager identityManager;

	@Autowired
	private UUIDPojoStore<Image> imageStorage;

	@Autowired
	private UUIDPojoStore<CardData> cardDataStore;

	public Set<String> listEditionNames() {
		Set<String> names = new HashSet<String>();
		Set<CardData> cardDatas = cardDataStore.getValues();
		for (CardData cardData : cardDatas) {
			names.add(cardData.edition);
		}
		return names;
	}

	public Result<UUID> create(String name, String edition, UUID cardImageId, Map<String, String> values) {
		if (!isCardNameAllowed(name)) {
			return Result.failure("Card name '%s' not allowed", name);
		}
		if (!isEditionNameAllowed(edition)) {
			return Result.failure("Edition name '%s' not allowed", edition);
		}
		if (values != null) {
			for (Entry<String, String> entry : values.entrySet()) {
				if (!isCardKeyValueAllowed(entry)) {
					return Result.failure("Key/Value['%s/%s'] not allowed", entry.getKey(), entry.getValue());
				}
			}
		}
		if (!isCardImageIdAllowed(cardImageId)) {
			return Result.failure("Image id %s not allowed", cardImageId);
		}

		UUID id = UUID.randomUUID();
		CardData cardData = new CardData(id, identityManager.getPublicKey(), name, edition, 1, cardImageId, values);
		cardDataStore.put(cardData.id, cardData);
		return Result.success(id);
	}

	public Result<CardData> get(UUID uuid) {
		Optional<CardData> cardData = cardDataStore.get(uuid);
		if (!cardData.isPresent()) {
			return Result.failure("Id %s is not a card data uuid", uuid);
		}
		return Result.success(cardData.get());

	}

	public Set<CardData> list() {
		return cardDataStore.getValues();
	}

	public Set<UUID> listUUIDs() {
		return cardDataStore.getKeys();
	}

	public Set<CardData> getAllInEdition(String edition) { // TODO: filter by
															// edition
		return cardDataStore.getValues();
	}

	private boolean isCardImageIdAllowed(UUID cardImageId) {
		Optional<Image> image = imageStorage.get(cardImageId);
		return image.isPresent();
	}

	private boolean isCardKeyValueAllowed(Entry<String, String> entry) {
		return entry.getKey() != null && entry.getValue() != null;
	}

	private boolean isEditionNameAllowed(String editionName) {
		return editionName != null;
	}

	private boolean isCardNameAllowed(String cardName) {
		return cardName != null;
	}

	public Result<CardData> update(UUID uuid, String name, String edition, UUID imageId, Map<String, String> values) {
		if (!isCardNameAllowed(name)) {
			return Result.failure("Card name '%s' not allowed", name);
		}
		if (!isEditionNameAllowed(edition)) {
			return Result.failure("Edition name '%s' not allowed", edition);
		}
		if (values != null) {
			for (Entry<String, String> entry : values.entrySet()) {
				if (!isCardKeyValueAllowed(entry)) {
					return Result.failure("Key/Value['%s/%s'] not allowed", entry.getKey(), entry.getValue());
				}
			}
		}
		if (!isCardImageIdAllowed(imageId)) {
			return Result.failure("Image id %s not allowed", imageId);
		}

		Optional<Integer> revision = cardDataStore.getRevision(uuid);
		if (!revision.isPresent()) {
			return Result.failure("Revision of card data not found");
		}
		CardData cardData = new CardData(uuid, identityManager.getPublicKey(), name, edition, revision.get() + 1,
				imageId, values);
		cardDataStore.put(cardData.id, cardData);

		return Result.success(cardData);
	}

	public Result<CardData> get(UUID uuid, int revision) {
		Optional<CardData> cardData = cardDataStore.get(uuid, revision);
		if (!cardData.isPresent()) {
			return Result.failure("Id %s is not a card data uuid", uuid);
		}
		return Result.success(cardData.get());
	}

}
