package fitnesse.wiki.fs.exceptions;

public class NotEnoughWorkspaceItemSetsException extends RuntimeException {
    public NotEnoughWorkspaceItemSetsException(String mappedServerPath) {
        super(String.format("Found the server path mapping, but calling getItems returned null or an empty array for %s", mappedServerPath));
    }
}
