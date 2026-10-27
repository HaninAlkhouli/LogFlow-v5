public interface Stage<I, O> {
    void process(I input, Emitter<O> out) throws StageException;

    default void open() {
    }

    default void close() {
    }
}
