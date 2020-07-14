package bloomd;

import bloomd.args.CreateFilterArgs;
import bloomd.args.StateArgs;
import bloomd.decoders.*;
import bloomd.replies.*;
import io.netty.channel.Channel;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

public class BloomdClientImpl implements BloomdClient, BloomdHandler.OnReplyReceivedListener {
    private final Object clientLock = new Object();

    private final Channel ch;
    private final BloomdHandler bloomdHandler;
    private final Queue<CompletableFuture> commandsQueue;
    private boolean blocked = false;

    public BloomdClientImpl(Channel channel) {
        this.ch = channel;
        this.bloomdHandler = new BloomdHandler(this);
        this.commandsQueue = new ConcurrentLinkedQueue<>();
    }

    @Override
    public Future<List<BloomdFilter>> list() {
        return list(null);
    }

    @Override
    public Future<List<BloomdFilter>> list(String prefix) {
        Request<String, List<BloomdFilter>> listRequest = new ListRequest();
        return send(listRequest, prefix == null ? "" : prefix);
    }

    @Override
    public Future<CreateResult> create(String filterName) {
        CreateFilterArgs args = new CreateFilterArgs.Builder()
                .setFilterName(filterName)
                .build();

        return create(args);
    }

    @Override
    public Future<CreateResult> create(CreateFilterArgs args) {
        checkFilterNameValid(args.getFilterName());
        Request<CreateFilterArgs, CreateResult> createRequest = new CreateRequest();
        return send(createRequest, args);
    }

    @Override
    public Future<Boolean> drop(String filterName) {
        checkFilterNameValid(filterName);
        Request<String, Boolean> dropRequest = new SingleArgRequest("drop");
        return send(dropRequest, filterName);
    }

    @Override
    public Future<Boolean> close(String filterName) {
        checkFilterNameValid(filterName);
        Request<String, Boolean> closeRequest = new SingleArgRequest("close");
        return send(closeRequest, filterName);
    }

    @Override
    public Future<ClearResult> clear(String filterName) {
        checkFilterNameValid(filterName);
        Request<String, ClearResult> clearRequest = new ClearRequest();
        return send(clearRequest, filterName);
    }

    @Override
    public Future<StateResult> check(String filterName, String key) {
        StateArgs args = new StateArgs.Builder().setFilterName(filterName).addKey(key).build();
        Request<StateArgs, StateResult> checkRequest = new GenericStateRequest<>("c", true);
        return send(checkRequest, args);
    }

    @Override
    public Future<StateResult> set(String filterName, String key) {
        StateArgs args = new StateArgs.Builder().setFilterName(filterName).addKey(key).build();
        Request<StateArgs, StateResult> setRequest = new GenericStateRequest<>("s", true);
        return send(setRequest, args);
    }

    @Override
    public Future<List<StateResult>> multi(String filterName, String... keys) {
        StateArgs.Builder builder = new StateArgs.Builder().setFilterName(filterName);
        for (String key : keys) {
            builder.addKey(key);
        }
        Request<StateArgs, List<StateResult>> multiRequest = new GenericStateRequest<>("m", false);
        return send(multiRequest, builder.build());
    }

    @Override
    public Future<List<StateResult>> bulk(String filterName, String... keys) {
        StateArgs.Builder builder = new StateArgs.Builder().setFilterName(filterName);
        for (String key : keys) {
            builder.addKey(key);
        }
        Request<StateArgs, List<StateResult>> bulkRequest = new GenericStateRequest<>("b", false);
        return send(bulkRequest, builder.build());
    }

    @Override
    public Future<BloomdInfo> info(String filterName) {
        checkFilterNameValid(filterName);
        Request<String, BloomdInfo> infoRequest = new InfoRequest();
        return send(infoRequest, filterName);
    }

    @Override
    public Future<Boolean> flush(String filterName) {
        checkFilterNameValid(filterName);
        Request<String, Boolean> flushRequest = new SingleArgRequest("flush");
        return send(flushRequest, filterName);
    }

    private void checkFilterNameValid(String filterName) {
        if (filterName == null || filterName.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid filter name: " + filterName);
        }
    }

    public <T, R> CompletableFuture<R> send(Request<T, R> request, T args) {
        if (blocked) {
            throw new IllegalStateException("Client was released from the pool");
        }

        if (!ch.isActive()) {
            throw new IllegalStateException("Client is not connected to the server");
        }

        // queue a future to be completed with the result of this command
        CompletableFuture<R> replyCompletableFuture = new CompletableFuture<>();
        synchronized (clientLock) {
            commandsQueue.add(replyCompletableFuture);

            // replace the request in the pipeline with the appropriate instance
            bloomdHandler.queueCodec(request);

            // sends the command arguments through the pipeline
            ch.writeAndFlush(args);
        }

        return request;
    }

    public BloomdHandler getBloomdHandler() {
        return bloomdHandler;
    }

    @Override
    public void onReplyReceived(Object reply) {
        //noinspection unchecked
        CompletableFuture<Object> future = commandsQueue.poll();
        if (future == null) {
            throw new IllegalStateException("Promise queue is empty, received reply");
        }
        future.complete(reply);
    }

    @Override
    public void onDisconnect() {
        CompletableFuture<Object> future;
        //noinspection unchecked
        while ((future = commandsQueue.poll()) != null) {
            future.completeExceptionally(new IllegalStateException("Connection has been dropped"));
        }
    }

    @Override
    public void onError(Exception e) {
        CompletableFuture<Object> future;
        //noinspection unchecked
        while ((future = commandsQueue.poll()) != null) {
            future.completeExceptionally(e);
        }
    }

    public Channel getChannel() {
        return ch;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }
}
