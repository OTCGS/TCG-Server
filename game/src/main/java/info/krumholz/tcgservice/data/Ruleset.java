package info.krumholz.tcgservice.data;

import java.util.UUID;

public class Ruleset {

	public UUID id;
	public String name;
	public String text;
	public int revision;
	
	public Ruleset() {}

	public Ruleset(UUID id, String name, String text, int revision) {
		this.id = id;
		this.name = name;
		this.text = text;
		this.revision = revision;
	}

}
