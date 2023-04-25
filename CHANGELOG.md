## Unreleased

- Bugfix: Ignore deaths and player kills in instanced regions that are safe. (#221)
- Dev: Improve test suite reliability for uploading screenshots. (#219)

## 1.4.0

- Major: Add notifier to capture player kills while hitsplats are still visible. (#214)
- Bugfix: Improve reliablity of hidden chat setting for collection log screenshots. (#217)
- Bugfix: Fire death notification when multiple similar NPCs are attacking the player that lack scraped wiki HP data. (#216)
- Dev: Update gradle wrapper to v8.1.1 patch version. (#215)

## 1.3.3

- Minor: Include NPC killer in death notification metadata. (#207)
- Minor: Add advanced setting to hide chat when screenshotting. (#208)
- Minor: Rescale screenshots to comply with Discord's 8MB size limit. (#200)
- Minor: Include combat level in skill notifications. (#203)
- Bugfix: Skip quest notification for Hazeel cult partial completion. (#211)
- Dev: Update grade wrapper to v8.1 minor version. (#209)
- Dev: Refactor killer identification in death notifier. (#197, #210)

## 1.3.2

- Minor: Include loot source category in notification metadata. (#196)
- Minor: Avoid death notifications for more safe activities: Barbarian Assault, Chambers of Xeric, Clan Wars, Last Man Standing, Nightmare Zone, Soul Wars, TzHaar Fight Pit. (#194)
- Minor: Avoid unwarranted settings warnings when world hopping. (#183)
- Minor: Include number of quest completions and quest points in quest notifications. (#178)
- Minor: Collapse all notifier configuration sections by default. (#176)
- Minor: Include number of completed entries in collection log notification. (#174, #181)
- Bugfix: Read pet acquisition milestone from delayed clan messages. (#193)
- Bugfix: Fire slayer notifications beyond 999 points. (#182)
- Dev: Improve thread-safety of slayer notifier. (#184)
- Dev: Update Gradle to 8.0.2 patch version. (#185)
- Dev: Add issue tracker link to README. (#179)

## 1.3.1

- Minor: Include pet acquisition milestone in webhook metadata from clan message. (#169)
- Minor: Add setting to skip death notifications with low value of items lost. (#166)
- Bugfix: Associate fight duration to delayed kill count messages. (#171)
- Bugfix: Fire skill notification when jumping over a level that matches the configured interval. (#164)
- Dev: Upgrade Gradle from v7.6 to v8.0. (#167, #170)
- Dev: Improve thread-safety of level notifier. (#165)

## 1.3.0

- Major: Add Barbarian Assault high level gambling notifications. (#150)
- Minor: Add plugin config export and import chat commands. (#155, #160)
- Minor: Make time units of advanced settings more obvious in the user interface. (#158)
- Minor: Clean up setting tooltips that stretched too wide, making them difficult to read. (#151)
- Minor: Add warning when in-game kill count chat spam filter is enabled. (#154)
- Minor: Include pet name in webhooks when available in chat via clan, collection log, or untradeable drop notifications. (#149, #153)
- Bugfix: Improve identification of diary completions via message box events. (#157, #159)
- Bugfix: Report correct item quantity from unsired loot. (#147)
- Dev: Improve thread-safety of notifiers. (#156)

## 1.2.2

- Bugfix: Fix embed author url breaking notifications when the player has a space in their name. (#143)

## 1.2.1

- Minor: Add setting to ignore clue scrolls for loot notifications. (#135)
- Minor: Add setting to skip notifications for low tier clue scrolls. (#134)
- Minor: Add embed field on a max level up indicating total 99+ skills. (#115)
- Minor: Include all lost items in extra death notification data. (#132)
- Minor: Reduce ticks to initialize diary notifier and improve logging for edge cases. (#129)
- Minor: Made Collection Log notifications display the icon of the obtained item. (#128)
- Minor: Made player names in Discord notifications link to a player profile page. This feature can be customized to different providers such as WiseOldMan & CrystalMathLabs in the Advanced menu. (#126)
- Minor: Add setting to skip virtual level notifications. (#122)
- Minor: Update the README to be less confusing to users. (#123, #136)
- Minor: Update embed icons for level up and slayer task notifications. (#119)
- Bugfix: Improve PKer identification in death notifier using characteristics such as clan. (#137)
- Bugfix: Improve item price lookup for clue and loot notifiers so that coins are not worthless. (#131)

## 1.2.0

- Major: Use Discord rich embed format for webhooks. This is technically a possible breaking change for users that target non-Discord webhook servers; these users can revert to the old notification format by disabling the `Use Rich Embeds` setting in the `Advanced` section. (#110)
- Minor: Add configurable player name ignore list for notifications. (#114)
- Bugfix: Read unsired sprite dialog at end of game tick. This is still experimental for wider testing. (#112)
- Dev: Allow full plugin mocking in unit tests. (#117)
- Dev: Allow unit tests to post to an actual webhook server. (#110, #113)

## 1.1.3

- Minor: Add personal best time message to kill count notifier. (#106)
- Minor: Add better descriptions for `Min Value` settings. (#100)
- Minor: Add death notifier setting to disable kept item embeds. (#99)
- Minor: Add clue and loot notifier setting to skip screenshots for low item values. (#98)
- Minor: Add level up setting to skip notifications below a specified level. (#97)
- Minor: Retry failed webhook messages with exponential backoff. (#94)
- Minor: Add warning logs for improper runescape settings that impact notifiers. (#92)
- Bugfix: Track Unsired drops for loot notifier. This is still experimental for wider testing - please report any problems to our [issue tracker](https://github.com/pajlads/DinkPlugin/issues) (#89)
- Bugfix: Reduce timeouts from uploading screenshots on slow connections. (#96)
- Bugfix: Ensure boss name is included in boss slayer messages. (#88)
- Dev: Clean up utility code package. (#108)
- Dev: Bump mockito version to 4.11.0. (#107)
- Dev: Add more plugin-hub search tags (#101)
- Dev: Simplify notifier send image evaluation. (#102)
- Dev: Bump mockito version to 4.10.0. (#95)

## 1.1.2

- Minor: Add loot notifier setting to ignore player loot. (#82)
- Bugfix: Ensure some death notifications aren't skipped. (#84)
- Bugfix: Avoid level notifier edge cases related to plugin toggling. (#83)

## 1.1.1

- Minor: Use notifier-specific screenshot filenames. (#79)
- Minor: Support notifier-specific webhook URLs. (#78)
- Dev: Explicitly define achievement diaries. (#80)
- Dev: Add mockito test suite for notifiers. (#74, #75)
- Dev: Utilize more dependency injection. (#73)

## 1.1.0

- Major: Add achievement diary notifications. (#67)
- Major: Add combat achievement notifications. (#63)
- Major: Add additional information for PvP deaths. (#55, #58, #71)
- Major: Add boss kill count notifications. (#50)
- Minor: Add note about Collection Log requiring in-game setting (#70)
- Minor: Add config to ignore slayer tasks with low point values. (#61)
- Bugfix: Prevent old clue or slayer data from entering later notifications. (#68)
- Dev: Bump `runeliteVersion` to always use `latest.release` (#64)
- Dev: Add plugin-hub icon. (#65)

## 1.0.3

- Bugfix: Ensure send image configs are adhered to. (#51)
- Dev: camelCase test methods. (#52)
- Dev: Remove leftover `gradle.properties` file. (#52)
- Dev: Update RuneLite API from v1.9.1 to v1.9.3. (#49)
- Dev: Fix `@Singular` warning caused by downgrading lombok. (#49)

## 1.0.2

- Dev: Adapt build for plugin-hub requirements. (#45, #46, #47)

## 1.0.1

- Dev: Fix gradle build file constraints.

## 1.0.0

- Major: Add speedrun notifications (#23)
- Minor: Add total value pattern for loot and clue scrolls (#33)
- Minor: Notify on virtual level ups (#32)
- Minor: Update description & tags (#30)
- Minor: Standardize settings order (#17)
- Minor: Add additional data to webhook data (#15)
- Minor: This is not UniversalDiscordNotifier, this is Dink now! (#10, #19)
- Bugfix: Fix `PET_REGEX` not matching when you already have a pet out + add tests (#35)
- Bugfix: Ignore more world types (#34)
- Bugfix: Ignore notifications in speedrun worlds (#24)
- Bugfix: Avoid death notifications for actors with the same name (#29)
- Bugfix: Honor notify loot setting for pickpocket and containers (#28)
- Bugfix: Fix levelup notifications always firing (#18)
- Bugfix: Match non-latin slayer tasks (#9)
- Bugfix: Fix %POINTS% outputting total points instead of points recieved (#5)
- Dev: Simplify notifiers (#39)
- Dev: Add a Run config for running Runelite with the plugin (#38)
- Dev: Move logic from plugin to notifier (#36)
- Dev: General code cleanup (#26)
- Dev: Migrate gradle from groovy to kotlin (#21)
- Dev: Add a CI (#14)
- Dev: Create an .editorconfig (#13)
- Dev: Simplify and add tests for `COLLECTION_LOG_REGEX` (#12)
- Dev: Ensure slayer task match is accurate (#11)
