package com.legacy.server.plugins.misc;

import static com.legacy.server.plugins.Functions.*;

import com.legacy.server.model.container.Item;
import com.legacy.server.model.entity.player.Player;
import com.legacy.server.plugins.listeners.action.InvActionListener;
import com.legacy.server.plugins.listeners.executive.InvActionExecutiveListener;

public class ItemCards implements InvActionListener, InvActionExecutiveListener {

	public static final int RUNE_PLATE_CARD = 2258;
	public static final int DRAGON_SWORD_CARD = 2259;
	public static final int DRAGON_AXE_CARD = 2260;
	public static final int KLANK_CARD = 2261;
	public static final int DRAGON_SQ_CARD = 2262;

	private void executeItemAbility(Player p, Item i) {
		p.message("Would you like to activate this card?");
		if(inArray(i.getID(), RUNE_PLATE_CARD, DRAGON_SWORD_CARD, DRAGON_AXE_CARD, KLANK_CARD, DRAGON_SQ_CARD)) {
			switch(i.getID()) {
			case RUNE_PLATE_CARD:
				if(p.getCache().hasKey("rune_plate_card")) {
					p.message("You have already activated this card.");
					return;
				}
				int menu = showMenu(p, "Activate my rune plate body card.", "Cancel.");
				if(menu == 0) {
					if(p.getSkills().getLevel(MAGIC) >= 33) {
						p.message("@gre@Congratulations! You now have the ability to carry rune plate mail body");
						if(!p.getCache().hasKey("rune_plate_card")) {
							p.getCache().store("rune_plate_card", true);
						}
						removeItem(p, RUNE_PLATE_CARD, 1);
					} else {
						message(p, 1200, "Rune plate mail body requires 33 magic, you have magic level: " + p.getSkills().getLevel(MAGIC) + ".",
								"To earn the ability to wear rune plate without 33 magic",
								"it has a cost of 1,000,000GP.");
						int magicMenu = showMenu(p, "Yes activate my card.", "Cancel.");
						if(magicMenu == 0) {
							if(hasItem(p, 10, 1000000)) {
								p.message("@gre@Congratulations! You now have the ability to carry rune plate mail body");
								if(!p.getCache().hasKey("rune_plate_card")) {
									p.getCache().store("rune_plate_card", true);
								}
								removeItem(p, RUNE_PLATE_CARD, 1);
								removeItem(p, 10, 1000000);
							} else {
								p.message("It looks like you don't have 1,000,000 coins in your inventory.");
							}
						} else if(magicMenu == 1) {
							p.message("You decide to not activate the card.");
						}
					}
				} else if(menu == 1) {
					p.message("You decide to not activate the rune plate body card.");
				}
				break;
			case DRAGON_SWORD_CARD:
				if(p.getCache().hasKey("dragon_sword_card")) {
					p.message("You have already activated this card.");
					return;
				}
				int menu2 = showMenu(p, "Activate my dragon sword card.", "Cancel.");
				if(menu2 == 0) {
					p.message("@gre@Congratulations! You now have the ability to carry dragon sword");
					if(!p.getCache().hasKey("dragon_sword_card")) {
						p.getCache().store("dragon_sword_card", true);
					}
					removeItem(p, DRAGON_SWORD_CARD, 1);
				} else if(menu2 == 1) {
					p.message("You decide to not activate the dragon sword card.");
				}
				break;
			case DRAGON_AXE_CARD:
				if(p.getCache().hasKey("dragon_axe_card")) {
					p.message("You have already activated this card.");
					return;
				}
				int menu3 = showMenu(p, "Activate my dragon axe card.", "Cancel.");
				if(menu3 == 0) {
					p.message("@gre@Congratulations! You now have the ability to carry dragon axe");
					if(!p.getCache().hasKey("dragon_axe_card")) {
						p.getCache().store("dragon_axe_card", true);
					}
					removeItem(p, DRAGON_AXE_CARD, 1);
				} else if(menu3 == 1) {
					p.message("You decide to not activate the dragon axe card.");
				}
				break;
			case KLANK_CARD:
				if(p.getCache().hasKey("klank_card")) {
					p.message("You have already activated this card.");
					return;
				}
				int menu4 = showMenu(p, "Activate my klank card.", "Cancel.");
				if(menu4 == 0) {
					p.message("@gre@Congratulations! You now have the ability to carry klanks gauntlets");
					if(!p.getCache().hasKey("klank_card")) {
						p.getCache().store("klank_card", true);
					}
					removeItem(p, KLANK_CARD, 1);
				} else if(menu4 == 1) {
					p.message("You decide to not activate the klank card.");
				}
				break;
			case DRAGON_SQ_CARD:
				if(p.getCache().hasKey("dragon_sq_card")) {
					p.message("You have already activated this card.");
					return;
				}
				int menu5 = showMenu(p, "Activate my dragon square shield card.", "Cancel.");
				if(menu5 == 0) {
					p.message("@gre@Congratulations! You now have the ability to carry dragon square shield");
					if(!p.getCache().hasKey("dragon_sq_card")) {
						p.getCache().store("dragon_sq_card", true);
					}
					removeItem(p, DRAGON_SQ_CARD, 1);
				} else if(menu5 == 1) {
					p.message("You decide to not activate the dragon square shield card.");
				}
				break;
			}
		}
	}

	@Override
	public boolean blockInvAction(Item item, Player p) {
		return inArray(item.getID(), RUNE_PLATE_CARD, DRAGON_SWORD_CARD, DRAGON_AXE_CARD, KLANK_CARD, DRAGON_SQ_CARD);
	}

	@Override
	public void onInvAction(Item item, Player p) {
		if(inArray(item.getID(), RUNE_PLATE_CARD, DRAGON_SWORD_CARD, DRAGON_AXE_CARD, KLANK_CARD, DRAGON_SQ_CARD)) {
			executeItemAbility(p, item);
		}
	}
}
