package com.legacy.server.external;

public class ItemCraftingDef {
    /**
     * The exp given
     */
    public int exp;
    /**
     * The ID of the item produced
     */
    public int itemID;
    /**
     * The crafting level required to make this item
     */
    public int requiredLvl;

    public ItemCraftingDef(int level, int itemID, int exp) {
		this.requiredLvl = level;
		this.itemID = itemID;
		this.exp = exp;
	}

	public int getExp() {
        return exp;
    }

    public int getItemID() {
        return itemID;
    }

    public int getReqLevel() {
        return requiredLvl;
    }
}