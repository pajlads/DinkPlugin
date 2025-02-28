# External Plugin Messaging

Other Plugin Hub plugins can publish a `PluginMessage` via `EventBus#post` that instructs Dink to submit a webhook request.

Users can opt-out of this capability by disabling `External Plugin Requests > Enable External Plugin Notifications`.

Plugins can request that a screenshot is included with the notification, but users can also opt-out by
disabling `External Plugin Requests > Send Image` (default: on) and `External Plugin Requests > Allow Overriding 'Send Image'` (default: off).

Plugins can include a Discord url for the webhook, otherwise Dink will utilize `External Webhook Override`
(or `Primary Webhook URLs` if an external url override is not specified).
If a plugin requests a non-Discord url, it will be ignored in favor of the Dink configuration.

Otherwise, external plugins have a great deal of customization for the webhook body.

## Payload

The `namespace` for the `PluginMessage` should be `dink` and the `name` should be `notify`.

The `Map<String, Object>` that is supplied to `PluginMessage` will be converted into [`ExternalNotificationRequest`](../src/main/java/dinkplugin/domain/ExternalNotificationRequest.java).

| Field            | Required | Type             | Description                                                                                                                                                                             |
| ---------------- | -------- | ---------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `text`           | Y        | String           | The body text of the notification. This field supports templating (see `replacements` below) and by default `%PLAYER%` is an available replacement.                                     |
| `url`            | N        | String           | The Discord URLs that the notification should be sent to (newline separated).                                                                                                           |
| `title`          | N        | String           | The title for the Discord embed.                                                                                                                                                        |
| `thumbnail`      | N        | String           | A URL to an image for the thumbnail icon of the Discord embed.                                                                                                                          |
| `footer`         | N        | String           | The footer text for the Discord embed.                                                                                                                                                  |
| `imageRequested` | N        | boolean          | Whether dink should include a screenshot with the notification.                                                                                                                         |
| `image`          | N        | `java.awt.Image` | The image to use in place of a screenshot.                                                                                                                                              |
| `fields`         | N        | List             | A list of [embed fields](https://discord.com/developers/docs/resources/message#embed-object-embed-field-structure). The contained objects should have `name` and `value` properties.    |
| `replacements`   | N        | Map              | A map of strings to be replaced to objects containing `value` (and optionally `richValue`) that indicate what the template string should be replaced with for plain text and rich text. |

We strongly recommend you include your plugin name within the `footer` so it is clear where notifications are coming from.

## Example

The example below assumes you already have injected RuneLite's eventbus into your plugin like so: `private @Inject EventBus eventBus;`

```java
Map<String, Object> data = new HashMap<>();
data.put("text", "This is the primary content within the webhook. %PLAYER% will automatically be replaced with the player name and you can define your own template replacements like %XYZ%");
data.put("replacements", Map.of("%XYZ%", new Replacement("sample replacement")));
data.put("footer", "Sent by INSERT_YOUR_PLUGIN_NAME_HERE via Dink");
data.put("title", "An optional embed title for your notification");
data.put("imageRequested", true);
data.put("fields", List.of(new Field("sample key", "sample value")));

PluginMessage dinkRequest = new PluginMessage("dink", "notify", data);
eventBus.post(dinkRequest);
```

### Useful Classes

```java
@Value
public class Field {
    String name;
    String value;
    Boolean inline;

    public Field(String name, String value) {
        this(name, value, null);
    }
}
```

```java
@Value
public class Replacement {
    String value;
    String richValue;

    public Replacement(String value) {
        this(value, null);
    }

    public static Replacement ofLink(String text, String link) {
        return new Replacement(text, String.format("[%s](%s)", text, link));
    }

    public static Replacement ofWiki(String text, String searchPhrase) {
        return ofLink(text, "https://oldschool.runescape.wiki/w/Special:Search?search=" + UrlEscapers.urlPathSegmentEscaper().escape(searchPhrase));
    }
}
```
