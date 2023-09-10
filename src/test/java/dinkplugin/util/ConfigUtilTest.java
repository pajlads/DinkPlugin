package dinkplugin.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConfigUtilTest {

    @Test
    void readDelimited() {
        assertEquals(Collections.emptyList(), ConfigUtil.readDelimited(null).collect(Collectors.toList()));
        assertEquals(Collections.singletonList("ruby"), ConfigUtil.readDelimited("ruby").collect(Collectors.toList()));

        // Empty lines are stripped
        assertEquals(Collections.singletonList("ruby"), ConfigUtil.readDelimited("ruby\n").collect(Collectors.toList()));

        // spaces before and after entries are trimmed
        assertEquals(Collections.singletonList("ruby"), ConfigUtil.readDelimited("ruby ").collect(Collectors.toList()));

        // Delimitations work with ,
        assertEquals(Arrays.asList("ruby", "blueberry"), ConfigUtil.readDelimited("ruby , blueberry").collect(Collectors.toList()));

        // Delimitations work with ;
        assertEquals(Arrays.asList("ruby", "blueberry"), ConfigUtil.readDelimited("ruby ; blueberry").collect(Collectors.toList()));

        // Delimitations work with new line
        assertEquals(Arrays.asList("ruby", "blueberry"), ConfigUtil.readDelimited("ruby \n blueberry").collect(Collectors.toList()));
    }

}
