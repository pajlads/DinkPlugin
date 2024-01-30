### All

JSON sent with every notification:

```json5
{
  "content": "Text message as set by the user",
  "extra": {},
  "type": "NOTIFICATION_TYPE",
  "playerName": "your rsn",
  "accountType": "NORMAL | IRONMAN | HARDCORE_IRONMAN",
  "dinkAccountHash": "abcdefghijklmnopqrstuvwxyz1234abcdefghijklmnopqrstuvwxyz",
  "embeds": []
}
```

JSON sent with every notification but only in certain circumstances:

```json5
{
  "clanName": "Dink QA",
  "groupIronClanName":"Dink QA",
},
  "discordUser":{
    "id":"012345678910111213",
    "name":"Gamer",
    "avatarHash":"abc123def345abc123def345abc123de"
   },
```

`clanName` is only sent when the player is in a clan and has the advanced setting `Send Clan Name` enabled.  
`groupIronClanName` is only sent with the player is a GIM and has the advanced setting `Send GIM Clan Name` enabled.  
`discordUser` and its members are only sent when the advanced setting `Send Discord Profile` is enabled.

Note: The examples below omit `embeds`, `playerName`, `accountType`, and `dinkAccountHash` keys because they are always the same.

### Deaths

JSON for non-combat death:

```json5
{
  "content": "%USERNAME% has died...",
  "extra": {
    "valueLost": 300,
    "isPvp": false,
    "keptItems": [],
    "lostItems": [
      {
        "id": 314,
        "quantity": 100,
        "priceEach": 3,
        "name": "Feather"
      }
    ]
  },
  "type": "DEATH"
}
```

JSON for PvP scenarios:

```json5
{
  "content": "%USERNAME% has just been PKed by %PKER% for %VALUELOST% gp...",
  "extra": {
    "valueLost": 300,
    "isPvp": true,
    "killerName": "%PKER%",
    "keptItems": [],
    "lostItems": [
      {
        "id": 314,
        "quantity": 100,
        "priceEach": 3,
        "name": "Feather"
      }
    ]
  },
  "type": "DEATH"
}
```

JSON for NPC scenarios:

```json5
{
  "content": "%USERNAME% has died...",
  "extra": {
    "valueLost": 300,
    "isPvp": false,
    "killerName": "%NPC%",
    "killerNpcId": 69,
    "keptItems": [],
    "lostItems": [
      {
        "id": 314,
        "quantity": 100,
        "priceEach": 3,
        "name": "Feather"
      }
    ]
  },
  "type": "DEATH"
}
```

### Collection

JSON for Collection Notifications:

```json5
{
  "content": "%USERNAME% has added %ITEM% to their collection",
  "extra": {
    "itemName": "Zamorak chaps",
    "itemId": 10372,
    "price": 500812,
    "completedEntries": 420,
    "totalEntries": 1443
  },
  "type": "COLLECTION"
}
```

### Level

JSON for Levelups:

```json5
{
  "content": "%USERNAME% has levelled %SKILL%",
  "extra": {
    "levelledSkills": {
      // These are the skills that dinked
      "Skill name": 30
    },
    "allSkills": {
      // These are all the skills
      "Skill name": 30,
      "Other skill": 1
    },
    "combatLevel": {
      "value": 50,
      "increased": false
    }
  },
  "type": "LEVEL"
}
```

Note: Level 127 in JSON corresponds to attaining max experience in a skill (200M).

### Loot

JSON for Loot Notifications:

```json5
{
  "content": "%USERNAME% has looted: \n\n%LOOT%\nFrom: %SOURCE%",
  "extra": {
    "items": [
      {
        // type of this object is SerializedItemStack

        "id": 1234,
        "quantity": 1,
        "priceEach": 42069,
        // priceEach is the GE price of the item
        "name": "Some item"
      }
    ],
    "source": "Giant rat",
    "category": "NPC",
    "killCount": 60
  },
  "type": "LOOT"
}
```

`killCount` is only specified for NPC loot with the base RuneLite Loot Tracker plugin enabled.

### Slayer

JSON for Slayer Notifications:

```json5
{
  "content": "%USERNAME% has completed a slayer task: %TASK%, getting %POINTS% points and making that %TASKCOUNT% tasks completed",
  "extra": {
    "slayerTask": "Slayer task name",
    "slayerCompleted": "30",
    "slayerPoints": "15",
    "killCount": 135,
    "monster": "Kalphite"
  },
  "type": "SLAYER"
}
```

### Quests

JSON for Quest Notifications:

```json5
{
  "content": "%USERNAME% has completed a quest: %QUEST%",
  "extra": {
    "questName": "Dragon Slayer I",
    "completedQuests": 22,
    "totalQuests": 156,
    "questPoints": 44,
    "totalQuestPoints": 293
  },
  "type": "QUEST"
}
```

### Clue

JSON for Clue Notifications:

```json5
{
  "content": "%USERNAME% has completed a %CLUE% clue, they have completed %COUNT%.\nThey obtained:\n\n%LOOT%",
  "extra": {
    "clueType": "Beginner",
    "numberCompleted": 123,
    "items": [
      {
        // the type of this object SerializedItemStack

        "id": 1234,
        "quantity": 1,
        "priceEach": 42069,
        // priceEach is the GE price of the item
        "name": "Some item"
      }
    ]
  },
  "type": "CLUE"
}
```

### Kill Count

JSON for Kill Count Notifications:

```json5
{
  "content": "%USERNAME% has defeated %BOSS% with a completion count of %COUNT%",
  "extra": {
    "boss": "King Black Dragon",
    "count": 69,
    "gameMessage": "Your King Black Dragon kill count is: 69."
  },
  "type": "KILL_COUNT"
}
```

Note: when `boss` is `Penance Queen`, `count` refers to the high level gamble count, rather than kill count.

### Combat Achievements

JSON for Combat Achievement Notifications:

```json5
{
  "content": "%USERNAME% has completed %TIER% combat task: %TASK%",
  "extra": {
    "tier": "GRANDMASTER",
    "task": "Peach Conjurer",
    "taskPoints": 6,
    "totalPoints": 1337,
    "tierProgress": 517,
    "tierTotalPoints": 645
  },
  "type": "COMBAT_ACHIEVEMENT"
}
```

JSON for Combat Achievement Tier Completion Notifications:

```json5
{
  "content": "%USERNAME% has unlocked the rewards for the %COMPLETED% tier, by completing the combat task: %TASK%",
  "extra": {
    "tier": "GRANDMASTER",
    "task": "Peach Conjurer",
    "taskPoints": 6,
    "totalPoints": 1465,
    "tierProgress": 0,
    "tierTotalPoints": 540,
    "justCompletedTier": "MASTER"
  },
  "type": "COMBAT_ACHIEVEMENT"
}
```

### Achievement Diary

JSON for Achievement Diary Notifications:

```json5
{
  "content": "%USERNAME% has completed the %DIFFICULTY% %AREA% Achievement Diary, for a total of %TOTAL% diaries completed",
  "extra": {
    "area": "Varrock",
    "difficulty": "HARD",
    "total": 15,
    "tasksCompleted": 152,
    "tasksTotal": 492,
    "areaTasksCompleted": 37,
    "areaTasksTotal": 42
  },
  "type": "ACHIEVEMENT_DIARY"
}
```

### Pets

JSON for Pet Notifications:

```json5
{
  "content": "%USERNAME% has a funny feeling they are being followed",
  "extra": {
    "petName": "Ikkle hydra",
    "milestone": "5,000 killcount",
    "duplicate": false
  },
  "type": "PET"
}
```

`petName` is only included if the game sent it to the users chat via untradeable drop, collection log, or clan notifications.
`milestone` is only included if a clan notification was triggered.

### Speedrunning

JSON for Personal Best Speedrun Notifications:

```json5
{
  "content": "%USERNAME% has just beat their personal best in a speedrun of %QUEST% with a time of %TIME%",
  "extra": {
    "questName": "Cook's Assistant",
    "personalBest": "1:13.20",
    "currentTime": "1:13.20",
    "isPersonalBest": true
  },
  "type": "SPEEDRUN"
}
```

JSON for Normal Speedrun Notifications:

```json5
{
  "content": "%USERNAME% has just finished a speedrun of %QUEST% with a time of %TIME% (their PB is %BEST%)",
  "extra": {
    "questName": "Cook's Assistant",
    "personalBest": "1:13.20",
    "currentTime": "1:22.20"
  },
  "type": "SPEEDRUN"
}
```

### BA Gambles

JSON for BA Gambles Notifications:

```json5
{
  "content": "%USERNAME% has reached %COUNT% high gambles",
  "extra": {
    "gambleCount": 500,
    "items": [
      {
        "id": 3122,
        "quantity": 1,
        "priceEach": 35500,
        "name": "Granite shield"
      }
    ]
  },
  "type": "BARBARIAN_ASSAULT_GAMBLE"
}
```

### Player Kills

JSON for PK Notifications:

```json5
{
  "content": "%USERNAME% has PK'd %TARGET%",
  "type": "PLAYER_KILL",
  "extra": {
    "victimName": "%TARGET%",
    "victimCombatLevel": 69,
    "victimEquipment": {
      "AMULET": {
        "id": 1731,
        "priceEach": 1987,
        "name": "Amulet of power"
      },
      "WEAPON": {
        "id": 1333,
        "priceEach": 14971,
        "name": "Rune scimitar"
      },
      "TORSO": {
        "id": 1135,
        "priceEach": 4343,
        "name": "Green d'hide body"
      },
      "LEGS": {
        "id": 1099,
        "priceEach": 2077,
        "name": "Green d'hide chaps"
      },
      "HANDS": {
        "id": 1065,
        "priceEach": 1392,
        "name": "Green d'hide vambraces"
      }
    },
    "world": 394,
    "location": {
      "x": 3334,
      "y": 4761,
      "plane": 0
    },
    "myHitpoints": 20,
    "myLastDamage": 12
  }
}
```

`world` and `location` are _not_ sent if the user has disabled the "Include Location" notifier setting.

### Group Storage

JSON for GIM Bank Notifications:

```json5
{
  "content": "%USERNAME% has deposited: %DEPOSITED% | %USERNAME% has withdrawn: %WITHDRAWN%",
  "type": "GROUP_STORAGE",
  "accountType": "GROUP_IRONMAN | HARDCORE_GROUP_IRONMAN",
  "extra": {
    "groupName": "group name",
    "deposits": [
      {
        "id": 315,
        "name": "Shrimps",
        "quantity": 2,
        "priceEach": 56
      },
      {
        "id": 1205,
        "name": "Bronze dagger",
        "quantity": 1,
        "priceEach": 53
      }
    ],
    "withdrawals": [
      {
        "id": 1265,
        "name": "Bronze pickaxe",
        "quantity": 1,
        "priceEach": 22
      }
    ],
    "netValue": 143
  }
}
```

`accountType` is always `GROUP_IRONMAN` or `HARDCORE_GROUP_IRONMAN` for Group Storage notifications.

### Grand Exchange

JSON for GE Notifications:

```json5
{
  "content": "%USERNAME% %TYPE% %ITEM% on the GE",
  "type": "GRAND_EXCHANGE",
  "extra": {
    "slot": 1,
    "status": "SOLD",
    "item": {
      "id": 314,
      "quantity": 2,
      "priceEach": 3,
      "name": "Feather"
    },
    "marketPrice": 2,
    "targetPrice": 3,
    "targetQuantity": 2,
    "sellerTax": 0
  }
}
```

Unlike `GrandExchangeOfferChanged#getSlot`, `extra.slot` is one-indexed;
values can range from 1 to 8 (inclusive) for members, and 1 to 3 (inclusive) for F2P.

See [javadocs](https://static.runelite.net/api/runelite-api/net/runelite/api/GrandExchangeOfferState.html) for the possible values of `extra.status`.

### Player Trades

JSON for Player Trade Notifications:

```json5
{
  "content": "%USERNAME% traded with %COUNTERPARTY%",
  "type": "TRADE",
  "extra": {
    "counterparty": "%COUNTERPARTY%",
    "receivedItems": [
      {
        "id": 314,
        "quantity": 100,
        "priceEach": 2,
        "name": "Feather"
      }
    ],
    "givenItems": [
      {
        "id": 2,
        "quantity": 3,
        "priceEach": 150,
        "name": "Cannonball"
      }
    ],
    "receivedValue": 200,
    "givenValue": 450
  }
}
```

### Leagues

JSON for Area Unlock Notifications:

```json5
{
  "type": "LEAGUES_AREA",
  "content": "%USERNAME% selected their second region: Kandarin.",
  "accountType": "IRONMAN",
  "seasonalWorld": true,
  "extra": {
    "area": "Kandarin",
    "index": 2,
    "tasksCompleted": 200,
    "tasksUntilNextArea": 200
  }
}
```

Note: `index` refers to the order of region unlocks.
Here, Kandarin was the second region selected.
For all players, Karamja is the _zeroth_ region selected (and there is no notification for Misthalin).

JSON for Relic Chosen Notifications:

```json5
{
  "type": "LEAGUES_RELIC",
  "content": "%USERNAME% unlocked a Tier 1 Relic: Production Prodigy.",
  "accountType": "IRONMAN",
  "seasonalWorld": true,
  "extra": {
    "relic": "Production Prodigy",
    "tier": 1,
    "requiredPoints": 0,
    "totalPoints": 20,
    "pointsUntilNextTier": 480
  }
}
```

JSON for Task Completed Notifications:

```json5
{
  "type": "LEAGUES_TASK",
  "content": "%USERNAME% completed a Easy task: Pickpocket a Citizen.",
  "accountType": "IRONMAN",
  "seasonalWorld": true,
  "extra": {
    "taskName": "Pickpocket a Citizen",
    "difficulty": "EASY",
    "taskPoints": 10,
    "totalPoints": 30,
    "tasksCompleted": 3,
    "tasksUntilNextArea": 57,
    "pointsUntilNextRelic": 470,
    "pointsUntilNextTrophy": 2470
  }
}
```

JSON for Task Notifications that unlocked a Trophy:

```json5
{
  "type": "LEAGUES_TASK",
  "content": "%USERNAME% completed a Hard task, The Frozen Door, unlocking the Bronze trophy!",
  "accountType": "IRONMAN",
  "seasonalWorld": true,
  "extra": {
    "taskName": "The Frozen Door",
    "difficulty": "HARD",
    "taskPoints": 80,
    "totalPoints": 2520,
    "tasksCompleted": 119,
    "tasksUntilNextArea": 81,
    "pointsUntilNextRelic": 1480,
    "pointsUntilNextTrophy": 2480,
    "earnedTrophy": "Bronze"
  }
}
```

Note: Fields like `tasksUntilNextArea`, `pointsUntilNextRelic`, and `pointsUntilNextTrophy` can be omitted if there is no next level of progression
(i.e., all three regions selected, all relic tiers unlocked, all trophies acquired).

`accountType` is always `IRONMAN` for Leagues, unrelated to what the user is outside of Leagues.

### Metadata

JSON for Login Notifications:

```json5
{
  "content": "%USERNAME% logged into World %WORLD%",
  "type": "LOGIN",
  "extra": {
    "world": 338,
    "collectionLog": {
      "completed": 651,
      "total": 1477
    },
    "combatAchievementPoints": {
      "completed": 503,
      "total": 2005
    },
    "achievementDiary": {
      "completed": 42,
      "total": 48
    },
    "achievementDiaryTasks": {
      "completed": 477,
      "total": 492
    },
    "barbarianAssault": {
      "highGambleCount": 0
    },
    "skills": {
      "totalExperience": 346380298,
      "totalLevel": 2164,
      "levels": {
        "Hunter": 90,
        "Thieving": 86,
        "Runecraft": 86,
        "Construction": 86,
        "Cooking": 103,
        "Magic": 106,
        "Fletching": 99,
        "Herblore": 91,
        "Firemaking": 100,
        "Attack": 107,
        "Fishing": 92,
        "Crafting": 96,
        "Hitpoints": 111,
        "Ranged": 110,
        "Mining": 88,
        "Smithing": 91,
        "Agility": 82,
        "Woodcutting": 96,
        "Slayer": 104,
        "Defence": 103,
        "Strength": 104,
        "Prayer": 91,
        "Farming": 100
      },
      "experience": {
        "Hunter": 5420696,
        "Thieving": 3696420,
        "Runecraft": 3969420,
        "Construction": 3680085,
        "Cooking": 19696420,
        "Magic": 28008135,
        "Fletching": 13696420,
        "Herblore": 5969420,
        "Firemaking": 14420666,
        "Attack": 30696420,
        "Fishing": 6632248,
        "Crafting": 9696420,
        "Hitpoints": 46969666,
        "Ranged": 42069420,
        "Mining": 4696420,
        "Smithing": 6428696,
        "Agility": 2666420,
        "Woodcutting": 9696666,
        "Slayer": 21420696,
        "Defence": 21212121,
        "Strength": 23601337,
        "Prayer": 6369666,
        "Farming": 15666420
      }
    },
    "questCount": {
      "completed": 156,
      "total": 158
    },
    "questPoints": {
      "completed": 296,
      "total": 300
    },
    "slayer": {
      "points": 2204,
      "streak": 1074
    },
    "pets": [
      {
        "itemId": 11995,
        "name": "Pet chaos elemental"
      },
      {
        "itemId": 13071,
        "name": "Chompy chick"
      }
    ]
  }
}
```

`extra.pets` requires the base Chat Commands plugin to be enabled.
`collectionLog` data can be missing if the user does not have the Character Summary tab selected (since the client otherwise is not sent that data).
