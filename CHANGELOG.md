## Unreleased

- Bugfix: Fire death notifications for Doom modifier in Fortis Colosseum. (#474)
- Dev: Allow custom webhook handlers to use HTTP status code 307 and 308 to redirect requests. (#484)
- Dev: Add message source to chat notification metadata. (#476)

## 1.10.1

- Minor: Fire notifications for XP milestones beyond level 99. (#462)
- Bugfix: Distinguish corrupted from normal Gauntlet for loot notifications. (#470)
- Bugfix: Avoid double PK notification when multi-tick special attack had already killed the target. (#467)
- Dev: Avoid benign NPE when config items are reset. (#464)

## 1.10.0

- Major: Add notifier for chat messages that match custom patterns. (#391, #450)
- Minor: Include Smol heredit pet name in pet notifications. (#458)
- Minor: Prevent RuneLite from resetting empty config values for footer, ignored death regions, and chat patterns. (#454)
- Minor: `Completed Entries` field in Discord rich embed is now called `Completed`. (#448)
- Bugfix: Allow Lunar Chest openings to trigger kill count notifier. (#449)
- Bugfix: Exclude denylisted items from loot rarity override consideration. (#447)
- Dev: Enable reproducible builds. (#344)

## 1.9.1

- Minor: Include Quetzin pet name in pet notifications. (#442)

## 1.9.0

- Major: Add item rarity to monster loot and collection notifications. (#425, #427)
- Minor: Add `%COMPLETED` and `%TOTAL_POSSIBLE%` variables for collection log template. (#439)
- Minor: Add wiki link for loot source in rich embeds. (#431)
- Minor: Add chat message upon noteworthy plugin updates. (#429, #436)
- Minor: Allow loot notifications for rare items below the min value threshold. (#426)
- Minor: Include relevant kill count in collection log notifications. (#424)
- Minor: Obtain kill count from chat commands plugin for loot notifications. (#392)
- Minor: Add chat command to obtain the player's dink hash. (#408)
- Bugfix: Include estimated kills without drops in KC for loot and collection notifications. (#432)
- Dev: Refactor kill count tracking from `LootNotifier` to `KillCountService`. (#392)

## 1.8.4

- Minor: Fire loot notification for PK loot chests with sufficiently high total value. (#417)
- Minor: Include region information in death notification metadata. (#420)
- Minor: Allow customization of region IDs where deaths should be ignored. (#415)
- Bugfix: Treat deaths in the graveyard room of the mage training arena as safe. (#418)
- Dev: Clarify the plugin description. (#419)

## 1.8.3

- Minor: Include Scurrius pet name in pet notifications. (#410)
- Bugfix: Update death safe exceptions on config import from clipboard. (#406)
- Dev: Disable leagues notifier. (#411)

## 1.8.2

- Dev: Dynamically obtain coin item variations. (#398)
- Dev: Changed the plugin icon back to the original version. (#401)

## 1.8.1

- Dev: Changed the plugin icon to a Christmas version. (#393)
- Dev: Refactor widget ID packing. (#389)

## 1.8.0

- Major: Add notifier to capture trades with other players. (#361)
- Minor: Add loot notifier setting that redirects pk loot to the pk notifier override url. (#353)
- Minor: Allow customization of which safe deaths can trigger notifications. (#363, #384, #385)
- Bugfix: Avoid erroneous level notification on login when initial data is delayed. (#383)
- Bugfix: Ensure diary notifications below the configured minimum difficulty are not sent. (#382)
- Bugfix: Update set of items that are never kept on dangerous deaths. (#364)
- Bugfix: Don't report inaccurate completed collections count, when character summary tab was not selected. (#374)
- Bugfix: Avoid undercounting diary completions for notifications that occur shortly after a teleport. (#373)
- Dev: Small refactor of leagues notifier. (#387)
- Dev: Execute screenshot rescaling on background thread to minimize FPS impact. (#378)

## 1.7.2

- Dev: Utilize runelite event for unsired loot instead of custom widget handler (#375).

## 1.7.1

- Bugfix: Report correct remaining tasks until next area unlock in Leagues notifications. (#369)

## 1.7.0

- Major: Add leagues notifier for areas, relics, and tasks. (#366)

## 1.6.5

- Minor: Allow notifications on seasonal worlds to be ignored via advanced config. (#357)
- Minor: Prefer historical name for first Recipe for Disaster quest. (#352)
- Minor: Include owned pets in login notification metadata. (#347)
- Minor: Include individual skill XP in login notification metadata. (#345)
- Bugfix: Classify reminisced Galvek deaths as safe. (#351)
- Bugfix: Improve handling of queued notifications upon concurrent config changes. (#355)
- Dev: Migrate to new RuneLite Widget API. (#358, #359)

## 1.6.4

- Minor: Add task progress metadata for diary notifications. (#331)
- Minor: Use boss chat message for latest kill count in loot notifications. This is only for "normal" bosses and won't yet work for things such as raids/barrows/hespori. (#324)
- Minor: Include hashed account unique identifier in notification metadata. (#334)
- Minor: Made the default setting for screenshots enabled for consistency across all notifiers. (#330)
- Minor: Send character information on login to custom handlers via advanced config. (#321)
- Minor: Add key in the Speedrun notifier extra object for whether the run is a personal best or not. (#329)
- Minor: Indicate in pet notification metadata when a pet was previously owned but lost. (#314)
- Minor: Support wildcards in loot item name filters. (#312)
- Bugfix: Use correct duration time for gauntlet minigame completions. (#341)
- Bugfix: Allow the first xp drop after login to trigger level notifications. (#332)
- Bugfix: Don't treat personal best ties as personal bests for Speedrun notifications. (#329)
- Bugfix: Classify Gauntlet deaths as safe, unless the player is a hardcore ironman. (#327)
- Bugfix: Classify deaths in Tombs of Abascut as safe or dangerous depending on the attempt invocations. (#317)
- Dev: Update gradle wrapper to v8.4 minor version. (#328)
- Dev: Add pet test for Lil' creator. (#325)
- Dev: Bump Java source & target compatability from 8 to 11. (#318)

## 1.6.3

- Minor: Add item name allowlist and denylist for loot notifications. (#310)
- Minor: Add allowlist mode for Filtered RSNs under the Advanced category. Use this setting with caution. (#306)
- Minor: Send kill count notifications for Penance Queen kills. (#304)
- Minor: Include popup notification widget in collection log screenshots. (#309)
- Minor: Support new thread creation for Discord forum channels. (#302)
- Minor: Indicate whether pets are already owned in pet notifications. (#303, #307)
- Minor: Include NPC kill count in loot notifications. (#299)
- Minor: Add option to disable price in Group Storage notifications. (#298)
- Minor: Add `%TOTAL_LEVEL%` message template for level notifications. (#300)
- Dev: Update gradle wrapper to v8.3 minor version. (#293)

## 1.6.2

- Bugfix: Avoid duplicate grand exchange notifications for partial transactions at trade limit.

## 1.6.1

- Minor: Include new pet names from Desert Treasure II bosses in notification metadata. (#279)
- Bugfix: Fire loot notifications for The Whisperer kills. (#286)
- Bugfix: Classify webhook overrides for player kill, grand exchange, and group storage notifiers appropriately for config exports. (#284)

## 1.6.0

- Major: Add notifier for when items are bought or sold on the Grand Exchange. (#275, #277)
- Minor: Fire level notification upon reaching max skill experience of 200M. (#273)
- Minor: Include clan name in notificiation metadata. (#274)
- Dev: Update gradle wrapper to v8.2.1 patch version. (#276)

## 1.5.3

- Minor: Adds option to announce every level-up past a certain level. (#265)
- Bugfix: Increase level notifier initialization delay, as it sometimes occurred too early causing incorrect levelup notifications to trigger. (#264)
- Dev: Optimize skill level initialization algorithm. (#269)
- Dev: Add gradle 7.4 runner to ensure plugin hub compatibility. (#268)
- Dev: Remove references to deprecated Skill.OVERALL enum value. (#266)
- Dev: Avoid compiler exception when building the plugin with JDK 17+. (#267)
- Dev: Update gradle wrapper to v8.2, which includes path traversal fixes. (#263)
- Dev: Optimize notification templating engine performance. (#258)

## 1.5.2

- Minor: Include link to killed player profile in PK notifier. (#246)
- Minor: Add setting to exclude group name from GIM shared storage notifications. (#247)
- Minor: Include relevant wiki links in rich embed content. (#243, #248)
- Minor: Re-implemented README info for generating a Discord webhook. (#256, #257)
- Bugfix: Improve kept or lost classification of stackable items for death notifications. (#255)
- Bugfix: Ignore notifications from new beta worlds. (#253)
- Bugfix: Avoid combat level notifications that don't conform to the configured interval. (#250, #251)

## 1.5.1

- Minor: Track points progress towards next combat achievement tier rewards unlock. (#236)
- Minor: Add combat achievement points as rich embed field. (#235)
- Minor: Use item image as rich embed thumbnail for loot notifications. (#234)
- Bugfix: Avoid combat level up notifications when combat level did not change. (#240)
- Bugfix: Prevent combat level notifications when the notifier is disabled. (#239)
- Dev: Redefine account type enum to workaround upstream deprecation. (#238)

## 1.5.0

- Major: Add notifications for depositing or withdrawing items from Group Ironman Shared Storage. (#225)
- Minor: Include discord user profile in notification metadata. (#226)
- Minor: Add setting to allow death notifications during safe activities. (#227)
- Minor: Allow player name in Discord notifications to link to CollectionLog.net profile. (#224)
- Bugfix: Ensure lost items metadata is empty upon safe deaths in TzHaar Fight Cave or Inferno. (#230)

## 1.4.1

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
