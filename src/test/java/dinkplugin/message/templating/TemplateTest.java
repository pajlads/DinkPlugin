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

}
