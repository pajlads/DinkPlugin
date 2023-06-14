package dinkplugin.message.templating;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TemplateTest {

    @Test
    void noReplacements() {
        assertEquals(
            "Hello world!",
            Template.builder()
                .template("Hello world!")
                .build()
                .evaluate(true)
        );
    }

    @Test
    void noReplacementsFast() {
        assertEquals(
            "Hello world!",
            Template.builder()
                .template("Hello world!")
                .replacementBoundary("%")
                .build()
                .evaluate(true)
        );
    }

    @Test
    void unusedReplacements() {
        assertEquals(
            "Hello world!",
            Template.builder()
                .template("Hello world!")
                .replacement("%PLANET%", Replacements.ofText("Earth"))
                .build()
                .evaluate(true)
        );
    }

    @Test
    void unusedReplacementsFast() {
        assertEquals(
            "Hello world!",
            Template.builder()
                .template("Hello world!")
                .replacementBoundary("%")
                .replacement("%PLANET%", Replacements.ofText("Earth"))
                .build()
                .evaluate(true)
        );
    }

    @Test
    void withReplacements() {
        assertEquals(
            "dank dank has killed Monk",
            Template.builder()
                .template("%USERNAME% has killed %TARGET%")
                .replacement("%USERNAME%", Replacements.ofText("dank dank"))
                .replacement("%TARGET%", Replacements.ofWiki("Monk"))
                .build()
                .evaluate(false)
        );
    }

    @Test
    void withReplacementsFast() {
        assertEquals(
            "dank dank has killed Monk",
            Template.builder()
                .template("%USERNAME% has killed %TARGET%")
                .replacementBoundary("%")
                .replacement("%USERNAME%", Replacements.ofText("dank dank"))
                .replacement("%TARGET%", Replacements.ofWiki("Monk"))
                .build()
                .evaluate(false)
        );
    }

    @Test
    void withRichReplacements() {
        assertEquals(
            "dank dank has killed [Monk](https://oldschool.runescape.wiki/w/Special:Search?search=Monk)",
            Template.builder()
                .template("%USERNAME% has killed %TARGET%")
                .replacement("%USERNAME%", Replacements.ofText("dank dank"))
                .replacement("%TARGET%", Replacements.ofWiki("Monk"))
                .build()
                .evaluate(true)
        );
    }

    @Test
    void withRichReplacementsFast() {
        assertEquals(
            "dank dank has killed [Monk](https://oldschool.runescape.wiki/w/Special:Search?search=Monk)",
            Template.builder()
                .template("%USERNAME% has killed %TARGET%")
                .replacementBoundary("%")
                .replacement("%USERNAME%", Replacements.ofText("dank dank"))
                .replacement("%TARGET%", Replacements.ofWiki("Monk"))
                .build()
                .evaluate(true)
        );
    }

    @Test
    void missingReplacements() {
        assertEquals(
            "%USERNAME% has killed Monk",
            Template.builder()
                .template("%USERNAME% has killed %TARGET%")
                .replacement("%TARGET%", Replacements.ofWiki("Monk"))
                .build()
                .evaluate(false)
        );
    }

    @Test
    void missingReplacementsFast() {
        assertEquals(
            "%USERNAME% has killed Monk",
            Template.builder()
                .template("%USERNAME% has killed %TARGET%")
                .replacementBoundary("%")
                .replacement("%TARGET%", Replacements.ofWiki("Monk"))
                .build()
                .evaluate(false)
        );
    }

    @Test
    void withRichLink() {
        assertEquals(
            "dank dank has pk'd [Forsen](https://secure.runescape.com/m=hiscore_oldschool/hiscorepersonal?user1=Forsen)",
            Template.builder()
                .template("%USERNAME% has pk'd %TARGET%")
                .replacement("%USERNAME%", Replacements.ofText("dank dank"))
                .replacement("%TARGET%", Replacements.ofLink("Forsen", "https://secure.runescape.com/m=hiscore_oldschool/hiscorepersonal?user1=Forsen"))
                .build()
                .evaluate(true)
        );
    }

    @Test
    void withRichLinkFast() {
        assertEquals(
            "dank dank has pk'd [Forsen](https://secure.runescape.com/m=hiscore_oldschool/hiscorepersonal?user1=Forsen)",
            Template.builder()
                .template("%USERNAME% has pk'd %TARGET%")
                .replacementBoundary("%")
                .replacement("%USERNAME%", Replacements.ofText("dank dank"))
                .replacement("%TARGET%", Replacements.ofLink("Forsen", "https://secure.runescape.com/m=hiscore_oldschool/hiscorepersonal?user1=Forsen"))
                .build()
                .evaluate(true)
        );
    }

    @Test
    void withRichNullLink() {
        assertEquals(
            "dank dank has pk'd Forsen",
            Template.builder()
                .template("%USERNAME% has pk'd %TARGET%")
                .replacement("%USERNAME%", Replacements.ofText("dank dank"))
                .replacement("%TARGET%", Replacements.ofLink("Forsen", null))
                .build()
                .evaluate(true)
        );
    }

    @Test
    void withRichNullLinkFast() {
        assertEquals(
            "dank dank has pk'd Forsen",
            Template.builder()
                .template("%USERNAME% has pk'd %TARGET%")
                .replacementBoundary("%")
                .replacement("%USERNAME%", Replacements.ofText("dank dank"))
                .replacement("%TARGET%", Replacements.ofLink("Forsen", null))
                .build()
                .evaluate(true)
        );
    }
}
