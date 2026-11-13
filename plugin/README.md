# SnailMail plugin

The custom Paper plugin for the SnailMail server: mail, Shells (currency), fishing and bugs, museum, seasons and the news board, built wave by wave.

- Paper 26.3, Java 25
- Java players get chest menus, Bedrock players get native forms through Floodgate

## Commands

| Command | What it does |
|---|---|
| `/shells` (`/bal`) | Your Shells balance |
| `/shells <player>` | Someone else's balance |
| `/pay <player> <amount>` | Pay another player |
| `/shells give\|take\|set <player> <amount>` | Admin only (`snailmail.admin`) |
| `/mail` (`/mailbox`) | Open your mailbox: a chest menu on Java, a form on Bedrock |
| `/mail send <player> <letter>` | Post a letter. It arrives the next morning at 6 AM town time |
| `/mail gift <player> [note]` | Post the item in your hand, with an optional note |
| `/snailmail reload` | Reload the config (admin) |

Shells are whole numbers and are registered with Vault, so other plugins can use them. Data lives in `plugins/SnailMail/snailmail.db` (SQLite).

## Build

```
cd plugin
./gradlew build   # also runs the tests
```

The jar lands in `plugin/build/libs/`. Gradle downloads Java 25 by itself if it isn't installed.

This repo is public, so never commit passwords, tokens or other secrets.
