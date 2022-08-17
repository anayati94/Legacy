package com.legacy.server.external;

/**
 * The definition wrapper for fish
 */
public class ObjectFishDef {

    /**
     * How much experience this fish should give
     */
    public int exp;
    /**
     * The id of the fish
     */
    public int fishId;
    /**
     * The fishing level required to fish
     */
    public int requiredLevel;

	public ObjectFishDef(int fishId, int requiredLevel, int exp) {
		this.fishId = fishId;
		this.requiredLevel = requiredLevel;
		this.exp = exp;
	}

	public int getExp() {
        return exp;
    }

    public int getId() {
        return fishId;
    }

    public int getReqLevel() {
        return requiredLevel;
    }

}
