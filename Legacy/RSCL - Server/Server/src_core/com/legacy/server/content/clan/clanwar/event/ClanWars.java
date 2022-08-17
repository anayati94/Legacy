package com.legacy.server.content.clan.clanwar.event;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.legacy.server.content.clan.Clan;
import com.legacy.server.event.DelayedEvent;
import com.legacy.server.model.entity.player.Player;
import com.legacy.server.model.world.World;
import com.legacy.server.util.rsc.DataConversions;

public class ClanWars extends DelayedEvent {
	
	/**
     * The asynchronous logger.
     */
    private static final Logger LOGGER = LogManager.getLogger();

	private final int hours = 2;
	private final int minutes = 15;

	private long lastAnnouncement;
	public long nextWar = 0L;

	private ArrayList<Player> players = new ArrayList<Player>();

	HashMap<Player,String> playerListFromClans = new HashMap<Player,String>();

	public HashMap<Player, String> getPlayerListFromClans() {
		return playerListFromClans;
	}

	public void setPlayerListFromClans(HashMap<Player, String> playerListFromClans) {
		this.playerListFromClans = playerListFromClans;
	}

	private boolean started;

	public ClanWars() {
		super(null, 1000);
		setNextWar();
		if(warRules == null)
			generateClanWarRules();
	}

	public long getNextWar() {
		return nextWar;
	}

	public ArrayList<Player> getPlayers() {
		return players;
	}

	public int[] getTimeToWarLeft() {
		long timeLeftMillis = (nextWar - System.currentTimeMillis());
		int[] time = new int[3];

		time[0] = (int) timeLeftMillis / 1000;
		time[1] = (int) time[0] / 60;
		time[2] = (int) time[1] / 60;

		int[] correctedFormat = new int[3];
		correctedFormat[0] = (time[0] % 60 < 10 ? 0 + (time[0] % 60) : (time[0] % 60));
		correctedFormat[1] = (time[1] % 60 < 10 ? 0 + (time[1] % 60) : (time[1] % 60));
		correctedFormat[2] = time[2];

		return correctedFormat;
	}

	@Override
	public void run() {
		try {
			if (System.currentTimeMillis() - lastAnnouncement >= 10000 && !started) {
				int[] timeTillWar = getTimeToWarLeft();
				for (Player player : World.getWorld().getPlayers()) {
					player.message(
							"@cla@@whi@[@or3@Clanwars@whi@] @whi@Clanwars starts in: @or3@" +timeTillWar[2] + " @whi@hours @or3@" + timeTillWar[1] + "@whi@ mins @or3@" + timeTillWar[0] + "@whi@ seconds.");
					player.message("@cla@@whi@[@or3@Rules@whi@]@whi@ " + warRules);
				}
				lastAnnouncement = System.currentTimeMillis();
			}
			if (getTimeToWarLeft()[1] <= 0 && getTimeToWarLeft()[2] <= 0
					&& !started) {
				if (getTimeToWarLeft()[0] > 29 && getTimeToWarLeft()[0] < 31) {
					globalWarMessage("@red@[@whi@Clan Wars@red@]@whi@: @ran@Clan Wars start in: "
							+ getTimeToWarLeft()[0] + " seconds");
				}
				if (getTimeToWarLeft()[0] < 5 && getTimeToWarLeft()[0] > 0) {
					globalWarMessage("@red@[@whi@Clan Wars@red@]@whi@: @ran@Clan Wars start in: "
							+ getTimeToWarLeft()[0] + " seconds");
					globalWarMessage("@red@[@whi@Clan Wars@red@]@whi@: Get ready for battle!");
				} else if (getTimeToWarLeft()[0] <= 0) {
					globalWarMessage("@ran@Clan wars have begun!!!");
				}
			}
			if (!isStarted() & shouldStart()) {
				if (start()) {

				} else {
					globalMessage("@cla@@whi@[@or3@Clanwars@whi@] Not enough players, clan wars has been postponed @or3@15 @whi@minutes!");
				}
			}
			if (isStarted()) {
				ArrayList<Clan> tmpList = new ArrayList<Clan>();
				ArrayList<Player> toRemove = new ArrayList<Player>();
				for (Player p : players) {
					if (p.isRemoved() || !p.getLocation().inClanWarArena()) {
						toRemove.add(p);
						continue;
					}
					Clan clan = p.getClan();
					if (!tmpList.contains(clan)) {
						tmpList.add(clan);
					}
				}
				/*
				 * Preventing ConcurrentModificationException
				 */
				for (Player pRemove : toRemove) {
					pRemove.setInClanWars(false);
					players.remove(pRemove);
				}
				if (tmpList.size() == 1) {
					Clan winnerClan = tmpList.get(0);
					//World.getWorld().getTerritory().setOwner(winnerClan);
					//globalWonTerritoryMessage("@cla@@whi@[@or3@Clanwars@whi@] @cya@"
					//	+ winnerClan.getTitle() + " @whi@has won the territory!");
					for (Player player : players) {
						player.teleport(216, 451, false);
						player.setInClanWars(false);
						player.removeSkull();
					}
					resetClanWars();
				} else if (tmpList.size() < 1) {
					resetClanWars();
				}
			}
		} catch (Exception e) {
			LOGGER.catching(e);
		}
	}

	public boolean start() {
		if (players.size() <= 0) {
			World.getWorld().getClanWars().setNextWar(0, 15);
			setStarted(false);
			return false;
		}
		setStarted(true);
		return true;
	}

	public void globalMessage(String s) {
		for (Player p : World.getWorld().getPlayers()) {
			if(p.getClan() != null) {
				p.message(s);
			}
		}
	}

	public void globalWonTerritoryMessage(String s) {
		for (Player p : World.getWorld().getPlayers()) {
			p.message(s);
			//p.getActionSender().sendTerritoryOwner();
		}
	}

	public void globalWarMessage(String s) {
		for (Player p : players) {
			p.message(s);
		}
	}

	public void setNextWar() {
		nextWar = System.currentTimeMillis()
				+ (((hours * 60) + minutes) * 60 * 1000);
	}

	public void setNextWar(int hourse, int minutee) {
		nextWar = System.currentTimeMillis()
				+ (((hourse * 60) + minutee) * 60 * 1000);
	}

	public void setNextWar(long nextWar) {
		this.nextWar = nextWar;
	}

	private boolean shouldStart() {
		return (getTimeToWarLeft()[2] <= 0 && getTimeToWarLeft()[1] <= 0 && getTimeToWarLeft()[0] <= 0);
	}

	public void resetClanWars() {
		setStarted(false);
		setNextWar();
		warRules = null;
		players.clear();
	}

	public void remove(Player toRemove) {
		players.remove(toRemove);
	}

	public void add(Player add) {
		players.add(add);
	}

	public boolean isStarted() {
		return started;
	}

	public void setStarted(boolean started) {
		this.started = started;
	}

	public String warRules = null;

	private final int[] CLAN_WAR_LEVELS = { 33, 41, 88 };
	public int CLANWAR_LEVEL;
	public boolean CLANWAR_GODMAGE;
	public boolean CLANWAR_MAGE;
	public boolean CLANWAR_RANGE;
	public boolean CLANWAR_F2P_OR_P2P;

	public void generateClanWarRules() {
		final int FINAL_CW_LEVEL = (int) (Math.random() * (CLAN_WAR_LEVELS.length - 1));
		CLANWAR_LEVEL = CLAN_WAR_LEVELS[FINAL_CW_LEVEL];
		switch(CLANWAR_LEVEL) {
		case 33:
		case 41:
			CLANWAR_F2P_OR_P2P = true; /** TRUE = F2P - FALSE = P2P **/
			CLANWAR_RANGE = true;
			CLANWAR_GODMAGE = false;
			CLANWAR_MAGE = false;
			break;
		case 88:
			CLANWAR_F2P_OR_P2P = true; /** TRUE = F2P - FALSE = P2P **/
			// Randomize godmage if P2P is selected otherwise no godmage.
			if(!CLANWAR_F2P_OR_P2P) { 
				CLANWAR_GODMAGE = DataConversions.random(0, 1) == 1; 
			} else {
				CLANWAR_GODMAGE = false;
			}
			CLANWAR_RANGE = true;
			CLANWAR_MAGE = true;
			break;
		}
		warRules = 
				"Combat Level: @yel@" + CLANWAR_LEVEL + (CLANWAR_LEVEL == 88 ? "+" : "") 
				+ " @whi@Ranged: " + (CLANWAR_RANGE ? "@gre@On" : "@red@Off") 
				+ " @whi@Magic: " + (CLANWAR_MAGE ? "@gre@On" : "@red@Off") 
				+ " @whi@God Mage: " + (CLANWAR_GODMAGE ? "@gre@On" : "@red@Off") 
				+ " @whi@Mode: " + (CLANWAR_F2P_OR_P2P ? "@cya@F2P" : "@cya@P2P");
	}
}

