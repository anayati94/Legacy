package com.legacy.server.external;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class handles the loading of entities from the database worldpopulation manager, and provides
 * methods for relaying these entities to the user.
 */
public final class EntityHandler {

	private static HashMap<Integer, ItemArrowHeadDef> arrowHeads;
	private static HashMap<Integer, ItemBowStringDef> bowString;
	private static HashMap<Integer, CerterDef> certers;
	private static HashMap<Integer, ItemDartTipDef> dartTips;

	private static ArrayList<DoorDef> doors;
	private static HashMap<Integer, FiremakingDef> firemaking;
	private static ArrayList<GameObjectDef> gameObjects;
	private static HashMap<Integer, ItemGemDef> gems;
	private static ArrayList<ItemHerbSecond> herbSeconds;
	private static HashMap<Integer, int[]> itemAffectedTypes;
	private static HashMap<Integer, ItemCookingDef> itemCooking;
	private static ArrayList<ItemCraftingDef> itemCrafting;
	private static HashMap<Integer, ItemEdibleDef> itemEdibleHeals;
	private static HashMap<Integer, ItemHerbDef> itemHerb;
	public static ArrayList<ItemDefinition> items;
	private static HashMap<Integer, ItemSmeltingDef> itemSmelting;
	private static ArrayList<ItemSmithingDef> itemSmithing;
	private static HashMap<Integer, ItemUnIdentHerbDef> itemUnIdentHerb;
	private static HashMap<Integer, ItemLogCutDef> logCut;

	public static ArrayList<NPCDef> npcs;
	private static HashMap<Integer, ArrayList<ObjectFishingDef>> objectFishing;

	private static HashMap<Integer, ObjectMiningDef> objectMining;
	private static HashMap<Integer, ObjectWoodcuttingDef> objectWoodcutting;
	private static ArrayList<PrayerDef> prayers;
	private static ArrayList<SpellDef> spells;
	private static ArrayList<TileDef> tiles;

	public static void setTileDefinitions(ArrayList<TileDef> tileDefs) {tiles = tileDefs;}
	public static void setDoorDefinitions(ArrayList<DoorDef> doorDefs) {doors = doorDefs;}
	public static void setGameObjectDefinitions(ArrayList<GameObjectDef> gameObjectDefs) {gameObjects = gameObjectDefs;}
	public static void setItemDefinitions(ArrayList<ItemDefinition> itemDefs) {items = itemDefs;}
	public static void setNpcDefinitions(ArrayList<NPCDef> npcDefs) {npcs = npcDefs;}
	public static void setSpellDefinitions(ArrayList<SpellDef> spellDefs) {spells = spellDefs;}
	public static void setPrayerDefinitions(ArrayList<PrayerDef> prayerDefs) {prayers = prayerDefs;}
	public static void setFiremakingDefinitions(HashMap<Integer, FiremakingDef> firemakingDefs) {firemaking = firemakingDefs;}
	public static void setMiningDefinitions(HashMap<Integer, ObjectMiningDef> objectMiningDefs) {objectMining = objectMiningDefs;}
	public static void setWoodcutDefinitions(HashMap<Integer, ObjectWoodcuttingDef> woodcutDefinitions) {objectWoodcutting = woodcutDefinitions;}
	public static void setCookingDefinitions(HashMap<Integer, ItemCookingDef> itemCookingDefs) {itemCooking = itemCookingDefs;}
	public static void setGemDefinitions(HashMap<Integer, ItemGemDef> gemDefs) {gems = gemDefs;}
	public static void setLogCutDefinitions(HashMap<Integer, ItemLogCutDef> logCutDefs) {logCut = logCutDefs;}
	public static void setCraftingDefinitions(ArrayList<ItemCraftingDef> itemCraftingDefs) {itemCrafting = itemCraftingDefs;}
	public static void setArrowHeadDefinitions(HashMap<Integer, ItemArrowHeadDef> arrowHeadDefs) {arrowHeads = arrowHeadDefs;}
	public static void setDartTipDefinitions(HashMap<Integer, ItemDartTipDef> dartTipDefs) {dartTips = dartTipDefs;}
	public static void setFishingDefinitions(HashMap<Integer, ArrayList<ObjectFishingDef>> objectFishingDefs) {objectFishing = objectFishingDefs;}
	public static void setItemHealingDefinitions(HashMap<Integer, ItemEdibleDef> itemEdibleHealDefs) {itemEdibleHeals = itemEdibleHealDefs;}
	public static void setHerbDefinitions(HashMap<Integer, ItemHerbDef> itemHerbDefs) {itemHerb = itemHerbDefs;}
	public static void setUnidentifiedHerbDefinitions(HashMap<Integer, ItemUnIdentHerbDef> itemUnIdentHerbDefs) {itemUnIdentHerb = itemUnIdentHerbDefs;}
	public static void setHerbSecondaryDefinitions(ArrayList<ItemHerbSecond> herbSecondDefs) {herbSeconds = herbSecondDefs;}
	public static void setBowstringDefinitions(HashMap<Integer, ItemBowStringDef> bowStringDef) {bowString = bowStringDef;}
	public static void setSmeltingDefinitions(HashMap<Integer, ItemSmeltingDef> itemSmeltingDefs) {itemSmelting = itemSmeltingDefs;}
	public static void setSmithingDefinitions(ArrayList<ItemSmithingDef> itemSmithingDefs) {itemSmithing = itemSmithingDefs;}
	public static void setCerterDefinitions(HashMap<Integer, CerterDef> certerDefs) {certers = certerDefs;}

	static {
		itemAffectedTypes = new HashMap<Integer, int[]>();
		int[] types = {8, 24};
		itemAffectedTypes.put(8217, types);
		types = new int[] {8, 24, 8216, 8217};
		itemAffectedTypes.put(8, types);
		types = new int[] {16, 24, 8216};
		itemAffectedTypes.put(16, types);
		types = new int[]  {8, 16, 24, 8216, 8217};
		itemAffectedTypes.put(24, types);
		types = new int[] {8, 16, 24, 8216};
		itemAffectedTypes.put(8216, types);
		types = new int[]  {33, 32};
		itemAffectedTypes.put(32, types);
		itemAffectedTypes.put(33, types);
		types = new int[]  {64, 322};
		itemAffectedTypes.put(64, types);
		types = new int[]  {128, 640, 644};
		itemAffectedTypes.put(128, types);
		types = new int[] {256, 322};
		itemAffectedTypes.put(256, types);
		types = new int[]  {512, 640, 644, 3000};
		itemAffectedTypes.put(512, types);
		types = new int[]  {128, 512, 640, 644};
		itemAffectedTypes.put(640, types);
		types = new int[]  {128, 512, 640, 644};
		itemAffectedTypes.put(644, types);
		types = new int[]  {1024};
		itemAffectedTypes.put(1024, types);
		types = new int[]  {2048};
		itemAffectedTypes.put(2048, types);
		types = new int[] {64, 256, 322};
		itemAffectedTypes.put(322, types);
		types = new int[] {3000, 512};
		itemAffectedTypes.put(3000, types);
	}

	/**
	 * @param id the npcs ID
	 * @return the CerterDef for the given npc
	 */
	public static CerterDef getCerterDef(int id) {
		return certers.get(id);
	}

	/**
	 * @return the ItemCraftingDef for the requested item
	 */
	public static ItemCraftingDef getCraftingDef(int id) {
		if(id < 0 || id >= itemCrafting.size()) {
			return null;
		}
		return itemCrafting.get(id);
	}

	/**
	 * @param id the entities ID
	 * @return the DoorDef with the given ID
	 */
	public static DoorDef getDoorDef(int id) {
		if(id < 0 || id >= doors.size()) {
			return null;
		}
		return doors.get(id);
	}

	/**
	 * @return the FiremakingDef for the given log
	 */
	public static FiremakingDef getFiremakingDef(int id) {
		return firemaking.get(id);
	}

	/**
	 * @param id the entities ID
	 * @return the GameObjectDef with the given ID
	 */
	public static GameObjectDef getGameObjectDef(int id) {
		if(id < 0 || id >= gameObjects.size()) {
			return null;
		}
		return gameObjects.get(id);
	}

	/**
	 * @param the items type
	 * @return the types of items affected
	 */
	public static int[] getAffectedTypes(int type) {
		int[] affectedTypes = itemAffectedTypes.get(type);
		if (affectedTypes != null) {
			return affectedTypes;
		}
		return new int[0];
	}


	/**
	 * @return the ItemArrowHeadDef for the given arrow
	 */
	public static ItemArrowHeadDef getItemArrowHeadDef(int id) {
		return arrowHeads.get(id);
	}

	/**
	 * @return the ItemBowStringDef for the given bow
	 */
	public static ItemBowStringDef getItemBowStringDef(int id) {
		return bowString.get(id);
	}

	/**
	 * @param id the entities ID
	 * @return the ItemCookingDef with the given ID
	 */
	public static ItemCookingDef getItemCookingDef(int id) {
		return itemCooking.get(id);
	}

	/**
	 * @return the ItemDartTipDef for the given tip
	 */
	public static ItemDartTipDef getItemDartTipDef(int id) {
		return dartTips.get(id);
	}

	/**
	 * @param id the entities ID
	 * @return the ItemDef with the given ID
	 */
	public static ItemDefinition getItemDef(int id) {
		if(id < 0 || id >= items.size()) {
			return null;
		}
		return items.get(id);
	}

	/**
	 * @param the items id
	 * @return the amount eating the item should heal
	 */
	public static ItemEdibleDef getItemEdibleHeals(int id) {
		return itemEdibleHeals.get(id);
	}

	/**
	 * @return the ItemGemDef for the given gem
	 */
	public static ItemGemDef getItemGemDef(int id) {
		return gems.get(id);
	}

	/**
	 * @param id the entities ID
	 * @return the ItemHerbDef with the given ID
	 */
	public static ItemHerbDef getItemHerbDef(int id) {
		return itemHerb.get(id);
	}

	/**
	 * @return the ItemHerbSecond for the given second ingredient
	 */
	public static ItemHerbSecond getItemHerbSecond(int secondID, int unfinishedID) {
		for (ItemHerbSecond def : herbSeconds) {
			if (def.getSecondID() == secondID && def.getUnfinishedID() == unfinishedID) {
				return def;
			}
		}
		return null;
	}

	/**
	 * @return the ItemLogCutDef for the given log
	 */
	public static ItemLogCutDef getItemLogCutDef(int id) {
		return logCut.get(id);
	}

	/**
	 * @param id the entities ID
	 * @return the ItemSmeltingDef with the given ID
	 */
	public static ItemSmeltingDef getItemSmeltingDef(int id) {
		return itemSmelting.get(id);
	}

	/**
	 * @param id the entities ID
	 * @return the ItemUnIdentHerbDef with the given ID
	 */
	public static ItemUnIdentHerbDef getItemUnIdentHerbDef(int id) {
		return itemUnIdentHerb.get(id);
	}

	/**
	 * @param id the entities ID
	 * @return the NPCDef with the given ID
	 */
	public static NPCDef getNpcDef(int id) {
		if (id < 0 || id >= npcs.size()) {
			return null;
		}
		return npcs.get(id);
	}


	/**
	 * @param id the entities ID
	 * @return the ObjectFishingDef with the given ID
	 */
	public static ObjectFishingDef getObjectFishingDef(int id, int click) {
		return objectFishing.get(id).get(click);

	}

	/**
	 * @param id the entities ID
	 * @return the ObjectMiningDef with the given ID
	 */
	public static ObjectMiningDef getObjectMiningDef(int id) {
		return objectMining.get(id);
	}

	/**
	 * @param id the entities ID
	 * @return the ObjectWoodcuttingDef with the given ID
	 */
	public static ObjectWoodcuttingDef getObjectWoodcuttingDef(int id) {
		return objectWoodcutting.get(id);
	}

	/**
	 * @param id the entities ID
	 * @return the PrayerDef with the given ID
	 */
	public static PrayerDef getPrayerDef(int id) {
		if(id < 0 || id >= prayers.size()) {
			return null;
		}
		return prayers.get(id);
	}

	/**
	 * @return the ItemSmithingDef for the requested item
	 */
	public static ItemSmithingDef getSmithingDef(int id) {
		if(id < 0 || id >= itemSmithing.size()) {
			return null;
		}
		return itemSmithing.get(id);
	}

	/**
	 * @return the ItemSmithingDef for the requested item
	 */
	public static ItemSmithingDef getSmithingDefbyID(int itemID) {
		for (ItemSmithingDef i : itemSmithing) {
			if (i.itemID == itemID)
				return i;
		}
		return null;
	}

	/**
	 * @param id the entities ID
	 * @return the SpellDef with the given ID
	 */
	public static SpellDef getSpellDef(int id) {
		if(id < 0 || id >= spells.size()) {
			return null;
		}
		return spells.get(id);
	}

	/**
	 * @param id the entities ID
	 * @return the TileDef with the given ID
	 */
	public static TileDef getTileDef(int id) {
		if(id < 0 || id >= tiles.size()) {
			return null;
		}
		return tiles.get(id);
	}
}
