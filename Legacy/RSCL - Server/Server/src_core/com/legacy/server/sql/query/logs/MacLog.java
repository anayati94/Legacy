package com.legacy.server.sql.query.logs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import com.legacy.server.Constants;
import com.legacy.server.sql.query.Query;

public class MacLog extends Query {

	private String macAddress;
	
	public MacLog(String mac) {
		super("INSERT INTO `" + Constants.GameServer.MYSQL_TABLE_PREFIX + "mac_log`(`mac`, `time`) VALUES(?, ?)");
		this.macAddress = mac;
	}

	@Override
	public PreparedStatement prepareStatement(Connection connection) throws SQLException {
		PreparedStatement statement = connection.prepareStatement(query);
		statement.setString(1, macAddress);
		statement.setLong(2, time);
		return statement;
	}
	
	@Override
	public Query build() {
		return this;
	}

}
