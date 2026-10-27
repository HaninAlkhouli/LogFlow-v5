/** Internal link used to connect stages with different input/output types. */
interface PipelineNode<T> {
    void open();

    void produce(Emitter<T> out);

    void close();
}
