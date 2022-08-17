package com.legacy.server.net.rsc.handlers;

import com.legacy.server.Constants;
import com.legacy.server.external.EntityHandler;
import com.legacy.server.external.ItemDefinition;
import com.legacy.server.model.Shop;
import com.legacy.server.model.container.Item;
import com.legacy.server.model.entity.player.Player;
import com.legacy.server.net.Packet;
import com.legacy.server.net.rsc.ActionSender;
import com.legacy.server.net.rsc.OpcodeIn;
import com.legacy.server.net.rsc.PacketHandler;
import com.legacy.server.sql.GameLogging;
import com.legacy.server.sql.query.logs.GenericLog;

public final class InterfaceShopHandler implements PacketHandler {

	/**
	 * Author: Imposter
	 */

	public void handlePacket(Packet p, Player player) throws Exception {

		int pID = p.getID();
		if (player.isBusy()) {
			player.resetShop();
			return;
		}

		final Shop shop = player.getShop();
		if (shop == null) {
			player.setSuspiciousPlayer(true);
			player.resetShop();
			return;
		}

		int packetOne = OpcodeIn.SHOP_CLOSE.getOpcode();
		int packetTwo = OpcodeIn.SHOP_BUY.getOpcode();
		int packetThree = OpcodeIn.SHOP_SELL.getOpcode();

		if (pID == packetOne) { // Close shop
			player.resetShop();
			return;
		}
		int itemID = p.readShort();
		int shopAmount = p.readShort();
		int amount = p.readShort();
		ItemDefinition def = EntityHandler.getItemDef(itemID);
		if (def.isMembersOnly() && !Constants.GameServer.MEMBER_WORLD) {
			player.sendMemberErrorMessage();
			return;
		}
		if (pID == packetTwo) { // Buy item
			int totalBought = 0;
			int totalMoneySpent = 0;

			int price = shop.getItemBuyPrice(itemID, def.getDefaultPrice(), 0);
			if (player.getInventory().countId(10) == price && player.getInventory().size() == 30 && amount == 1) {
				if (shop.getItemCount(itemID) - totalBought < 1) {
					player.message("The shop has ran out of stock");
					return;
				}
				totalBought++;
				totalMoneySpent += price;
				amount = 0;
			}

			for (int i = 0; i < amount; i++) {
				Item itemBeingBought = new Item(itemID, 1);
				if ((player.isIronMan(1) || player.isIronMan(2) || player.isIronMan(3)) && shop.getItemCount(itemID) > shop.getStock(itemID)) {
					player.message("Iron Men may not buy items that are over-stocked in a shop.");
					break;
				}
				if (shop.getItemCount(itemID) - totalBought < 1) {
					player.message("The shop has ran out of stock");
					break;
				}
				if(itemBeingBought.getID() == 1006 && (player.getQuestStage(Constants.Quests.UNDERGROUND_PASS) != -1 && !player.getCache().hasKey("klank_card"))) {
					player.message("You need to activate the Klank Card or complete Underground pass quest first.");
					break;
				}
				price = shop.getItemBuyPrice(itemID, def.getDefaultPrice(), totalBought);
				if (player.getInventory().countId(10) < (totalMoneySpent + price)) {
					player.message("You don't have enough coins");
					break;
				}
				if (!player.getInventory().canHold(itemBeingBought, totalBought)) {
					player.message("You can't hold the objects you are trying to buy!");
					break;
				}
				if (player.getInventory().size() + totalBought >= 30 && !itemBeingBought.getDef().isStackable()) {
					break;
				}
				totalMoneySpent += price;
				totalBought++;
			}

			if (totalBought <= 0 && totalMoneySpent <= 0) {
				return;
			}
			shop.removeShopItem(new Item(itemID, totalBought));
			player.getInventory().remove(10, totalMoneySpent);
			int correctItemsBought = totalBought;
			for (; totalBought > 0; totalBought--) {
				player.getInventory().add(new Item(itemID, 1), false);
			}
			ActionSender.sendInventory(player);
			player.playSound("coins");
			GameLogging.addQuery(new GenericLog(player.getUsername(),"bought " + def.getName() + " x" + correctItemsBought + " for " + totalMoneySpent + "gp" + " at " + player.getLocation().toString()));

		} else if (pID == packetThree) { // Sell item
			if (def.isUntradable() || !shop.shouldStock(itemID)) {
				player.message("This object can't be sold in shops");
				return;
			}
			
			if (!shop.canHoldItem(new Item(itemID, 1))) {
				player.message("The shop is currently full!");
				return;
			}
			
			if (def.getName().equalsIgnoreCase("Trout")) {
				player.message("The shop is not interested in buying this item");
				return;
			}
			
			if (amount < 1) {
				return;
			}

			if (player.getInventory().countId(itemID) < amount) {
				player.message("You don't have that many items");
				if (player.getInventory().countId(itemID) == 0) {
					return;
				}
				amount = player.getInventory().countId(itemID);
			}
			
			int totalMoney = 0;
			int totalSold = 0;
			for (int i = 0; i < amount; i++) {
				totalSold++;
				if (def.getOriginalItemID() != -1) {
					totalMoney += shop.getItemSellPrice(def.getOriginalItemID(),
							EntityHandler.getItemDef(def.getOriginalItemID()).getDefaultPrice(), totalSold);
				} else {
					totalMoney += shop.getItemSellPrice(itemID, def.getDefaultPrice(), totalSold);
				}
			}
			
			if (totalMoney > 0) {
				player.getInventory().add(new Item(10, totalMoney), false);
			} 
			
			Item originalItem = null;
			if (def.getOriginalItemID() != -1) {
				originalItem = new Item(def.getOriginalItemID(), totalSold);
			}
			
			shop.addShopItem(originalItem != null ? originalItem : new Item(itemID, totalSold));
			if(!def.isStackable()) {
				for (int i = 0; i < amount; ++i)
					player.getInventory().remove(player.getInventory().get(player.getInventory().getLastIndexById(itemID)));
			} else {
				player.getInventory().remove(itemID, totalSold);
			}
			
			shop.updatePlayers();
			player.playSound("coins");
			ActionSender.sendInventory(player);
			GameLogging.addQuery(new GenericLog(player.getUsername(), "sold " + def.getName() + " x" + totalSold
					+ " for " + totalMoney + "gp" + " at " + player.getLocation().toString()));
		}
	}
}
