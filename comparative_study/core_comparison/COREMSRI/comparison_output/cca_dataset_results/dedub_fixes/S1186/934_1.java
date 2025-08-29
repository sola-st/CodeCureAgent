package amidst.mojangapi.file.json.player;

import amidst.documentation.GsonConstructor;

public class SimplePlayerJson {
	private String id;
	private String name;

	@GsonConstructor
	public SimplePlayerJson() {
		// Default constructor needed for Gson deserialization
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}
}
