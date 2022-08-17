package com.legacy.server.plugins.commands;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.legacy.server.Constants;
import com.legacy.server.Server;
import com.legacy.server.event.SingleEvent;
import com.legacy.server.external.EntityHandler;
import com.legacy.server.model.Point;
import com.legacy.server.model.entity.npc.Npc;
import com.legacy.server.model.entity.player.Player;
import com.legacy.server.model.world.World;
import com.legacy.server.net.rsc.ActionSender;
import com.legacy.server.plugins.listeners.action.CommandListener;
import com.legacy.server.sql.DatabaseConnection;
import com.legacy.server.sql.GameLogging;
import com.legacy.server.sql.query.logs.StaffLog;
import com.legacy.server.util.rsc.DataConversions;
import com.legacy.server.util.rsc.MessageType;

public class Event implements CommandListener {
	
	/**
	 * The asynchronous logger.
	 */
	private static final Logger LOGGER = LogManager.getLogger();

	public static final World world = World.getWorld();
	
	private static final String[] towns = { "varrock", "falador", "draynor", "portsarim", "karamja", "alkharid",
			"lumbridge", "edgeville", "castle", "taverly", "clubhouse", "seers", "barbarian", "rimmington", "catherby",
			"ardougne", "yanille", "lostcity", "gnome", "shilovillage", "tutorial", "modroom" };

	private static final Point[] townLocations = { Point.location(122, 509), Point.location(304, 542),
			Point.location(214, 632), Point.location(269, 643), Point.location(370, 685), Point.location(89, 693),
			Point.location(120, 648), Point.location(217, 449), Point.location(270, 352), Point.location(373, 498),
			Point.location(653, 491), Point.location(501, 450), Point.location(233, 513), Point.location(325, 663),
			Point.location(440, 501), Point.location(549, 589), Point.location(583, 747), Point.location(127, 3518),
			Point.location(703, 527), Point.location(400, 850), Point.location(217, 740), Point.location(75, 1641) };

	private void sendInvalidArguments(Player p, String... strings) {
		StringBuilder sb = new StringBuilder(COMMAND_PREFIX + "Invalid arguments @red@Syntax: @whi@");

		for (int i = 0; i < strings.length; i++) {
			sb.append(i == 0 ? strings[i].toUpperCase() : strings[i]).append(i == (strings.length - 1) ? "" : " ");
		}
		p.message(sb.toString());
	}

	private static final String COMMAND_PREFIX = "@red@SERVER: @whi@";

	@Override
	public void onCommand(String command, String[] args, Player player) {
		if (!player.isMod()) {
			return;
		}
		if (command.equals("stopevent")) {
			World.EVENT_X = -1;
			World.EVENT_Y = -1;
			World.EVENT = false;
			World.EVENT_COMBAT_MIN = -1;
			World.EVENT_COMBAT_MAX = -1;
			player.message("Event disabled");
			GameLogging.addQuery(new StaffLog(player, 8, "Stopped an ongoing event"));
		}
		if (command.equals("setevent")) {
			int x = Integer.parseInt(args[0]);
			int y = Integer.parseInt(args[1]);
			int cmin = Integer.parseInt(args[2]);
			int cmax = Integer.parseInt(args[3]);

			World.EVENT_X = x;
			World.EVENT_Y = y;
			World.EVENT = true;
			World.EVENT_COMBAT_MIN = cmin;
			World.EVENT_COMBAT_MAX = cmax;
			player.message("Event enabled: " + x + ", " + y + ", Combat level range: " + World.EVENT_COMBAT_MIN + " - "
					+ World.EVENT_COMBAT_MAX + "");
			GameLogging.addQuery(new StaffLog(player, 9, "Created event at: (" + x + ", " + y + ") cb-min: "
					+ World.EVENT_COMBAT_MIN + " cb-max: " + World.EVENT_COMBAT_MAX + ""));
		}
		if (command.equalsIgnoreCase("town")) {
			try {
				String town = args[0];
				if (town != null) {
					for (int i = 0; i < towns.length; i++)
						if (town.equalsIgnoreCase(towns[i])) {
							GameLogging.addQuery(new StaffLog(player, 17,
									"Teleported to: " + town + " " + townLocations[i].toString()));
							player.teleport(townLocations[i].getX(), townLocations[i].getY(), false);
							break;
						}
				}
			} catch (Exception e) {
				LOGGER.catching(e);
			}
		}
		if (command.equals("goto") || command.equals("summon")) {
			boolean summon = command.equals("summon");

			if (args.length != 1) {
				sendInvalidArguments(player, summon ? "summon" : "goto", "name");
				return;
			}
			long usernameHash = DataConversions.usernameToHash(args[0]);
			Player affectedPlayer = world.getPlayer(usernameHash);

			if (affectedPlayer != null) {
				if (summon) {
					GameLogging.addQuery(new StaffLog(player, 2, affectedPlayer));
					affectedPlayer.teleport(player.getX(), player.getY(), true);
				} else {
					GameLogging.addQuery(new StaffLog(player, 3, affectedPlayer));
					player.teleport(affectedPlayer.getX(), affectedPlayer.getY(), false);
				}
			} else {
				player.message(COMMAND_PREFIX + "Invalid player");
				return;
			}
		}
		
		if (command.equals("send")) {
			if (args.length != 3) {
				player.message("Invalid args. Syntax: SEND playername x y");
				return;
			}
			long usernameHash = DataConversions.usernameToHash(args[0]);
			Player p = world.getPlayer(usernameHash);
			int x = Integer.parseInt(args[1]);
			int y = Integer.parseInt(args[2]);
			if (world.withinWorld(x, y) && p != null) {
				p.message("You were teleported from " + p.getLocation().toString() + " to (" + x + ", " + y + ")");
				player.message("You teleported " + p.getUsername() + " from " + p.getLocation().toString() + " to (" + x
						+ ", " + y + ")");
				GameLogging.addQuery(new StaffLog(player, 16, p, p.getUsername() + " was sent from: "
						+ p.getLocation().toString() + " to (" + x + ", " + y + ")"));
				p.teleport(x, y, false);
			} else {
				player.message("Invalid coordinates or player!");
			}
			return;
		}
		
		if (command.equals("teleport")) {
			if (args.length != 2) {
				player.message("Invalid args. Syntax: TELEPORT x y");
				return;
			}
			int x = Integer.parseInt(args[0]);
			int y = Integer.parseInt(args[1]);
			if (world.withinWorld(x, y)) {
				GameLogging.addQuery(new StaffLog(player, 15,
						"From: " + player.getLocation().toString() + " to (" + x + ", " + y + ")"));
				player.teleport(x, y, true);
			} else {
				player.message("Invalid coordinates!");
			}
		}
		
		if (command.equals("check")) {
			if (args.length < 1) {
				sendInvalidArguments(player, "check", "name");
				return;
			}
			long hash = DataConversions.usernameToHash(args[0]);
			String username = DataConversions.hashToUsername(hash);
			String currentIp = null;
			Player target = World.getWorld().getPlayer(hash);
			if (target == null) {
				player.message(
						COMMAND_PREFIX + "No online character found named '" + args[0] + "'.. checking database..");
				try (Connection connection = DatabaseConnection.getDatabase().getConnection();
						PreparedStatement statement = connection.prepareStatement("SELECT * FROM `"
								+ Constants.GameServer.MYSQL_TABLE_PREFIX + "players` WHERE `username`=?")) {
					statement.setString(1, username);
					try(ResultSet result = statement.executeQuery()) {
						if (!result.next()) {
							player.message(COMMAND_PREFIX + "Error character not found in MySQL");
							return;
						}
						currentIp = result.getString("login_ip");
					}
					player.message(COMMAND_PREFIX + "Found character '" + args[0] + "' with IP: " + currentIp
							+ ", fetching other characters..");
				} catch (SQLException e) {
					e.printStackTrace();
					player.message(COMMAND_PREFIX + "A MySQL error has occured! " + e.getMessage());
					return;
				}
			} else {
				currentIp = target.getCurrentIP();
			}

			if (currentIp == null) {
				player.message(COMMAND_PREFIX + "An unknown error has occured!");
				return;
			}

			try (Connection connection = DatabaseConnection.getDatabase().getConnection();
					PreparedStatement statement = connection.prepareStatement("SELECT `username` FROM `"
							+ Constants.GameServer.MYSQL_TABLE_PREFIX + "players` WHERE `login_ip` LIKE ?")) {
				statement.setString(1, currentIp);
				try(ResultSet result = statement.executeQuery()) {

					List<String> names = new ArrayList<>();
					while (result.next()) {
						names.add(result.getString("username"));
					}
					StringBuilder builder = new StringBuilder("@red@").append(args[0].toUpperCase())
							.append(" @whi@currently has ").append(names.size() > 0 ? "@gre@" : "@red@")
							.append(names.size()).append(" @whi@registered characters.");

					if (names.size() > 0) {
						builder.append(" % % They are: ");
					}
					for (int i = 0; i < names.size(); i++) {

						builder.append("@yel@")
						.append((World.getWorld().getPlayer(DataConversions.usernameToHash(names.get(i))) != null
						? "@gre@" : "@red@") + names.get(i));

						if (i != names.size() - 1) {
							builder.append("@whi@, ");
						}
					}
					ActionSender.sendBox(player, builder.toString(), names.size() > 10);
				}
				GameLogging.addQuery(new StaffLog(player, 18, target));
			} catch (SQLException e) {
				player.message(COMMAND_PREFIX + "A MySQL error has occured! " + e.getMessage());
			}
		}
		
		if (command.equals("say")) {
			String newStr = "";

			for (int i = 0; i < args.length; i++) {
				newStr += args[i] + " ";
			}
			GameLogging.addQuery(new StaffLog(player, 13, newStr.toString()));
			newStr = player.getRankHeader() + player.getUsername() + ": @whi@" + newStr;
			for (Player p : World.getWorld().getPlayers()) {
				ActionSender.sendMessage(p, player, 1, MessageType.GLOBAL_CHAT, newStr, player.getIcon());
			}
		}
		
		if (command.equals("spawnnpc")) {
			if (args.length != 3) {
				player.message("Wrong syntax. ::spawnnpc <id> <radius> (time in minutes)");
				return;
			}
			int id = Integer.parseInt(args[0]);
			int radius = Integer.parseInt(args[1]);
			int time = Integer.parseInt(args[2]);
			if (EntityHandler.getNpcDef(id) != null) {
				player.message("[DEV]: You have spawned " + EntityHandler.getNpcDef(id).getName() + ", radius: "
						+ radius + " for " + time + " minutes");
				final Npc n = new Npc(id, player.getX(), player.getY(), player.getX() - radius, player.getX() + radius,
						player.getY() - radius, player.getY() + radius);
				n.setShouldRespawn(false);
				World.getWorld().registerNpc(n);
				Server.getServer().getEventHandler().add(new SingleEvent(null, time * 60000) {
					@Override
					public void action() {
						n.remove();
					}
				});
			} else {
				player.message("Invalid spawn npc id");
			}
		}
	}
}
