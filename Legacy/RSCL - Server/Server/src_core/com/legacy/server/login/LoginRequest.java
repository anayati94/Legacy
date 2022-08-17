package com.legacy.server.login;

import com.legacy.server.model.entity.player.Player;
import com.legacy.server.util.rsc.DataConversions;

import io.netty.channel.Channel;

/**
 * Container for all the login data which will be used to construct a player
 * @author n0m
 *
 */
public abstract class LoginRequest {
	
	private String ipAddress;
	private String username;
	private String password;
	private String macAddress;
	private long usernameHash;
	private Channel channel;
	private int clientVersion;
	private long UID;
	
	protected Player loadedPlayer;
	
	public LoginRequest(String username, String password, int clientVersion, long uid, String macAddress, Channel channel) {
		this.setUsername(username);
		this.setPassword(password);
		this.setIpAddress(channel.remoteAddress().toString());
		this.setClientVersion(clientVersion);
		this.setUID(uid);
		this.setMacAddress(macAddress);
		this.setUsernameHash(DataConversions.usernameToHash(username));
		this.setChannel(channel);
	}
	
	public long getUID() {
		return UID;
	}
	
	public void setUID(long uid) {
		this.UID = uid;
	}
	
	public String getMacAddress() {
		return macAddress;
	}
	
	public void setMacAddress(String macAddress) {
		this.macAddress = macAddress;
	}
	
	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public long getUsernameHash() {
		return usernameHash;
	}

	public void setUsernameHash(long usernameHash) {
		this.usernameHash = usernameHash;
	}

	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}

	public int getClientVersion() {
		return clientVersion;
	}

	public void setClientVersion(int clientVersion) {
		this.clientVersion = clientVersion;
	}
	
	public abstract void loginValidated(int response);
	public abstract void loadingComplete(Player loadedPlayer);
}
