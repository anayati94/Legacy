package com.legacy.server.net;

import java.util.concurrent.atomic.AtomicReference;

import com.legacy.server.model.entity.player.Player;

public class ConnectionAttachment {
	
	public AtomicReference<Player> player = new AtomicReference<Player>();
	
}
