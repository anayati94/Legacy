package com.legacy.server.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.legacy.server.Constants;
import com.legacy.server.external.CertDef;
import com.legacy.server.external.CerterDef;
import com.legacy.server.external.DoorDef;
import com.legacy.server.external.EntityHandler;
import com.legacy.server.external.FiremakingDef;
import com.legacy.server.external.GameObjectDef;
import com.legacy.server.external.ItemArrowHeadDef;
import com.legacy.server.external.ItemBowStringDef;
import com.legacy.server.external.ItemCookingDef;
import com.legacy.server.external.ItemCraftingDef;
import com.legacy.server.external.ItemDartTipDef;
import com.legacy.server.external.ItemDefinition;
import com.legacy.server.external.ItemDropDef;
import com.legacy.server.external.ItemEdibleDef;
import com.legacy.server.external.ItemGemDef;
import com.legacy.server.external.ItemHerbDef;
import com.legacy.server.external.ItemHerbSecond;
import com.legacy.server.external.ItemLoc;
import com.legacy.server.external.ItemLogCutDef;
import com.legacy.server.external.ItemSmeltingDef;
import com.legacy.server.external.ItemSmithingDef;
import com.legacy.server.external.ItemUnIdentHerbDef;
import com.legacy.server.external.NPCDef;
import com.legacy.server.external.NPCLoc;
import com.legacy.server.external.ObjectFishDef;
import com.legacy.server.external.ObjectFishingDef;
import com.legacy.server.external.ObjectMiningDef;
import com.legacy.server.external.ObjectWoodcuttingDef;
import com.legacy.server.external.PrayerDef;
import com.legacy.server.external.ReqOreDef;
import com.legacy.server.external.SpellDef;
import com.legacy.server.external.TileDef;
import com.legacy.server.model.Point;
import com.legacy.server.model.entity.GameObject;
import com.legacy.server.model.entity.GroundItem;
import com.legacy.server.model.entity.npc.Npc;
import com.legacy.server.model.world.World;
import com.legacy.server.util.rsc.Formulae;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public final class WorldPopulation {

	/**
	 * The asynchronous logger.
	 */
	private static final Logger LOGGER = LogManager.getLogger();

	private static final int QUERY_FAILED = -1;

	private static WorldPopulation singleton;

	private static HikariDataSource dataSource;

	private final HikariConfig hikariConfig;

	/**
	 * Instantiates a new database connection
	 */
	public WorldPopulation() {
		hikariConfig = new HikariConfig("resources/db/game_definition_database.properties");
		hikariConfig.setLeakDetectionThreshold(10000);
		dataSource = new HikariDataSource(hikariConfig);
	}

	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	public static WorldPopulation getDatabase() {
		return singleton;
	}
	
	public static void init() {
		LOGGER.info("Creating world population connection...");
		singleton = new WorldPopulation();
		LOGGER.info("World population connection created.");
	}

	public int executeUpdate(String query) {
		try(Connection connection = dataSource.getConnection()) {
			try(PreparedStatement ps = connection.prepareStatement(query)) {
				return ps.executeUpdate();
			}
		} catch (SQLException e) {
			LOGGER.catching(e);
			return QUERY_FAILED;
		}
	}

	public void close() {
		dataSource.close();
	}

	public static void populateWorldDefinitions() throws SQLException {
		final long start = System.currentTimeMillis();
		EntityHandler.setNpcDefinitions(loadNpcDefinitions());
		EntityHandler.setTileDefinitions(loadTileDefinitions());
		EntityHandler.setDoorDefinitions(loadDoorDefinitions());
		EntityHandler.setGameObjectDefinitions(loadGameObjectDefinitions());
		EntityHandler.setItemDefinitions(loadItemDefinitions());
		EntityHandler.setSpellDefinitions(loadSpellDefinitions());
		EntityHandler.setPrayerDefinitions(loadPrayerDefinitions());
		EntityHandler.setFiremakingDefinitions(loadFiremakingDefinitions());
		EntityHandler.setMiningDefinitions(loadMiningDefinitions());
		EntityHandler.setWoodcutDefinitions(loadWoodcuttingDefinitions());
		EntityHandler.setCookingDefinitions(loadCookingDefinitions());
		EntityHandler.setGemDefinitions(loadGemDefinitions());
		EntityHandler.setLogCutDefinitions(loadLogCutDefinitions());
		EntityHandler.setCraftingDefinitions(loadCraftingDefinitions());
		EntityHandler.setArrowHeadDefinitions(loadArrowHeadDefinitions());
		EntityHandler.setDartTipDefinitions(loadDartTipDefinitions());
		EntityHandler.setFishingDefinitions(loadFishingDefinitions());
		EntityHandler.setItemHealingDefinitions(loadItemEdibleHeals());
		EntityHandler.setUnidentifiedHerbDefinitions(loadUnidentifiedHerbDefinitions());
		EntityHandler.setHerbDefinitions(loadHerbDefinitions());
		EntityHandler.setHerbSecondaryDefinitions(loadHerbSecondaryDefinitions());
		EntityHandler.setBowstringDefinitions(loadItemBowStringDefinitions());
		EntityHandler.setSmeltingDefinitions(loadSmeltingDefinitions());
		EntityHandler.setSmithingDefinitions(loadSmithingDefinitions());
		EntityHandler.setCerterDefinitions(loadCerterDefinitions());
		LOGGER.info(((System.currentTimeMillis() - start) / 1000) + "s to load world definitions.");
	}

	private static HashMap<Integer, CerterDef> loadCerterDefinitions() throws SQLException {
		HashMap<Integer, CerterDef> certerDefs = new HashMap<Integer, CerterDef>();
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection
						.prepareStatement("SELECT `npc_id`, `type` FROM `"
								+ Constants.GameServer.MYSQL_TABLE_PREFIX + "certer_npc_def`");
				ResultSet result = statement.executeQuery()) {
			while (result.next()) {
				certerDefs.put(result.getInt("npc_id"), new CerterDef(result.getString("type")));
			}
		}
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection
						.prepareStatement("SELECT `certer_id`, `cert_name`, `cert_id`, `item_id` FROM `"
								+ Constants.GameServer.MYSQL_TABLE_PREFIX + "certdef`");
				ResultSet result = statement.executeQuery()) {
			while (result.next()) {
				certerDefs.get(result.getInt("certer_id")).getCerts().add(new CertDef(result.getString("cert_name"),
						result.getInt("cert_id"), result.getInt("item_id")));
			}
		}
		return certerDefs;
	}

	private static ArrayList<ItemSmithingDef> loadSmithingDefinitions() throws SQLException {
		ArrayList<ItemSmithingDef> smithingDefs = new ArrayList<ItemSmithingDef>();
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection
						.prepareStatement("SELECT `level`, `bars`, `item_id`, `amount` FROM `"
								+ Constants.GameServer.MYSQL_TABLE_PREFIX + "smithingdef`");
				ResultSet result = statement.executeQuery()) {
			while (result.next()) {
				smithingDefs.add(new ItemSmithingDef(result.getInt("level"), result.getInt("bars"),
						result.getInt("item_id"), result.getInt("amount")));
			}
		}
		return smithingDefs;
	}

	private static HashMap<Integer, ItemSmeltingDef> loadSmeltingDefinitions() throws SQLException {
		HashMap<Integer, ItemSmeltingDef> smeltingDefs = new HashMap<Integer, ItemSmeltingDef>();
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection
						.prepareStatement("SELECT `ore`, `bar`, `alternate_ore`, `alternate_amount`, `level`, `experience` FROM `"
								+ Constants.GameServer.MYSQL_TABLE_PREFIX + "smeltingdef`");
				ResultSet result = statement.executeQuery()) {
			while (result.next()) {
				ReqOreDef[] reqOreDef = null;
				if (result.getInt("alternate_ore") != -1) {
					ReqOreDef req = new ReqOreDef(result.getInt("alternate_ore"),
							result.getInt("alternate_amount"));
					reqOreDef = new ReqOreDef[] { req };
				}
				smeltingDefs.put(result.getInt("ore"), new ItemSmeltingDef(result.getInt("experience"),
						result.getInt("bar"), result.getInt("level"), reqOreDef));
			}
		}
		return smeltingDefs;
	}

	private static HashMap<Integer, ItemBowStringDef> loadItemBowStringDefinitions() throws SQLException {
		HashMap<Integer, ItemBowStringDef> bowStringDefs = new HashMap<Integer, ItemBowStringDef>();
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection
						.prepareStatement("SELECT `unstrung_bow_id`, `finished_bow_id`, `level`, `experience` FROM `"
								+ Constants.GameServer.MYSQL_TABLE_PREFIX + "bow_string_def`");
				ResultSet result = statement.executeQuery()) {
			while (result.next()) {
				bowStringDefs.put(result.getInt("unstrung_bow_id"), new ItemBowStringDef(
						result.getInt("finished_bow_id"), result.getInt("level"), result.getInt("experience")));
			}
		}
		return bowStringDefs;
	}

	private static ArrayList<ItemHerbSecond> loadHerbSecondaryDefinitions() throws SQLException {
		ArrayList<ItemHerbSecond> secondaries = new ArrayList<ItemHerbSecond>();
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection
						.prepareStatement("SELECT `secondary_id`, `unfinished_id`, `potion_id`, `level`, `experience` FROM `"
								+ Constants.GameServer.MYSQL_TABLE_PREFIX + "herb_second_def`");
				ResultSet result = statement.executeQuery()) {
			while (result.next()) {
				secondaries.add(new ItemHerbSecond(result.getInt("level"), result.getInt("experience"),
						result.getInt("potion_id"), result.getInt("unfinished_id"), result.getInt("secondary_id")));
			}
		}
		return secondaries;
	}

	private static HashMap<Integer, ItemUnIdentHerbDef> loadUnidentifiedHerbDefinitions() throws SQLException {
		HashMap<Integer, ItemUnIdentHerbDef> unidentifiedDefs = new HashMap<Integer, ItemUnIdentHerbDef>();
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection
						.prepareStatement("SELECT `unidentified_id`, `identified_id`, `level`, `experience` FROM `"
								+ Constants.GameServer.MYSQL_TABLE_PREFIX + "unidentified_herb_def`");
				ResultSet result = statement.executeQuery()) {
			while (result.next()) {
				unidentifiedDefs.put(result.getInt("unidentified_id"), new ItemUnIdentHerbDef(
						result.getInt("level"), result.getInt("identified_id"), result.getInt("experience")));
			}
		}
		return unidentifiedDefs;
	}

	private static HashMap<Integer, ItemHerbDef> loadHerbDefinitions() throws SQLException {
		HashMap<Integer, ItemHerbDef> herbs = new HashMap<Integer, ItemHerbDef>();
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection
						.prepareStatement("SELECT `herb_id`, `potion_id`, `level`, `experience` FROM `"
								+ Constants.GameServer.MYSQL_TABLE_PREFIX + "herbdef`");
				ResultSet result = statement.executeQuery()) {
			while (result.next()) {
				herbs.put(result.getInt("herb_id"), new ItemHerbDef(result.getInt("level"),
						result.getInt("experience"), result.getInt("potion_id")));
			}
		}
		return herbs;
	}

	public static HashMap<Integer, ItemEdibleDef> loadItemEdibleHeals() throws SQLException {
		HashMap<Integer, ItemEdibleDef> edibleHeals = new HashMap<Integer, ItemEdibleDef>();
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection
						.prepareStatement("SELECT `food_id`, `heals` FROM `"
								+ Constants.GameServer.MYSQL_TABLE_PREFIX + "edible_heals_def`");
				ResultSet result = statement.executeQuery()) {
			while (result.next()) {
				edibleHeals.put(result.getInt("food_id"),
						new ItemEdibleDef(result.getInt("heals")));
			}
		}
		return edibleHeals;
	}

	private static HashMap<Integer, ArrayList<ObjectFishingDef>> loadFishingDefinitions() throws SQLException {
		HashMap<Integer, ArrayList<ObjectFishingDef>> objectFishingDefs = new HashMap<Integer, ArrayList<ObjectFishingDef>>();
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection
						.prepareStatement("SELECT `object_id`, `net_id`, `bait_id` FROM `"
								+ Constants.GameServer.MYSQL_TABLE_PREFIX + "object_fishing_def`");
				ResultSet result = statement.executeQuery()) {
			while (result.next()) {
				if (objectFishingDefs.containsKey(result.getInt("object_id"))) {
					objectFishingDefs.get(result.getInt("object_id")).add(new ObjectFishingDef(
							result.getInt("object_id"), result.getInt("net_id"), result.getInt("bait_id")));
				} else {
					objectFishingDefs.put(result.getInt("object_id"), new ArrayList<ObjectFishingDef>());
					objectFishingDefs.get(result.getInt("object_id")).add(new ObjectFishingDef(
							result.getInt("object_id"), result.getInt("net_id"), result.getInt("bait_id")));
				}
			}
		}
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection
						.prepareStatement("SELECT `object_id`, `net_id`, `bait_id`, `fish_id`, `level`, `experience` FROM `"
								+ Constants.GameServer.MYSQL_TABLE_PREFIX + "fishdef`");
				ResultSet result = statement.executeQuery()) {
			while (result.next()) {
				for (ArrayList<ObjectFishingDef> defs : objectFishingDefs.values()) {
					boolean broken = false;
					for (ObjectFishingDef def : defs) {
						if (def.getObjectId() == result.getInt("object_id")
								&& def.getNetId() == result.getInt("net_id")
								&& def.getBaitId() == result.getInt("bait_id")) {
							def.getFishDefs().add(new ObjectFishDef(result.getInt("fish_id"),
									result.getInt("level"), result.getInt("experience")));
							broken = true;
							break;
						}
					}
					if (broken) {
						break;
					}
				}
			}
		}
		return objectFishingDefs;
	}

	public static HashMap<Integer, ItemDartTipDef> loadDartTipDefinitions() throws SQLException {
		HashMap<Integer, ItemDartTipDef> tips = new HashMap<Integer, ItemDartTipDef>();
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection
						.prepareStatement("SELECT `tip_id`, `dart_id`, `experience`, `level` FROM `"
								+ Constants.GameServer.MYSQL_TABLE_PREFIX + "dart_tip_def`");
				ResultSet result = statement.executeQuery()) {
			while (result.next()) {
				tips.put(result.getInt("tip_id"), new ItemDartTipDef(result.getInt("level"),
						result.getInt("experience"), result.getInt("dart_id")));
			}
		}
		return tips;
	}

	public static HashMap<Integer, ItemArrowHeadDef> loadArrowHeadDefinitions() throws SQLException {
		HashMap<Integer, ItemArrowHeadDef> arrowheads = new HashMap<Integer, ItemArrowHeadDef>();
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection
						.prepareStatement("SELECT `arrowhead_id`, `arrow_id`, `level`, `experience` FROM `"
								+ Constants.GameServer.MYSQL_TABLE_PREFIX + "arrow_head_def`");
				ResultSet result = statement.executeQuery()) {
			while (result.next()) {
				arrowheads.put(result.getInt("arrowhead_id"), new ItemArrowHeadDef(result.getInt("level"),
						result.getDouble("experience"), result.getInt("arrow_id")));
			}
		}
		return arrowheads;
	}

	public static ArrayList<ItemCraftingDef> loadCraftingDefinitions() throws SQLException {
		ArrayList<ItemCraftingDef> defs = new ArrayList<ItemCraftingDef>();
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection
						.prepareStatement("SELECT `level`, `item_id`, `experience` FROM `"
								+ Constants.GameServer.MYSQL_TABLE_PREFIX + "craftingdef`");
				ResultSet result = statement.executeQuery()) {
			while (result.next()) {
				defs.add(new ItemCraftingDef(result.getInt("level"), result.getInt("item_id"),
						result.getInt("experience")));
			}
		}
		return defs;
	}

	private static HashMap<Integer, ItemLogCutDef> loadLogCutDefinitions() throws SQLException {
		HashMap<Integer, ItemLogCutDef> logCutDefs = new HashMap<Integer, ItemLogCutDef>();
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection
						.prepareStatement("SELECT `log`, `shaft_amount`, `shaft_level`, `shortbow_id`, `shortbow_level`, `shortbow_experience`, `longbow_id`, `longbow_level`, `longbow_experience` FROM `"
								+ Constants.GameServer.MYSQL_TABLE_PREFIX + "log_cut_def`");
				ResultSet result = statement.executeQuery()) {
			while (result.next()) {
				logCutDefs.put(result.getInt("log"),
						new ItemLogCutDef(result.getInt("shaft_amount"), result.getInt("shaft_level"),
								result.getInt("shortbow_id"), result.getInt("shortbow_level"),
								result.getInt("shortbow_experience"), result.getInt("longbow_id"),
								result.getInt("longbow_level"), result.getInt("longbow_experience")));
			}
		}
		return logCutDefs;
	}

	public static HashMap<Integer, ItemGemDef> loadGemDefinitions() throws SQLException {
		HashMap<Integer, ItemGemDef> gems = new HashMap<Integer, ItemGemDef>();
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection
						.prepareStatement("SELECT `uncut_id`, `cut_id`, `level`, `experience` FROM `"
								+ Constants.GameServer.MYSQL_TABLE_PREFIX + "gemdef`");
				ResultSet result = statement.executeQuery()) {
			while (result.next()) {
				gems.put(result.getInt("uncut_id"), new ItemGemDef(result.getInt("level"),
						result.getInt("experience"), result.getInt("cut_id")));
			}
		}
		return gems;
	}

	private static HashMap<Integer, ItemCookingDef> loadCookingDefinitions() throws SQLException {
		HashMap<Integer, ItemCookingDef> defs = new HashMap<Integer, ItemCookingDef>();
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection
						.prepareStatement("SELECT `raw_id`, `cooked_id`, `burned_id`, `experience`, `level` FROM `"
								+ Constants.GameServer.MYSQL_TABLE_PREFIX + "cookingdef`");
				ResultSet result = statement.executeQuery()) {
			while (result.next()) {
				defs.put(result.getInt("raw_id"), new ItemCookingDef(result.getInt("experience"),
						result.getInt("cooked_id"), result.getInt("burned_id"), result.getInt("level")));
			}
		}
		return defs;
	}

	public static HashMap<Integer, ObjectWoodcuttingDef> loadWoodcuttingDefinitions() throws SQLException {
		HashMap<Integer, ObjectWoodcuttingDef> defs = new HashMap<Integer, ObjectWoodcuttingDef>();
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection
						.prepareStatement("SELECT `tree_id`, `experience`, `level`, `fell`, `log_id`, `respawn_time` FROM `"
								+ Constants.GameServer.MYSQL_TABLE_PREFIX + "woodcutdef`");
				ResultSet result = statement.executeQuery()) {
			while (result.next()) {
				defs.put(result.getInt("tree_id"),
						new ObjectWoodcuttingDef(result.getInt("experience"), result.getInt("level"), result.getInt("fell"),
								result.getInt("log_id"), result.getInt("respawn_time")));
			}
		}
		return defs;
	}

	public static HashMap<Integer, ObjectMiningDef> loadMiningDefinitions() throws SQLException {
		HashMap<Integer, ObjectMiningDef> defs = new HashMap<Integer, ObjectMiningDef>();
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection
						.prepareStatement("SELECT `rock_id`, `ore_id`, `experience`, `level`, `respawn_time` FROM `"
								+ Constants.GameServer.MYSQL_TABLE_PREFIX + "miningdef`");
				ResultSet result = statement.executeQuery()) {
			while (result.next()) {
				defs.put(result.getInt("rock_id"), new ObjectMiningDef(result.getInt("ore_id"),
						result.getInt("experience"), result.getInt("level"), result.getInt("respawn_time")));
			}
		}
		return defs;
	}

	private static HashMap<Integer, FiremakingDef> loadFiremakingDefinitions() throws SQLException {
		HashMap<Integer, FiremakingDef> firemakings = new HashMap<Integer, FiremakingDef>();
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection
						.prepareStatement("SELECT `log_id`, `level`, `experience`, `length` FROM `"
								+ Constants.GameServer.MYSQL_TABLE_PREFIX + "firemakingdef`");
				ResultSet result = statement.executeQuery()) {
			while (result.next()) {
				firemakings.put(result.getInt("log_id"), new FiremakingDef(result.getInt("level"), result.getInt("experience"), result.getInt("length")));
			}
		}
		return firemakings;
	}

	private static ArrayList<PrayerDef> loadPrayerDefinitions() throws SQLException {
		ArrayList<PrayerDef> prayers = new ArrayList<PrayerDef>();
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection
						.prepareStatement("SELECT `level`, `drain_rate` FROM `"
								+ Constants.GameServer.MYSQL_TABLE_PREFIX + "prayerdef`");
				ResultSet result = statement.executeQuery()) {
			while (result.next()) {
				PrayerDef def = new PrayerDef();
				def.reqLevel = result.getInt("level");
				def.drainRate = result.getInt("drain_rate");
				prayers.add(def);
			}
		}
		return prayers;
	}

	private static ArrayList<SpellDef> loadSpellDefinitions() throws SQLException {
		ArrayList<SpellDef> spells = new ArrayList<SpellDef>();
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection
						.prepareStatement("SELECT `name`, `description`, `level`, `type`, `rune_count`, `required_rune1`, `amount1`, `required_rune2`, `amount2`, `required_rune3`, `amount3`, `experience`, `members` FROM `"
								+ Constants.GameServer.MYSQL_TABLE_PREFIX + "spelldef`");
				ResultSet result = statement.executeQuery()) {
			while (result.next()) {
				SpellDef def = new SpellDef();
				def.name = result.getString("name");
				def.description = result.getString("description");
				def.reqLevel = result.getInt("level");
				def.type = result.getInt("type");
				def.runeCount = result.getInt("rune_count");
				def.requiredRunes = new HashMap<Integer, Integer>(def.runeCount);
				if (result.getInt("required_rune1") != 0)
					def.requiredRunes.put(result.getInt("required_rune1"), result.getInt("amount1"));
				if (result.getInt("required_rune2") != 0)
					def.requiredRunes.put(result.getInt("required_rune2"), result.getInt("amount2"));
				if (result.getInt("required_rune3") != 0)
					def.requiredRunes.put(result.getInt("required_rune3"), result.getInt("amount3"));
				def.exp = result.getInt("experience");
				def.members = result.getInt("members") == 1;
				spells.add(def);
			}
		}
		return spells;
	}

	@SuppressWarnings("unchecked")
	private static ArrayList<NPCDef> loadNpcDefinitions() throws SQLException {
		ArrayList<NPCDef> npcDefinitions = new ArrayList<NPCDef>();
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection
						.prepareStatement("SELECT `id`, `name`, `description`, `command`, `command2`, "
								+ "`attack`, `strength`, `hits`, `defense`, `combatlvl`, `isMembers`, `attackable`, `aggressive`, `respawnTime`, "
								+ "`sprites1`, `sprites2`, `sprites3`, `sprites4`, `sprites5`, `sprites6`, `sprites7`, `sprites8`, `sprites9`, "
								+ "`sprites10`, `sprites11`, `sprites12`, `hairColour`, `topColour`, `bottomColour`, `skinColour`, `camera1`, "
								+ "`camera2`, `walkModel`, `combatModel`, `combatSprite` FROM `"
								+ Constants.GameServer.MYSQL_TABLE_PREFIX + "npcdef`");
				ResultSet result = statement.executeQuery()) {
			while (result.next()) {
				NPCDef def = new NPCDef();
				def.name = result.getString("name");
				def.description = result.getString("description");
				def.command1 = result.getString("command");
				def.command2 = result.getString("command2");
				def.attack = result.getInt("attack");
				def.strength = result.getInt("strength");
				def.hits = result.getInt("hits");
				def.defense = result.getInt("defense");
				def.combatLevel = result.getInt("combatlvl");
				def.members = result.getBoolean("isMembers");
				def.attackable = result.getBoolean("attackable");
				def.aggressive = result.getBoolean("aggressive");
				def.respawnTime = result.getInt("respawnTime");
				for (int i = 0; i < 12; i++) {
					def.sprites[i] = result.getInt("sprites" + (i + 1));
				}
				def.hairColour = result.getInt("hairColour");
				def.topColour = result.getInt("topColour");
				def.bottomColour = result.getInt("bottomColour");
				def.skinColour = result.getInt("skinColour");
				def.camera1 = result.getInt("camera1");
				def.camera2 = result.getInt("camera2");
				def.walkModel = result.getInt("walkModel");
				def.combatModel = result.getInt("combatModel");
				def.combatSprite = result.getInt("combatSprite");


				ArrayList<ItemDropDef> drops = new ArrayList<ItemDropDef>();

				try (Statement dropStatement = connection.createStatement();
						ResultSet dropResult = dropStatement.executeQuery(
								"SELECT `amount`, `id`, `weight` FROM `" + Constants.GameServer.MYSQL_TABLE_PREFIX
								+ "npcdrops` WHERE npcdef_id = '" + result.getInt("id") + "'")) {
					while (dropResult.next()) {
						ItemDropDef drop = new ItemDropDef(dropResult.getInt("id"), dropResult.getInt("amount"), dropResult.getInt("weight"));
						drops.add(drop);
					}
				}
				def.drops = drops.toArray(new ItemDropDef[] {});
				npcDefinitions.add(def);
			}
			EntityHandler.npcs = (ArrayList<NPCDef>) npcDefinitions.clone();
			for (NPCDef n : EntityHandler.npcs) {
				if (n.isAttackable()) {
					n.respawnTime -= (n.respawnTime / 3);
				}
			}
		}
		return npcDefinitions; 
	}


	private static ArrayList<ItemDefinition> loadItemDefinitions() throws SQLException {
		ArrayList<ItemDefinition> itemDefinitions = new ArrayList<ItemDefinition>();
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(
						"SELECT `name`, `description`, `command`, `isFemaleOnly`, `isMembersOnly`, `isStackable`, "
								+ "`isUntradable`, `isWearable`, `appearanceID`, `wearableID`, `wearSlot`, `requiredLevel`, `requiredSkillID`, "
								+ "`armourBonus`, `weaponAimBonus`, `weaponPowerBonus`, `magicBonus`, `prayerBonus`, `basePrice`, `bankNoteID`, "
								+ "originalItemID FROM `" + Constants.GameServer.MYSQL_TABLE_PREFIX
								+ "itemdef` order by id asc");
				ResultSet result = statement.executeQuery()) {
			while (result.next()) {
				itemDefinitions.add(new ItemDefinition(result.getString("name"), result.getString("description"),
						result.getString("command"), result.getInt("isFemaleOnly") == 1,
						result.getInt("isMembersOnly") == 1, result.getInt("isStackable") == 1,
						result.getInt("isUntradable") == 1, result.getInt("isWearable") == 1,
						result.getInt("appearanceID"), result.getInt("wearableID"), result.getInt("wearSlot"),
						result.getInt("requiredLevel"), result.getInt("requiredSkillID"), result.getInt("armourBonus"),
						result.getInt("weaponAimBonus"), result.getInt("weaponPowerBonus"), result.getInt("magicBonus"),
						result.getInt("prayerBonus"), result.getInt("basePrice"), result.getInt("bankNoteID"),
						result.getInt("originalItemID")));
			}
		}
		return itemDefinitions;
	}

	private static ArrayList<GameObjectDef> loadGameObjectDefinitions() throws SQLException {
		ArrayList<GameObjectDef> gameObjects = new ArrayList<GameObjectDef>();
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection
						.prepareStatement("SELECT `name`, `description`, `command1`, `command2`, `type`, `width`, `height`, `ground_item_var`, `object_model`, `blocks_ranged` FROM `" + Constants.GameServer.MYSQL_TABLE_PREFIX + "objectdef`")) {
			try (ResultSet result = statement.executeQuery()) {
				while (result.next()) {
					GameObjectDef def = new GameObjectDef();
					def.name = result.getString("name");
					def.description = result.getString("description");
					def.command1 = result.getString("command1");
					def.command2 = result.getString("command2");
					def.type = result.getInt("type");
					def.width = result.getInt("width");
					def.height = result.getInt("height");
					def.groundItemVar = result.getInt("ground_item_var");
					def.objectModel = result.getString("object_model");
					def.blocksRanged = result.getInt("blocks_ranged") == 1 ? true : false;
					gameObjects.add(def);
				}
			}
		}
		return gameObjects;
	}

	public static ArrayList<DoorDef> loadDoorDefinitions() throws SQLException {
		ArrayList<DoorDef> doors = new ArrayList<DoorDef>();
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection
						.prepareStatement("SELECT `name`, `command1`, `command2`, `door_type`, `unknown`, `model_var1`, `model_var2`, `model_var3`, `blocks_ranged` FROM `" + Constants.GameServer.MYSQL_TABLE_PREFIX + "doordef`")) {
			try (ResultSet result = statement.executeQuery()) {
				while (result.next()) {
					DoorDef def = new DoorDef();
					def.name = result.getString("name");
					def.command1 = result.getString("command1");
					def.command2 = result.getString("command2");
					def.doorType = result.getInt("door_type");
					def.unknown = result.getInt("unknown");
					def.modelVar1 = result.getInt("model_var1");
					def.modelVar2 = result.getInt("model_var2");
					def.modelVar3 = result.getInt("model_var3");
					def.blocksRanged = result.getInt("blocks_ranged") == 1 ? true : false;
					doors.add(def);
				}
			}
		}
		return doors;
	}

	public static ArrayList<TileDef> loadTileDefinitions() throws SQLException {
		ArrayList<TileDef> tiles = new ArrayList<TileDef>();
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection
						.prepareStatement("SELECT `colour`, `unknown`, `object_type` FROM `"
								+ Constants.GameServer.MYSQL_TABLE_PREFIX + "tiledef`");
				ResultSet result = statement.executeQuery()) {
			while (result.next()) {
				TileDef def = new TileDef();
				def.colour = result.getInt("colour");
				def.unknown = result.getInt("unknown");
				def.objectType = result.getInt("object_type");
				tiles.add(def);
			}
		}
		return tiles;
	}

	public static void populateWorldSpawns(World world) throws SQLException {
		final long start = System.currentTimeMillis();
		
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection
						.prepareStatement("SELECT `x`, `y`, `id`, `direction`, `type` FROM `"
								+ Constants.GameServer.MYSQL_TABLE_PREFIX + "objects`");
				ResultSet result = statement.executeQuery()) {
			while (result.next()) {
				Point p = new Point(result.getInt("x"), result.getInt("y"));
				if (Formulae.isP2P(false, p.getX(), p.getY()) && !Constants.GameServer.MEMBER_WORLD) {
					continue;
				}
				GameObject obj = new GameObject(p, result.getInt("id"), result.getInt("direction"),
						result.getInt("type"));

				world.registerGameObject(obj);
			}
		} catch (SQLException e) {
			LOGGER.catching(e);
		}

		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection
						.prepareStatement("SELECT `id`, `startX`, `startY`, `minX`, `maxX`, `minY`, `maxY` FROM `"
								+ Constants.GameServer.MYSQL_TABLE_PREFIX + "npclocs`");
				ResultSet result = statement.executeQuery()) {
			while (result.next()) {
				NPCLoc n = new NPCLoc(result.getInt("id"), result.getInt("startX"), result.getInt("startY"),
						result.getInt("minX"), result.getInt("maxX"), result.getInt("minY"), result.getInt("maxY"));

				if (!Constants.GameServer.MEMBER_WORLD) {
					if (EntityHandler.getNpcDef(n.id).isMembers()) {
						continue;
					}
				}
				if (Formulae.isP2P(false, n) && !Constants.GameServer.MEMBER_WORLD) {
					n = null;
					continue;
				}
				/*
				 * if(!Point.inWilderness(n.startX, n.startY) &&
				 * EntityHandler.getNpcDef(n.id).isAttackable() && n.id != 192
				 * && n.id != 35 && n.id != 196 && n.id != 50 && n.id != 70 &&
				 * n.id != 136 && n.id != 37) { for(int i = 0; i < 1; i++)
				 * world.registerNpc(new Npc(n)); }
				 */
				world.registerNpc(new Npc(n));
			}
		} catch (SQLException e) {
			LOGGER.catching(e);
		}

		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection
						.prepareStatement("SELECT `id`, `x`, `y`, `amount`, `respawn` FROM `"
								+ Constants.GameServer.MYSQL_TABLE_PREFIX + "grounditems`");
				ResultSet result = statement.executeQuery()) {
			while (result.next()) {
				ItemLoc i = new ItemLoc(result.getInt("id"), result.getInt("x"), result.getInt("y"),
						result.getInt("amount"), result.getInt("respawn"));
				if (!Constants.GameServer.MEMBER_WORLD) {
					if (EntityHandler.getItemDef(i.id).isMembersOnly()) {
						continue;
					}
				}
				if (Formulae.isP2P(false, i) && !Constants.GameServer.MEMBER_WORLD) {
					i = null;
					continue;
				}

				world.registerItem(new GroundItem(i));
			}
		} catch (SQLException e) {
			LOGGER.catching(e);
		}
		LOGGER.info(((System.currentTimeMillis() - start) / 1000) + "s to load world spawns.");
		
	}
}
