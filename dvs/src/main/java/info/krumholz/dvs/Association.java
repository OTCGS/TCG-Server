package info.krumholz.dvs;

public class Association {

	public final Id from;
	public final Id to;
	public final int revision;

	Association(Id from, Id to, int revision) {
		this.from = from;
		this.to = to;
		this.revision = revision;
	}
}
