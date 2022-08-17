package com.legacy.server.net;

import com.legacy.server.Constants;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

public class ChannelPipelineHandler extends ChannelInitializer<SocketChannel> {

	/**
	 * The part of the pipeline that limits connections, and checks for any banned hosts.
	 */
	private final RSCChannelFilter FILTER = new RSCChannelFilter();

	@Override
	protected void initChannel(SocketChannel channel) throws Exception {
		final ChannelPipeline pipeline = channel.pipeline();

		channel.attr(Constants.GameServer.SESSION_KEY).setIfAbsent(new ConnectionAttachment());

		pipeline.addLast("channel-filter", FILTER);
		pipeline.addLast("decoder", new RSCProtocolDecoder());
		pipeline.addLast("encoder", new RSCProtocolEncoder());
		pipeline.addLast("timeout", new IdleStateHandler(15, 0, 0));
		pipeline.addLast("channel-handler", new RSCConnectionHandler());

	}
}
