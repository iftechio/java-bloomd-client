package bloomd;

import bloomd.decoders.BloomdResponseDecoder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringEncoder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Creates a newly configured {@link ChannelPipeline} for a new channel.
 */
public class ClientInitializer {

    private static final Logger LOG = Logger.getLogger(ClientInitializer.class.getSimpleName());

    private final Map<Channel, BloomdClient> registry = new ConcurrentHashMap<>();

    public void initChannel(Channel ch) {
        // associate this new channel with a new BloomdClient implementation
        BloomdClientImpl bloomdClient = new BloomdClientImpl(ch);
        bloomdClient.setBlocked(false);
        registry.put(ch, bloomdClient);

        // reconfigure the pipeline of this channel
        ChannelPipeline pipeline = ch.pipeline();

        // add logging handler
        pipeline.addLast(new ChannelInboundHandlerAdapter() {
            @Override
            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                super.channelInactive(ctx);
                LOG.warning(String.format("Channel disconnected: %s", ctx.channel()));
            }
        });

        // 必要的，防止TCP相应"拆包"
        pipeline.addLast(new DelimiterBasedFrameDecoder(Integer.MAX_VALUE, Delimiters.lineDelimiter()));
        // Add the text line codec combination first
        pipeline.addLast(new BloomdResponseDecoder());
        pipeline.addLast(new StringEncoder());

        // and then business logic
        pipeline.addLast(bloomdClient.getConnectionHandler());
    }

    public BloomdClient getClient(Channel channel) {
        BloomdClient clientImpl = registry.get(channel);

        if (clientImpl == null) {
            throw new IllegalStateException("Could not find client implementation from channel " + channel);
        }

        return clientImpl;
    }

    public void unlockClient(Channel ch) {
        setClientBlocked(ch, false);
    }

    public void lockClient(Channel ch) {
        setClientBlocked(ch, true);
    }

    private void setClientBlocked(Channel ch, boolean blocked) {
        BloomdClient client = getClient(ch);

        if (client instanceof BloomdClientImpl) {
            BloomdClientImpl bloomdClient = (BloomdClientImpl) client;
            bloomdClient.setBlocked(blocked);
        }
    }
}
