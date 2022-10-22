# Dink
This is a custom version of *Universal Discord Notifier* which is meant to simplify rendering these on IRC and related platforms.


---
# Original readme

I was fed up of having to configure multiple different plugins to get various notifications in discord. So this plugin
aims to compile all of them into a single plugin.

I used the following plugins to help with the development of this:
- Discord Level Notifications: https://github.com/ATremonte/Discord-Level-Notifications
- Discord Collection Logger: https://github.com/PJGJ210/Discord-Collection-Logger
- Discord Loot Logger: https://github.com/Adam-/runelite-plugins/tree/discord-loot-logger/src/main/java/info/sigterm/plugins/discordlootlogger
- Enhanced Discord Notifications: https://github.com/oliverpatrick/Enhanced-Discord-Notifications/

It is not a carbon copy-paste, it was all still written from the ground up using bits of each of these to get it working.

---
## Other Setup

As the collection notification uses the chat message to determine when a collection log item has been added, these messages
need to be enable in game. You can find this option in `Settings > All Settings > Chat > Collection log - New addition notification`

![img.png](img.png)

---
## Config Options
Most of the config options are self-explanatory. But the notification messages for each notification type also
contain some bits that will be replaced with in-game values.

#### All messages:
`%USERNAME%` will be replaced with the username of the player

#### Collection:
`%ITEM%` will be replaced with the item that was dropped for the collection log.

#### Level:
`%SKILL%` will be replaced with the skill name and level that was achieved

#### Loot:
`%LOOT%` will be replaced with a list of the loot and value of said loot

`%TOTAL_VALUE%` will be replaced with the total value of the looted items

`%SOURCE%` will be replace with the source that dropped or gave the loot

#### Slayer:
`%TASK%` will be replaced with the task that you have completed. E.g. `50 monkeys`

`%TASKCOUNT%` will be replaced with the number of tasks that you have completed.

`%POINTS%` will be replaced with the number of points you obtained from the task

#### Quests:
`%QUEST%` will be replaced with the name of the quest completed

#### Clue Scrolls:
`%CLUE%` will be replaced with the type of clue (beginner, easy, etc...)

`%LOOT%` will be replaced with the loot that was obtained from the casket

`%TOTAL_VALUE%` will be replaced with the total value of the items from the reward casket

`%COUNT%` will be replaced by the number of times that you have completed that tier of clue scrolls

All of these are optional and can be omitted from the message if desired.
