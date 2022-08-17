package com.legacy.server.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.legacy.server.sql.query.Query;
import com.legacy.server.sql.query.ResultQuery;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public final class GameLogging {

	private static final Logger LOGGER = LogManager.getLogger();

	private final static AtomicBoolean running = new AtomicBoolean(true);

	private static GameLogging singleton;

	private final ExecutorService queryPool;

	private final HikariDataSource dataSource;

	private final HikariConfig hikariConfig;

	/**
	 * Instantiates a new database connection
	 */
	public GameLogging() {
		hikariConfig = new HikariConfig("resources/db/game_logging_database.properties");
		hikariConfig.setLeakDetectionThreshold(10000);
		dataSource = new HikariDataSource(hikariConfig);
		queryPool = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("QueryPool").build());
	}

	public static GameLogging singleton() {
		return singleton;
	}

	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	public static void init() {
		LOGGER.info("Creating game logging connection...");
		singleton = new GameLogging();
		singleton.start();
		LOGGER.info("Game logging connection created.");
	}

	public void start() {
		running.set(true);
	}

	public void shutdown() {
		if(running.get()) {
			queryPool.shutdown();
			running.set(false);
			close();
			LOGGER.info("GameLoggingPool shutdown completed.");
		}
	}

	private void runQuery(Query log) {
		try {
			if (log instanceof ResultQuery) {
				ResultQuery rq = (ResultQuery) log;
				try(Connection connection = dataSource.getConnection();
						PreparedStatement ps = rq.prepareStatement(connection)) {
					rq.onResult(ps.executeQuery());
				} catch (SQLException e) {
					LOGGER.catching(e);
				}
			} else {
				try(Connection connection = dataSource.getConnection();
						PreparedStatement ps = log.prepareStatement(connection)) {
					ps.execute();
				}
			}
		} catch (Exception e) {
			LOGGER.catching(e);
		}
	}

	public static void addQuery(Query log) {
		if(singleton().queryPool.isShutdown()) {
			return;
		}
		singleton().queryPool.execute(() -> {
			singleton().runQuery(log);
		});
	}

	public void close() {
		dataSource.close();
	}

}