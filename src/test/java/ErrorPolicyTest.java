import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ErrorPolicyTest {
    @Test
    void parsesAllPolicies() {
        assertEquals(ErrorPolicy.Action.FAIL_FAST, ErrorPolicy.parse("FAIL_FAST").action());
        assertEquals(ErrorPolicy.Action.SKIP, ErrorPolicy.parse("skip").action());
        assertEquals(ErrorPolicy.Action.DEAD_LETTER, ErrorPolicy.parse("DEAD_LETTER").action());
        assertEquals(3, ErrorPolicy.parse("RETRY(3)").retries());
    }

    @Test
    void rejectsInvalidPolicy() {
        assertThrows(ConfigException.class, () -> ErrorPolicy.parse("IGNORE"));
        assertThrows(ConfigException.class, () -> ErrorPolicy.parse("RETRY(0)"));
    }
}
