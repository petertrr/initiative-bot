# Initiative-bot
[![codecov](https://codecov.io/gh/petertrr/initiative-bot/branch/main/graph/badge.svg)](https://codecov.io/gh/petertrr/initiative-bot)
[![License](https://img.shields.io/github/license/petertrr/initiative-bot)](https://github.com/petertrr/initiative-bot/blob/main/LICENSE)
[![build](https://github.com/petertrr/initiative-bot/actions/workflows/build_and_test.yml/badge.svg)](https://github.com/petertrr/initiative-bot/actions)

A simple bot for Discord, that can track initiative in D&D and similar TTRPGs, based on alternative systems - such as 
Speed Factor from DMG (currently supported) or Greyhawk initiative from Unearthed Arcana (TBD). 

## Common commands:
* `!ib start` - start initiative
* `!ib end` - stop initiative, if it was started
* `!ib help` - display help message

## Commands for Speed Factor variant:
* `!ib add <base modifier> [name]` - add a participant `name` (defaults to username) with base initiative modifier
* `!ib remove [name]`
* `!ib round` - start a new round, but only if every combatant has rolled a new initiative
* `!ib end-round` - end the round, enable combatants edit and rolling
* `!ib roll <modifier> [name]` (`name` can be omitted if user has added a single character) - rolls initiative for the current round, adds `modifier` and base modifier

## Usage
To start encounter, DM calls `start` command. All combatants join using `add` command, stating their base modifiers. All combatants 
should state their actions and roll for initiative using `!ib roll <modifier> [name]` command. Once everyone (i.e., all, who has joined previously)
has rolled, DM starts a new round using `round` command, followed by `next` command. The bot will then ask the first combatant to act.
Once everyone has acted, DM should call `end-round`. After that, everyone rolls again. If needed, list of combatants can be altered
using `add` and `remove` commands. The bot stores the list of combatants from the previous rounds and won't let the new round start,
unless everyone has rolled.

# Build and run
This app requires Java installation of version 1.8 or greater.
To build the app, run `./gradlew build`.

To run it for your own server, you'll need to register a new discord app and obtain a token for it. Once this is done, you can either
* run initiative-bot-discord with gradle, run `./gradlew :initiative-bot-discord:run --args="<your discord token>"`.
* build distribution using `./gradlew :initiative-bot-discord:installDist` and run an executable script from `initiative-bot-discord/build/install/initiative-bot-discord-<version>/bin`.
* download a distribution zip from Github releases, unpack it and run an executable script
