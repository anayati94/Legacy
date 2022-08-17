package com.legacy.server.sql.query.logs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

import com.legacy.server.Constants;
import com.legacy.server.model.container.Item;
import com.legacy.server.model.entity.Mob;
import com.legacy.server.model.entity.player.Player;
import com.legacy.server.sql.query.Query;

public class DeathLog extends Query {

	private ArrayList<Item> droppedLoot = new ArrayList<Item>();
	private int x, y;
	private String killer, killed, ip, droppedItems;
	private boolean duel;
	
	public DeathLog(Player killed, Mob killer, boolean duel, String IP) {
		super("INSERT INTO `" + Constants.GameServer.MYSQL_TABLE_PREFIX + "death_log` (`player`, `ip`, `killer`, `duel`, `x` , `y`, `dropped_items`, `time`) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ");
		this.killed = killed.getUsername();
		this.ip = killed.getCurrentIP();
		this.killer = killer == null ? "null" : killer.toString();
		this.x = killed.getLocation().getX();
		this.y = killed.getLocation().getY();
		this.duel = duel;
	}
	
	public void addDroppedItem(Item item) {
		droppedLoot.add(item);
	}
	
	@Override
	public Query build() {
		String droppedString = "";
		for(Item item : droppedLoot) {
			droppedString += item.getID() + ":" + item.getAmount() + ",";
		}
		if (droppedString.length() > 0)
			droppedString.substring(0, droppedString.length() - 1);
		else
			droppedString = "Nothing";
		
		String killerName = "World";
		if(killer != null) {
			killerName = killer;
		}

		droppedItems = droppedString;
		killer = killerName;
		return this;
	}

	@Override
	public PreparedStatement prepareStatement(Connection connection) throws SQLException {
		PreparedStatement statement = connection.prepareStatement(query);
		int parameterIndex = 1;
		statement.setString(parameterIndex++, killed);
		statement.setString(parameterIndex++, ip);
		statement.setString(parameterIndex++, killer);
		statement.setInt(parameterIndex++, duel ? 1 : 0);
		statement.setInt(parameterIndex++, x);
		statement.setInt(parameterIndex++, y);
		statement.setString(parameterIndex++, droppedItems);
		statement.setLong(parameterIndex++, time);
		return statement;
	}

}
