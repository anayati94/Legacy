package com.legacy.server.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.legacy.server.Constants;
import com.legacy.server.login.LoginRequest;
import com.legacy.server.model.PlayerAppearance;
import com.legacy.server.model.Point;
import com.legacy.server.model.container.Bank;
import com.legacy.server.model.container.Inventory;
import com.legacy.server.model.container.Item;
import com.legacy.server.model.entity.player.Player;
import com.legacy.server.model.world.World;
import com.legacy.server.util.rsc.DataConversions;
import com.legacy.server.util.rsc.Formulae;
import com.legacy.server.util.rsc.LoginResponse;

public class DatabasePlayerLoader {

	/**
	 * The asynchronous logger.
	 */
	private static final Logger LOGGER = LogManager.getLogger();

	public boolean savePlayer(Player s) {
		if (!playerExists(s.getDatabaseID())) {
			LOGGER.error("ERROR SAVING: " + s.getUsername());
			return false;
		}

		try (Connection connection = DatabaseConnection.getDatabase().getConnection()) {
			updateLongs(Statements.save_DeleteBank, s.getDatabaseID());
			if (s.getBank().size() > 0) {
				try (PreparedStatement statement = connection.prepareStatement(Statements.save_AddBank)) {
					int slot = 0;
					for (Item item : s.getBank().getItems()) {
						statement.setInt(1, s.getDatabaseID());
						statement.setInt(2, item.getID());
						statement.setInt(3, item.getAmount());
						statement.setInt(4, slot++);
						statement.addBatch();
					}
					statement.executeBatch();
				}
			}
			updateLongs(Statements.save_DeleteInv, s.getDatabaseID());

			if (s.getInventory().size() > 0) {
				try (PreparedStatement statement = connection.prepareStatement(Statements.save_AddInvItem)) {
					int slot = 0;
					for (Item item : s.getInventory().getItems()) {
						statement.setInt(1, s.getDatabaseID());
						statement.setInt(2, item.getID());
						statement.setInt(3, item.getAmount());
						statement.setInt(4, (item.isWielded() ? 1 : 0));
						statement.setInt(5, slot++);
						statement.addBatch();
					}
					statement.executeBatch();
				}
			}

			updateLongs(Statements.save_DeleteQuests, s.getDatabaseID());
			if (s.getQuestStages().size() > 0) {
				try (PreparedStatement statement = connection.prepareStatement(Statements.save_AddQuest)) {
					Set<Integer> keys = s.getQuestStages().keySet();
					for (int id : keys) {
						statement.setInt(1, s.getDatabaseID());
						statement.setInt(2, id);
						statement.setInt(3, s.getQuestStage(id));
						statement.addBatch();
					}
					statement.executeBatch();
				}
			}

			updateLongs(Statements.save_DeleteCache, s.getDatabaseID());
			if (s.getCache().getCacheMap().size() > 0) {
				try (PreparedStatement statement = connection.prepareStatement("INSERT INTO `" + Statements.PREFIX
						+ "player_cache` (`playerID`, `type`, `key`, `value`) VALUES(?,?,?,?)")) {

					for (String key : s.getCache().getCacheMap().keySet()) {
						Object o = s.getCache().getCacheMap().get(key);
						if (o instanceof Integer) {
							statement.setInt(1, s.getDatabaseID());
							statement.setInt(2, 0);
							statement.setString(3, key);
							statement.setInt(4, (Integer) o);
							statement.addBatch();
						}
						if (o instanceof String) {
							statement.setInt(1, s.getDatabaseID());
							statement.setInt(2, 1);
							statement.setString(3, key);
							statement.setString(4, (String) o);
							statement.addBatch();

						}
						if (o instanceof Boolean) {
							statement.setInt(1, s.getDatabaseID());
							statement.setInt(2, 2);
							statement.setString(3, key);
							statement.setInt(4, ((Boolean) o) ? 1 : 0);
							statement.addBatch();
						}
						if (o instanceof Long) {
							statement.setInt(1, s.getDatabaseID());
							statement.setInt(2, 3);
							statement.setString(3, key);
							statement.setLong(4, ((Long) o));
							statement.addBatch();
						}
						statement.executeBatch();
					}
				}
			}

			try (PreparedStatement statement = connection.prepareStatement(Statements.save_UpdateBasicInfo)) {
				statement.setInt(1, s.getCombatLevel());
				statement.setInt(2, s.getSkills().getTotalLevel());
				statement.setInt(3, s.getX());
				statement.setInt(4, s.getY());
				statement.setInt(5, s.getFatigue());
				statement.setInt(6, s.getKills());
				statement.setInt(7, s.getDeaths());
				statement.setInt(8, s.getIronMan());
				statement.setInt(9, s.getIronManRestriction());
				statement.setInt(10, s.getHCIronmanDeath());
				statement.setInt(11, s.getQuestPoints());
				statement.setInt(12, s.getSettings().getAppearance().getHairColour());
				statement.setInt(13, s.getSettings().getAppearance().getTopColour());
				statement.setInt(14, s.getSettings().getAppearance().getTrouserColour());
				statement.setInt(15, s.getSettings().getAppearance().getSkinColour());
				statement.setInt(16, s.getSettings().getAppearance().getHead());
				statement.setInt(17, s.getSettings().getAppearance().getBody());
				statement.setInt(18, s.isMale() ? 1 : 0);
				statement.setLong(19, s.getSkullTime());
				statement.setLong(20, s.getChargeTime());
				statement.setInt(21, s.getCombatStyle());
				statement.setLong(22, s.getMuteExpires());
				statement.setLong(23, s.getBankSize());
				statement.setLong(24, s.getSkills().getTotalExperience());
				statement.setInt(25, s.isVeteran() ? 1 : 0);
				statement.setInt(26, s.getDatabaseID());
				statement.executeUpdate();
			}

			// PRIVACY SETTINGS
			setPrivacySettings(s.getSettings().getPrivacySettings(), s.getDatabaseID());

			// GAME SETTINGS
			setGameSettings(s.getSettings().getGameSettings(), s.getDatabaseID());

			try (PreparedStatement statement = connection.prepareStatement(Statements.updateExperience)) {
				statement.setInt(19, s.getDatabaseID());
				for (int index = 0; index < 18; index++)
					statement.setDouble(index + 1, s.getSkills().getExperience(index));
				statement.executeUpdate();
			}

			try (PreparedStatement statement = connection.prepareStatement(Statements.updateStats)) {
				statement.setInt(19, s.getDatabaseID());
				for (int index = 0; index < 18; index++)
					statement.setInt(index + 1, s.getSkills().getLevel(index));
				statement.executeUpdate();
			}
			return true;
		} catch (Exception e) {
			LOGGER.catching(e);
			return false;
		}
	}

	public void addFriend(int playerID, long friend, String friendName) {
		try (Connection connection = DatabaseConnection.getDatabase().getConnection();
				PreparedStatement statement = connection.prepareStatement(Statements.addFriend)) {
			statement.setInt(1, playerID);
			statement.setLong(2, friend);
			statement.setString(3, friendName);
			statement.executeUpdate();
		} catch (Exception e) {
			LOGGER.catching(e);
		}

	}

	public void removeFriend(int playerID, long friend) {
		try (Connection connection = DatabaseConnection.getDatabase().getConnection();
				PreparedStatement statement = connection.prepareStatement(Statements.removeFriend)) {
			statement.setInt(1, playerID);
			statement.setLong(2, friend);
			statement.executeUpdate();
		} catch (Exception e) {
			LOGGER.catching(e);
		}
	}

	public void addIgnore(int playerID, long friend) {
		try (Connection connection = DatabaseConnection.getDatabase().getConnection();
				PreparedStatement statement = connection.prepareStatement(Statements.addIgnore)) {
			statement.setInt(1, playerID);
			statement.setLong(2, friend);
			statement.executeUpdate();
		} catch (Exception e) {
			LOGGER.catching(e);
		}
	}

	public void removeIgnore(int playerID, long friend) {
		try (Connection connection = DatabaseConnection.getDatabase().getConnection();
				PreparedStatement statement = connection.prepareStatement(Statements.removeIgnore)) {
			statement.setInt(1, playerID);
			statement.setLong(2, friend);
			statement.executeUpdate();
		} catch (Exception e) {
			LOGGER.catching(e);
		}
	}

	public void chatBlock(int on, long user) {
		updateIntsLongs(Statements.chatBlock, new int[] { on }, new long[] { user });
	}

	public void privateBlock(int on, long user) {
		updateIntsLongs(Statements.privateBlock, new int[] { on }, new long[] { user });
	}

	public void tradeBlock(int on, long user) {
		updateIntsLongs(Statements.tradeBlock, new int[] { on }, new long[] { user });
	}

	public void duelBlock(int on, long user) {
		updateIntsLongs(Statements.duelBlock, new int[] { on }, new long[] { user });
	}

	public boolean playerExists(int user) {
		return hasNextFromInt(Statements.basicInfo, user);
	}

	private static final String[] gameSettings = { "cameraauto", "onemouse", "soundoff" };

	private static final String[] privacySettings = { "block_chat", "block_private", "block_trade", "block_duel" };

	public void setGameSettings(boolean settings[], int user) {
		for (int i = 0; i < settings.length; i++) {
			DatabaseConnection.getDatabase().executeUpdate("UPDATE `" + Statements.PREFIX + "players` SET " + gameSettings[i] + "="
					+ (settings[i] ? 1 : 0) + " WHERE id='" + user + "'");
		}
	}

	public void setPrivacySettings(boolean settings[], int user) {
		for (int i = 0; i < settings.length; i++) {
			DatabaseConnection.getDatabase().executeUpdate("UPDATE `" + Statements.PREFIX + "players` SET " + privacySettings[i] + "="
					+ (settings[i] ? 1 : 0) + " WHERE id='" + user + "'");
		}
	}

	public void setTeleportStones(int stones, int user) {
		DatabaseConnection.getDatabase().executeUpdate("UPDATE `users` SET teleport_stone=" + stones + " WHERE id='" + user + "'");
	}

	public Player loadPlayer(LoginRequest rq) {
		Player save = new Player(rq);
		try (Connection connection = DatabaseConnection.getDatabase().getConnection();
				PreparedStatement ps = connection.prepareStatement(Statements.playerData)) {
			addStringParametersToStatement(ps, save.getUsername());
			try (ResultSet result = ps.executeQuery()) {
				if (!result.next()) {
					return save;
				}
				save.setOwner(result.getInt("owner"));
				save.setDatabaseID(result.getInt("id"));
				save.setGroupID(result.getInt("group_id"));
				save.setSubscriptionExpires(result.getLong("sub_expires"));
				save.setPremiumExpires(result.getLong("platinum_expires"));
				save.setCombatSubExpires(result.getLong("combat_expires"));
				save.setCombatStyle((byte) result.getInt("combatstyle"));
				save.setLastLogin(result.getLong("login_date"));
				save.setLastIP(result.getString("login_ip"));
				save.setInitialLocation(new Point(result.getInt("x"), result.getInt("y")));

				save.setFatigue(result.getInt("fatigue"));
				save.setKills(result.getInt("kills"));
				save.setDeaths(result.getInt("deaths"));
				save.setIronMan(result.getInt("iron_man"));
				save.setIronManRestriction(result.getInt("iron_man_restriction"));
				save.setHCIronmanDeath(result.getInt("hc_ironman_death"));
				save.setQuestPoints(result.getShort("quest_points"));

				save.getSettings().setPrivacySetting(0, result.getInt("block_chat") == 1); // done
				save.getSettings().setPrivacySetting(1, result.getInt("block_private") == 1);
				save.getSettings().setPrivacySetting(2, result.getInt("block_trade") == 1);
				save.getSettings().setPrivacySetting(3, result.getInt("block_duel") == 1);

				save.getSettings().setGameSetting(0, result.getInt("cameraauto") == 1);
				save.getSettings().setGameSetting(1, result.getInt("onemouse") == 1);
				save.getSettings().setGameSetting(2, result.getInt("soundoff") == 1);

				save.setBankSize(result.getShort("bank_size"));
				save.setVeteran(result.getInt("veteran") == 1);

				PlayerAppearance pa = new PlayerAppearance(result.getInt("haircolour"), result.getInt("topcolour"),
						result.getInt("trousercolour"), result.getInt("skincolour"), result.getInt("headsprite"),
						result.getInt("bodysprite"));

				save.getSettings().setAppearance(pa);
				save.setMale(result.getInt("male") == 1);
				save.setWornItems(save.getSettings().getAppearance().getSprites());
				long skulled = result.getInt("skulled");
				if (skulled > 0) {
					save.addSkull(skulled);
				}

				long charged = result.getInt("charged");
				if (charged > 0) {
					save.addCharge(charged);
				}

				save.getSkills().loadExp(fetchExperience(save.getDatabaseID()));

				save.getSkills().loadLevels(fetchLevels(save.getDatabaseID()));
			}
		} catch (SQLException e) {
			LOGGER.catching(e);
		}

		try (Connection connection = DatabaseConnection.getDatabase().getConnection();
				PreparedStatement ps = connection.prepareStatement(Statements.playerInvItems)) {
			addIntegerParamsToStatement(ps, save.getDatabaseID());
			try (ResultSet result = ps.executeQuery()) {
				Inventory inv = new Inventory(save);
				while (result.next()) {
					Item item = new Item(result.getInt("id"), result.getInt("amount"));
					item.setWielded(result.getInt("wielded") == 1);
					inv.add(item, false);
					if (item.isWieldable() && result.getInt("wielded") == 1) {
						save.updateWornItems(item.getDef().getWieldPosition(), item.getDef().getAppearanceId());
						item.setWielded(true);
					}
				}
				save.setInventory(inv);
			}
		} catch (SQLException e) {
			LOGGER.catching(e);
		}

		try (Connection connection = DatabaseConnection.getDatabase().getConnection();
				PreparedStatement ps = connection.prepareStatement(Statements.playerBankItems)) {
			addIntegerParamsToStatement(ps, save.getDatabaseID());
			try (ResultSet result = ps.executeQuery()) {

				Bank bank = new Bank(save);
				while (result.next()) {
					bank.add(new Item(result.getInt("id"), result.getInt("amount")));
				}
				save.setBank(bank);
			}

		} catch (SQLException e) {
			LOGGER.catching(e);
		}

		try (Connection connection = DatabaseConnection.getDatabase().getConnection();
				PreparedStatement ps = connection.prepareStatement(Statements.playerFriends)) {
			addIntegerParamsToStatement(ps, save.getDatabaseID());

			try (ResultSet result = ps.executeQuery()) {
				save.getSocial().addFriends(longListFromResultSet(result, "friend"));
			}
		} catch (SQLException e) {
			LOGGER.catching(e);
		}

		try (Connection connection = DatabaseConnection.getDatabase().getConnection();
				PreparedStatement ps = connection.prepareStatement(Statements.playerIngored)) {
			addIntegerParamsToStatement(ps, save.getDatabaseID());

			try (ResultSet result = ps.executeQuery()) {
				save.getSocial().addIgnore(longListFromResultSet(result, "ignore"));
			}

		} catch (SQLException e) {
			LOGGER.catching(e);
		}

		try (Connection connection = DatabaseConnection.getDatabase().getConnection();
				PreparedStatement ps = connection.prepareStatement(Statements.playerQuests)) {
			addIntegerParamsToStatement(ps, save.getDatabaseID());

			try (ResultSet result = ps.executeQuery()) {
				while (result.next()) {
					save.setQuestStage(result.getInt("id"), result.getInt("stage"));
				}
			}

		} catch (SQLException e) {
			LOGGER.catching(e);
		}

		try (Connection connection = DatabaseConnection.getDatabase().getConnection();
				PreparedStatement ps = connection.prepareStatement(Statements.playerCache)) {
			addIntegerParamsToStatement(ps, save.getDatabaseID());

			try (ResultSet result = ps.executeQuery()) {
				while (result.next()) {
					int identifier = result.getInt("type");

					String key = result.getString("key");
					if (identifier == 0) {
						save.getCache().put(key, result.getInt("value"));
					}
					if (identifier == 1) {
						save.getCache().put(key, result.getString("value"));
					}
					if (identifier == 2) {
						save.getCache().put(key, result.getBoolean("value"));
					}
					if (identifier == 3) {
						save.getCache().put(key, result.getLong("value"));
					}
				}
			}

		} catch (SQLException e) {
			LOGGER.catching(e);
		}

		try (Connection connection = DatabaseConnection.getDatabase().getConnection();
				PreparedStatement ps = connection.prepareStatement(Statements.unreadMessages)) {
			addIntegerParamsToStatement(ps, save.getOwner());

			try (ResultSet result = ps.executeQuery()) {
				while (result.next()) {
					save.setUnreadMessages(result.getInt(1));
				}
			}
		} catch (SQLException e) {
			LOGGER.catching(e);
		}

		try (Connection connection = DatabaseConnection.getDatabase().getConnection();
				PreparedStatement ps = connection.prepareStatement(Statements.teleportStones)) {
			addIntegerParamsToStatement(ps, save.getOwner());

			try (ResultSet result = ps.executeQuery()) {
				while (result.next()) {
					save.setTeleportStones(result.getInt(1));
				}
			}
		} catch (SQLException e) {
			LOGGER.catching(e);
		}

		return save;
	}

	private void addStringParametersToStatement(PreparedStatement query, String... longA) throws SQLException {
		for (int i = 1; i <= longA.length; i++) {
			query.setString(i, longA[i - 1]);
		}
	}

	private void updateLongs(String statement, int... intA) {
		try (Connection connection = DatabaseConnection.getDatabase().getConnection();
				PreparedStatement prepared = connection.prepareStatement(statement)) {

			for (int i = 1; i <= intA.length; i++) {
				prepared.setInt(i, intA[i - 1]);
			}

			prepared.executeUpdate();
		} catch (SQLException e) {
			LOGGER.catching(e);
		}
	}

	private void updateIntsLongs(String statement, int[] intA, long[] longA) {
		try (Connection connection = DatabaseConnection.getDatabase().getConnection();
				PreparedStatement prepared = connection.prepareStatement(statement)) {
			for (int i = 1; i <= intA.length; i++) {
				prepared.setInt(i, intA[i - 1]);
			}
			int offset = intA.length + 1;
			for (int i = 0; i < longA.length; i++) {
				prepared.setLong(i + offset, longA[i]);
			}

			prepared.executeUpdate();
		} catch (SQLException e) {
			LOGGER.catching(e);
		}
	}

	public String banPlayer(String user, int time) {
		String query;
		String replyMessage;
		if (time == -1) {
			query = "UPDATE `" + Statements.PREFIX + "players` SET `banned`='" + time + "' WHERE `username` LIKE '"
					+ user + "'";
			replyMessage = user + " has been banned permanently";
		} else if (time == 0) {
			query = "UPDATE `" + Statements.PREFIX + "players` SET `banned`='" + time + "' WHERE `username` LIKE '"
					+ user + "'";
			replyMessage = user + " has been unbanned.";
		} else {
			query = "UPDATE `" + Statements.PREFIX + "players` SET `banned`='"
					+ (System.currentTimeMillis() + (time * 60000))
					+ "', offences = offences + 1 WHERE `username` LIKE '" + user + "'";
			replyMessage = user + " has been banned for " + time + " minutes";
		}
		int check = DatabaseConnection.getDatabase().executeUpdate(query);
		if(check == 0) {
			return "There is not an account by that username";
		} else {
			return replyMessage;
		}
	}

	public void playerOnlineFlagQuery(int playerID, String loginIP, boolean online) {
		try (Connection connection = DatabaseConnection.getDatabase().getConnection();
				PreparedStatement ps = connection.prepareStatement("UPDATE `" + Constants.GameServer.MYSQL_TABLE_PREFIX + "players` SET `online`=?, `login_date`=?, `login_ip`=? WHERE `id`=?")) {
			int id = 1;
			ps.setInt(id++, online ? 1 : 0);
			ps.setLong(id++, System.currentTimeMillis() / 1000);
			ps.setString(id++, loginIP);
			ps.setInt(id++, playerID);

			ps.executeUpdate();
		} catch (SQLException e) {
			LOGGER.catching(e);
		}
	}

	public void playerOnlineFlagQuery(int playerID, boolean online) {
		try (Connection connection = DatabaseConnection.getDatabase().getConnection();
				PreparedStatement ps = connection.prepareStatement("UPDATE `" + Constants.GameServer.MYSQL_TABLE_PREFIX + "players` SET `online`=? WHERE `id`=?")) {
			int id = 1;
			ps.setInt(id++, online ? 1 : 0);
			ps.setInt(id++, playerID);

			ps.executeUpdate();
		} catch (SQLException e) {
			LOGGER.catching(e);
		}
	}

	private int[] fetchLevels(int playerID) {
		try (Connection connection = DatabaseConnection.getDatabase().getConnection();
				PreparedStatement ps = connection.prepareStatement(Statements.playerCurExp)) {
			addIntegerParamsToStatement(ps, playerID);

			try (ResultSet result = ps.executeQuery()) {
				result.next();
				int[] data = new int[Formulae.statArray.length];
				for (int i = 0; i < data.length; i++) {
					try {
						data[i] = result.getInt("cur_" + Formulae.statArray[i]);
					} catch (SQLException e) {
						LOGGER.catching(e);
						return null;
					}
				}
				return data;
			}
		} catch (SQLException e) {
			LOGGER.catching(e);
			return null;
		}
	}

	private double[] fetchExperience(int playerID) {
		double[] data = new double[Formulae.statArray.length];
		try (Connection connection = DatabaseConnection.getDatabase().getConnection();
				PreparedStatement statement = connection.prepareStatement(Statements.playerExp)) {
			statement.setInt(1, playerID);

			try (ResultSet result = statement.executeQuery()) {
				result.next();

				for (int i = 0; i < data.length; i++) {
					try {
						data[i] = result.getDouble("exp_" + Formulae.statArray[i]);
					} catch (SQLException e) {
						LOGGER.catching(e);
						return null;
					}
				}
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
			return null;
		}
		return data;
	}

	private List<Long> longListFromResultSet(ResultSet result, String param) throws SQLException {
		List<Long> list = new ArrayList<Long>();

		while (result.next()) {
			list.add(result.getLong(param));
		}

		return list;
	}

	private void addIntegerParamsToStatement(PreparedStatement statement, int... longA) throws SQLException {
		for (int i = 1; i <= longA.length; i++) {
			statement.setInt(i, longA[i - 1]);
		}
	}

	private boolean hasNextFromInt(String statement, int... intA) {
		try (Connection connection = DatabaseConnection.getDatabase().getConnection();
				PreparedStatement prepared = connection.prepareStatement(statement)) {

			for (int i = 1; i <= intA.length; i++) {
				prepared.setInt(i, intA[i - 1]);
			}

			try (ResultSet rs = prepared.executeQuery()) {
				return rs.next();
			}
		} catch (SQLException e) {
			LOGGER.catching(e);
			return false;
		}
	}

	public class Statements {
		private static final String PREFIX = "rscl_";

		private static final String unreadMessages = "SELECT COUNT(*) FROM `messages` WHERE showed=0 AND show_message=1 AND owner=?";

		private static final String teleportStones = "SELECT `teleport_stone` FROM `users` WHERE id=?";

		private static final String addFriend = "INSERT INTO `" + PREFIX
				+ "friends`(`playerID`, `friend`, `friendName`) VALUES(?, ?, ?)";

		private static final String removeFriend = "DELETE FROM `" + PREFIX
				+ "friends` WHERE `playerID` LIKE ? AND `friend` LIKE ?";

		private static final String addIgnore = "INSERT INTO `" + PREFIX
				+ "ignores`(`playerID`, `ignore`) VALUES(?, ?)";

		private static final String removeIgnore = "DELETE FROM `" + PREFIX
				+ "ignores` WHERE `playerID` LIKE ? AND `ignore` LIKE ?";

		private static final String chatBlock = "UPDATE `" + PREFIX + "players` SET block_chat=? WHERE playerID=?";

		private static final String privateBlock = "UPDATE `" + PREFIX + "players` SET block_private=? WHERE id=?";

		private static final String tradeBlock = "UPDATE `" + PREFIX + "id` SET block_trade=? WHERE playerID=?";

		private static final String duelBlock = "UPDATE `" + PREFIX + "players` SET block_duel=? WHERE playerID=?";

		private static final String basicInfo = "SELECT 1 FROM `" + PREFIX + "players` WHERE `id` = ?";

		private static final String playerData = "SELECT `owner`, `id`, `group_id`, `sub_expires`, `platinum_expires`, `combat_expires`,"
				+ "`combatstyle`, `login_date`, `login_ip`, `x`, `y`, `fatigue`, `kills`,"
				+ "`deaths`, `iron_man`, `iron_man_restriction`,`hc_ironman_death`, `quest_points`, `block_chat`, `block_private`,"
				+ "`block_trade`, `block_duel`, `cameraauto`," + "`onemouse`, `soundoff`, `haircolour`, `topcolour`,"
				+ "`trousercolour`, `skincolour`, `headsprite`, `bodysprite`, `male`,"
				+ "`skulled`, `charged`, `pass`, `salt`, `banned`, `bank_size`, `veteran` FROM `" + PREFIX
				+ "players` WHERE `username`=?";

		private static final String playerExp = "SELECT `exp_attack`, `exp_defense`, `exp_strength`, "
				+ "`exp_hits`, `exp_ranged`, `exp_prayer`, `exp_magic`, `exp_cooking`, `exp_woodcut`,"
				+ "`exp_fletching`, `exp_fishing`, `exp_firemaking`, `exp_crafting`, `exp_smithing`,"
				+ "`exp_mining`, `exp_herblaw`, `exp_agility`, `exp_thieving` FROM `" + PREFIX
				+ "experience` WHERE `playerID`=?";

		private static final String playerCurExp = "SELECT `cur_attack`, `cur_defense`, `cur_strength`,"
				+ "`cur_hits`, `cur_ranged`, `cur_prayer`, `cur_magic`, `cur_cooking`, `cur_woodcut`,"
				+ "`cur_fletching`, `cur_fishing`, `cur_firemaking`, `cur_crafting`, `cur_smithing`,"
				+ "`cur_mining`, `cur_herblaw`, `cur_agility`, `cur_thieving` FROM `" + PREFIX
				+ "curstats` WHERE `playerID`=?";

		private static final String playerInvItems = "SELECT `id`,`amount`,`wielded` FROM `" + PREFIX
				+ "invitems` WHERE `playerID`=? ORDER BY `slot` ASC";

		private static final String playerBankItems = "SELECT `id`, `amount` FROM `" + PREFIX
				+ "bank` WHERE `playerID`=? ORDER BY `slot` ASC";

		private static final String playerFriends = "SELECT `friend` FROM `" + PREFIX + "friends` WHERE `playerID`=?";

		private static final String playerIngored = "SELECT `ignore` FROM `" + PREFIX + "ignores` WHERE `playerID`=?";

		private static final String playerQuests = "SELECT `id`, `stage` FROM `" + PREFIX
				+ "quests` WHERE `playerID`=?";

		//private static final String playerAchievements = "SELECT `id`, `status` FROM `" + PREFIX
		//	+ "achievement_status` WHERE `playerID`=?";

		private static final String playerCache = "SELECT `type`, `key`, `value` FROM `" + PREFIX
				+ "player_cache` WHERE `playerID`=?";

		private static final String save_DeleteBank = "DELETE FROM `" + PREFIX + "bank` WHERE `playerID`=?";

		private static final String save_AddBank = "INSERT INTO `" + PREFIX
				+ "bank`(`playerID`, `id`, `amount`, `slot`) VALUES(?, ?, ?, ?)";

		private static final String save_DeleteInv = "DELETE FROM `" + PREFIX + "invitems` WHERE `playerID`=?";

		private static final String save_AddInvItem = "INSERT INTO `" + PREFIX
				+ "invitems`(`playerID`, `id`, `amount`, `wielded`, `slot`) VALUES(?, ?, ?, ?, ?)";

		private static final String save_UpdateBasicInfo = "UPDATE `" + PREFIX
				+ "players` SET `combat`=?, skill_total=?, `x`=?, `y`=?, `fatigue`=?, `kills`=?, `deaths`=?, `iron_man`=?, `iron_man_restriction`=?, `hc_ironman_death`=?, `quest_points`=?, `haircolour`=?, `topcolour`=?, `trousercolour`=?, `skincolour`=?, `headsprite`=?, `bodysprite`=?, `male`=?, `skulled`=?, `charged`=?, `combatstyle`=?, `muted`=?, `bank_size`=?, `total_experience`=?, `veteran`=? WHERE `id`=?";

		private static final String save_DeleteQuests = "DELETE FROM `" + PREFIX + "quests` WHERE `playerID`=?";

		//private static final String save_DeleteAchievements = "DELETE FROM `" + PREFIX
		//	+ "achievement_status` WHERE `playerID`=?";

		private static final String save_DeleteCache = "DELETE FROM `" + PREFIX + "player_cache` WHERE `playerID`=?";

		private static final String save_AddQuest = "INSERT INTO `" + PREFIX
				+ "quests` (`playerID`, `id`, `stage`) VALUES(?, ?, ?)";

		//private static final String save_AddAchievement = "INSERT INTO `" + PREFIX
		//	+ "achievement_status` (`playerID`, `id`, `status`) VALUES(?, ?, ?)";

		private static final String updateExperience = "UPDATE `" + PREFIX
				+ "experience` SET `exp_attack`=?, `exp_defense`=?, "
				+ "`exp_strength`=?, `exp_hits`=?, `exp_ranged`=?, `exp_prayer`=?, `exp_magic`=?, `exp_cooking`=?, `exp_woodcut`=?, "
				+ "`exp_fletching`=?, `exp_fishing`=?, `exp_firemaking`=?, `exp_crafting`=?, `exp_smithing`=?, `exp_mining`=?, "
				+ "`exp_herblaw`=?, `exp_agility`=?, `exp_thieving`=? WHERE `playerID`=?";

		private static final String updateStats = "UPDATE `" + PREFIX
				+ "curstats` SET `cur_attack`=?, `cur_defense`=?, "
				+ "`cur_strength`=?, `cur_hits`=?, `cur_ranged`=?, `cur_prayer`=?, `cur_magic`=?, `cur_cooking`=?, `cur_woodcut`=?, "
				+ "`cur_fletching`=?, `cur_fishing`=?, `cur_firemaking`=?, `cur_crafting`=?, `cur_smithing`=?, `cur_mining`=?, "
				+ "`cur_herblaw`=?, `cur_agility`=?, `cur_thieving`=? WHERE `playerID`=?";

		private static final String playerLoginData = "SELECT `pass`, `salt`, `banned` FROM `" + PREFIX
				+ "players` WHERE `username`=?";
	}

	public byte validateLogin(LoginRequest request) {
		try (Connection connection = DatabaseConnection.getDatabase().getConnection();
				PreparedStatement statement = connection.prepareStatement(Statements.playerLoginData)) {
			statement.setString(1, request.getUsername());

			try (ResultSet playerSet = statement.executeQuery()) {
				if (!playerSet.next()) {
					return (byte) LoginResponse.INVALID_CREDENTIALS;
				}
				String hashedPassword = DataConversions.hmac("SHA512",
						playerSet.getString("salt") + request.getPassword(), Constants.GameServer.HMAC_PRIVATE_KEY);
				if (!hashedPassword.equals(playerSet.getString("pass"))) {
					return (byte) LoginResponse.INVALID_CREDENTIALS;
				}
				if (World.getWorld().getPlayer(request.getUsernameHash()) != null) {
					return (byte) LoginResponse.ACCOUNT_LOGGEDIN;
				}
				long banExpires = playerSet.getLong("banned");
				if (banExpires == -1) {
					return (byte) LoginResponse.ACCOUNT_PERM_DISABLED;
				}
				double timeBanLeft = (double) (banExpires - System.currentTimeMillis());
				if (timeBanLeft >= 1) {
					return (byte) LoginResponse.ACCOUNT_TEMP_DISABLED;
				}
				try (Connection gameLoggingCon = GameLogging.singleton().getConnection(); 
						PreparedStatement ps = gameLoggingCon.prepareStatement("SELECT 1 FROM rscl_mac_log WHERE `mac`='" + request.getMacAddress() + "'")) {
					try (ResultSet set = ps.executeQuery()) {
						if (set.next()) {
							LOGGER.info(request.getIpAddress() + " - " + request.getMacAddress() + " - " + request.getUsername() + " tried to login with an active mac ban.");
							return (byte) LoginResponse.NONE_OF_YOUR_CHARACTERS_CAN_LOGIN;
						}
					} catch (SQLException e) {
						LOGGER.catching(e);
					}
				}
				
			}
		} catch (SQLException e) {
			LOGGER.catching(e);
		}
		return (byte) LoginResponse.LOGIN_SUCCESSFUL;
	}

}
