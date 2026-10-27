import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ParserStageTest {
    private static final String VALID =
            "127.0.0.1 - frank [10/Oct/2000:13:55:36 -0700] "
                    + "\"GET /apache_pb.gif HTTP/1.0\" 200 2326 \"-\" "
                    + "\"Mozilla/5.0 (Test Browser)\"";

    @Test
    void parsesValidLine() throws StageException {
        LogRecord record = parse(VALID).get(0);
        assertEquals("127.0.0.1", record.clientIp());
        assertEquals(200, record.status());
    }

    @Test
    void rejectsMissingField() {
        assertRejected(VALID.replace("2326", ""));
    }

    @Test
    void rejectsInvalidTimestamp() {
        assertRejected(VALID.replace("10/Oct/2000", "31/Feb/2000"));
    }

    @Test
    void rejectsInvalidStatusCode() {
        assertRejected(VALID.replace(" 200 2326", " 999 2326"));
    }

    @Test
    void rejectsBlankLine() {
        assertRejected("   ");
    }

    @Test
    void rejectsInvalidUtf8() {
        ParserStage parser = new ParserStage();
        assertThrows(StageException.class,
                () -> parser.process(RawLine.fromBytes(new byte[] {(byte) 0xff}), ignored -> { }));
    }

    @Test
    void rejectsOversizedLine() {
        ParserStage parser = new ParserStage(10);
        assertThrows(StageException.class,
                () -> parser.process(RawLine.fromUtf8(VALID), ignored -> { }));
    }

    @Test
    void acceptsExtraWhitespace() throws StageException {
        assertEquals(1, parse("  " + VALID.replace(" - frank", "   -   frank") + "  ").size());
    }

    @Test
    void keepsSpacesInsideQuotedUserAgent() throws StageException {
        assertEquals("Mozilla/5.0 (Test Browser)", parse(VALID).get(0).userAgent());
    }

    @Test
    void keepsQueryStringAsAnAttribute() throws StageException {
        LogRecord record = parse(VALID.replace("/apache_pb.gif", "/items?id=42")).get(0);
        assertEquals("id=42", record.attributes().get("query"));
    }

    private static List<LogRecord> parse(String line) throws StageException {
        List<LogRecord> records = new ArrayList<>();
        new ParserStage().process(RawLine.fromUtf8(line), records::add);
        return records;
    }

    private static void assertRejected(String line) {
        assertThrows(StageException.class, () -> parse(line));
    }
}
