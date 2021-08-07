# Initiative-bot
A simple bot for Discord, that can track initiative in D&D and similar TTRPGs, based on alternative systems - such as 
Speed Factor from DMG or Greyhawk initiative from Unearthed Arcana. 

## Commands:
* `!ib start` - start initiative
* `!ib end` - stop initiative, if it was started
* `!ib add <base modifier> [name]` - add a participant `name` (defaults to username) with base initiative modifier
* `!ib remove [name]`
* `!ib round` - start a new round
* `!ib end-round` - end the round
* `!ib roll <modifier> [name]` (`name` defaults to the username) - rolls initiative for the current round, combining `modifier` and base modifier
* `!ib mods` - show modifiers for different actions

## Usage
To start encounter, DM calls `start` command. All combatants join using `add` command, stating their base modifiers. DM then starts
a new round using `round` command. All combatants should state their actions and roll for initiative using 
`!ib roll <modifier> [name]` command. Once everyone (i.e., all, who has joined previously) has rolled, the bot will ask the first combatant
to act. Once everyone has acted, the new round begins. If needed, list of combatants can be altered using `add` and `remove` commands.
The bot stores the list of combatants from the previous rounds and won't let the new round start, unless everyone has rolled.
