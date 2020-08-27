package com.zacharytalis.alttextbot;

import com.google.common.util.concurrent.FutureCallback;
import com.zacharytalis.alttextbot.entities.message.Message;
import com.zacharytalis.alttextbot.listener.message.MessageCreateListener;

import java.util.LinkedList;

/**
 * Bot for posting image alt-text as a separate Discord message.
 */
public class AltTextBot {

    // Used for user input parsing/message sending
    private String send;
    private String pm;
    private String[] userInput;

    // Constants
    private static final String BOT_TOKEN = System.getenv("bot_token");
    private final String ERROR_ALT = "Error with alt-text functionality. Do I have the right permissions?";
    private final LinkedList<Command> publicCommands = new LinkedList<>();

    /**
     * Constructor/runtime for AltTextBot.
     * @param token the String token used for the bot to connect to Discord.
     */
    private AltTextBot(String token) {

        ///// Set all public commands /////
        final Command COMMAND_HELP = new Command("!atbhelp",
                "Get all of the commands from AltTextBot.", publicCommands);
        final Command COMMAND_PING = new Command("!atbping",
                "Check to see if AltTextBot is alive.", publicCommands);
        final Command COMMAND_ALT = new Command("!alt",
                "Replace the user message with alt-text. Post your alt-text as a separate message with the " +
                        "format `!alt [alt-text]` (no brackets).", publicCommands);

        // Token is provided by the Discord bot page
        DiscordAPI api = Javacord.getApi(token, true);

        // Begin the connect
        api.connect(new FutureCallback<DiscordAPI>() {
            @Override
            public void onSuccess(DiscordAPI api) {
                // register listener
                api.registerListener(new MessageCreateListener() {
                    @Override
                    public void onMessageCreate (DiscordAPI api, Message message) {


                        // Get message input, and refresh send and pm
                        userInput = message.getContent().split(" ");
                        send = "";
                        pm = "";


                        ///// PUBLIC COMMANDS BELOW /////


                        /// COMMAND_HELP ///
                        if (check(COMMAND_HELP)) {
                            for (Command command : publicCommands) {
                                pm = pm.concat(command.getName()+" ~ "+command.getInfo()+"\n");
                            }
                        }


                        /// COMMAND_PING ///
                        if (check(COMMAND_PING)) {
                            add("Yes yes, I'm here.");
                        }


                        /// COMMAND_REACT ///
                        if (check(COMMAND_ALT)) {
                            try {

                                // Get content from message
                                send = message.getContent().substring(COMMAND_ALT.getName().length()).trim();
                                // Delete message
                                message.delete();

                            } catch (Exception exc) {
                                add(ERROR_ALT);
                            }
                        }


                        ///// Send out final message and/or PM /////
                        if (!send.equals("")) message.reply(send);
                        if (!pm.equals("")) message.getAuthor().sendMessage(pm);

                    }
                });
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
            }
        });
    }

    /**
     * Check to see if the first word in user input is for the given command.
     * @param check the command to check for.
     * @return whether or not the user input is what you were checking for.
     */
    private boolean check(Command check) {
        try {
            return userInput[0].equalsIgnoreCase(check.getName());
        } catch (IndexOutOfBoundsException exc) {
            System.out.print("input() improperly called (userInput not split).");
            return false;
        }
    }

    /**
     * Add a string message to the reply (followed by a newline).
     * @param message the message to add onto the reply.
     */
    private void add(String message) {
        send += message + "\n";
    }

    /**
     * Get the bot up and running.
     * @param args the executable arguments.
     */
    public static void main(String[] args) {

        // Create the bot
        @SuppressWarnings("unused") AltTextBot bot = new AltTextBot(BOT_TOKEN);

    }

}
