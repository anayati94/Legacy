package com.legacy.server.external;

/**
 * The definition wrapper for rocks
 */
public class ObjectMiningDef {

    /**
     * How much experience identifying gives
     */
    public int exp;
    /**
     * The id of the ore this turns into
     */
    private int oreId;
    /**
     * Herblaw level required to identify
     */
    public int requiredLvl;
    /**
     * How long the rock takes to respawn afterwards
     */
    public int respawnTime;

    public ObjectMiningDef(int oreId, int exp, int level, int respawnTime) {
		this.oreId = oreId;
		this.requiredLvl = level;
		this.exp = exp;
		this.respawnTime = respawnTime;
	}

	public int getExp() {
        return exp;
    }

    public int getOreId() {
        return oreId;
    }

    public int getReqLevel() {
        return requiredLvl;
    }

    public int getRespawnTime() {
        return respawnTime;
    }

}
