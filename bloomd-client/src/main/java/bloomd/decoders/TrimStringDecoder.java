package bloomd.decoders;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.nio.charset.Charset;
import java.util.List;

@Sharable
public class TrimStringDecoder extends MessageToMessageDecoder<ByteBuf> {
    private final Charset charset;

    /**
     * Creates a new instance with the current system character set.
     */
    public TrimStringDecoder() {
        this(Charset.defaultCharset());
    }

    /**
     * Creates a new instance with the specified character set.
     */
    public TrimStringDecoder(Charset charset) {
        if (charset == null) {
            throw new NullPointerException("charset");
        }
        this.charset = charset;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        out.add(msg.toString(charset).trim());
    }
}
