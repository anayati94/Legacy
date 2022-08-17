package com.legacy.server.model;

import com.legacy.server.model.entity.Mob;
import com.legacy.server.model.entity.npc.Npc;
import com.legacy.server.model.entity.player.Player;
import com.legacy.server.model.world.World;
import com.legacy.server.model.world.region.Region;
import com.legacy.server.model.world.region.RegionManager;

/**
 * <p>
 * A <code>WalkingQueue</code> stores steps the client needs to walk and allows
 * this queue of steps to be modified.
 * </p>
 * 
 * <p>
 * The class will also process these steps when {@link #processNextMovement()}
 * is called. This should be called once per server cycle.
 * </p>
 * 
 * @author Graham Edgecombe
 * 
 */
public class WalkingQueue {

	//private static final boolean CHARACTER_FACING = true;

	private Mob mob;
	
	private Path path;

	public WalkingQueue(Mob entity) {
		this.mob = entity;
	}
	
	private boolean isBlocking(int objectValue, byte bit) {
		if ((objectValue & bit) != 0) { // There is a wall in the way
			return true;
		}
		if ((objectValue & 16) != 0) { // There is a diagonal wall here: \
			return true;
		}
		if ((objectValue & 32) != 0) { // There is a diagonal wall here: /
			return true;
		}
		if ((objectValue & 64) != 0) { // This tile is unwalkable
			return true;
		}
		return false;
	}

	public boolean isBlocking(int x, int y, int bit) {
		if (!World.getWorld().withinWorld(x, y))
			return true;
		if (mob.getAttribute("no_clip", false)) {
			return false;
		}
		return isBlocking(World.mapValues[x][y], (byte)bit) || isBlocking(World.objectValues[x][y], (byte)bit) || isMobBlocking(x, y);
	}

	private boolean isMobBlocking(int x, int y) {
		Region region = RegionManager.getRegion(Point.location(x, y));
		if (mob.isNpc()) {
			Npc npc = region.getNpc(x, y);
			Npc n = ((Npc) mob);
			if (n.isChasing()) {
				Player chasedPlayer = (Player) n.getChasedPlayer();
				if (chasedPlayer != null) {
					if (!chasedPlayer.withinRange(mob, 2)) {
						return false;
					}
				}
			}
			if (npc != null) {
				return true;
			} 
		}
		return false;
	}

	/**
	 * Processes the next player's movement.
	 */
	public void processNextMovement() {
		if(path == null) {
			return;
		} else if(path.isEmpty()) {
			reset();
			return;
		}
		Point walkPoint = path.poll();
		
		if (mob.getAttribute("blink", false)) {
			if (path.size() >= 1) {
				walkPoint = path.getLastPoint();
				((Player) mob).teleport(walkPoint.getX(), walkPoint.getY(), false);
			}
			return;
		}
		
		int startX = mob.getX();
		int startY = mob.getY();
		int destX = walkPoint.getX();
		int destY = walkPoint.getY();

		if (!mob.getAttribute("noclip", false)) {
			int[] coords = { startX, startY };
			boolean myXBlocked = false, myYBlocked = false, newXBlocked = false, newYBlocked = false;
			if (startX > destX) {
				myXBlocked = isBlocking(startX - 1, startY, 8);
				coords[0] = startX - 1;
			} else if (startX < destX) {
				myXBlocked = isBlocking(startX + 1, startY, 2);
				coords[0] = startX + 1;
			}

			if (startY > destY) {
				myYBlocked = isBlocking(startX, startY - 1, 4);
				coords[1] = startY - 1;
			} else if (startY < destY) {
				myYBlocked = isBlocking(startX, startY + 1, 1);
				coords[1] = startY + 1;
			}

			if ((myXBlocked && myYBlocked) || (myXBlocked && startY == destY)
					|| (myYBlocked && startX == destX)) {
				reset();
				return;
			}

			if (coords[0] > startX) {
				newXBlocked = isBlocking(coords[0], coords[1], 2);
			} else if (coords[0] < startX) {
				newXBlocked = isBlocking(coords[0], coords[1], 8);
			}

			if (coords[1] > startY) {
				newYBlocked = isBlocking(coords[0], coords[1], 1);
			} else if (coords[1] < startY) {
				newYBlocked = isBlocking(coords[0], coords[1], 4);
			}

			if ((newXBlocked && newYBlocked) || (newXBlocked && startY == coords[1])
					|| (myYBlocked && startX == coords[0])) {
				reset();
				return;
			}
			if ((myXBlocked && newXBlocked) || (myYBlocked && newYBlocked)) {
				reset();
				return;
			}
		}
		mob.setLocation(Point.location(walkPoint.getX(), walkPoint.getY()));
	}


	public void reset() {
		path = null;
	}

	public boolean finished() {
		return path == null || path.isEmpty();
	}

	public void setPath(Path path) {
		this.path = path;
	}
}