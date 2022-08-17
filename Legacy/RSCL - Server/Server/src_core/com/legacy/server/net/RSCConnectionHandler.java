package com.legacy.server.net;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.legacy.server.Constants;
import com.legacy.server.model.entity.player.Player;
import com.legacy.server.net.rsc.LoginPacketHandler;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;

/**
 * 
 * @author Imposter
 * 
 */
@Sharable public final class RSCConnectionHandler extends SimpleChannelInboundHandler<Object> {

	/**
	 * The asynchronous logger.
	 */
	private static final Logger LOGGER = LogManager.getLogger();

	public LoginPacketHandler loginHandler = new LoginPacketHandler();

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object message) throws Exception {
		try {
			final Channel channel = ctx.channel();

			if (message instanceof Packet) {
				final Packet packet = (Packet) message;
				Player player = null;
				ConnectionAttachment att = channel.attr(Constants.GameServer.SESSION_KEY).get();
				if (att != null) {
					player = att.player.get();
				}
				if (player == null) {
					loginHandler.processLogin(packet, channel);
				} else {
					if (loginHandler != null) {
						loginHandler = null;
					}
					player.addToPacketQueue(packet);
				}
			}
		} catch(Exception e) {
			LOGGER.throwing(e);
		} finally {
			ReferenceCountUtil.release(message);
		}
	}

	@Override
	public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
		final Channel channel = ctx.channel();
		final ConnectionAttachment conn_attachment = channel.attr(Constants.GameServer.SESSION_KEY).get();

		Player player = null;
		if (conn_attachment != null) {
			player = conn_attachment.player.get();
		}
		if (player != null) {
			player.unregister(false, "Channel closed");
		}
	}
	
	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent event = (IdleStateEvent) evt;
			if (event.state() == IdleState.READER_IDLE) {
				ctx.channel().close();
			}
		}
	}

	@Override
	public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable e) throws Exception {
		if (ctx.channel().isActive())
			ctx.channel().close();
	}

}