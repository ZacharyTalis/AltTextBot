# AltTextBot

![AltTextBot logo, the lowercase letters a and t within a circle representing the @ symbol.](src/main/resources/logo_circle-200px.png)

**[Add AltTextBot to your server!](https://discord.com/oauth2/authorize?client_id=748589442549284865&scope=bot&permissions=11264)** - [Official website](https://zacharytalis.com/blog/atb/) - [Docker image](https://hub.docker.com/r/glossawy/alt-text-bot)

## Tentative deprecation notice

In Q3 of this year, [Discord plans to](https://github.com/discord/discord-api-docs/issues/2508#issuecomment-871904226) debut their own alt-text functionality! Depending on the robustness of this functionality, AltTextBot may or may not continute to operate.

**If we determine that Discord's implementation of alt-text is versatile enough, we're shutting down AltTextBot, though the source code will remain available.**

## About

AltTextBot is a [Javacord](https://github.com/Javacord/Javacord) bot for Discord, designed to reduce message clutter associated with image alt-text.

This bot utilizes Discord's blocked user functionality to create collapsible alt-text. Users who wish to view alt-text by default can keep AltTextBot unblocked.

## Instructions

1. Add the bot to a server using this link: *https://discord.com/oauth2/authorize?client_id=748589442549284865&scope=bot&permissions=11264*
    - Permissions:
        - `MANAGE_MESSAGES` to delete user alt-text submissions
        - `SEND_MESSAGES` to send alt-text
        - `READ_MESSAGES` to read alt-text (this does not include history)
2. Post an image you'd like to add alt-text underneath.
3. Post your alt-text as a separate message with the format `!alt [alt-text]` (no brackets).
4. AltTextBot will replace your message with its own copy. Block AltTextBot to make this copy collapsible.

## Other commands

- `!atbhelp`
  - Get all commands from AltTextBot in a direct message.
- `!atbping`
  - Check to see if AltTextBot is alive.
- `!atbabout`
  - Get AltTextBot's version and authorship info in a direct message.
- `!atbboard`
  - Display the current server's top alt-texters.

![AltTextBot displaying a top-five leaderboard embed of alt-text contributors. The ranks are signified by various emojis.](src/main/resources/leaderboard_screenshot.png)