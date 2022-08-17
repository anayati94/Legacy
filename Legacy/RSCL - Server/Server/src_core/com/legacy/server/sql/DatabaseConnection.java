package com.legacy.server.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.legacy.server.Constants;
import com.legacy.server.util.rsc.ChatFilter;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * 
 * @author Imposter
 *
 */
public class DatabaseConnection {
	/**
	 * The asynchronous logger.
	 */
	private static final Logger LOGGER = LogManager.getLogger();

	private static final int QUERY_FAILED = -1;

	private static DatabaseConnection singleton;

	private final HikariDataSource dataSource;

	private final HikariConfig hikariConfig;

	/**
	 * Instantiates a new database connection
	 */
	public DatabaseConnection(String string) {
		hikariConfig = new HikariConfig("resources/db/game_database.properties");
		hikariConfig.setLeakDetectionThreshold(10000);
		dataSource = new HikariDataSource(hikariConfig);
	}

	public boolean isConnected() {
		try(Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement("SELECT CURRENT_DATE")) {
			statement.executeQuery();
			return true;
		} catch (SQLException e) {
			return false;
		}
	}

	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	public static DatabaseConnection getDatabase() {
		return singleton;
	}

	public static void init() {
		LOGGER.info("Creating database main connection...");
		singleton = new DatabaseConnection("Singleton Connection");
		LOGGER.info("Database main connection created.");

		getDatabase().executeUpdate("UPDATE `" + Constants.GameServer.MYSQL_TABLE_PREFIX + "players` SET `online` = '0' WHERE `online` != '0'");
		LOGGER.info("Online statuses resetted.");
	}

	public void loadChatFilter() throws SQLException {
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection
						.prepareStatement("SELECT `search_for` FROM `censoring`");
				ResultSet result = statement.executeQuery()) {
			while (result.next()) {
				ChatFilter.add(result.getString("search_for").toLowerCase());
			}
		}
	}

	public void close() {
		dataSource.close();
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

}
