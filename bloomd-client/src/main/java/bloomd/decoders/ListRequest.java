package bloomd.decoders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import bloomd.replies.BloomdFilter;

public class ListRequest extends Request<List<BloomdFilter>> {

    private final String command;

    public ListRequest(String args) {
        if (args.isEmpty()) {
            this.command = "list";
        } else {
            this.command = "list " + args;
        }
    }

    @Override
    public String getCommand() {
        return command;
    }

    @Override
    public List<BloomdFilter> decode(String msg) throws Exception {
        List<BloomdFilter> filters = new ArrayList<>();

        switch (msg) {
            case "START":
                if (!filters.isEmpty()) {
                    filters.clear();
                    throw new IllegalStateException("START not expected. List already initialized.");
                }

                return null;

            case "END":
                List<BloomdFilter> results = Collections.unmodifiableList(new ArrayList<>(filters));

                // we use the same list to build different responses, so we have to clear it
                filters.clear();

                return results;

            default:
                String[] parts = msg.split(" ");

                String filterName = parts[0];
                float falsePositiveProbability = Float.parseFloat(parts[1]);
                long sizeBytes = Long.parseLong(parts[2]);
                long capacity = Long.parseLong(parts[3]);
                long size = Long.parseLong(parts[4]);

                filters.add(new BloomdFilter(filterName, falsePositiveProbability, sizeBytes, capacity, size));

                return null;
        }
    }
}
