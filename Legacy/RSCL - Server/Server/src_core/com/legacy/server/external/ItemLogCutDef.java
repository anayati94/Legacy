package com.legacy.server.external;

/**
 * The definition wrapper for items
 */
public class ItemLogCutDef {

    public int longbowExp;
    public int longbowID;
    public int longbowLvl;

    public int shaftAmount;
    public int shaftLvl;
    public int shortbowExp;

    public int shortbowID;
    public int shortbowLvl;

    public ItemLogCutDef(int shaftAmount, int shaftLvl, int shortbowID, int shortbowLvl, int shortbowExp, int longbowID, int longbowLvl, int longbowExp) {
		this.shaftAmount = shaftAmount;
		this.shaftLvl = shaftLvl;
		this.shortbowID = shortbowID;
		this.shortbowLvl = shortbowLvl;
		this.shortbowExp = shortbowExp;
		this.longbowID = longbowID;
		this.longbowLvl = longbowLvl;
		this.longbowExp = longbowExp;
	}

	public int getLongbowExp() {
        return longbowExp;
    }

    public int getLongbowID() {
        return longbowID;
    }

    public int getLongbowLvl() {
        return longbowLvl;
    }

    public int getShaftAmount() {
        return shaftAmount;
    }

    public int getShaftExp() {
        return shaftAmount;
    }

    public int getShaftLvl() {
        return shaftLvl;
    }

    public int getShortbowExp() {
        return shortbowExp;
    }

    public int getShortbowID() {
        return shortbowID;
    }

    public int getShortbowLvl() {
        return shortbowLvl;
    }

}
