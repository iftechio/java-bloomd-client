package bloomd.decoders;

import bloomd.FilterDoesNotExistException;
import bloomd.args.StateArgs;
import bloomd.replies.StateResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic codec implementation for the SET, CHECK, MULTI and BULK commands.
 *
 * They share the same syntax: command filter_name key [key2 [keyN]]
 */
public class GenericStateRequest<T> extends Request<T> {

    private final boolean singleItem;
    private final String command;

    public GenericStateRequest(String cmd, StateArgs args, boolean singleItem) {
        this.singleItem = singleItem;

        StringBuilder builder = new StringBuilder();
        builder.append(cmd);
        builder.append(" ");

        builder.append(args.getFilterName());

        for (String key : args.getKeys()) {
            builder.append(" ");
            builder.append(key);
        }

        this.command = builder.toString();
    }

    @Override
    public String getCommand() {
        return command;
    }

    @Override
    public T decode(String msg) throws Exception {
        List<StateResult> checkResults = parseStateResult(msg);
        if (singleItem) {
            T result = (T) checkResults.get(0);
            return result;
        } else {
            T result = (T) checkResults;
            return result;
        }
    }

    private List<StateResult> parseStateResult(String msg) {
        switch (msg) {
            case "Filter does not exist":
                throw new FilterDoesNotExistException(msg);

            default:
                String[] parts = msg.split(" ");

                List<StateResult> result = new ArrayList<>();

                for (String part : parts) {
                    switch (part) {
                        case "Yes":
                            result.add(StateResult.YES);
                            break;
                        case "No":
                            result.add(StateResult.NO);
                            break;
                        default:
                            throw new IllegalStateException("Invalid result: " + msg);
                    }
                }

                return result;
        }
    }
}
