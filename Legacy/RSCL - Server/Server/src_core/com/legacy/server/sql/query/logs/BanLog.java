package com.legacy.server.sql.query.logs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import com.legacy.server.Constants;
import com.legacy.server.sql.query.Query;

public class BanLog extends Query {

	private String player, staff, ip, mac, reason;
	private long UID, timeEnd;
	private int banType, playerID;
	
	public BanLog(int account, String ip, long uid, String mac, int type, String playerName, String staffName, String reason, long timeEnd) {
		super("INSERT INTO `" + Constants.GameServer.MYSQL_TABLE_PREFIX + "ban_log`(`account`, `ip`, `uid`, `mac`, `type`, `player`, `staff`, `reason`, `time`, `time_end`) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		this.playerID = account;
		this.ip = ip;
		this.UID = uid;
		this.mac = mac;
		this.banType = type;
		this.player = playerName;
		this.staff = staffName;
		this.reason = reason;
		this.timeEnd = timeEnd;
	}

	@Override
	public PreparedStatement prepareStatement(Connection connection) throws SQLException {
		PreparedStatement statement = connection.prepareStatement(query);
		statement.setInt(1, playerID);
		statement.setString(2, ip);
		statement.setLong(3, UID);
		statement.setString(4, mac);
		statement.setInt(5, banType);
		statement.setString(6, player);
		statement.setString(7, staff);
		statement.setString(8, reason);
		statement.setLong(9, time);
		statement.setLong(10, timeEnd);
		return statement;
	}
	
	@Override
	public Query build() {
		return this;
	}

}
