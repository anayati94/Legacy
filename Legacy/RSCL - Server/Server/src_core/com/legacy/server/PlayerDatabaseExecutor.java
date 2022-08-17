package com.legacy.server;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.legacy.server.event.rsc.ImmediateEvent;
import com.legacy.server.login.LoginRequest;
import com.legacy.server.login.LoginTask;
import com.legacy.server.model.entity.player.Player;
import com.legacy.server.model.world.World;
import com.legacy.server.sql.DatabasePlayerLoader;

public class PlayerDatabaseExecutor implements Runnable  {
	/**
	 * The asynchronous logger.
	 */
	private static final Logger LOGGER = LogManager.getLogger();

	private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("PlayerDataProcessor").build());

	private final ScheduledExecutorService savePool = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("SavePool").build());

	private Queue<LoginRequest> loadRequests = new ConcurrentLinkedQueue<LoginRequest>();

	public Queue<Player> saveRequests = new ConcurrentLinkedQueue<Player>();

	private DatabasePlayerLoader database = new DatabasePlayerLoader();

	@Override
	public void run() {
		try {
			LoginRequest loginRequest = null;
			while((loginRequest = loadRequests.poll()) != null) {
				int loginResponse = database.validateLogin(loginRequest);
				loginRequest.loginValidated(loginResponse);
				if(loginResponse == 0) {
					final Player loadedPlayer = database.loadPlayer(loginRequest);

					LoginTask loginTask = new LoginTask(loginRequest, loadedPlayer);
					Server.getServer().getGameEventHandler().add(new ImmediateEvent() {
						@Override
						public void action() {
							loginTask.run();
						}
					});

				}
				LOGGER.info("Processed login request for " + loginRequest.getUsername() + " response: " + loginResponse);
			}
			Player playerToSave = null;
			while((playerToSave = saveRequests.poll()) != null) {
				getDatabase().savePlayer(playerToSave);
				LOGGER.info("Saved player " + playerToSave.getUsername() + "");
			}
		} catch (Exception e) {
			LOGGER.catching(e);
		}
	}

	public DatabasePlayerLoader getDatabase() {
		return database;
	}


	public void addLoginRequest(LoginRequest request) {
		loadRequests.add(request);
	}


	public void addSaveRequest(Player player) {
		saveRequests.add(player);
	}

	public void start() {
		LOGGER.info("Starting PlayerDataBase executor and save pool...");
		scheduledExecutor.scheduleAtFixedRate(this, 50, 50, TimeUnit.MILLISECONDS);
		savePool.scheduleAtFixedRate(() -> checkpoint(), 15, 15, TimeUnit.MINUTES);
		LOGGER.info("PlayerDataBase executor and Save Pool Completed");
	}

	private void checkpoint() {
		World.getWorld().getPlayers().forEach(this::savePlayer);
	}

	private void savePlayer(Player player) {
		getDatabase().savePlayer(player);
		LOGGER.debug("Saved player {}", player);
	}

	public void shutdown() {
		scheduledExecutor.shutdown();
		LOGGER.info("PlayerDatabase scheduler shutdown completed.");

		savePool.shutdown();
		LOGGER.info("Save pool shutdown completed.");
	}
}
