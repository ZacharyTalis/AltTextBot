package com.zacharytalis.alttextbot.commands.runners;

import com.zacharytalis.alttextbot.utils.Futures;
import com.zacharytalis.alttextbot.utils.Inflections;
import com.zacharytalis.alttextbot.utils.Ref;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageDecoration;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class AboutRunner {
    private IAboutProvider provider;

    public AboutRunner(IAboutProvider provider) {
        this.provider = provider;
    }

    public CompletableFuture<String> getAboutText() {
        return authorsAsync(provider.api()).thenApply(authors -> {
           return new MessageBuilder()
               .append("Hello! I'm ")
               .append(provider.api().getYourself().getNicknameMentionTag())
               .append(" and I'm running on ")
               .append(provider.bot().internalName() + " v" + provider.bot().version(), MessageDecoration.BOLD, MessageDecoration.UNDERLINE)
               .append(".")
               .appendNewLine()
               .append("I was created by ")
               .append(Inflections.join(authors.iterator()))
               .append(" and my code can be found at ")
               .append(Ref.GITHUB_REPO)
               .append(".")
               .getStringBuilder().toString();
        });
    }

    private CompletableFuture<List<Ref.ProjectAuthor.ProjectAuthorWithUser>> authorsAsync(DiscordApi api) {
        return Futures.lift(Ref.authors.stream().map(author -> author.withUser(api)).collect(Collectors.toList()));
    }
}
