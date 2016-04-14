package info.krumholz.tcgservice.interfaces.soap;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import com.google.common.io.BaseEncoding;

import info.krumholz.tcgservice.BroadcastMessageRequest;
import info.krumholz.tcgservice.BroadcastMessageResponse;
import info.krumholz.tcgservice.CardData.Values;
import info.krumholz.tcgservice.CardData.Values.KeyValue;
import info.krumholz.tcgservice.CreateBoosterRequest;
import info.krumholz.tcgservice.CreateBoosterResponse;
import info.krumholz.tcgservice.CreateBoosterResponse.Transactions;
import info.krumholz.tcgservice.CreateCardInstanceRequest;
import info.krumholz.tcgservice.CreateCardInstanceResponse;
import info.krumholz.tcgservice.GetCardDataRequest;
import info.krumholz.tcgservice.GetCardDataResponse;
import info.krumholz.tcgservice.GetCardInstanceRequest;
import info.krumholz.tcgservice.GetCardInstanceResponse;
import info.krumholz.tcgservice.GetHeadsRequest;
import info.krumholz.tcgservice.GetHeadsResponse;
import info.krumholz.tcgservice.GetImageDataRequest;
import info.krumholz.tcgservice.GetImageDataResponse;
import info.krumholz.tcgservice.GetRelayMessageRequest;
import info.krumholz.tcgservice.GetRelayMessageResponse;
import info.krumholz.tcgservice.GetRuleSetRequest;
import info.krumholz.tcgservice.GetRuleSetResponse;
import info.krumholz.tcgservice.GetTransactionRequest;
import info.krumholz.tcgservice.GetTransactionResponse;
import info.krumholz.tcgservice.ImageData;
import info.krumholz.tcgservice.Key;
import info.krumholz.tcgservice.ListCardDataRequest;
import info.krumholz.tcgservice.ListCardDataResponse;
import info.krumholz.tcgservice.ListCardDataResponse.Uuids;
import info.krumholz.tcgservice.ListRuleSetRequest;
import info.krumholz.tcgservice.ListRuleSetResponse;
import info.krumholz.tcgservice.RegisterRequest;
import info.krumholz.tcgservice.RegisterResponse;
import info.krumholz.tcgservice.RelayMessage;
import info.krumholz.tcgservice.RelayMessageRequest;
import info.krumholz.tcgservice.RelayMessageResponse;
import info.krumholz.tcgservice.ServerIdentity;
import info.krumholz.tcgservice.ServerIdentityRequest;
import info.krumholz.tcgservice.ServerIdentityResponse;
import info.krumholz.tcgservice.SubmitTransactionsRequest;
import info.krumholz.tcgservice.SubmitTransactionsResponse;
import info.krumholz.tcgservice.data.CardData;
import info.krumholz.tcgservice.data.CardInstance;
import info.krumholz.tcgservice.data.Image;
import info.krumholz.tcgservice.data.Ruleset;
import info.krumholz.tcgservice.data.Transaction;
import info.krumholz.tcgservice.signing.IdentityManager;
import info.krumholz.tcgservice.signing.Signed;
import info.krumholz.tcgservice.storage.CardDataStorage;
import info.krumholz.tcgservice.storage.SettingsStorage;
import info.krumholz.tcgservice.storage.UUIDPojoStore;
import info.krumholz.tcgservice.transactions.TransactionManager;
import info.krumholz.tcgservice.utils.Result;

@org.springframework.stereotype.Controller
@RequestMapping("/api/relays")
@Endpoint
public class Controller {

	private static final String NAMESPACE_URI = "http://krumholz.info/tcgservice";
	private static final Logger logger = LoggerFactory.getLogger(Controller.class);

	@Autowired
	private UUIDPojoStore<Image> imageStore;

	@Autowired
	private UUIDPojoStore<CardInstance> cardInstanceStorage;

	@Autowired
	private CardDataStorage cardDataStorage;

	@Autowired
	private IdentityManager identityService;

	@Autowired
	private SettingsStorage settingsService;

	@Autowired
	private TransactionManager transactionManager;

	@Autowired
	private UUIDPojoStore<Ruleset> rulesetStore;

	private Map<Key, List<RelayMessage>> messages = new ConcurrentSkipListMap<Key, List<RelayMessage>>(
			new Comparator<Key>() {

				@Override
				public int compare(info.krumholz.tcgservice.Key o1, info.krumholz.tcgservice.Key o2) {
					return new BigInteger(o1.getModulus()).compareTo(new BigInteger(o2.getModulus()));
				}
			});

	private ConcurrentLinkedQueue<RelayMessage> relayedMessages = new ConcurrentLinkedQueue<RelayMessage>();

	private Set<Key> users = new ConcurrentSkipListSet<Key>(new Comparator<Key>() {

		@Override
		public int compare(info.krumholz.tcgservice.Key o1, info.krumholz.tcgservice.Key o2) {
			return new BigInteger(o1.getModulus()).compareTo(new BigInteger(o2.getModulus()));
		}
	});

	@RequestMapping(value = "", method = RequestMethod.GET)
	public @ResponseBody String monitorRelay() {
		StringBuilder result = new StringBuilder();
		for (RelayMessage message : relayedMessages) {
			result.append("From: ");
			result.append(BaseEncoding.base16().encode(message.getFrom().getModulus()).substring(0, 8));
			result.append("</br>To: ");
			result.append(BaseEncoding.base16().encode(message.getTo().getModulus()).substring(0, 8));
			result.append("</br>Message: ");
			String data = new String(message.getData(), Charset.forName("UTF-8"));
			result.append(data.substring(0, Integer.min(20, data.length() - 1)));
			result.append("</br>");
		}
		relayedMessages.clear();
		return result.toString();
	}

	@PayloadRoot(namespace = NAMESPACE_URI, localPart = "broadcastMessageRequest")
	@ResponsePayload
	public BroadcastMessageResponse broadcastMessage(@RequestPayload BroadcastMessageRequest request) {
		for (Key user : users) {
			RelayMessage msg = new RelayMessage();
			msg.setFrom(request.getRelayMessage().getFrom());
			msg.setData(request.getRelayMessage().getData());
			msg.setTo(user);
			List<RelayMessage> list = messages.get(msg.getTo());
			if (list == null) {
				messages.put(msg.getTo(), new ArrayList<RelayMessage>());
			}
			list = messages.get(msg.getTo());
			list.add(request.getRelayMessage());
		}

		BroadcastMessageResponse response = new BroadcastMessageResponse();
		return response;
	}

	@PayloadRoot(namespace = NAMESPACE_URI, localPart = "relayMessageRequest")
	@ResponsePayload
	public RelayMessageResponse relayMessage(@RequestPayload RelayMessageRequest request) {
		Key to = request.getRelayMessage().getTo();
		List<RelayMessage> list = messages.get(to);
		if (list == null) {
			messages.put(to, new ArrayList<RelayMessage>());
		}
		list = messages.get(to);
		list.add(request.getRelayMessage());
		relayedMessages.add(request.getRelayMessage());
		if (relayedMessages.size() > 100) {
			relayedMessages.clear();
			relayedMessages.add(new RelayMessage());
		}
		RelayMessageResponse response = new RelayMessageResponse();
		return response;
	}

	@PayloadRoot(namespace = NAMESPACE_URI, localPart = "registerRequest")
	@ResponsePayload
	public RegisterResponse register(@RequestPayload RegisterRequest request) {
		Key user = request.getUser();
		users.add(user);
		RegisterResponse response = new RegisterResponse();
		for (Key key : users) {
			response.getUsers().add(key);
		}
		return response;
	}

	@PayloadRoot(namespace = NAMESPACE_URI, localPart = "getRelayMessageRequest")
	@ResponsePayload
	public GetRelayMessageResponse getRelayMessage(@RequestPayload GetRelayMessageRequest request) {
		List<RelayMessage> relayMessages = messages.get(request.getFor());

		GetRelayMessageResponse response = new GetRelayMessageResponse();
		if (relayMessages == null) {
			relayMessages = new ArrayList<>();
		}
		Iterator<RelayMessage> iterator = relayMessages.iterator();
		while (iterator.hasNext()) {
			RelayMessage message = iterator.next();
			response.getRelayMessage().add(message);
			iterator.remove();
		}
		return response;
	}

	@PayloadRoot(namespace = NAMESPACE_URI, localPart = "ServerIdentityRequest")
	@ResponsePayload
	public ServerIdentityResponse serverIdentity(@RequestPayload ServerIdentityRequest request) {
		UUID imageId = settingsService.getSettings().serverIconId;
		ServerIdentity serverIdentity = new ServerIdentity();
		serverIdentity.setIcon(imageId != null ? imageId.toString() : null);
		Key key = Translator.key(identityService.getPublicKey());
		serverIdentity.setKey(key);
		serverIdentity.setName(settingsService.getSettings().serverName);
		serverIdentity.setRevision(settingsService.getRevision());
		serverIdentity.setUri(settingsService.getSettings().serverUrl);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DataOutputStream writer = new DataOutputStream(out);
		Serializer.write(writer, serverIdentity);
		Optional<byte[]> signData = identityService.signData(out.toByteArray());
		if (!signData.isPresent()) {
			throw new WebServiceException("Server is unable to sign your requested data. Please try again later.");
		}
		serverIdentity.setSignature(signData.get());

		ServerIdentityResponse response = new ServerIdentityResponse();
		response.setIdentity(serverIdentity);
		return response;
	}

	@PayloadRoot(namespace = NAMESPACE_URI, localPart = "submitTransactionsRequest")
	@ResponsePayload
	public SubmitTransactionsResponse submitTransactions(@RequestPayload SubmitTransactionsRequest request) {
		if (request.getTransaction() == null || request.getTransaction().size() == 0) {
			throw new WebServiceException("You submited an empty or null transaction list");
		}

		List<Signed<Transaction>> translatedTransactions = new ArrayList<>();
		for (info.krumholz.tcgservice.Transaction requestTransaction : request.getTransaction()) {
			translatedTransactions.add(Translator.transaction(requestTransaction));
		}
		boolean success = transactionManager.submitTransactions(translatedTransactions);

		SubmitTransactionsResponse response = new SubmitTransactionsResponse();
		response.setSuccess(success);
		response.setErrorMessage("Tried to submit illegal transactions");
		return response;
	}

	@PayloadRoot(namespace = NAMESPACE_URI, localPart = "getTransactionRequest")
	@ResponsePayload
	public GetTransactionResponse getTransaction(@RequestPayload GetTransactionRequest request) {
		byte[] hash = request.getHash();
		Signed<Transaction> transaction = transactionManager.getTransaction(hash);
		GetTransactionResponse response = new GetTransactionResponse();
		if (transaction != null) {
			response.setTransaction(Translator.transaction(transaction));
		}
		return response;
	}

	@PayloadRoot(namespace = NAMESPACE_URI, localPart = "createCardInstanceRequest")
	@ResponsePayload
	public CreateCardInstanceResponse createCardInstance(@RequestPayload CreateCardInstanceRequest request) {
		throw new WebServiceException("creating card instances not implemented");
	}

	@PayloadRoot(namespace = NAMESPACE_URI, localPart = "getImageDataRequest")
	@ResponsePayload
	public GetImageDataResponse getImageData(@RequestPayload GetImageDataRequest request) {

		UUID imageId = UUID.fromString(request.getImageId());
		Optional<Image> imageQuery = imageStore.get(imageId);

		if (!imageQuery.isPresent()) {
			throw new WebServiceException("Could not find image with id " + request.getImageId());
		}
		Image image = imageQuery.get();

		ImageData imageData = new ImageData();
		Key serverKey = Translator.key(identityService.getPublicKey());
		imageData.setCreator(serverKey);
		imageData.setId(imageId.toString());
		imageData.setImage(image.data);

		// signature id, data, creator
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DataOutputStream writer = new DataOutputStream(out);
		try {
			writer.writeLong(imageId.getMostSignificantBits());
			writer.writeLong(imageId.getLeastSignificantBits());
			writer.write(image.data);
			writer.write(serverKey.getModulus());
			writer.write(serverKey.getExponent());
			writer.flush();
		} catch (IOException e) {
			throw new WebServiceException("Could not write image data\n" + e.getMessage());
		}

		Optional<byte[]> signature = identityService.signData(out.toByteArray());
		if (!signature.isPresent()) {
			throw new WebServiceException("Server is unable to sign the requested data. Please try again later.");
		}
		imageData.setSignature(signature.get());

		GetImageDataResponse response = new GetImageDataResponse();
		response.setImageData(imageData);

		return response;
	}

	@PayloadRoot(namespace = NAMESPACE_URI, localPart = "getCardDataRequest")
	@ResponsePayload
	public GetCardDataResponse getCardData(@RequestPayload GetCardDataRequest request) {

		final UUID uuid = getUUIDFromString(request.getCardDataId());

		Result<CardData> cardDataQuery = cardDataStorage.get(uuid);
		if (cardDataQuery.isFailed()) {
			throw new WebServiceException("Card with uuid %s not found.", uuid);
		}
		CardData cardData = cardDataQuery.get();

		info.krumholz.tcgservice.CardData cardDataResponse = new info.krumholz.tcgservice.CardData();

		cardDataResponse.setCardRevision(cardData.revision);

		// TODO: save public key to identify users who retrieved data
		Key key = Translator.key(identityService.getPublicKey());
		cardDataResponse.setCreator(key);

		cardDataResponse.setEdition(cardData.edition);

		cardDataResponse.setId(cardData.id.toString());

		cardDataResponse.setImageId(cardData.imageId.toString());

		cardDataResponse.setName(cardData.name);

		Stream<Entry<String, String>> sorted = cardData.values.entrySet().stream().sorted((e1, e2) -> {
			return e1.getKey().compareTo(e2.getKey());
		});
		Values values = new Values();
		sorted.forEach((e) -> {
			KeyValue keyValue = new KeyValue();
			keyValue.setKey(e.getKey());
			keyValue.setType("String");
			keyValue.setValue(e.getValue());
			values.getKeyValue().add(keyValue);
		});
		cardDataResponse.setValues(values);

		Result<byte[]> signatureQuery = signCardData(cardData);
		if (signatureQuery.isFailed()) {
			throw new WebServiceException("Failed to sign card data with\n%s", signatureQuery.getFailureReason());
		}
		cardDataResponse.setSignature(signatureQuery.get());

		GetCardDataResponse response = new GetCardDataResponse();
		response.setCardData(cardDataResponse);

		return response;
	}

	private UUID getUUIDFromString(String uuidAsString) {
		final UUID uuid;
		try {
			uuid = UUID.fromString(uuidAsString);
		} catch (Exception e) {
			throw new WebServiceException("Illegal uuid %s.", uuidAsString);
		}
		return uuid;
	}

	private Result<byte[]> signCardInstance(CardInstance cardInstance) {
		ByteArrayOutputStream signatureDataStream = new ByteArrayOutputStream();
		DataOutputStream writer = new DataOutputStream(signatureDataStream);
		try {
			writer.writeLong(cardInstance.id.getMostSignificantBits());
			writer.writeLong(cardInstance.id.getLeastSignificantBits());
			writer.writeLong(cardInstance.cardDataId.getMostSignificantBits());
			writer.writeLong(cardInstance.cardDataId.getLeastSignificantBits());
			writer.write(cardInstance.creator.getModulus().toByteArray());
			writer.write(cardInstance.creator.getPublicExponent().toByteArray());
		} catch (IOException e) {
			return Result.failure("Failed to create signature");
		}
		Optional<byte[]> result = identityService.signData(signatureDataStream.toByteArray());
		if (!result.isPresent()) {
			throw new WebServiceException("Server failed to sign requested data. Please try again later.");
		}
		return Result.success(result.get());
	}

	private Result<byte[]> signRuleSet(Ruleset ruleSet) {
		ByteArrayOutputStream signatureDataStream = new ByteArrayOutputStream();
		DataOutputStream writer = new DataOutputStream(signatureDataStream);
		try {
			// id
			writer.writeLong(ruleSet.id.getMostSignificantBits());
			writer.writeLong(ruleSet.id.getLeastSignificantBits());

			// creator
			RSAPublicKey publicKey = identityService.getPublicKey();
			writer.write(publicKey.getModulus().toByteArray());
			writer.write(publicKey.getPublicExponent().toByteArray());

			// name
			writer.write(ruleSet.name.getBytes(Charset.forName("UTF-8")));

			// revision
			writer.writeInt(ruleSet.revision);

			// text
			writer.write(ruleSet.text.getBytes(Charset.forName("UTF-8")));

			// TODO: mandatory keys
			// values
			/*
			 * Stream<Entry<String, String>> sorted = cardData.values.entrySet()
			 * .stream().sorted((entry1, entry2) -> { return
			 * entry1.getKey().compareTo(entry2.getKey()); }); Object[] entries
			 * = sorted.toArray(); for (Object entry : entries) {
			 * 
			 * @SuppressWarnings("unchecked") Entry<String, String> realEntry =
			 * (Entry<String, String>) entry;
			 * writer.write(realEntry.getKey().getBytes(
			 * Charset.forName("UTF-8")));
			 * writer.write(realEntry.getValue().getBytes(
			 * Charset.forName("UTF-8"))); }
			 */
		} catch (IOException e) {
			return Result.failure("Failed to create signature with\n%s", e.getMessage());
		}
		Optional<byte[]> signData = identityService.signData(signatureDataStream.toByteArray());
		if (!signData.isPresent()) {
			return Result.failure("Failed to create signature.");
		}
		return Result.success(signData.get());
	}

	private Result<byte[]> signCardData(CardData cardData) {
		ByteArrayOutputStream signatureDataStream = new ByteArrayOutputStream();
		DataOutputStream writer = new DataOutputStream(signatureDataStream);
		try {
			// id
			writer.writeLong(cardData.id.getMostSignificantBits());
			writer.writeLong(cardData.id.getLeastSignificantBits());

			// creator
			RSAPublicKey publicKey = identityService.getPublicKey();
			writer.write(publicKey.getModulus().toByteArray());
			writer.write(publicKey.getPublicExponent().toByteArray());

			// edition name
			writer.write(cardData.edition.getBytes(Charset.forName("UTF-8")));

			// card revision
			writer.writeInt(cardData.revision);

			// imageId
			writer.writeLong(cardData.imageId.getMostSignificantBits());
			writer.writeLong(cardData.imageId.getLeastSignificantBits());

			// card name
			writer.write(cardData.name.getBytes(Charset.forName("UTF-8")));

			// values
			Stream<Entry<String, String>> sorted = cardData.values.entrySet().stream().sorted((entry1, entry2) -> {
				return entry1.getKey().compareTo(entry2.getKey());
			});
			sorted.forEachOrdered((entry) -> {
				try {
					writer.write(entry.getKey().getBytes(Charset.forName("UTF-8")));
					writer.write(entry.getValue().getBytes(Charset.forName("UTF-8")));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		} catch (Exception e) {
			return Result.failure("Failed to create signature with\n%s", e.getMessage());
		}
		Optional<byte[]> signData = identityService.signData(signatureDataStream.toByteArray());
		if (!signData.isPresent()) {
			return Result.failure("Failed to create signature.");
		}
		return Result.success(signData.get());
	}

	@PayloadRoot(namespace = NAMESPACE_URI, localPart = "getCardInstanceRequest")
	@ResponsePayload
	public GetCardInstanceResponse getCardInstance(@RequestPayload GetCardInstanceRequest request) {

		final UUID uuid = getUUIDFromString(request.getCardInstanceId());
		Optional<CardInstance> cardInstanceQuery = cardInstanceStorage.get(uuid);

		if (!cardInstanceQuery.isPresent()) {
			throw new WebServiceException("Couldn't find card instance");
		}
		CardInstance cardInstanceStored = cardInstanceQuery.get();

		info.krumholz.tcgservice.CardInstance cardInstance = new info.krumholz.tcgservice.CardInstance();
		cardInstance.setCardDataId(cardInstanceStored.cardDataId.toString());

		Key key = new Key();
		RSAPublicKey rsaKey = identityService.getPublicKey();
		key.setExponent(rsaKey.getPublicExponent().toByteArray());
		key.setModulus(rsaKey.getModulus().toByteArray());

		cardInstance.setCreator(key);
		cardInstance.setId(cardInstanceStored.id.toString());
		Result<byte[]> signCardInstance = signCardInstance(cardInstanceStored);
		if (signCardInstance.isFailed()) {
			throw new WebServiceException(signCardInstance.getFailureReason());
		}
		cardInstance.setSignature(signCardInstance.get());

		GetCardInstanceResponse response = new GetCardInstanceResponse();
		response.setCardInstance(cardInstance);

		return response;
	}

	@PayloadRoot(namespace = NAMESPACE_URI, localPart = "getHeadsRequest")
	@ResponsePayload
	public GetHeadsResponse getHeads(@RequestPayload GetHeadsRequest request) {
		GetHeadsResponse response = new GetHeadsResponse();
		for (Signed<Transaction> transaction : transactionManager.getHeads()) {
			response.getTransactions().add(Translator.transaction(transaction));
		}
		return response;
	}

	@PayloadRoot(namespace = NAMESPACE_URI, localPart = "listRuleSetRequest")
	@ResponsePayload
	public ListRuleSetResponse listRuleSet(@RequestPayload ListRuleSetRequest request) {
		Set<UUID> storedUuids = rulesetStore.getKeys();
		ListRuleSetResponse response = new ListRuleSetResponse();
		ListRuleSetResponse.Uuids uuids = new ListRuleSetResponse.Uuids();
		for (UUID uuid : storedUuids) {
			uuids.getUuid().add(uuid.toString());
		}
		response.setUuids(uuids);
		return response;
	}

	@PayloadRoot(namespace = NAMESPACE_URI, localPart = "getRuleSetRequest")
	@ResponsePayload
	public GetRuleSetResponse getRuleset(@RequestPayload GetRuleSetRequest request) {
		String idAsString = request.getId();
		final UUID uuid;
		try {
			uuid = UUID.fromString(idAsString);
		} catch (IllegalArgumentException e) {
			throw new WebServiceException(idAsString + " is not a legal uuid.");
		}
		Optional<Ruleset> result = rulesetStore.get(uuid);
		if (!result.isPresent()) {
			throw new WebServiceException("Failed to retrieve ruleset");
		}
		Ruleset storedRuleset = result.get();

		info.krumholz.tcgservice.RuleSet ruleSet = new info.krumholz.tcgservice.RuleSet();
		ruleSet.setId(idAsString);
		ruleSet.setName(storedRuleset.name);
		ruleSet.setRevision(storedRuleset.revision);
		ruleSet.setScript(storedRuleset.text);
		ruleSet.setCreator(Translator.key(identityService.getPublicKey()));
		// TODO: mandatory keys
		ruleSet.setMandatoryKeys(new info.krumholz.tcgservice.RuleSet.MandatoryKeys());
		Result<byte[]> signRuleSet = signRuleSet(storedRuleset);
		if (signRuleSet.isFailed()) {
			throw new WebServiceException(signRuleSet.getFailureReason());
		}
		ruleSet.setSignature(signRuleSet.get());

		GetRuleSetResponse response = new GetRuleSetResponse();
		response.setRuleSet(ruleSet);
		return response;
	}

	@PayloadRoot(namespace = NAMESPACE_URI, localPart = "createBoosterRequest")
	@ResponsePayload
	public CreateBoosterResponse booster(@RequestPayload CreateBoosterRequest request) {
		Set<UUID> listCardDataUuids = cardDataStorage.listUUIDs();

		int boosterSize = settingsService.getSettings().boosterSize;
		if (listCardDataUuids.size() == 0) {
			throw new WebServiceException("There is not a single card on the server");
		}

		Transactions transactions = new Transactions();
		Random r = new SecureRandom();
		for (int i = 0; i < boosterSize; i += 1) {
			// randomly select a card
			int selectCardDataId = r.nextInt(listCardDataUuids.size());
			UUID cardDataId = null;
			Iterator<UUID> iterator = listCardDataUuids.iterator();
			while (selectCardDataId >= 0) {
				cardDataId = iterator.next();
				selectCardDataId -= 1;
			}

			CardInstance newCardInstance = new CardInstance(UUID.randomUUID(), cardDataId,
					identityService.getPublicKey());
			cardInstanceStorage.put(newCardInstance.id, newCardInstance);

			RSAPublicKey clientKey = Translator.key(request.getOwnerKey()).get();
			Optional<Signed<Transaction>> t = transactionManager.createValue(newCardInstance.id, clientKey);
			if (!t.isPresent()) {
				throw new WebServiceException("No new card could be created");
			}
			transactions.getTransaction().add(Translator.transaction(t.get()));
		}

		CreateBoosterResponse response = new CreateBoosterResponse();
		response.setTransactions(transactions);
		return response;
	}

	@PayloadRoot(namespace = NAMESPACE_URI, localPart = "listCardDataRequest")
	@ResponsePayload
	public ListCardDataResponse listCardData(@RequestPayload ListCardDataRequest request) {
		logger.debug("Received listCardDataRequest");
		Set<UUID> cardDataUuids = cardDataStorage.listUUIDs();

		Uuids uuids = new Uuids();
		for (UUID id : cardDataUuids) {
			uuids.getUuid().add(id.toString());
		}

		ListCardDataResponse response = new ListCardDataResponse();
		response.setUuids(uuids);
		return response;

	}

}
