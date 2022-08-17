package com.legacy.server.sql.query.logs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import com.legacy.server.Constants;
import com.legacy.server.model.container.Item;
import com.legacy.server.sql.query.Query;

public class DuelLog extends Query {

	private int[] duelOptions;

	private String opponentIP, opponent_account, staker, stakerIP, playerOnesOffer, playerTwosOffer;

	private List<Item> player1Offer, player2Offer;
	
	public DuelLog(String account, String IP, String opponent_account, String opponentIP, int[] duelOptions, List<Item> player1Offer, List<Item> player2Offer) {
		super("INSERT INTO `" + Constants.GameServer.MYSQL_TABLE_PREFIX + "duel_log`(`user1`, `user1_ip`, `user2`, `user2_ip`, `no_retreating`, `no_prayer`, `no_magic`, `no_weapons`, `user1_items`, `user2_items`, `time`) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		this.staker = account;
		this.stakerIP = IP;
		this.opponent_account = opponent_account;
		this.opponentIP = opponentIP;
		this.duelOptions = duelOptions;
		this.player1Offer = player1Offer;
		this.player2Offer = player2Offer;
	}
	
	@Override
	public Query build() {
		StringBuilder sb = new StringBuilder();
		
		for (Item i : player1Offer) {
			sb.append(i.getID()).append(":").append(i.getAmount()).append(",");
		}
		
		playerOnesOffer = sb.toString();
		sb = new StringBuilder();
		
		for (Item i : player2Offer) {
			sb.append(i.getID()).append(":").append(i.getAmount()).append(",");
		}
		
		playerTwosOffer = sb.toString();
		
		return this;
	}

	@Override
	public PreparedStatement prepareStatement(Connection connection) throws SQLException {
		PreparedStatement statement = connection.prepareStatement(query);
		statement.setString(1, staker);
		statement.setString(2, stakerIP);
		statement.setString(3, opponent_account);
		statement.setString(4, opponentIP);
		statement.setInt(5, duelOptions[0]);
		statement.setInt(6, duelOptions[1]);
		statement.setInt(7, duelOptions[2]);
		statement.setInt(8, duelOptions[3]);
		statement.setString(9, playerOnesOffer);
		statement.setString(10, playerTwosOffer);
		statement.setLong(11, time);
		return statement;
	}

}
