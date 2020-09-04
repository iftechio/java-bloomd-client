package bloomd;

import bloomd.decoders.Request;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;

import io.netty.channel.ChannelPromise;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.logging.Logger;

public class ConnectionHandler extends ChannelDuplexHandler {
    private static final Logger LOG = Logger.getLogger(ConnectionHandler.class.getSimpleName());

    private final Queue<Request<?>> queue = new ArrayDeque<>();

    public ConnectionHandler() {
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
        } catch (FilterDoesNotExistException e) {
            LOG.warning(e.toString());
        } catch (Exception e) {
            ctx.channel().close();
        }
    }
}
