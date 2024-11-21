# Dink

Dink sends webhook messages upon noteworthy in-game events.
While Dink supports the Discord webhook format (with rich embeds and optional screenshots), it also includes additional metadata that allows custom webhook servers to analyze messages or even generate their own messages.
Examples of the additional metadata can be found [here](docs/json-examples.md).
This project was forked from UniversalDiscordNotifier, but has more features, reliability, configurability, testing, and maintainer activity.
You can compare Dink to other Discord webhook plugins [here](docs/comparison.md).

Have a suggestion (e.g., new notifier, additional data), bug report (as rare as it may be), or question? Let us know on our [issue tracker](https://github.com/pajlads/DinkPlugin/issues)!

## Basic Setup

To use this plugin, a webhook URL is required; you can obtain one from Discord with the following steps:  
<sub>If you already have a link, skip to step 4.</sub>

1. Click the server name (at the top-left of your screen) and select `Server Settings`.
2. Select the `Integrations` tab on the left side and click `Create Webhook` (if other webhooks already exist, click `View Webhooks` and `New Webhook`).
3. Click the newly created webhook, select the target Discord channel, and click `Copy Webhook URL`.
4. Paste the copied link into the `Primary Webhook URLs` box in the Dink plugin settings.
5. (Optional): If you would like different webhook URLs to receive different sets of notifications, you can instead paste the link into each relevant box in the `Webhook Overrides` section. Note: when a notifier has an override URL, the notifier ignores the primary URL box.

## Notifiers

- [Death](#death): Send a webhook message upon dying (with special configuration for PK deaths)
- [Collection](#collection): Send a webhook message upon adding an item to your collection log
- [Level](#level): Send a webhook message upon leveling up a skill (with support for virtual levels and XP milestones)
- [Loot](#loot): Send a webhook message upon receiving valuable loot (with item rarity for monster drops)
- [Slayer](#slayer): Send a webhook message upon completing a slayer task (with a customizable point threshold)
- [Quests](#quests): Send a webhook message upon completing a quest
- [Clue Scrolls](#clue-scrolls): Send a webhook message upon solving a clue scroll (with customizable tier/value thresholds)
- [Kill Count](#kill-count): Send a webhook message upon defeating a boss (with special configuration for personal best times)
- [Combat Achievements](#combat-achievements): Send a webhook message upon completing a combat task (with customizable tier threshold)
- [Achievement Diaries](#achievement-diary): Send a webhook message upon completing an achievement diary (with customizable difficulty threshold)
- [Pet](#pet): Send a webhook message upon receiving a pet
- [Speedrunning](#speedrunning): Send a webhook message upon completing a quest speedrun (with special configuration for personal best times)
- [BA Gambles](#ba-gambles): Sends a webhook message upon receiving high level gambles from Barbarian Assault
- [Player Kills](#player-kills): Sends a webhook message upon killing another player (while hitsplats are still visible)
- [Group Storage](#group-storage): Sends a webhook message upon Group Ironman Shared Bank transactions (i.e., depositing or withdrawing items)
- [Grand Exchange](#grand-exchange): Sends a webhook message upon buying or selling items on the GE (with customizable value threshold)
- [Trades](#trades): Sends a webhook message upon completing a trade with another player (with customizable item value threshold)
- [Leagues](#leagues): Sends a webhook message upon completing a Leagues IV task or unlocking a region/relic
- [Chat](#chat): Sends a webhook message upon receiving a chat message that matches a user-specified pattern

## Other Setup

Some notifiers require in-game settings to be configured to send chat messages upon certain events (so these events can serve as triggers for webhook notifications).

- Collection notifier requires `Settings > All Settings > Chat > Collection log - New addition notification` (or `New addition popup`) to be enabled
- Pet notifier recommends `Settings > All Settings > Chat > Untradeable loot notifications` to be enabled (which requires `Settings > All Settings > Chat > Loot drop notifications`) in order to determine the name of the pet
- For Kill Count notifier, ensure you do _not_ enable `Settings > All Settings > Chat > Filter out boss kill-count with spam-filter` (note: this setting is already disabled by default by Jagex)

### Example

![img.png](img.png)

## Advanced Features

- Multiple webhook urls are supported; simply place each on a separate line
- Each notifier can send webhook messages to separate "override" urls
- Screenshots can be individually configured for each notifier
- Screenshots are compressed if needed to comply with Discord limits
- The chat box (and private messages above chat) can be hidden from screenshots
- The plugin can skip notifications for player names that do not comply with the user-configured RSN filter list
- Users can choose whether their webhook messages are sent in Discord's rich embed format or a traditional format
- The player name in Discord rich embeds can be linked to various tracking services (from HiScores to Wise Old Man)
- Discord rich embed footers can be customized with user-specified text and image url
- When network issues occur, Dink can make repeated attempts to send the webhook (with exponential backoff)
- Notifications can be sent to [Discord Forum Channels](https://support.discord.com/hc/en-us/articles/6208479917079-Forum-Channels-FAQ); append `?forum` to the end of the webhook url to create a new thread per message or use `?thread_id=123456` to post to an existing forum thread (be sure to change `123456` with the actual thread ID). For forum channels, you can also include `&applied_tags=123,456` to specify certain tags for the new thread (be sure to change `123`, `456` with the tag IDs you wish to apply). To achieve different tags for different notification types, you should utilize the `Webhook Overrides` section (and can share these settings via [config export](#export-current-configuration-via-dinkexport))
- Character [metadata](#metadata) can be sent to custom webhook handlers on login for tracking relevant statistics.

## Chat Commands

### Export Current Configuration via `::dinkexport`

Dink allows you to export your current plugin configuration to the clipboard via the `::dinkexport` chat command.

You can share this produced JSON to friends who want to send similarly configured messages.

This export includes settings across all of the notifiers, but omits webhook URLs. If you also want to include webhook URLs in the export, you can use the `all` parameter to the command: `::dinkexport all`.

If you _only_ want to export the webhook URLs, run the `::dinkexport webhooks` chat command.

You can export just the settings for select notifiers.  
Simply run: `::dinkexport <notifier section header name without spaces>`.  
For example: `::dinkexport pet` or `::dinkexport collectionlog`.

#### Examples

- Export notifier settings, primary webhook URLs & webhook override URLs  
  `::dinkexport all`
- Export Slayer & BA Gambles Notifier settings  
  `::dinkexport slayer bagambles`
- Export webhook overrides only  
  `::dinkexport webhookoverrides`
- Export all webhooks & the Levels notifier settings:  
  `::dinkexport webhooks levels`

### Import Configuration via `::dinkimport`

With the output of the above command (`::dinkexport`) copied to your clipboard, you can merge these settings with your own via the `::dinkimport` chat command.

This import can replace all of your notifier settings.
However, webhook URL lists, filtered RSNs, and filtered item names for the loot notifier would be combined, rather than outright replaced.
If you would like all settings overwritten rather than merged during import, simply press the `Reset` button at the bottom of the plugin settings panel to clear out all settings (including URLs) before running `::dinkimport`.

After an import, if the dink plugin settings panel was open, simply close and open it for the updated configuration to be reflected in the user interface.

Note: There is no undo button for this command, so consider making a backup of your current Dink configuration by using the `::dinkexport all` command explained above and saving that to a file on your computer.

Warning: If you import override URLs for a notifier (that previously did not have any overrides), this will result in the plugin no longer sending messages from that notifier to your old primary URLs.
As such, you can manually add your primary URLs to the newly populated override URL boxes so that notifications are still sent to the old primary URLs.

### Migrate configuration from other webhook plugins via `::DinkMigrate`

When switching to Dink from other Discord webhook plugins, you can utilize the `::DinkMigrate` command to automatically import your configuration from the other plugins into Dink on a best-effort basis.
Like `::DinkImport`, most settings are replaced outright while others are merged (e.g., webhook URLs, filtered RSNs, and filtered item names).

This migration can be imperfect so we recommend verifying the updated Dink configuration post-migration.
Also, if you already have used Dink, we recommend saving the output of `::DinkExport all` before migrating just in case you want to revert any changes.

When executing the `::DinkMigrate` command, you must specify which plugin to import or `all` to migrate all supported plugins.
In particular, Dink supports migrating the following plugins: `BetterDiscordLootLogger`, `DiscordCollectionLogger`, `DiscordDeathNotifications`, `DiscordLevelNotifications`, `DiscordLootLogger`, `DiscordRareDropNotifier`, `GIMBankDiscord`, `RaidShamer`, and `UniversalDiscordNotifications`.

After migration, you should disable the migrated webhook plugins to avoid sending multiple notifications upon event triggers. Also, if the Dink config panel was already open, please close and reopen it to view the latest changes.

#### Examples

- Migrate config from BossHuso's Discord Rare Drop Notificater  
  `::dinkmigrate DiscordRareDropNotifier` or `::DinkMigrate rare` or `::DinkMigrate huso`
- Migrate config from all supported webhook plugins  
  `::dinkmigrate all`

#### Limitations

##### Better Discord Loot Logger

- `Screenshot Keybind` is not migrated; Dink does not react to any keybinds
- `Include raid loot (Experimental)` is not migrated; utilize Dink's value, rarity, or item allowlist settings instead

##### Discord Collection Logger

- `Include Username` is not migrated; you should modify the notification template manually
- `Include Collection Image` is not migrated; Dink only provides a screenshot upon receiving a collection log item
- `Include Pets` can enable the pet notifier in Dink, but if the collection webhook URL override is used, you should manually edit the pet webhook URL override

##### Discord Death Notifications

- `includeName` is not migrated; you should manually edit the death notification template

##### Discord Loot Logger

- `Loot NPCs` intentionally does not disable Dink's `Include PK Loot` or `Include Clue Loot` within the loot notifier
- `Include Low Value Items` is ignored in favor of the `Loot Value` setting
- `Include Stack Value` is not migrated; Dink already includes this information in the message body
- `Include Username` is not migrated; you should modify the notification template manually

##### Discord Rare Drop Notifier

- `Always send uniques (events)` is not migrated; utilize Dink's value, rarity, or item allowlist settings instead
- `Whitelisted RSNs` is not migrated if Dink's `RSN Filter Mode` was already set to `Deny` (and `Filtered RSNs` was not empty)
- `sendRarityAndValue` is not migrated; Dink already includes this information as embed fields
- This plugin prioritizes the item allowlist over the item denylist. Dink, however, prioritizes the denylist

##### Raid Shamer (aka Death Shamer)

- `captureFriendDeathsOnly` is not migrated; Dink only notifies upon local player deaths
- `activeInCoX`, `activeInToB`, `activeInToA`, and `activeOutsideOfRaids` are not migrated; you should manually configure `Ignore Safe Deaths` and `Safe Exceptions` within Dink's death notifier
- `webhookEnabled` is not migrated; Dink only submits the image to a URL (and does not save to a local file)

### Get your Dink Hash via `::dinkhash`

Dink notification metadata includes a player hash that custom webhook servers can utilize to uniquely identify players (persistent across name changes).

You can obtain your dink hash via the `::dinkhash` chat command. Feel free to provide this value to third-party services that may request it.

### Get Current Region ID via `::dinkregion`

The death notifier allows you to customize any region that should be ignored.
This is particularly relevant for ultimate ironmen (UIM) who frequently use particular locations to deathbank/deathpile.

To facilitate this process, the `::dinkregion` chat command outputs the player's current region ID.

For example, Prifddinas spans the following region IDs: 12894, 12895, 13150, and 13151.

---

## Notifier Configuration

Most of the config options are self-explanatory. But the notification messages for each notification type also
contain some words that will be replaced with in-game values.

### All messages:

`%USERNAME%` will be replaced with the username of the player.

### Death:

`%VALUELOST%` will be replaced with the price of the items you lost. If you died in PvP, `%PKER%` will be replaced with the name of your killer.

By default, to avoid spam, Dink will ignore deaths from the following [safe](https://oldschool.runescape.wiki/w/Minigames#Safe) activities/areas: Barbarian Assault, Castle Wars, Chambers of Xeric (CoX), Clan Wars, Creature Graveyard of Mage Training Arena, Last Man Standing (LMS), Nightmare Zone (NMZ), Pest Control, player-owned houses (POH), Soul Wars, TzHaar Fight Pit.
However, PvM deaths as a hardcore group ironman are _not_ considered to be safe (and _will_ trigger a notification in these areas).
Lastly, Dink makes exceptions for Inferno and TzHaar Fight Cave; deaths in these areas _do_ trigger notifications (despite technically being safe).

**Note**: If _Distinguish PvP deaths_ is disabled, the message content will be the non-PvP version.

### Collection:

`%ITEM%` will be replaced with the item that was dropped for the collection log.

`%COMPLETED%` will be replaced with the number of unique entries that have been completed.

`%TOTAL_POSSIBLE%` will be replaced with the total number of unique entries that are tracked in the collection log.

Note: `%COMPLETED%` may not be populated if the [Character Summary](https://oldschool.runescape.wiki/w/Character_Summary) tab was never selected since logging in.

### Level:

`%SKILL%` will be replaced with the skill name and level that was achieved

`%TOTAL_LEVEL%` will be replaced with the updated total level across all skills.

`%TOTAL_XP%` will be replaced with the updated total experience across all skills.

### Loot:

`%LOOT%` will be replaced with a list of the loot and value of said loot

`%TOTAL_VALUE%` will be replaced with the total value of the looted items

`%SOURCE%` will be replace with the source that dropped or gave the loot

### Slayer:

`%TASK%` will be replaced with the task that you have completed. E.g. `50 monkeys`

`%TASKCOUNT%` will be replaced with the number of tasks that you have completed.

`%POINTS%` will be replaced with the number of points you obtained from the task

### Quests:

`%QUEST%` will be replaced with the name of the quest completed

### Clue Scrolls:

`%CLUE%` will be replaced with the type of clue (beginner, easy, etc...)

`%LOOT%` will be replaced with the loot that was obtained from the casket

`%TOTAL_VALUE%` will be replaced with the total value of the items from the reward casket

`%COUNT%` will be replaced by the number of times that you have completed that tier of clue scrolls

### Kill Count:

`%BOSS%` will be replaced with the boss name (be it the NPC, raid, etc.)

`%COUNT%` will be replaced with the kill count (or, generically: completion count)

### Combat Achievements:

`%TIER%` will be replaced with the combat achievement tier (e.g., Easy, Hard, Grandmaster)

`%TASK%` will be replaced with the name of the combat task (e.g., Peach Conjurer)

`%POINTS%` will be replaced with the number of points you earned from the combat achievement.

`%TOTAL_POINTS%` will be replaced with the total points that have been earned across tasks.

If the task completion unlocked rewards for a tier, `%COMPLETED%` will be replaced with the tier that was completed.

### Achievement Diary:

`%AREA%` will be replaced with the geographic area of the achievement diary tasks (e.g., Varrock)

`%DIFFICULTY%` will be replaced with the level of the achievement diary (e.g., Hard)

`%TOTAL%` will be replaced with the total number of achievement diaries completed across all locations and difficulties

`%TASKS_COMPLETE%` will be replaced with the number of tasks completed across all locations and difficulties

`%TASKS_TOTAL%` will be replaced with the total number of tasks possible across all locations and difficulties

`%AREA_TASKS_COMPLETE%` will be replaced with the number of tasks completed within the area

`%AREA_TASKS_TOTAL%` will be replaced with the total number of tasks possible within the area

### Pet:

`%GAME_MESSAGE%` will be replaced with the game message associated with this type of pet drop

### Speedrunning:

`%QUEST%` will be replaced with the name of the quest (e.g., Cook's Assistant)

`%TIME%` will be replaced with the time for the latest run

`%BEST%` will be replaced with the personal best time for this quest (note: only if the run was not a PB)

### BA Gambles:

`%COUNT%` will be replaced with the high level gamble count

`%LOOT%` will be replaced with the loot received from the gamble
(by default, this is included only in rare loot notifications)

### Player Kills:

`%TARGET%` will be replaced with the victim's user name

### Group Storage:

`%DEPOSITED%` will be replaced with the list of deposited items

`%WITHDRAWN%` will be replaced with the list of withdrawn items

### Grand Exchange:

`%TYPE%` will be replaced with the transaction type (i.e., bought or sold)

`%ITEM%` will be replaced with the transacted item

`%STATUS%` will be replaced with the offer status (i.e., Completed, In Progress, or Cancelled)

### Trades:

`%COUNTERPARTY%` will be replaced with the name of the other user involved in the trade

`%GROSS_VALUE%` will be replaced with the sum of item values offered by both parties in the transaction.

`%NET_VALUE%` will be replaced with the value of the received items _minus_ the value of the given items.

### Leagues:

Leagues notifications include: region unlocked, relic unlocked, and task completed (with customizable difficulty threshold).

Each of these events can be independently enabled or disabled in the notifier settings.

### Chat:

The chat notifier enables notifications for messages that are otherwise not covered by our other notifiers.

You can customize the message patterns to your liking (`*` is a wildcard), and specify which types of messages to check (e.g., game, trade, clan notification, user chat).

`%MESSAGE%` will be replaced with the chat message the matched one of the patterns.

### Metadata:

On login, Dink can submit a character summary containing data that spans multiple notifiers to a custom webhook handler (configurable in the `Advanced` section). This login notification is delayed by at least 5 seconds in order to gather all of the relevant data.

## Credits

This plugin uses code from [Universal Discord Notifier](https://github.com/MidgetJake/UniversalDiscordNotifier).

Item rarity data is sourced from the OSRS Wiki (licensed under [CC BY-NC-SA 3.0](https://creativecommons.org/licenses/by-nc-sa/3.0/)),
which was conveniently parsed by [Flipping Utilities](https://github.com/Flipping-Utilities/parsed-osrs) (and [transformed](https://github.com/pajlads/DinkPlugin/blob/master/src/test/java/dinkplugin/RarityCalculator.java) by pajlads).
