package dinkplugin.message.templating;

import com.google.common.net.UrlEscapers;
import lombok.Value;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Replacements {

    public Evaluable ofText(String text) {
        return new Text(text);
    }

    public Evaluable ofLink(String text, String link) {
        return new TextWithLink(text, link);
    }

    public Evaluable ofWiki(String text, String searchPhrase) {
        return ofLink(text, "https://oldschool.runescape.wiki/w/Special:Search?search=" + UrlEscapers.urlPathSegmentEscaper().escape(searchPhrase));
    }

    public Evaluable ofWiki(String phrase) {
        return ofWiki(phrase, phrase);
    }

    @Value
    private static class Text implements Evaluable {
        String text;

        @Override
        public String evaluate(boolean rich) {
            return text;
        }
    }

    @Value
    private static class TextWithLink implements Evaluable {
        String text;
        String link;

        @Override
        public String evaluate(boolean rich) {
            return rich ? String.format("[%s](%s)", text, link) : text;
        }
    }

}
