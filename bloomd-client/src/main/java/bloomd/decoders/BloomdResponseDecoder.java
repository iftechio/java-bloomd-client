package bloomd.decoders;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

@Sharable
public class BloomdResponseDecoder extends MessageToMessageDecoder<ByteBuf> {
    private final Charset charset;

    /**
     * Creates a new instance with the current system character set.
     */
    public BloomdResponseDecoder() {
        this(Charset.defaultCharset());
    }

    /**
     * Creates a new instance with the specified character set.
     */
    public BloomdResponseDecoder(Charset charset) {
        if (charset == null) {
            throw new NullPointerException("charset");
        }
        this.charset = charset;
    }

    private List<String> multiLineResponse;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        String message = msg.toString(charset);
        switch (message) {
            case "START":
                multiLineResponse = new ArrayList<>();
                multiLineResponse.add(message);
                break;
            case "END":
                multiLineResponse.add(message);
                out.add(String.join("\n", multiLineResponse));
                multiLineResponse = null;
                break;
            default:
                if (multiLineResponse != null) {
                    multiLineResponse.add(message);
                } else {
                    out.add(message);
                }
                break;
        }
    }
}
