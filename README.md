# Initiative-bot
A simple bot for Discord, that can track initiative in D&D and similar TTRPGs, based on alternative systems - such as 
Speed Factor from DMG or Greyhawk initiative from Unearthed Arcana. 

## Commands:
* `!ib start`
* `!ib end`
* `!ib round`
* `!ib roll <modifier> [name]` (`name` defaults to the username)
* `!ib add [name]`
* `!ib remove [name]`
* `!ib mods` - show modifiers for different actions

## Usage
To start encounter, DM calls `start` command. Then all combatants should state their actions and roll for initiative using 
`!ib roll <modifier> [name]` command. Once everyone has rolled, DM can call `!ib round` command, and the bot will ask the first combatant
to act. Once everyone has acted, the new round begins. If needed, list of combatants can be altered using `add` and `remove` commands.
The bot stores the list of combatants from the previous rounds and won't let the new round start, unless everyone has rolled.
