package com.legacy.server.external;

public class ItemSmithingDef {
    /**
     * The amount of the item produced
     */
    public int amount;
    /**
     * How many bars are required
     */
    public int bars;
    /**
     * The ID of the item produced
     */
    public int itemID;
    /**
     * The smithing level required to make this item
     */
    public int level;

    public ItemSmithingDef(int level, int bars, int itemID, int amount) {
		this.level = level;
		this.bars = bars;
		this.itemID = itemID;
		this.amount = amount;
	}

	public int getAmount() {
        return amount;
    }

    public int getItemID() {
        return itemID;
    }

    public int getRequiredBars() {
        return bars;
    }

    public int getRequiredLevel() {
        return level;
    }
}