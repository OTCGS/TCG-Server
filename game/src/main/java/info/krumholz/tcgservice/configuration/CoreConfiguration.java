package info.krumholz.tcgservice.configuration;

import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import info.krumholz.dvs.DistributedAssociationStore;
import info.krumholz.dvs.DistributedValueStore;
import info.krumholz.tcgservice.data.CardData;
import info.krumholz.tcgservice.data.CardInstance;
import info.krumholz.tcgservice.data.Image;
import info.krumholz.tcgservice.data.Ruleset;
import info.krumholz.tcgservice.data.Settings;
import info.krumholz.tcgservice.data.User;
import info.krumholz.tcgservice.signing.IdentityManager;
import info.krumholz.tcgservice.storage.CardDataStorage;
import info.krumholz.tcgservice.storage.SettingsStorage;
import info.krumholz.tcgservice.storage.UUIDPojoStore;
import info.krumholz.tcgservice.transactions.TransactionManager;
import info.krumholz.tcgservice.utils.CommonlyUsedMethods;

@Configuration
public class CoreConfiguration {

	@Bean
	public UUIDPojoStore<Settings> settingsStore() {
		return new UUIDPojoStore<Settings>("info.krumholz.tcgservice.setting", Settings.class);
	}

	@Bean
	public UUIDPojoStore<Ruleset> rulesetStore() {
		return new UUIDPojoStore<Ruleset>("info.krumholz.tcgservice.ruleset", Ruleset.class);
	}

	@Bean
	public UUIDPojoStore<User> userStore() {
		return new UUIDPojoStore<User>("info.krumholz.tcgservice.user", User.class);
	}

	@Bean
	public UUIDPojoStore<Image> imageStorage() {
		return new UUIDPojoStore<Image>("application.prs.tcg.image", Image.class);
	}

	@Bean
	public UUIDPojoStore<CardData> cardDataStore() {
		return new UUIDPojoStore<CardData>("application.prs.tcg.cardData", CardData.class);
	}

	@Bean
	public UUIDPojoStore<CardInstance> cardInstanceStore() {
		return new UUIDPojoStore<CardInstance>("application.prs.tcg.cardInstance", CardInstance.class);
	}

	@Bean
	public IdentityManager identityManager() {
		return new IdentityManager();
	}

	@Bean
	public Base64.Encoder base64Encoder() {
		return Base64.getEncoder();
	}

	@Bean
	public HashFunction md5Hashfunction() {
		return Hashing.md5();
	}

	@Bean
	public Path propertiesFilePath() {
		return CommonlyUsedMethods.getPath("server.properties");
	}

	@Bean
	public SettingsStorage settingsManager() {
		return new SettingsStorage();
	}

	@Bean
	public Path storageDirectory() {
		return CommonlyUsedMethods.getPath("db/dvs");
	}

	@Bean
	public Path associationStorageDirectory() {
		return CommonlyUsedMethods.getPath("db/das");
	}

	@Bean
	public DistributedAssociationStore das() {
		DistributedAssociationStore das = new DistributedAssociationStore();
		return das;
	}

	@Bean
	public DistributedValueStore dvs(Path storageDirectory) {
		DistributedValueStore dvs = new DistributedValueStore();
		SimpleModule simpleModule = new SimpleModule("RSASerialiser");
		simpleModule.addSerializer(new StdSerializer<RSAPublicKey>(RSAPublicKey.class) {

			@Override
			public void serialize(RSAPublicKey value, JsonGenerator jgen, SerializerProvider provider)
					throws IOException, JsonGenerationException {
				jgen.writeBinary(value.getEncoded());
			}
		});
		simpleModule.addDeserializer(RSAPublicKey.class, new StdDeserializer<RSAPublicKey>(RSAPublicKey.class) {

			private static final long serialVersionUID = 1L;

			@Override
			public RSAPublicKey deserialize(JsonParser jp, DeserializationContext ctxt)
					throws IOException, JsonProcessingException {
				try {
					X509EncodedKeySpec spec = new X509EncodedKeySpec(jp.getBinaryValue());
					return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
				} catch (InvalidKeySpecException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return null;
			}
		});
		dvs.registerModule(simpleModule);
		dvs.setStorageDirectory(CommonlyUsedMethods.getPath("db/dvs"));
		return dvs;
	}

	@Bean
	public TransactionManager transactionService() {
		return new TransactionManager();
	}

	@Bean
	public CardDataStorage cardDataStorage() {
		return new CardDataStorage();
	}

}
