package com.legacy.server.external;

/**
 * The definition wrapper for herbs
 */
public class ItemUnIdentHerbDef {

    /**
     * How much experience identifying gives
     */
    public int exp;
    /**
     * The id of the herb this turns into
     */
    private int newId;
    /**
     * Herblaw level required to identify
     */
    public int requiredLvl;

    public ItemUnIdentHerbDef(int level, int newId, int exp) {
		this.requiredLvl = level;
		this.newId = newId;
		this.exp = exp;
	}

	public int getExp() {
        return exp;
    }

    public int getLevelRequired() {
        return requiredLvl;
    }

    public int getNewId() {
        return newId;
    }

}
