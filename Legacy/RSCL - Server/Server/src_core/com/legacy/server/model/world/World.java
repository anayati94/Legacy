package com.legacy.server.model.world;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.legacy.server.Server;
import com.legacy.server.content.clan.ClanManager;
import com.legacy.server.content.clan.Territory;
import com.legacy.server.content.clan.clanwar.event.ClanWars;
import com.legacy.server.content.minigame.fishingtrawler.FishingTrawler;
import com.legacy.server.event.SingleEvent;
import com.legacy.server.external.GameObjectLoc;
import com.legacy.server.external.NPCLoc;
import com.legacy.server.io.WorldLoader;
import com.legacy.server.model.Point;
import com.legacy.server.model.Shop;
import com.legacy.server.model.entity.GameObject;
import com.legacy.server.model.entity.GroundItem;
import com.legacy.server.model.entity.Mob;
import com.legacy.server.model.entity.npc.Npc;
import com.legacy.server.model.entity.player.Player;
import com.legacy.server.model.snapshot.Snapshot;
import com.legacy.server.model.world.region.RegionManager;
import com.legacy.server.net.rsc.ActionSender;
import com.legacy.server.plugins.QuestInterface;
import com.legacy.server.sql.DatabaseConnection;
import com.legacy.server.sql.GameLogging;
import com.legacy.server.sql.WorldPopulation;
import com.legacy.server.sql.query.logs.LoginLog;
import com.legacy.server.sql.web.AvatarGenerator;
import com.legacy.server.util.rsc.EntityList;
import com.legacy.server.util.rsc.MessageType;

public final class World {

	/**
	 * The asynchronous logger.
	 */
	private static final Logger LOGGER = LogManager.getLogger();

	/**
	 * Double ended queue to store snapshots into
	 */
	private Deque<Snapshot> snapshots = new LinkedList<Snapshot>();

	/**
	 * Returns double-ended queue for snapshots.
	 */
	public synchronized Deque<Snapshot> getSnapshots() {
		return snapshots;
	}
	/**
	 * Add entry to snapshots
	 */
	public synchronized void addEntryToSnapshots(Snapshot snapshot) {
		snapshots.offerFirst(snapshot);
	}

	/**
	 * Avatar generator upon logout save to PNG.
	 */
	private final static AvatarGenerator avatarGenerator = new AvatarGenerator();

	public static int membersWildStart = 48;
	public static int membersWildMax = 56;

	public static int godSpellsStart = 1;
	public static int godSpellsMax = 5;

	public static final int MAX_HEIGHT = 3776; // 3776

	public static final int MAX_WIDTH = 944; // 944

	public static boolean EVENT = false;
	public static int EVENT_X = -1, EVENT_Y = -1;

	private static World worldInstance;
	public static int EVENT_COMBAT_MIN, EVENT_COMBAT_MAX;

	public static boolean WORLD_TELEGRAB_TOGGLE = false;

	public static synchronized World getWorld() {
		if (worldInstance == null) {
			worldInstance = new World();

		}
		return worldInstance;
	}

	private final WorldLoader db = new WorldLoader();

	private final EntityList<Npc> npcs = new EntityList<Npc>(4000);

	private final EntityList<Player> players = new EntityList<Player>(2000);

	private final List<QuestInterface> quests = new LinkedList<QuestInterface>();

	private final List<Shop> shopData = new ArrayList<Shop>();

	private final List<Shop> shops = new ArrayList<Shop>();

	public WorldLoader wl;

	private FishingTrawler fishingTrawler;

	public void addShopData(Shop... shop) {
		shopData.addAll(Arrays.asList(shop));
	}

	public void clearShopData() {
		shopData.clear();
	}

	public int countNpcs() {
		return npcs.size();
	}

	public int countPlayers() {
		return players.size();
	}

	public void delayedRemoveObject(final GameObject object, final int delay) {
		Server.getServer().getEventHandler().add(new SingleEvent(null, delay) {
			public void action() {
				unregisterGameObject(object);
			}
		});
	}

	/**
	 * Adds a DelayedEvent that will spawn a GameObject
	 */
	public void delayedSpawnObject(final GameObjectLoc loc, final int respawnTime) {
		Server.getServer().getEventHandler().add(new SingleEvent(null, respawnTime) {
			public void action() {
				registerGameObject(new GameObject(loc));
			}
		});
	}

	public WorldLoader getDB() {
		return db;
	}

	public Npc getNpc(int idx) {
		try {
			return npcs.get(idx);
		} catch (Exception e) {
			return null;
		}
	}

	public Npc getNpc(int id, int minX, int maxX, int minY, int maxY) {
		for (Npc npc : npcs) {
			if (npc.getID() == id && npc.getX() >= minX && npc.getX() <= maxX && npc.getY() >= minY
					&& npc.getY() <= maxY) {
				return npc;
			}
		}
		return null;
	}

	public Npc getNpc(int id, int minX, int maxX, int minY, int maxY, boolean notNull) {
		for (Npc npc : npcs) {
			if (npc.getID() == id && npc.getX() >= minX && npc.getX() <= maxX && npc.getY() >= minY
					&& npc.getY() <= maxY) {
				if (!npc.inCombat()) {
					return npc;
				}
			}
		}
		return null;
	}

	public Npc getNpcById(int id) {
		for (Npc npc : npcs) {
			if (npc.getID() == id) {
				return npc;
			}
		}
		return null;
	}

	/**
	 * Gets the list of npcs on the server
	 */
	public synchronized EntityList<Npc> getNpcs() {
		return npcs;
	}

	/**
	 * Gets a Player by their server index
	 */
	public Player getPlayer(int idx) {
		try {
			Player p = players.get(idx);
			return p;
		} catch (Exception e) {
			return null;
		}

	}

	/**
	 * Gets a player by their username hash
	 */
	public Player getPlayer(long usernameHash) {
		for (Player p : players) {
			if (p.getUsernameHash() == usernameHash) {
				return p;
			}
		}
		return null;
	}
	/**
	 * Gets a player by their username hash
	 */
	public Player getPlayerID(int databaseID) {
		for (Player p : players) {
			if (p.getDatabaseID() == databaseID) {
				return p;
			}
		}
		return null;
	}

	public synchronized EntityList<Player> getPlayers() {
		return players;
	}

	/**
	 * Finds a specific quest by ID
	 * 
	 * @param q
	 * @return
	 * @throws IllegalArgumentException
	 *             when a quest by that ID isn't found
	 */
	public QuestInterface getQuest(int q) throws IllegalArgumentException {
		for (QuestInterface quest : this.getQuests()) {
			if (quest.getQuestId() == q) {
				return quest;
			}
		}
		throw new IllegalArgumentException("No quest found");
	}

	public List<QuestInterface> getQuests() {
		return quests;
	}

	public List<Shop> getShops() {
		return shops;
	}

	public boolean hasNpc(Npc n) {
		return npcs.contains(n);
	}

	public boolean hasPlayer(Player p) {
		return players.contains(p);
	}

	public boolean isLoggedIn(long usernameHash) {
		Player friend = getPlayer(usernameHash);
		if (friend != null) {
			return friend.loggedIn();
		}
		return false;
	}

	public void load() {
		try {
			LOGGER.info("Loading World...");
			DatabaseConnection.getDatabase().loadChatFilter();
			WorldPopulation.populateWorldDefinitions();
			worldInstance.wl = new WorldLoader();
			worldInstance.wl.loadWorld(worldInstance);
			WorldPopulation.populateWorldSpawns(worldInstance);
			LOGGER.info("World Completed");
			//AchievementSystem.loadAchievements();
			// Server.getServer().getEventHandler().add(new WildernessCycleEvent());
			//setFishingTrawler(new FishingTrawler());
			//Server.getServer().getEventHandler().add(getFishingTrawler());
			//setClanWars(new ClanWars());
			//Server.getServer().getEventHandler().add(clanwars);
			//clanwars.generateClanWarRules();
		} catch (Exception e) {
			LOGGER.catching(e);
		}
	}
	/*
	 * Note to self - Remove CollidingWallObject, Remove getWallGameObject, And others if this doesn't work in long run.
	 * Classes - viewArea, world, region, gameObjectAction, GameObjectWallAction, ItemUseOnObject
	 */

	public void registerGameObject(GameObject o) {
		Point objectCoordinates = Point.location(o.getLoc().getX(), o.getLoc().getY());
		GameObject collidingGameObject = RegionManager.getRegion(objectCoordinates).getGameObject(objectCoordinates);
		GameObject collidingWallObject = RegionManager.getRegion(objectCoordinates).getWallGameObject(objectCoordinates, o.getLoc().getDirection());
		if (collidingGameObject != null && o.getType() == 0) {
			unregisterGameObject(collidingGameObject);
		}
		if (collidingWallObject != null && o.getType() == 1) {
			unregisterGameObject(collidingWallObject);
		}
		o.setLocation(Point.location(o.getLoc().getX(), o.getLoc().getY()));

		int dir = o.getDirection();
		if(o.getID() == 1147) {
			return;
		}
		switch (o.getType()) {
		case 0:
			if (o.getGameObjectDef().getType() != 1 && o.getGameObjectDef().getType() != 2) {
				return;
			}
			int width, height;
			if (dir == 0 || dir == 4) {
				width = o.getGameObjectDef().getWidth();
				height = o.getGameObjectDef().getHeight();
			} else {
				height = o.getGameObjectDef().getWidth();
				width = o.getGameObjectDef().getHeight();
			}
			for (int x = o.getX(); x < o.getX() + width; ++x) {
				for (int y = o.getY(); y < o.getY() + height; ++y) {
					if (o.getGameObjectDef().getType() == 1)
						objectValues[x][y] |= 0x40;
					else if (dir == 0) {
						objectValues[x][y] |= 2;
						objectValues[x - 1][y] |= 8;
					} else if (dir == 2) {
						objectValues[x][y] |= 4;
						objectValues[x][y + 1] |= 1;
					} else if (dir == 4) {
						objectValues[x][y] |= 8;
						objectValues[x + 1][y] |= 2;
					} else if (dir == 6) {
						objectValues[x][y] |= 1;
						objectValues[x][y - 1]|= 4;
					}
				}
			}
			break;

		case 1:
			if (o.getDoorDef().getDoorType() != 1) {
				return;
			}
			int x = o.getX(), y = o.getY();
			if (dir == 0) {
				objectValues[x][y] |= 1;
				objectValues[x][y - 1] |= 4;
			} else if (dir == 1) {
				objectValues[x][y]|= 2;
				objectValues[x - 1][y] |= 8;
			} else if (dir == 2)
				objectValues[x][y]|= 0x10; // 16
			else if (dir == 3)
				objectValues[x][y] |= 0x20; //32

			break;
		}
	}
	
	public static byte[][] mapValues = new byte[MAX_WIDTH][MAX_HEIGHT];
	public static byte[][] objectValues = new byte[MAX_WIDTH][MAX_HEIGHT];
	public static byte[][] mapSIDValues = new byte[MAX_WIDTH][MAX_HEIGHT];
	public static byte[][] mapNIDValues = new byte[MAX_WIDTH][MAX_HEIGHT];
	public static byte[][] mapEIDValues = new byte[MAX_WIDTH][MAX_HEIGHT];
	public static byte[][] mapWIDValues = new byte[MAX_WIDTH][MAX_HEIGHT];
	public static byte[][] mapDIDValues = new byte[MAX_WIDTH][MAX_HEIGHT];
	public static byte[][] groundOverlayValues = new byte[MAX_WIDTH][MAX_HEIGHT];
	public static boolean projectTileAllowed;

	public void registerItem(final GroundItem i) {
		try {
			if (i.getLoc() == null) {
				Server.getServer().getEventHandler().add(new SingleEvent(null, 180000) {
					public void action() {
						unregisterItem(i);
					}
				});
			}
		} catch (Exception e) {
			i.remove();
			LOGGER.catching(e);
		}
	}

	public Npc registerNpc(Npc n) {
		NPCLoc npc = n.getLoc();
		if (npc.startX < npc.minX || npc.startX > npc.maxX || npc.startY < npc.minY || npc.startY > npc.maxY || (World.mapValues[npc.startX][npc.startY] & 64) != 0) {
			System.out.println("Broken NPC: " + npc.id + " " + npc.startX + " " + npc.startY);
		}
		npcs.add(n);
		return n;
	}

	public void registerObjects(GameObject... obs) {
		for (GameObject o : obs) {
			o.setLocation(Point.location(o.getLoc().getX(), o.getLoc().getY()));
		}
	}

	public boolean registerPlayer(Player player) {
		
		if (!players.contains(player)) {
			players.add(player);
			player.updateRegion();
			if (Server.getPlayerDataProcessor() != null) {
				Server.getPlayerDataProcessor().getDatabase().playerOnlineFlagQuery(player.getDatabaseID(), player.getCurrentIP(), true);
				//GameLogging.addQuery(new PlayerOnlineFlagQuery(player.getDatabaseID(), player.getCurrentIP(), true));
				GameLogging.addQuery(new LoginLog(player.getDatabaseID(), player.getCurrentIP(), player.getUID(), player.getMacAddress()));
			}
			for (Player other : getPlayers()) {
				other.getSocial().alertOfLogin(player);
			}
			ClanManager.checkAndAttachToClan(player);
			LOGGER.info("Registered " + player.getUsername() + " to server");
			return true;
		}
		return false;
	}

	public void registerQuest(QuestInterface quest) {
		if (quest.getQuestName() == null) {
			throw new IllegalArgumentException("Quest name cannot be null");
		} else if (quest.getQuestName().length() > 40) {
			throw new IllegalArgumentException("Quest name cannot be longer then 40 characters");
		}
		for (QuestInterface q : quests) {
			if (q.getQuestId() == quest.getQuestId()) {
				throw new IllegalArgumentException("Quest ID must be unique");
			}
		}
		quests.add(quest);
	}

	public void registerShop(Shop shop) {
		shops.add(shop);
	}

	public void registerShops(Shop... shop) {
		shops.addAll(Arrays.asList(shop));
	}

	public void replaceGameObject(GameObject old, GameObject _new) {
		unregisterGameObject(old);
		registerGameObject(_new);
	}

	public void sendKilledUpdate(long killedHash, long killerHash, int type) {
		for (final Player player : players)
			ActionSender.sendKillUpdate(player, killedHash, killerHash, type);
	}

	public void sendModAnnouncement(String string) {
		for (Player p : players) {
			if (p.isMod()) {
				p.message("[@cya@SERVER@whi@]: " + string);
			}
		}
	}

	public void sendWorldAnnouncement(String msg) {
		for (Player p : getPlayers()) {
			p.playerServerMessage(MessageType.QUEST, "@gre@[Global] @whi@" + msg);
		}
	}

	public void sendWorldMessage(String msg) {
		synchronized (players) {
			for (Player p : players) {
				p.playerServerMessage(MessageType.QUEST, msg);
			}
		}
	}

	/**
	 * Removes an object from the server
	 */
	public void unregisterGameObject(GameObject o) {
		o.remove();
		int dir = o.getDirection();
		switch (o.getType()) {
		case 0:
			if (o.getGameObjectDef().getType() != 1 && o.getGameObjectDef().getType() != 2) {
				return;
			}
			int width, height;
			if (dir == 0 || dir == 4) {
				width = o.getGameObjectDef().getWidth();
				height = o.getGameObjectDef().getHeight();
			} else {
				height = o.getGameObjectDef().getWidth();
				width = o.getGameObjectDef().getHeight();
			}
			for (int x = o.getX(); x < o.getX() + width; ++x) {
				for (int y = o.getY(); y < o.getY() + height; ++y) {
					if (o.getGameObjectDef().getType() == 1)
						objectValues[x][y] &= 0xffbf;
					else if (dir == 0) {
						objectValues[x][y] &= 0xfffd;
						objectValues[x - 1][y] &= 65535 - 8;
					} else if (dir == 2) {
						objectValues[x][y] &= 0xfffb;
						objectValues[x][y + 1] &= 65535 - 1;
					} else if (dir == 4) {
						objectValues[x][y] &= 0xfff7;
						objectValues[x + 1][y] &= 65535 - 2;
					} else if (dir == 6) {
						objectValues[x][y] &= 0xfffe;
						objectValues[x][y - 1] &= 65535 - 4;
					}
				}
			}
			break;
		case 1:
			if (o.getDoorDef().getDoorType() != 1) {
				return;
			}
			int x = o.getX(), y = o.getY();
			if (dir == 0) {
				objectValues[x][y] &= 0xfffe;
				objectValues[x][y - 1] &= 65535 - 4;
			} else if (dir == 1) {
				objectValues[x][y] &= 0xfffd;
				objectValues[x - 1][y] &= 65535 - 8;
			} else if (dir == 2)
				objectValues[x][y] &= 0xffef;
			else if (dir == 3)
				objectValues[x][y] &= 0xffdf;
			break;
		}
	}

	/**
	 * Removes an item from the server
	 */
	public void unregisterItem(GroundItem i) {
		i.remove();
	}

	/**
	 * Removes an npc from the server
	 */
	public void unregisterNpc(Npc n) {
		if (hasNpc(n)) {
			npcs.remove(n);
		}
		n.superRemove();
	}
	
	/**
	 * Removes a player from the server and saves their account
	 */
	public void unregisterPlayer(final Player player) {
		try {
			ActionSender.sendLogoutRequestConfirm(player);
			player.setLoggedIn(false);
			player.resetAll();

			Mob opponent = player.getOpponent();
			if (opponent != null) {
				player.resetCombatEvent();
			}
			if (Server.getPlayerDataProcessor() != null) {
				Server.getPlayerDataProcessor().getDatabase().playerOnlineFlagQuery(player.getDatabaseID(), false);
				avatarGenerator.generateAvatar(player.getDatabaseID(), player.getSettings().getAppearance(), player.getWornItems());
			}
			/*if(getFishingTrawler().getPlayers().contains(player)) {
				getFishingTrawler().quitPlayer(player);
			}*/
			if(player.getLocation().inMageArena()) {
				player.teleport(228, 121);
			}
			player.save();
			player.remove();
			players.remove(player);
			
			for (Player other : getPlayers()) {
				other.getSocial().alertOfLogout(player);
			}

			ClanManager.checkAndUnattachFromClan(player);
			LOGGER.info("Unregistered " + player.getUsername() + " from player list.");
		} catch (Exception e) {
			LOGGER.catching(e);
		}

	}

	public void unregisterQuest(QuestInterface quest) {
		if (quests.contains(quest)) {
			quests.remove(quest);
		}
	}

	/**
	 * Are the given coords within the world boundaries
	 */
	public boolean withinWorld(int x, int y) {
		return x >= 0 && x < MAX_WIDTH && y >= 0 && y < MAX_HEIGHT;
	}

	public FishingTrawler getFishingTrawler() {
		return fishingTrawler;
	}

	public void setFishingTrawler(FishingTrawler fishingTrawler) {
		this.fishingTrawler = fishingTrawler;
	}
	
	public ClanWars clanwars;
	private Territory territory;
	
	public ClanWars getClanWars() {
		return clanwars;
	}
	
	public void setClanWars(ClanWars clanwars) {
		this.clanwars = clanwars;
	}
	
	public Territory getTerritory() {
		return territory;
	}
	
	
	
}
