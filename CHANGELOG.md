## Unreleased

- Minor: Add personal best time message to kill count notifier. (#106)
- Minor: Add better descriptions for `Min Value` settings. (#100)
- Minor: Add death notifier setting to disable kept item embeds. (#99)
- Minor: Add clue and loot notifier setting to skip screenshots for low item values. (#98)
- Minor: Add level up setting to skip notifications below a specified level. (#97)
- Minor: Retry failed webhook messages with exponential backoff. (#94)
- Minor: Add warning logs for improper runescape settings that impact notifiers. (#92)
- Bugfix: Track Unsired drops for loot notifier. (#89)
- Bugfix: Reduce timeouts from uploading screenshots on slow connections. (#96)
- Bugfix: Ensure boss name is included in boss slayer messages. (#88)
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
