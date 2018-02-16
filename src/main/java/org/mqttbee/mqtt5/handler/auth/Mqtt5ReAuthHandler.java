package org.mqttbee.mqtt5.handler.auth;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.mqttbee.annotations.NotNull;
import org.mqttbee.api.mqtt5.auth.Mqtt5EnhancedAuthProvider;
import org.mqttbee.api.mqtt5.message.disconnect.Mqtt5DisconnectReasonCode;
import org.mqttbee.mqtt5.Mqtt5ClientDataImpl;
import org.mqttbee.mqtt5.Mqtt5Util;
import org.mqttbee.mqtt5.message.auth.Mqtt5AuthBuilderImpl;
import org.mqttbee.mqtt5.message.auth.Mqtt5AuthImpl;
import org.mqttbee.mqtt5.message.disconnect.Mqtt5DisconnectImpl;

import javax.inject.Inject;
import javax.inject.Singleton;

import static org.mqttbee.api.mqtt5.message.auth.Mqtt5AuthReasonCode.CONTINUE_AUTHENTICATION;
import static org.mqttbee.api.mqtt5.message.auth.Mqtt5AuthReasonCode.REAUTHENTICATE;
import static org.mqttbee.mqtt5.handler.auth.Mqtt5AuthHandlerUtil.*;

/**
 * @author Silvio Giebl
 */
@ChannelHandler.Sharable
@Singleton
public class Mqtt5ReAuthHandler extends ChannelInboundHandlerAdapter {

    public static final String NAME = "reauth";

    @Inject
    Mqtt5ReAuthHandler() {
    }

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) {
        if (evt instanceof Mqtt5ReAuthEvent) {
            writeReAuth(ctx);
        } else {
            ctx.fireUserEventTriggered(evt);
        }
    }

    private void writeReAuth(@NotNull final ChannelHandlerContext ctx) {
        final Mqtt5ClientDataImpl clientData = Mqtt5ClientDataImpl.from(ctx.channel());
        final Mqtt5EnhancedAuthProvider enhancedAuthProvider = getEnhancedAuthProvider(clientData);
        final Mqtt5AuthBuilderImpl authBuilder = getAuthBuilder(REAUTHENTICATE, enhancedAuthProvider);

        enhancedAuthProvider.onReAuth(clientData, authBuilder)
                .thenRunAsync(() -> ctx.writeAndFlush(authBuilder.build()), ctx.executor());
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        if (msg instanceof Mqtt5AuthImpl) {
            readAuth((Mqtt5AuthImpl) msg, ctx);
        } else if (msg instanceof Mqtt5DisconnectImpl) {
            readDisconnect((Mqtt5DisconnectImpl) msg, ctx);
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private void readAuth(@NotNull final Mqtt5AuthImpl auth, @NotNull final ChannelHandlerContext ctx) {
        final Mqtt5ClientDataImpl clientData = Mqtt5ClientDataImpl.from(ctx.channel());
        final Mqtt5EnhancedAuthProvider enhancedAuthProvider = getEnhancedAuthProvider(clientData);

        switch (auth.getReasonCode()) {
            case CONTINUE_AUTHENTICATION:
                readAuthContinue(ctx, auth, clientData, enhancedAuthProvider);
                break;
            case SUCCESS:
                readAuthSuccess(ctx, auth, clientData, enhancedAuthProvider);
                break;
            case REAUTHENTICATE:
                readReAuth(ctx, auth, clientData, enhancedAuthProvider);
                break;
        }
    }

    private void readAuthSuccess(
            @NotNull final ChannelHandlerContext ctx, @NotNull final Mqtt5AuthImpl auth,
            @NotNull final Mqtt5ClientDataImpl clientData,
            @NotNull final Mqtt5EnhancedAuthProvider enhancedAuthProvider) {

        enhancedAuthProvider.onReAuthSuccess(clientData, auth).thenAcceptAsync(accepted -> {
            if (!accepted) {
                writeDisconnect(ctx.channel());
            }
        }, ctx.executor());
    }

    private void readReAuth(
            @NotNull final ChannelHandlerContext ctx, @NotNull final Mqtt5AuthImpl auth,
            @NotNull final Mqtt5ClientDataImpl clientData,
            @NotNull final Mqtt5EnhancedAuthProvider enhancedAuthProvider) {

        if (clientData.allowsServerReAuth()) {
            final Mqtt5AuthBuilderImpl authBuilder = getAuthBuilder(CONTINUE_AUTHENTICATION, enhancedAuthProvider);

            enhancedAuthProvider.onServerReAuth(clientData, auth, authBuilder).thenAcceptAsync(accepted -> {
                if (accepted) {
                    ctx.writeAndFlush(authBuilder.build());
                } else {
                    writeDisconnect(ctx.channel());
                }
            }, ctx.executor());
        } else {
            Mqtt5Util.disconnect(Mqtt5DisconnectReasonCode.PROTOCOL_ERROR,
                    "Server must not send AUTH with the Reason Code REAUTHENTICATE", ctx.channel()); // TODO notify API
        }
    }

    private void readDisconnect(
            @NotNull final Mqtt5DisconnectImpl disconnect, @NotNull final ChannelHandlerContext ctx) {

        final Mqtt5ClientDataImpl clientData = Mqtt5ClientDataImpl.from(ctx.channel());
        final Mqtt5EnhancedAuthProvider enhancedAuthProvider = getEnhancedAuthProvider(clientData);

        enhancedAuthProvider.onReAuthError(clientData, disconnect);
    }


    @Singleton
    public static class Mqtt5ReAuthEvent {
        @Inject
        Mqtt5ReAuthEvent() {
        }
    }

}
