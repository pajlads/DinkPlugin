package dinkplugin.message.templating;

@FunctionalInterface
public interface Evaluable {
    String evaluate(boolean rich);
}
