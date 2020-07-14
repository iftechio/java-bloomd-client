package bloomd.decoders;

import java.util.concurrent.CompletableFuture;

public abstract class Request<I, O> extends CompletableFuture<O> implements BloomdCommandCodec<I, O> {
    public CompletableFuture<O> asFuture() {
        return this;
    }
}
