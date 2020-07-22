package bloomd.decoders;

import bloomd.args.CreateFilterArgs;
import bloomd.replies.CreateResult;

public class CreateRequest extends Request<CreateResult> {

    private final String command;

    public CreateRequest(CreateFilterArgs args) {
        StringBuilder builder = new StringBuilder();

        builder.append("create ");
        builder.append(args.getFilterName());

        if (args.getCapacity() != null) {
            builder.append(" capacity=");
            builder.append(args.getCapacity());
        }

        if (args.getFalsePositiveProbability() != null) {
            builder.append(" prob=");
            builder.append(String.format("%f", args.getFalsePositiveProbability()));
        }

        if (args.getInMemory() != null) {
            builder.append(" in_memory=");
            builder.append(args.getInMemory() ? 1 : 0);
        }

        command = builder.toString();
    }

    @Override
    public String getCommand() {
        return command;
    }

    @Override
    public CreateResult decode(String msg) throws Exception {
        switch (msg) {
            case "Done":
                return CreateResult.DONE;

            case "Exists":
                return CreateResult.EXISTS;

            case "Delete in progress":
                return CreateResult.DELETE_IN_PROGRESS;

            default:
                throw new RuntimeException(msg);
        }
    }
}
