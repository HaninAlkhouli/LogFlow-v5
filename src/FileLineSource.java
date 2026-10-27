import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Reads physical lines as bytes so malformed UTF-8 can be handled by a stage policy. */
public final class FileLineSource implements Source<RawLine> {
    private final Path filePath;

    public FileLineSource(String fileName) {
        this(Path.of(fileName));
    }

    public FileLineSource(Path filePath) {
        this.filePath = filePath;
    }

    @Override
    public void produce(Emitter<RawLine> out) {
        try (InputStream input = new BufferedInputStream(Files.newInputStream(filePath))) {
            ByteArrayOutputStream line = new ByteArrayOutputStream();
            int value;
            while ((value = input.read()) != -1) {
                if (value == '\n') {
                    emitLine(line, out);
                    line.reset();
                } else {
                    line.write(value);
                }
            }
            if (line.size() > 0) emitLine(line, out);
        } catch (IOException e) {
            throw new PipelineException("Could not read file: " + filePath, e);
        }
    }

    private static void emitLine(ByteArrayOutputStream line, Emitter<RawLine> out) {
        byte[] bytes = line.toByteArray();
        if (bytes.length > 0 && bytes[bytes.length - 1] == '\r') {
            byte[] withoutCarriageReturn = new byte[bytes.length - 1];
            System.arraycopy(bytes, 0, withoutCarriageReturn, 0, withoutCarriageReturn.length);
            bytes = withoutCarriageReturn;
        }
        out.emit(RawLine.fromBytes(bytes));
    }
}
