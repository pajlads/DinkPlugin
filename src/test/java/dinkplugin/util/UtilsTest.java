package dinkplugin.util;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertEquals("Hello…", Utils.truncate("Hello worldly beings", 13));
    }

    @Test
    void regexify() {
        Pattern a = Utils.regexify("Hello world!");
        assertEquals("^\\QHello world!\\E$", a.pattern());
        assertTrue(a.matcher("Hello world!").find());
        assertTrue(a.matcher("hello world!").find());
        assertFalse(a.matcher("Hello world").find());
        assertFalse(a.matcher("Hello world!!").find());
        assertFalse(a.matcher("Hi Hello world!").find());

        Pattern b = Utils.regexify("Hello.world!");
        assertEquals("^\\QHello.world!\\E$", b.pattern());
        assertTrue(b.matcher("Hello.world!").find());
        assertFalse(b.matcher("Hello world!").find());
        assertFalse(b.matcher("Hello.world!!").find());

        Pattern c = Utils.regexify("Hello world!*");
        assertEquals("^\\QHello world!\\E.*", c.pattern());
        assertTrue(c.matcher("Hello world!").find());
        assertTrue(c.matcher("Hello world!~").find());
        assertFalse(c.matcher("Hi Hello world!").find());
        assertFalse(c.matcher("Hello world").find());

        Pattern d = Utils.regexify("*Hello world!");
        assertEquals("\\QHello world!\\E$", d.pattern());
        assertTrue(d.matcher("Hello world!").find());
        assertTrue(d.matcher("Hi Hello world!").find());
        assertFalse(d.matcher("Hello world!!").find());
        assertFalse(d.matcher("Hello world").find());

        Pattern e = Utils.regexify("*Hello world!*");
        assertEquals("\\QHello world!\\E.*", e.pattern());
        assertTrue(e.matcher("Hello world!").find());
        assertTrue(e.matcher("Hi Hello world!").find());
        assertTrue(e.matcher("Hello world!!").find());
        assertTrue(e.matcher("Hi Hello world!!").find());
        assertFalse(e.matcher("Hi Hello cruel world!!").find());

        Pattern f = Utils.regexify("*Hello*world!*");
        assertEquals("\\QHello\\E.*\\Qworld!\\E.*", f.pattern());
        assertTrue(f.matcher("Hello world!").find());
        assertTrue(f.matcher("Hi Hello world!").find());
        assertTrue(f.matcher("Hello world!!").find());
        assertTrue(f.matcher("Hi Hello world!!").find());
        assertTrue(f.matcher("Hi hello World!!").find());
        assertTrue(f.matcher("Hi Hello cruel world!!").find());

        Pattern g = Utils.regexify("Membership's price is $12.49");
        assertTrue(g.matcher("Membership's price is $12.49").find());
        assertFalse(g.matcher("Membership's price is $12.499").find());
        assertFalse(g.matcher("Membershipss price is $12.49").find());
        assertFalse(g.matcher("Membership's price is $12349").find());

        Pattern h = Utils.regexify("Membership's price is $12.49*");
        assertTrue(h.matcher("Membership's price is $12.49").find());
        assertTrue(h.matcher("Membership's price is $12.499").find());
        assertFalse(h.matcher("A Membership's price is $12.49").find());

        Pattern i = Utils.regexify("dragon*");
        assertTrue(i.matcher("dragon pickaxe").find());
        assertTrue(i.matcher("dragon claws").find());
        assertFalse(i.matcher("iron pickaxe").find());

        Pattern j = Utils.regexify("*orb");
        assertTrue(j.matcher("awakener's orb").find());
        assertTrue(j.matcher("commorb").find());
        assertFalse(j.matcher("commorb v2").find());

        Pattern k = Utils.regexify("vorkath's head");
        assertTrue(k.matcher("vorkath's head").find());
        assertFalse(k.matcher("vorki").find());
        assertFalse(k.matcher("iron pickaxe").find());

        Pattern l = Utils.regexify("clue scroll*");
        assertTrue(l.matcher("clue scroll (elite)").find());
        assertTrue(l.matcher("clue scroll (beginner)").find());
        assertTrue(l.matcher("clue scroll (easy)").find());
        assertFalse(l.matcher("clue bottle (beginner)").find());
        assertFalse(l.matcher("iron pickaxe").find());

        Pattern m = Utils.regexify("jar of*");
        assertTrue(m.matcher("jar of dirt").find());
        assertTrue(m.matcher("jar of smoke").find());
        assertTrue(m.matcher("jar of dust").find());
        assertTrue(m.matcher("jar of spirits").find());
        assertFalse(m.matcher("iron pickaxe").find());
    }

    @Test
    void sanitize() {
        assertEquals("Congratulations, you've unlocked a new Relic: Archer's Embrace.", Utils.sanitize("Congratulations, you've unlocked a new Relic: <col=ff7700>Archer's Embrace</col>."));
        assertEquals("Congratulations, you've completed an easy task: Obtain a Gem While Mining.", Utils.sanitize("Congratulations, you've completed an easy task: <col=ff7700>Obtain a Gem While Mining</col>."));

        assertEquals("", Utils.sanitize(null));
        assertEquals("", Utils.sanitize(""));

        assertEquals("foo\nbar", Utils.sanitize("foo<br>bar"));

        assertEquals("foo bar", Utils.sanitize("foo\u00A0bar"));
    }

}
