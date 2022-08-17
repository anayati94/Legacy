package com.legacy.server.content.clan;

public class Territory {
	
    private Clan owner;
	
	public void setOwner(Clan clan) {
		this.owner = clan;
	}
	
	public Clan getOwner() {
		return owner;
	}

}
