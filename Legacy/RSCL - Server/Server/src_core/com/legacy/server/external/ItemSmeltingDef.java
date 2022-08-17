package com.legacy.server.external;


/**
 * The definition wrapper for items
 */
public class ItemSmeltingDef {

    /**
     * The id of the related bar
     */
    public int barId;
    /**
     * The exp smelting this item gives
     */
    public int exp;
    /**
     * The ores required in addition to this one
     */
    public ReqOreDef[] reqOres;
    /**
     * The level required to smelt this
     */
    public int requiredLvl;

	public ItemSmeltingDef(int exp, int barId, int level, ReqOreDef[] reqOres) {
		this.exp = exp;
		this.barId = barId;
		this.requiredLvl = level;
		this.reqOres = reqOres;
	}

	public int getBarId() {
        return barId;
    }

    public int getExp() {
        return exp;
    }

    public int getReqLevel() {
        return requiredLvl;
    }

    public ReqOreDef[] getReqOres() {
        return reqOres;
    }

}
