package com.legacy.server;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.legacy.server.content.clan.ClanManager;
import com.legacy.server.content.market.Market;
import com.legacy.server.model.world.World;
import com.legacy.server.plugins.PluginHandler;
import com.legacy.server.sql.DatabaseConnection;
import com.legacy.server.sql.GameLogging;
import com.legacy.server.sql.WorldPopulation;

public class ShutdownHandler implements Runnable {

	private static final Logger LOGGER = LogManager.getLogger();

	private final ExecutorService scheduledExecutor;

	private final AtomicBoolean shuttingDown;

	public ShutdownHandler(ExecutorService scheduledExecutor2) {
		this.scheduledExecutor = scheduledExecutor2;
		shuttingDown = new AtomicBoolean(false);
	}

	@Override
	public void run() {
		if(shuttingDown.compareAndSet(false, true)) {
			LOGGER.info(Constants.GameServer.SERVER_NAME + " is shutting down...");	
			scheduledExecutor.execute(() -> {
				try {
					World.getWorld().getPlayers().forEach(p -> World.getWorld().unregisterPlayer(p));

					while (!Server.getPlayerDataProcessor().saveRequests.isEmpty()) {
						Thread.sleep(1000);
						LOGGER.info("Shutdown thread sleeping - waiting to save all players!");
					}
				} catch (Exception e) {
					LOGGER.error(e);
				}
			});
			scheduledExecutor.execute(() -> ClanManager.saveClans());
			scheduledExecutor.execute(() -> Server.getServer().setRunning(false));
			scheduledExecutor.execute(() -> Server.getServer().unbind());
			scheduledExecutor.execute(() -> PluginHandler.getPluginHandler().unload());
			scheduledExecutor.execute(() -> Market.getInstance().shutdown());
			scheduledExecutor.execute(() -> GameLogging.singleton().shutdown());
			scheduledExecutor.execute(() -> WorldPopulation.getDatabase().close());
			scheduledExecutor.execute(() -> Server.getPlayerDataProcessor().shutdown());
			scheduledExecutor.execute(() -> DatabaseConnection.getDatabase().close());
			scheduledExecutor.execute(() -> LogManager.shutdown());

			scheduledExecutor.shutdown();
		}
	}

	public boolean alreadyShuttingDown() {
		return shuttingDown.get();
	}

}
