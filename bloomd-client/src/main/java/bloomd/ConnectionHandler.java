package bloomd;

import bloomd.decoders.Request;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;

import io.netty.channel.ChannelPromise;
import java.util.ArrayDeque;
import java.util.Queue;

public class ConnectionHandler extends ChannelDuplexHandler {

    private final Queue<Request<?>> queue = new ArrayDeque<>();
    private final ConnectionListener listener;

    public ConnectionHandler(ConnectionListener listener) {
        this.listener = listener;
    }

    @Override
    public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
        Request<?> request = (Request<?>) msg;
        queue.add(request);

        // send command
        ctx.writeAndFlush(request.getCommand() + "\r\n");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        final Request<?> request = queue.poll();
        if (request == null) {
            throw new Exception("Unexpected response: " + msg);
        }

        try {
            request.handle((String) msg);
        } catch (Exception e) {
            listener.onError(e);
            ctx.channel().close();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        listener.onDisconnect();
    }

    public interface ConnectionListener {
        void onDisconnect();
        void onError(Exception e);
    }
}
