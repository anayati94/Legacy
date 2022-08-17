package com.legacy.server.sql.query.logs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.legacy.server.Constants;
import com.legacy.server.sql.query.Query;

public final class LoginLog extends Query {

	private final int player;
	private final long uid;
	private final String ip, mac;

	public LoginLog(int player, String ip, long uid, String mac) {
		super("INSERT INTO `" + Constants.GameServer.MYSQL_TABLE_PREFIX + "logins`(`playerID`, `ip`, `uid`, `mac`, `time`) VALUES(?, ?, ?, ?, ?)");
		this.player = player;
		this.ip = ip;
		this.uid = uid;
		this.mac = mac;
	}

	@Override
	public PreparedStatement prepareStatement(Connection connection) throws SQLException {
		PreparedStatement statement = connection.prepareStatement(query);
		statement.setInt(1, player);
		statement.setString(2, ip);
		statement.setLong(3, uid);
		statement.setString(4, mac);
		statement.setLong(5, time);
		return statement;
	}

	@Override
	public Query build() {
		return this;
	}

}
