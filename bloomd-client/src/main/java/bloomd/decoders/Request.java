package bloomd.decoders;

import java.util.concurrent.CompletableFuture;

public abstract class Request<V> extends CompletableFuture<V> {
    public CompletableFuture<V> asFuture() {
        return this;
    }

    public abstract String getCommand();

    public abstract V decode(String msg) throws Exception;

    public void handle(String msg) throws Exception {
        try {
            V value = decode(msg);
            complete(value);
        } catch (Exception e) {
            completeExceptionally(e);
            throw e;
        }
    }
}
