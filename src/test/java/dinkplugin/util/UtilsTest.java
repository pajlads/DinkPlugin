package dinkplugin.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UtilsTest {

    @Test
    void truncate() {
        assertEquals("Hello world", Utils.truncate("Hello world", 200));
        assertEquals("Hello world", Utils.truncate("Hello world", 11));
        assertEquals("Helloworld", Utils.truncate("Helloworld", 10));
        assertEquals("Hellowor…", Utils.truncate("Helloworld", 9));
        assertEquals("Hello…", Utils.truncate("Hello world", 9));
        assertEquals("Hello…", Utils.truncate("Hello worldly beings", 10));
        assertEquals("Hello worldly…", Utils.truncate("Hello worldly beings", 16));
        assertEquals("Hello worldly…", Utils.truncate("Hello worldly beings", 15));
        assertEquals("Hello worldly…", Utils.truncate("Hello worldly beings", 14));
    }

}
