package com.legacy.server;

import static org.apache.logging.log4j.util.Unbox.box;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.legacy.server.content.clan.ClanManager;
import com.legacy.server.event.rsc.impl.combat.scripts.CombatScriptLoader;
import com.legacy.server.model.entity.player.Player;
import com.legacy.server.model.world.World;
import com.legacy.server.net.ChannelPipelineHandler;
import com.legacy.server.plugins.PluginHandler;
import com.legacy.server.sql.DatabaseConnection;
import com.legacy.server.sql.GameLogging;
import com.legacy.server.sql.WorldPopulation;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;

public final class Server implements Runnable {

	private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("GameEngine").build());

	private final GameStateUpdater gameUpdater = new GameStateUpdater();

	private final GameTickEventHandler tickEventHandler = new GameTickEventHandler();

	private final ServerEventHandler eventHandler = new ServerEventHandler();

	private static PlayerDatabaseExecutor playerDataProcessor;

	private long lastClientUpdate;

	private static Server server = null;

	private final AtomicInteger timeLeftForScheduledShutdown;

	private final ShutdownHandler shutdownHandler;

	/**
	 * The asynchronous logger.
	 */
	private static final Logger LOGGER;

	static {
		try {
			System.setProperty("log4j.configurationFile", "resources/log4j2.xml"); 
			/* Enables asynchronous, garbage-free logging. */
			System.setProperty("Log4jContextSelector", 
					"org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");

			LOGGER = LogManager.getLogger();
		} catch (Exception e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	public static void main(String[] args) throws IOException {
		LOGGER.info("Launching RSCLegacy Game Server...");
		if (args.length == 0) {
			Constants.GameServer.initConfig("members.conf");
			LOGGER.info("Server Configuration file not provided. Default: free.conf");
		} else {
			Constants.GameServer.initConfig(args[0]);
			LOGGER.info("Server Configuration file: " + args[0]);
			LOGGER.info("Game Tick Cycle: {}", box(Constants.GameServer.GAME_TICK));
			LOGGER.info("Client Version: {}", box(Constants.GameServer.CLIENT_VERSION));
			LOGGER.info("Server type: " + (Constants.GameServer.MEMBER_WORLD ? "MEMBERS" : "FREE") + " world.");
			LOGGER.info("Combat Experience Rate: {}", box(Constants.GameServer.COMBAT_EXP_RATE));
			LOGGER.info("Skilling Experience Rate: {}", box(Constants.GameServer.SKILLING_EXP_RATE));
			LOGGER.info("Standard Subscription Rate: {}", box(Constants.GameServer.SUBSCRIBER_EXP_RATE));
			LOGGER.info("Premium Subscription Rate: {}", box(Constants.GameServer.PREMIUM_EXP_RATE));
			LOGGER.info("Wilderness Experience Boost: {}", box(Constants.GameServer.WILDERNESS_BOOST));
			LOGGER.info("Skull Experience Boost: {}", box(Constants.GameServer.SKULL_BOOST)); 
			LOGGER.info("Double experience: " + (Constants.GameServer.IS_DOUBLE_EXP ? "Enabled" : "Disabled")); 
		}
		if(server == null) {
			server = new Server();
			server.initialize();
			server.start();
		}
	}

	private boolean running;

	private NioEventLoopGroup loopGroup;

	public Server() {
		running = true;
		playerDataProcessor = new PlayerDatabaseExecutor();
		shutdownHandler = new ShutdownHandler(scheduledExecutor);
		timeLeftForScheduledShutdown = new AtomicInteger(-1);
		Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
	}

	public void shutdown() {
		timeLeftForScheduledShutdown.set(0);
		this.shutdownHandler.run();
	}

	private void initialize() {
		try {
			final ExecutorService serviceLoader = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("GameLoadingThread").build());
			
			serviceLoader.execute(() -> DatabaseConnection.init());
			serviceLoader.execute(() -> WorldPopulation.init());
			serviceLoader.execute(() -> GameLogging.init());
			serviceLoader.execute(() -> PluginHandler.getPluginHandler().initPlugins());
			serviceLoader.execute(() -> CombatScriptLoader.init());
			serviceLoader.execute(() -> ClanManager.init());
			serviceLoader.execute(() -> World.getWorld().load());
			serviceLoader.execute(() -> playerDataProcessor.start());
			serviceLoader.shutdown();
			
			if (!serviceLoader.awaitTermination(15, TimeUnit.MINUTES))
				throw new IllegalStateException("Loading up the game took too long!");

			ResourceLeakDetector.setLevel(Level.DISABLED);
			loopGroup = new NioEventLoopGroup();
			final ServerBootstrap bootstrap = new ServerBootstrap();

			bootstrap.group(loopGroup).channel(NioServerSocketChannel.class)
			.childHandler(new ChannelPipelineHandler());

			PluginHandler.getPluginHandler().handleAction("Startup", new Object[] {});
			serverChannel = bootstrap.bind(new InetSocketAddress(Constants.GameServer.SERVER_PORT)).syncUninterruptibly();
			LOGGER.info("RSCLegacy channel is now online on port {}!", box(Constants.GameServer.SERVER_PORT)); 

		} catch (Exception e) {
			LOGGER.catching(e);
			System.exit(1);
		}
	}
	
	private ChannelFuture serverChannel;

	public boolean isRunning() {
		return running;
	}

	public void unbind() {
		serverChannel.cancel(true);

		try {
			serverChannel.channel().close().get();
		} catch (InterruptedException | ExecutionException e) {
			LOGGER.error("An error occurred while stopping server {}", e);
		}

		loopGroup.shutdownGracefully();
	}

	public static Server getServer() {
		return server;
	}

	public static PlayerDatabaseExecutor getPlayerDataProcessor() {
		return playerDataProcessor;
	}

	public void run() {
		for (Player p : World.getWorld().getPlayers()) {
			p.processIncomingPackets();
		}

		getEventHandler().doEvents();

		if (System.currentTimeMillis() - lastClientUpdate >= Constants.GameServer.GAME_TICK) {
			lastClientUpdate = System.currentTimeMillis();
			tickEventHandler.doGameEvents();
			try {
				gameUpdater.updateClients();
			} catch (Exception e) {
				LOGGER.catching(e);
			}
		}
		
		for (Player p : World.getWorld().getPlayers()) {
			p.sendOutgoingPackets();
		}
	}

	public ServerEventHandler getEventHandler() {
		return eventHandler;
	}

	public GameTickEventHandler getGameEventHandler() {
		return tickEventHandler;
	}

	public void submitTask(Runnable r) {
		scheduledExecutor.submit(r);
	}

	public void start() {
		scheduledExecutor.scheduleAtFixedRate(this, 0, 50, TimeUnit.MILLISECONDS);
	}

	public void setRunning(boolean running) {
		this.running = running;
	}

	public boolean scheduleShutDown(int seconds) {
		if(shutdownHandler.alreadyShuttingDown()) {
			return false;
		}
		scheduleShutdown(seconds);
		return true;
	}

	private void scheduleShutdown(int seconds) {
		ExecutorService shutdownPool = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("ShutdownPool").build());

		shutdownPool.execute(() -> {
			for(int i = 0; i < seconds; i++) {
				timeLeftForScheduledShutdown.set(seconds - i);

				try {
					Thread.sleep(TimeUnit.SECONDS.toMillis(1));
				} catch (Exception e) {
					e.printStackTrace();
				}

				LOGGER.debug("Total time remaining in shutdown {}", timeLeftForScheduledShutdown.get());
			}

			this.shutdown();
		});

		shutdownPool.shutdown();
	}

	public int timeTillShutdown() {
		return timeLeftForScheduledShutdown.get();
	}
}