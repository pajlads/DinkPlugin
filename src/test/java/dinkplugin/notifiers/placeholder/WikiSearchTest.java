package dinkplugin.notifiers.placeholder;

import dinkplugin.message.placeholder.WikiSearchPlaceholder;
import org.junit.jupiter.api.Test;

import static dinkplugin.util.Utils.getWikiSearchUrl;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class WikiSearchTest {

    private final WikiSearchPlaceholder wikiSearch = new WikiSearchPlaceholder();

    @Test
    public void testWikiSearchPlaceholderReplacing() {
        String item = "Black king dragon";
        String placeholder = wikiSearch.asPlaceholder(item);
        assertEquals(placeholder, "[" + item + "](" + item + ")");

        String replaced = wikiSearch.replacePlaceholder(placeholder);
        assertEquals("[" + item + "](" + getWikiSearchUrl(item) + ")", replaced);
    }

    @Test
    public void testWikiSearchPlaceholderRemoving() {
        String item = "Black king dragon";
        String placeholder = wikiSearch.asPlaceholder(item);
        assertEquals(placeholder, "[" + item + "](" + item + ")");

        String replaced = wikiSearch.removePlaceholder(placeholder);
        assertEquals(item, replaced);
    }

    @Test
    public void testWikiSearchPlaceholderReplacingInsideText() {
        String text = "dank dank has completed a [medium](Clue scroll (medium)) clue, for a total of 1312. They obtained: 1 x [Ruby](Ruby)";

        String replaced = wikiSearch.replacePlaceholder(text);
        assertEquals("dank dank has completed a [medium](" + getWikiSearchUrl("Clue scroll (medium)") + ") clue, for a total of 1312. They obtained: 1 x [Ruby](" + getWikiSearchUrl("Ruby") + ")", replaced);
    }

    @Test
    public void testWikiSearchPlaceholderRemovingInsideText() {
        String text = "dank dank has completed a [medium](Clue scroll (medium)) clue, for a total of 1312. They obtained: 1 x [Ruby](Ruby)";

        String replaced = wikiSearch.removePlaceholder(text);
        assertEquals("dank dank has completed a medium clue, for a total of 1312. They obtained: 1 x Ruby", replaced);
    }

    @Test
    public void testWikiSearchPlaceholderWithDataReplacing() {
        String text = "medium";
        String linkData = "Clue scroll (medium)";
        String placeholder = wikiSearch.asPlaceholder(text, linkData);
        assertEquals(placeholder, "[" + text + "](" + linkData + ")");

        String replaced = wikiSearch.replacePlaceholder(placeholder);
        assertEquals("[" + text + "](" + getWikiSearchUrl(linkData) + ")", replaced);
    }

    @Test
    public void testWikiSearchPlaceholderWithDataRemoving() {
        String text = "medium";
        String linkData = "Clue scroll (medium)";
        String placeholder = wikiSearch.asPlaceholder(text, linkData);
        assertEquals(placeholder, "[" + text + "](" + linkData + ")");

        String replaced = wikiSearch.removePlaceholder(placeholder);
        assertEquals(text, replaced);
    }
}
