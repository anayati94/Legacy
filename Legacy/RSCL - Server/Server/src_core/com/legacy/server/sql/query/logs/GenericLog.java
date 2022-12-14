package com.legacy.server.sql.query.logs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.legacy.server.Constants;
import com.legacy.server.sql.query.Query;

public final class GenericLog extends Query {
	
	private final String message, player;
	
	public GenericLog(String player, String message) {
		super("INSERT INTO `" + Constants.GameServer.MYSQL_TABLE_PREFIX + "generic_logs`(`player`, `message`, `time`) VALUES(?, ?, ?)");
		this.player = player;
		this.message = message;
	}

	@Override
	public PreparedStatement prepareStatement(Connection connection) throws SQLException {
		PreparedStatement statement = connection.prepareStatement(query);
		statement.setString(1, player);
		statement.setString(2, message);
		statement.setLong(3, time);
		return statement;
	}

	@Override
	public Query build() {
		return this;
	}
	
}
