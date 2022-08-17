package com.legacy.server.plugins.commands;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.legacy.server.Constants;
import com.legacy.server.Server;
import com.legacy.server.model.entity.player.Player;
import com.legacy.server.model.world.World;
import com.legacy.server.net.rsc.ActionSender;
import com.legacy.server.plugins.listeners.action.CommandListener;
import com.legacy.server.sql.DatabaseConnection;
import com.legacy.server.sql.GameLogging;
import com.legacy.server.sql.query.logs.BanLog;
import com.legacy.server.sql.query.logs.StaffLog;
import com.legacy.server.util.rsc.DataConversions;

public final class Moderator implements CommandListener {
	
	public static final World world = World.getWorld();

	private static final String COMMAND_PREFIX = "@red@SERVER: @whi@";

	@Override
	public void onCommand(String command, String[] args, Player player) {
		if (!player.isMod() || player.isEventMod()) {
			return;
		}
		if (command.equals("resetq")) {
			final Player scrn = World.getWorld().getPlayer(DataConversions.usernameToHash(args[0]));
			if (scrn != null && args.length == 3) {
				int quest = Integer.parseInt(args[1]);
				int stage = Integer.parseInt(args[2]);

				scrn.updateQuestStage(quest, stage);
				player.message("You have changed " + scrn.getUsername() + "'s QuestID: " + quest + " to Stage: " + stage
						+ ".");
			} else {
				player.message("User is null or you didn't type in all the 3 arguments");
				player.message("::resetq <playername>, <questid>, <stage>");
			}
		}
		if (command.equals("gmute")) {
			if (args.length != 2) {
				player.message("Wrong syntax. ::mute <name> <time in minutes> (-1 for permanent)");
				return;
			}
			final Player playerToMute = World.getWorld().getPlayer(DataConversions.usernameToHash(args[0]));
			if (playerToMute != null) {
				int minutes = Integer.parseInt(args[1]);
				if (minutes == -1) {
					player.message("You have given " + playerToMute.getUsername() + " a permanent mute from ::g chat.");
					playerToMute.message("You have received a permanent mute from (::g) chat.");
					playerToMute.getCache().store("global_mute", -1);
				} else {
					player.message("You have given " + playerToMute.getUsername() + " a " + minutes
							+ " minute mute from ::g chat.");
					playerToMute.message("You have received a " + minutes + " minute mute in (::g) chat.");
					playerToMute.getCache().store("global_mute", (System.currentTimeMillis() + (minutes * 60000)));
				}
				GameLogging.addQuery(new StaffLog(player, 0, playerToMute,
						playerToMute.getUsername() + " was given a " + (minutes == -1 ? "permanent mute"
								: " temporary mute for " + minutes + " minutes in (::g) chat.")));
			} else {
				player.message("User is offline...");
			}
		}
		if (command.equals("mute")) {
			if (args.length != 2) {
				player.message("Wrong syntax. ::mute <name> <time in minutes> (-1 for permanent)");
				return;
			}
			final Player playerToMute = World.getWorld().getPlayer(DataConversions.usernameToHash(args[0]));
			if (playerToMute != null) {
				int minutes = Integer.parseInt(args[1]);
				if (minutes == -1) {
					player.message("You have given " + playerToMute.getUsername() + " a permanent mute.");
					playerToMute.message("You have received a permanent mute. Appeal on forums if you wish.");
					playerToMute.setMuteExpires(-1);
				} else {
					player.message("You have given " + playerToMute.getUsername() + " a " + minutes + " minute mute.");
					playerToMute
					.message("You have received a " + minutes + " minute mute. Appeal on forums if you wish.");
					playerToMute.setMuteExpires((System.currentTimeMillis() + (minutes * 60000)));
				}
				GameLogging.addQuery(new StaffLog(player, 0, playerToMute, playerToMute.getUsername() + " was given a "
						+ (minutes == -1 ? "permanent mute" : " temporary mute for " + minutes + " minutes")));
			} else {
				player.message("User must be online to be able to mute.");
			}
		}
		if (command.equals("blink")) {
			player.setAttribute("blink", !player.getAttribute("blink", false));
			player.message("Your blink status is now " + player.getAttribute("blink", false));
			GameLogging.addQuery(new StaffLog(player, 10, "Blink was set - " + player.getAttribute("blink", false)));

		}
		if (command.equals("tban")) {
			if (args.length < 3) {
				player.message("Wrong syntax. ::tban <name> <time in minutes> <reason>");
				return;
			}
			long user = DataConversions.usernameToHash(args[0]);
			String reason = "";
			for(String reasons : args) {
				if(args[0] != reasons && args[1] != reasons)
					reason += reasons.replaceFirst(" ", "") + " ";
			}
			String username = DataConversions.hashToUsername(user);
			int time = Integer.parseInt(args[1]);
			Player bannedPlayer = World.getWorld().getPlayer(user);
			if ((time == -1 || time == 0) && !player.isAdmin()) {
				return;
			}
			
			player.message(Server.getPlayerDataProcessor().getDatabase().banPlayer(username, time));
			if (bannedPlayer != null) {
				bannedPlayer.unregister(true, "Banned by " + player.getUsername() + " for " + time + " minutes");
				GameLogging.addQuery(new BanLog(bannedPlayer.getDatabaseID(), bannedPlayer.getCurrentIP(), bannedPlayer.getUID(), bannedPlayer.getMacAddress(), 1, bannedPlayer.getUsername(), player.getUsername(), reason, ((System.currentTimeMillis() / 1000) + (time * 60))));
			} else {
				try (Connection connection = DatabaseConnection.getDatabase().getConnection();
						PreparedStatement statement = connection.prepareStatement("SELECT `id` FROM `"
								+ Constants.GameServer.MYSQL_TABLE_PREFIX + "players` WHERE `username`=?")) {
					statement.setString(1, DataConversions.hashToUsername(user));
					try(ResultSet result = statement.executeQuery()) {
						if (!result.next()) {
							player.message("Error character not found in MySQL");
							return;
						}
						try (Connection gameLogCon = GameLogging.singleton().getConnection();
								PreparedStatement statement2 = gameLogCon.prepareStatement("SELECT `playerID`, `ip`, `uid`, `mac` FROM `"
										+ Constants.GameServer.MYSQL_TABLE_PREFIX + "logins` WHERE `playerID`=? ORDER BY `dbid` DESC LIMIT 1")) {
							statement2.setInt(1, result.getInt("id"));
							try(ResultSet result2 = statement2.executeQuery()) {
								if (!result2.next()) {
									player.message("Attention - no login records - inserting log without (UID, MAC, IP)");
									GameLogging.addQuery(new BanLog(result.getInt("id"), null, 0, null, 1, DataConversions.hashToUsername(user), player.getUsername(), reason, ((System.currentTimeMillis() / 1000) + (time * 60))));
									return;
								}
								GameLogging.addQuery(new BanLog(result.getInt("id"), result2.getString("ip"), result2.getLong("uid"), result2.getString("mac"), 1, DataConversions.hashToUsername(user), player.getUsername(), reason, ((System.currentTimeMillis() / 1000) + (time * 60))));
							}
						} catch (SQLException e) {
							e.printStackTrace();
							player.message("A MySQL error has occured! " + e.getMessage());
							return;
						}
					}
				} catch (SQLException e) {
					e.printStackTrace();
					player.message("A MySQL error has occured! " + e.getMessage());
					return;
				}
			}
		}
		if (command.equals("pinfo")) 
		{
			if (args.length != 1) 
			{
				player.message("Invalid args. Syntax: ::pinfo [name]");
				return;
			}
			
			Player p = World.getWorld().getPlayer(DataConversions.usernameToHash(args[0]));
			
			if (p != null) 
			{
				ActionSender.sendBox(player, p.getUsername() + " (" + p.getStatus() + ") at " + p.getLocation().toString() + " (" + p.getLocation().getDescription() + ") % % Logged in: " + (DataConversions.getDateFromMsec(System.currentTimeMillis() - (p.getLastLogin() * 1000))) + " % % Last moved: " + DataConversions.getDateFromMsec(System.currentTimeMillis() - p.getLastMoved()) + " % % Fatigue: " + p.getFatigue() * 10 / 750 + " percent % % Busy: " + (p.isBusy() ? "true" : "false"), true);
			} 
			else
				player.message("Invalid name or not online");
		} 
		if (command.equals("alert")) 
		{
			String message = "";
			if (args.length > 0) 
			{
				Player p = World.getWorld().getPlayer(DataConversions.usernameToHash(args[0]));
				if (p != null)
				{
					for (int i = 1; i < args.length; i++)
						message += args[i] + " ";
					ActionSender.sendBox(p, player.getRankHeader() + player.getUsername() + ":@whi@ " + message, false);
					player.message("Alert box has been sent to " + p.getUsername());
					//Logger.log(new GenericLog(player.getUsername() + " alerted " + p.getUsername() +": " + message, DataConversions.getTimeStamp()));
				}
			} 
			else
				player.message("Syntax: ::ALERT [name] [message]");	
		} 
		if (command.equalsIgnoreCase("putfatigue")) {
			long PlayerHash = DataConversions.usernameToHash(args[0]);
			int fatPercentage = Integer.parseInt(args[1]);
			Player p = world.getPlayer(PlayerHash);
			if (p != null) {
				p.setFatigue((fatPercentage * 750 / 10));
				player.message("You have set " + p.getUsername() + " fatigue to " + fatPercentage + "%.");
				GameLogging
				.addQuery(new StaffLog(player, 12, p, "Fatigue percentage was set to " + fatPercentage + "%"));
			} else {
				player.message("Invalid username or the player is currently offline.");
			}
		}
		if (command.equalsIgnoreCase("kick")) {
			long user = DataConversions.usernameToHash(args[0]);
			Player toKick = World.getWorld().getPlayer(user);
			if (toKick != null) {
				GameLogging.addQuery(new StaffLog(player, 6, toKick));
				toKick.unregister(true, "Kicked by " + player.getUsername());
				player.message(toKick.getUsername() + " has been kicked.");
			} else {
				player.message("This player does not seem to be online");
			}
			return;
		}
		if (command.equals("invis")) {
			if (player.getAttribute("invisible", false)) {
				player.setAttribute("invisible", false);
			} else {
				player.setTeleporting(true);
				player.setAttribute("invisible", true);
			}
			player.message(COMMAND_PREFIX + "You are now "
					+ (player.getAttribute("invisible", false) ? "invisible" : "visible"));

			GameLogging.addQuery(
					new StaffLog(player, 14, "Invisible: " + (player.getAttribute("invisible", false) ? "Yes" : "No")));
		}
		if (command.equals("take") || command.equals("put")) {
			boolean take = command.equals("take");
			if (args.length != 1) {
				player.message("Invalid args. Syntax: TAKE name");
				return;
			}
			Player affectedPlayer = world.getPlayer(DataConversions.usernameToHash(args[0]));
			if (affectedPlayer == null) {
				player.message("Invalid player, maybe they aren't currently online?");
				return;
			}
			affectedPlayer.getCache().set("return_x", affectedPlayer.getX());
			affectedPlayer.getCache().set("return_y", affectedPlayer.getY());

			if (take) {
				GameLogging.addQuery(new StaffLog(player, 4, affectedPlayer));
				player.teleport(76, 1642, false);
			} else {
				GameLogging.addQuery(new StaffLog(player, 5, affectedPlayer));
				affectedPlayer.teleport(78, 1642, false);
			}
		}
		if (command.equals("return")) {
			if (args.length != 1) {
				player.message("Invalid args. Syntax: return name");
				return;
			}
			Player affectedPlayer = world.getPlayer(DataConversions.usernameToHash(args[0]));
			if (affectedPlayer == null) {
				player.message("Invalid player, maybe they aren't currently online?");
				return;
			}
			if (!affectedPlayer.getCache().hasKey("return_x") || !affectedPlayer.getCache().hasKey("return_y")) {
				player.message("No return coordinates found for that player.");
				return;
			}
			int return_x = affectedPlayer.getCache().getInt("return_x");
			int return_y = affectedPlayer.getCache().getInt("return_y");

			affectedPlayer.teleport(return_x, return_y, false);
		}
	}
}
