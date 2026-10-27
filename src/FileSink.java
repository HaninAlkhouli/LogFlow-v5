import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes projected records to a UTF-8 text file. */
public final class FileSink implements Sink<ProjectedRecord> {
    private final Path output;
    private BufferedWriter writer;

    public FileSink(Path output) {
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
            throw new PipelineException("Could not open output file: " + output, error);
        }
    }

    @Override
    public void consume(ProjectedRecord record) {
        if (writer == null) throw new IllegalStateException("FileSink is not open");
        try {
            writer.write(record.toLine());
            writer.newLine();
        } catch (IOException error) {
            throw new PipelineException("Could not write output file: " + output, error);
        }
    }

    @Override
    public void close() {
        if (writer == null) return;
        try {
            writer.close();
        } catch (IOException error) {
            throw new PipelineException("Could not close output file: " + output, error);
        } finally {
            writer = null;
        }
    }
}
