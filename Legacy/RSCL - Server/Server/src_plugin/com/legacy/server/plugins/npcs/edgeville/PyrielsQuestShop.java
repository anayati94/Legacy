package com.legacy.server.plugins.npcs.edgeville;

import static com.legacy.server.plugins.Functions.*;

import com.legacy.server.model.Shop;
import com.legacy.server.model.container.Item;
import com.legacy.server.model.entity.npc.Npc;
import com.legacy.server.model.entity.player.Player;
import com.legacy.server.net.rsc.ActionSender;
import com.legacy.server.plugins.ShopInterface;
import com.legacy.server.plugins.listeners.action.TalkToNpcListener;
import com.legacy.server.plugins.listeners.executive.TalkToNpcExecutiveListener;

public class PyrielsQuestShop implements ShopInterface, TalkToNpcListener, TalkToNpcExecutiveListener {

	private final Shop shop = new Shop(false, 60000, 100, 50, 2, new Item(2258, 3),
			new Item(2259, 3), new Item(2260, 3), new Item(2261, 3), new Item(2262, 3),
			new Item(401, 5), new Item(407, 5), new Item(593, 3), new Item(594, 3),
			new Item(1006, 10));

	private static final int PYRIEL = 803;

	@Override
	public boolean blockTalkToNpc(Player p, Npc n) {
		return n.getID() == PYRIEL;
	}

	@Override
	public void onTalkToNpc(Player p, Npc n) {
		if(n.getID() == PYRIEL) {
			npcTalk(p, n, "Hello, my shop has quest goods.", 
					"Is there anything you need help with?");
			int menu = showMenu(p, n,
					"Yes please, I'd like to see your shop.",
					"Can you tell me about these goods?",
					"No thanks.");
			if (menu == 0) {
				npcTalk(p, n, "Have a look.");
				p.setAccessingShop(shop);
				ActionSender.showShop(p, shop);
			} else if (menu == 1) {
				npcTalk(p, n, "Item cards can be activated once to give",
						"you the permanent ability to wear these items without",
						"completing the corresponding quest.");
			} else if (menu == 2) {
				npcTalk(p, n, "Enjoy your day.");
			}
		}
	}

	@Override
	public Shop[] getShops() {
		return new Shop[] { shop };
	}

	@Override
	public boolean isMembers() {
		return true;
	}

}
