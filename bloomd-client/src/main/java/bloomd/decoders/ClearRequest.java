package bloomd.decoders;

import bloomd.replies.ClearResult;

public class ClearRequest extends Request<ClearResult> {
    private final String command;

    public ClearRequest(String filterName) {
        command = "clear " + filterName;
    }

    @Override
    public String getCommand(){
        return command;
    }

    @Override
    public ClearResult decode(String msg) throws Exception {
        switch (msg) {
            case "Done":
                return ClearResult.CLEARED;

            case "Filter does not exist":
                return ClearResult.FILTER_DOES_NOT_EXISTS;

            case "Filter is not proxied. Close it first.":
                return ClearResult.CANNOT_CLEAR;

            default:
                throw new RuntimeException(msg);
        }
    }
}
