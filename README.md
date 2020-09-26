# AltTextBot

<!--suppress CheckImageSize -->
<img src="src/main/resources/logo.png" width="200px" alt="AltTextBot logo.">

**[Add AltTextBot to your server!](https://discord.com/oauth2/authorize?client_id=748589442549284865&scope=bot&permissions=11264)**

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
- `!atbboard`
  - Display the server's top alt-texters.
- `!alt <alt-text>`
  - Replace the user message with alt-text.
