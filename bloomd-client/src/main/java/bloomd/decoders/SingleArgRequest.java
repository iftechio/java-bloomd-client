package bloomd.decoders;

import bloomd.FilterDoesNotExistException;

/**
 * Single arg commands codec. Used to implement `close`, `drop` and `flush`.
 */
public class SingleArgRequest extends Request<String, Boolean> {

    private final String command;

    public SingleArgRequest(String command) {
        this.command = command;
    }

    @Override
    public String buildCommand(String filterName) {
        return command + " " + filterName;
    }

    @Override
    public Boolean decode(String msg) throws Exception {
        switch (msg) {
            case "Done":
                return true;

            case "Filter does not exist":
                throw new FilterDoesNotExistException(msg);

            default:
                throw new RuntimeException(msg);
        }
    }
}
