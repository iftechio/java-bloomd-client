package bloomd;

import bloomd.ConnectionHandler.ConnectionListener;
import bloomd.args.CreateFilterArgs;
import bloomd.args.StateArgs;
import bloomd.decoders.*;
import bloomd.replies.*;
import io.netty.channel.Channel;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class BloomdClientImpl implements BloomdClient, ConnectionListener {
    private final Channel ch;
    private final ConnectionHandler connectionHandler;
    private boolean blocked = false;

    public BloomdClientImpl(Channel channel) {
        this.ch = channel;
        this.connectionHandler = new ConnectionHandler(this);
    }

    @Override
    public Future<List<BloomdFilter>> list() {
        return list(null);
    }

    @Override
    public Future<List<BloomdFilter>> list(String prefix) {
        Request<List<BloomdFilter>> listRequest = new ListRequest(prefix == null ? "" : prefix);
        return send(listRequest);
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
        Request<CreateResult> createRequest = new CreateRequest(args);
        return send(createRequest);
    }

    @Override
    public Future<Boolean> drop(String filterName) {
        checkFilterNameValid(filterName);
        Request<Boolean> dropRequest = new SingleArgRequest("drop", filterName);
        return send(dropRequest);
    }

    @Override
    public Future<Boolean> close(String filterName) {
        checkFilterNameValid(filterName);
        Request<Boolean> closeRequest = new SingleArgRequest("close", filterName);
        return send(closeRequest);
    }

    @Override
    public Future<ClearResult> clear(String filterName) {
        checkFilterNameValid(filterName);
        Request<ClearResult> clearRequest = new ClearRequest(filterName);
        return send(clearRequest);
    }

    @Override
    public Future<StateResult> check(String filterName, String key) {
        StateArgs args = new StateArgs.Builder().setFilterName(filterName).addKey(key).build();
        Request<StateResult> checkRequest = new GenericStateRequest<>("c", args,true);
        return send(checkRequest);
    }

    @Override
    public Future<StateResult> set(String filterName, String key) {
        StateArgs args = new StateArgs.Builder().setFilterName(filterName).addKey(key).build();
        Request<StateResult> setRequest = new GenericStateRequest<>("s", args, true);
        return send(setRequest);
    }

    @Override
    public Future<List<StateResult>> multi(String filterName, String... keys) {
        StateArgs.Builder builder = new StateArgs.Builder().setFilterName(filterName);
        for (String key : keys) {
            builder.addKey(key);
        }
        Request<List<StateResult>> multiRequest = new GenericStateRequest<>("m", builder.build(), false);
        return send(multiRequest);
    }

    @Override
    public Future<List<StateResult>> bulk(String filterName, String... keys) {
        StateArgs.Builder builder = new StateArgs.Builder().setFilterName(filterName);
        for (String key : keys) {
            builder.addKey(key);
        }
        Request<List<StateResult>> bulkRequest = new GenericStateRequest<>("b", builder.build(), false);
        return send(bulkRequest);
    }

    @Override
    public Future<BloomdInfo> info(String filterName) {
        checkFilterNameValid(filterName);
        Request<BloomdInfo> infoRequest = new InfoRequest(filterName);
        return send(infoRequest);
    }

    @Override
    public Future<Boolean> flush(String filterName) {
        checkFilterNameValid(filterName);
        Request<Boolean> flushRequest = new SingleArgRequest("flush", filterName);
        return send(flushRequest);
    }

    private void checkFilterNameValid(String filterName) {
        if (filterName == null || filterName.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid filter name: " + filterName);
        }
    }

    public <V> CompletableFuture<V> send(Request<V> request) {
        if (blocked) {
            throw new IllegalStateException("Client was released from the pool");
        }

        if (!ch.isActive()) {
            throw new IllegalStateException("Client is not connected to the server");
        }

        // sends the command arguments through the pipeline
        ch.writeAndFlush(request);

        return request;
    }

    public ConnectionHandler getConnectionHandler() {
        return connectionHandler;
    }

    @Override
    public void onDisconnect() {
        setBlocked(true);
    }

    @Override
    public void onError(Exception e) {
        setBlocked(true);
    }

    public Channel getChannel() {
        return ch;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }
}
