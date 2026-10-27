import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes FailedRecord values to a separate UTF-8 dead-letter file. */
public final class DeadLetterFileSink implements Sink<FailedRecord> {
    private final Path output;
    private BufferedWriter writer;

    public DeadLetterFileSink(Path output) {
        this.output = output;
    }

    public Path output() {
        return output;
    }

    @Override
    public void open() {
        try {
            Path parent = output.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
            writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new PipelineException("Could not open dead-letter file: " + output, error);
        }
    }

    @Override
    public void consume(FailedRecord record) {
        if (writer == null) throw new IllegalStateException("DeadLetterFileSink is not open");
        try {
            writer.write(record.toLine());
            writer.newLine();
        } catch (IOException error) {
            throw new PipelineException("Could not write dead-letter file: " + output, error);
        }
    }

    @Override
    public void close() {
        if (writer == null) return;
        try {
            writer.close();
        } catch (IOException error) {
            throw new PipelineException("Could not close dead-letter file: " + output, error);
        } finally {
            writer = null;
        }
    }
}
