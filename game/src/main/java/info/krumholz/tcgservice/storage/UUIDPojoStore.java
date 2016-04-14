package info.krumholz.tcgservice.storage;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import info.krumholz.dvs.Association;
import info.krumholz.dvs.DistributedAssociationStore;
import info.krumholz.dvs.DistributedValueStore;
import info.krumholz.dvs.Id;

public class UUIDPojoStore<VALUE> {

	private final String pointerName;
	private final Class<VALUE> valueClass;

	@Autowired
	private DistributedValueStore dvs;

	@Autowired
	private DistributedAssociationStore das;

	public UUIDPojoStore(String pointerName, Class<VALUE> valueClass) {
		this.pointerName = pointerName;
		this.valueClass = valueClass;
	}

	public void put(UUID key, VALUE value) {
		Id id1 = dvs.storeUUID(key);
		Id id2 = dvs.storePojo(value);
		das.storeAssociation(pointerName, id1, id2);
	}

	public Optional<VALUE> get(UUID key, int revision) {
		Id id = dvs.storeUUID(key);
		Optional<Association> associationQuery = das.retrieveAssociation(pointerName, id, revision);
		Optional<VALUE> result = associationQuery.flatMap((association) -> {
			Optional<VALUE> retrievePojo = dvs.retrievePojo(association.to, valueClass);
			return retrievePojo.map((pojo) -> pojo);
		});
		return result;
	}

	public Optional<VALUE> get(UUID key) {
		Id id = dvs.storeUUID(key);
		Optional<Association> associationQuery = das.retrieveAssociation(pointerName, id);
		Optional<VALUE> result = associationQuery.flatMap((association) -> {
			Optional<VALUE> retrievePojo = dvs.retrievePojo(association.to, valueClass);
			return retrievePojo.map((pojo) -> pojo);
		});
		return result;
	}

	public Set<VALUE> getValues() {
		Set<Id> keys = das.getIdsWith_ALL_Associations(pointerName);
		Set<VALUE> values = new HashSet<VALUE>();
		for (Id key : keys) {
			Optional<UUID> value = dvs.retrieveUUID(key);
			value.ifPresent((v) -> get(v).ifPresent((v2) -> values.add(v2)));
		}
		return values;
	}

	public Set<UUID> getKeys() {
		// TODO: deleting values will lead to dangling associations
		Set<Id> keys = das.getIdsWith_ALL_Associations(pointerName);
		Set<UUID> values = new HashSet<UUID>();
		for (Id id : keys) {
			final Optional<UUID> value;
			value = dvs.retrieveUUID(id);
			if (value.isPresent()) {
				values.add(value.get());
			}
		}
		return values;
	}

	public Optional<Integer> getRevision(UUID key) {
		Optional<Association> associationQuery = das.retrieveAssociation(pointerName, dvs.storeUUID(key));
		return associationQuery.map((associaton) -> associaton.revision);
	}

	void setDas(DistributedAssociationStore das) {
		this.das = das;
	}

	void setDvs(DistributedValueStore dvs) {
		this.dvs = dvs;
	}

}
