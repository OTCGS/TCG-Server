package info.krumholz.dvs;

import org.junit.Before;
import org.junit.Test;

import info.krumholz.dvs.exceptions.StoreException;

public class TestDistributedAssociationStoreConstruction {

	private DistributedAssociationStore das;

	@Before
	public void setUp() throws Exception {
		das = new DistributedAssociationStore();
	}

	@Test(expected = StoreException.class)
	public void callingRetrieveAssociation_withoutInit_throws() {
		das.retrieveAssociation("foo", Id.fromData(new byte[] { 1, 2, 3 }));
	}

	@Test(expected = StoreException.class)
	public void callingStoreAssociation_withoutInit_throws() {
		das.storeAssociation("foo", Id.fromData(new byte[] { 1, 2, 3 }), Id.fromData(new byte[] { 1, 2, 3 }));
	}
}
