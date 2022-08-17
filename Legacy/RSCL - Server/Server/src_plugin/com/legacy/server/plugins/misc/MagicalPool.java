package com.legacy.server.plugins.misc;

import static com.legacy.server.plugins.Functions.message;
import static com.legacy.server.plugins.Functions.movePlayer;
import static com.legacy.server.plugins.Functions.showMenu;

import com.legacy.server.model.entity.GameObject;
import com.legacy.server.model.entity.player.Player;
import com.legacy.server.plugins.listeners.action.ObjectActionListener;
import com.legacy.server.plugins.listeners.executive.ObjectActionExecutiveListener;
import com.legacy.server.util.rsc.DataConversions;

public class MagicalPool implements ObjectActionListener, ObjectActionExecutiveListener {


	@Override
	public boolean blockObjectAction(GameObject obj, String command, Player player) {
		if(obj.getID() == 1166) { // mage arena gods place pool.
			return true;
		}
		if (obj.getID() == 1155) {
			return true;
		}
		return false;
	}

	@Override
	public void onObjectAction(GameObject obj, String command, Player player) {
		if (obj.getID() == 1155) {
			if (!player.canUsePool()) {
				player.message("You have just died, you must wait for "
										+ player.secondsUntillPool()
										+ " seconds before using this pool again");
				return;
			}
			while (System.currentTimeMillis()
					- player.getLastMoved() < 10000
					&& player.getLocation().inWilderness()) {
				player.message("You must stand still for 10 seconds before using portal");
				return;
			}
			while (System.currentTimeMillis()
					- player.getCombatTimer() < 10000
					&& player.getLocation().inWilderness()) {
				player.message("You must be out of combat for 10 seconds before using portal");
				return;
			}
			int option = showMenu(player, "Edgeville", "Varrock",
					"Castle (dangerous)", "Graveyard (dangerous)", "Hobgoblins (dangerous)", "Altar (dangerous)",
					"Dragon Maze (dangerous)", "Mage Arena (dangerous)", "Rune rocks (dangerous)", "Red dragons (dangerous)", "Further underground mage arena");
			
			if (option == 0) {
				player.teleport(215, 436);
			} else if (option == 1) {
				player.teleport(111, 505);
			} else if (option == 2) {
				player.teleport(DataConversions.random(265, 279), DataConversions.random(344, 364)); // CASTLE
			} else if (option == 3) {
				player.teleport(DataConversions.random(178, 198), DataConversions.random(284, 304));
			} else if (option == 4) {
				player.teleport(DataConversions.random(207, 227), DataConversions.random(260, 280));
			} else if (option == 5) {
				player.teleport(DataConversions.random(305, 325), DataConversions.random(190, 210));
			} else if (option == 6) {
				player.teleport(DataConversions.random(264, 279), DataConversions.random(191, 205));
			} else if (option == 7) {
				player.teleport(224, 110); // MAGE ARENA
			} else if (option == 8) {
				player.teleport(DataConversions.random(262, 275), DataConversions.random(142, 154));
			} else if(option == 9) {
				player.teleport(143, 173); // TODO RED DRAGONS
			} else if(option == 10) {
				movePlayer(player, 471, 3385);
				player.message("you are teleported further under ground");
			}

		}
		if (obj.getID() == 1166) {
			message(player, 1200, "you step into the sparkling water",
					"you feel energy rush through your veins");
			movePlayer(player, 447, 3373);
			player.message("you are teleported to kolodions cave");
		}
	}
}
