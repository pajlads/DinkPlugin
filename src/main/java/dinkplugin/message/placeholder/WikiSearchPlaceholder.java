package dinkplugin.message.placeholder;

import org.jetbrains.annotations.Nullable;

import javax.inject.Singleton;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dinkplugin.util.Utils.getWikiSearchUrl;

@Singleton
public class WikiSearchPlaceholder {

    private final Pattern pattern = Pattern.compile("\\[(?<name>.+?)]\\((?<data>.+?)\\)(?!\\))");

    public Pattern pattern() {
        return pattern;
    }

    public String asPlaceholder(String linkName) {
        return this.asPlaceholder(linkName, null);
    }

    public String asPlaceholder(String linkName, @Nullable String searchParam) {
        return String.format("[%s](%s)", linkName, searchParam != null ? searchParam : linkName);
    }

    public String replacePlaceholder(String inputText) {
        return replacePlaceholderWith(inputText, this::replacementForLinkName);
    }

    public String removePlaceholder(String text) {
        return replacePlaceholderWith(text, (linkName, linkData) -> linkName);
    }

    public String replacementForLinkName(String linkName, String linkData) {
        String link =  "[" + linkName + "](" + getWikiSearchUrl(linkData) + ")";

        return link;
    }

    private String replacePlaceholderWith(String inputText, WikiSearchPlaceholder.Replacer replacer) {
        Matcher matcher = pattern().matcher(inputText);

        StringBuffer outputText = new StringBuffer();
        while (matcher.find()) {
            String linkName = matcher.group("name");
            String linkData = matcher.group("data");
            String replacement = replacer.replace(linkName, linkData);

            matcher.appendReplacement(outputText, replacement);
        }
        matcher.appendTail(outputText);

        return outputText.toString();
    }

    private interface Replacer {
        String replace(String linkName, String linkData);
    }
}
