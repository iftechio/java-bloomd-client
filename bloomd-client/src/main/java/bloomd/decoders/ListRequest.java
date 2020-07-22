package bloomd.decoders;

import java.util.ArrayList;
import java.util.List;

import bloomd.replies.BloomdFilter;

public class ListRequest extends Request<List<BloomdFilter>> {

    private final String command;
    private final List<BloomdFilter> filters;

    public ListRequest(String args) {
        if (args.isEmpty()) {
            this.command = "list";
        } else {
            this.command = "list " + args;
        }
        this.filters = new ArrayList<>();
    }

    @Override
    public String getCommand() {
        return command;
    }

    @Override
    public List<BloomdFilter> decode(String msg) throws Exception {
        String[] lines = msg.split("\\r?\\n");
        for (String line : lines) {
            decodeLine(line);
        }
        return filters;
    }

    private void decodeLine(String line) {
        switch (line) {
            case "START":
                if (!filters.isEmpty()) {
                    throw new IllegalStateException("START not expected. List already initialized.");
                }
                break;
            case "END":
                break;
            default:
                String[] parts = line.split(" ");

                String filterName = parts[0];
                float falsePositiveProbability = Float.parseFloat(parts[1]);
                long sizeBytes = Long.parseLong(parts[2]);
                long capacity = Long.parseLong(parts[3]);
                long size = Long.parseLong(parts[4]);

                filters.add(new BloomdFilter(filterName, falsePositiveProbability, sizeBytes, capacity, size));
                break;
        }
    }
}
