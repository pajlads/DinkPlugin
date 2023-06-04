package dinkplugin.message.templating;

import com.google.common.net.UrlEscapers;
import dinkplugin.message.Field;
import dinkplugin.message.templating.impl.JoiningReplacement;
import lombok.Value;
import lombok.experimental.UtilityClass;

import java.util.Arrays;

@UtilityClass
public class Replacements {

    public Evaluable ofText(String text) {
        return new Text(text);
    }

    public Evaluable ofLink(String text, String link) {
        return link != null ? new TextWithLink(text, link) : ofText(text);
    }

    public Evaluable ofWiki(String text, String searchPhrase) {
        return ofLink(text, "https://oldschool.runescape.wiki/w/Special:Search?search=" + UrlEscapers.urlPathSegmentEscaper().escape(searchPhrase));
    }

    public Evaluable ofWiki(String phrase) {
        return ofWiki(phrase, phrase);
    }

    public Evaluable ofBlock(String language, String content) {
        return new CodeBlock(language, content);
    }

    public Evaluable ofMultiple(String delim, Evaluable... components) {
        return JoiningReplacement.builder()
            .delimiter(delim)
            .components(Arrays.asList(components))
            .build();
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

    @Value
    private static class CodeBlock implements Evaluable {
        String language;
        String text;

        @Override
        public String evaluate(boolean rich) {
            return rich ? Field.formatBlock(language, text) : text;
        }
    }

}
