package com.zacharytalis.alttextbot.utils;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ForwardingObject;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.emoji.CustomEmoji;
import org.javacord.api.entity.message.*;
import org.javacord.api.entity.message.component.HighLevelComponent;
import org.javacord.api.entity.message.embed.Embed;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import org.javacord.api.listener.ObjectAttachableListener;
import org.javacord.api.listener.message.*;
import org.javacord.api.listener.message.reaction.ReactionAddListener;
import org.javacord.api.listener.message.reaction.ReactionRemoveAllListener;
import org.javacord.api.listener.message.reaction.ReactionRemoveListener;
import org.javacord.api.util.event.ListenerManager;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class CommandMessage extends ForwardingObject implements Message, Loggable {
    // ! or ? start commands, tries to fetch the first token (starter + word) at beginning of message.
    // Stores in group named "prefix" in addition to group index 1.
    private static final Pattern PREFIX_PATTERN = Pattern.compile("^(?<prefix>[!?]\\S+)");
    private static final String PREFIX_GROUP = "prefix";

    private final Message delegate;

    public CommandMessage(Message decorated) {
        this.delegate = decorated;
    }

    @Override
    protected Object delegate() {
        return delegate;
    }

    public Optional<String> getCommandPrefix() {
        final var content = getContent();
        final var matcher = PREFIX_PATTERN.matcher(content);
        final var success = matcher.lookingAt();
        final Optional<String> result = success ? Optional.of(matcher.group(PREFIX_GROUP)) : Optional.empty();

        Toolbox.testingOnly(() -> {
            Toolbox.getLogger(CommandMessage.class).debug(
                "success: {}, prefix_group: {}, {}",
                success,
                result.orElse("<none>"),
                this
            );
        });

        return result;
    }

    public boolean isCommandLike() {
        return getCommandPrefix().isPresent();
    }

    public MessageAuthorInfo getAuthorInfo() {
        return new MessageAuthorInfo(getAuthor());
    }

    public long getServerID() {
        return getServer().get().getId();
    }

    @Override
    public String toLoggerString() {
        final var author = getAuthorInfo();
        final var channel = DiscordEntities.getNamedIdentifierOrElse(this::getServerTextChannel, "unknown");
        final var server = DiscordEntities.getNamedIdentifierOrElse(this::getServer, "unknown");

        var arguments = new Object[] { getIdAsString(), author, server, channel };
        var logFormat = "id: {}, {}, server: {}, channel: {}";

        if (Ref.currentEnv().isTesting()) {
            logFormat = "content: ({}), " + logFormat;
            arguments = Arrays.prepend(
                Object.class,
                CharMatcher.is('\n').replaceFrom(getContent(), "\\n"),
                arguments
            );
        }


        return Toolbox.loggerFormat(logFormat, arguments);
    }

    @Override
    public String getContent() {
        return delegate.getContent();
    }

    @Override
    public Optional<Instant> getLastEditTimestamp() {
        return delegate.getLastEditTimestamp();
    }

    @Override
    public List<MessageAttachment> getAttachments() {
        return delegate.getAttachments();
    }

    @Override
    public List<CustomEmoji> getCustomEmojis() {
        return delegate.getCustomEmojis();
    }

    @Override
    public MessageType getType() {
        return delegate.getType();
    }

    @Override
    public TextChannel getChannel() {
        return delegate.getChannel();
    }

    @Override
    public Optional<MessageActivity> getActivity() {
        return delegate.getActivity();
    }

    @Override
    public boolean isPinned() {
        return delegate.isPinned();
    }

    @Override
    public boolean isTts() {
        return delegate.isTts();
    }

    @Override
    public boolean mentionsEveryone() {
        return delegate.mentionsEveryone();
    }

    @Override
    public List<Embed> getEmbeds() {
        return delegate.getEmbeds();
    }

    @Override
    public Optional<User> getUserAuthor() {
        return delegate.getUserAuthor();
    }

    @Override
    public MessageAuthor getAuthor() {
        return delegate.getAuthor();
    }

    @Override
    public Optional<MessageReference> getMessageReference() {
        return delegate.getMessageReference();
    }

    @Override
    public Optional<Message> getReferencedMessage() {
        return delegate.getReferencedMessage();
    }

    @Override
    public boolean isCachedForever() {
        return delegate.isCachedForever();
    }

    @Override
    public void setCachedForever(boolean cachedForever) {
        delegate.setCachedForever(cachedForever);
    }

    @Override
    public List<Reaction> getReactions() {
        return delegate.getReactions();
    }

    @Override
    public List<HighLevelComponent> getComponents() {
        return delegate.getComponents();
    }

    @Override
    public List<User> getMentionedUsers() {
        return delegate.getMentionedUsers();
    }

    @Override
    public List<Role> getMentionedRoles() {
        return delegate.getMentionedRoles();
    }

    @Override
    public Optional<String> getNonce() {
        return delegate.getNonce();
    }

    @Override
    public CompletableFuture<Void> addReactions(String... unicodeEmojis) {
        return delegate.addReactions(unicodeEmojis);
    }

    @Override
    public CompletableFuture<Void> removeReactionByEmoji(User user, String unicodeEmoji) {
        return delegate.removeReactionByEmoji(user, unicodeEmoji);
    }

    @Override
    public CompletableFuture<Void> removeReactionByEmoji(String unicodeEmoji) {
        return delegate.removeReactionByEmoji(unicodeEmoji);
    }

    @Override
    public CompletableFuture<Void> removeReactionsByEmoji(User user, String... unicodeEmojis) {
        return delegate.removeReactionsByEmoji(user, unicodeEmojis);
    }

    @Override
    public CompletableFuture<Void> removeReactionsByEmoji(String... unicodeEmojis) {
        return delegate.removeReactionsByEmoji(unicodeEmojis);
    }

    @Override
    public CompletableFuture<Void> removeOwnReactionByEmoji(String unicodeEmoji) {
        return delegate.removeOwnReactionByEmoji(unicodeEmoji);
    }

    @Override
    public CompletableFuture<Void> removeOwnReactionsByEmoji(String... unicodeEmojis) {
        return delegate.removeOwnReactionsByEmoji(unicodeEmojis);
    }

    @Override
    public int compareTo(Message o) {
        return delegate.compareTo(o);
    }

    @Override
    public DiscordApi getApi() {
        return delegate.getApi();
    }

    @Override
    public long getId() {
        return delegate.getId();
    }

    @Override
    public ListenerManager<ReactionRemoveAllListener> addReactionRemoveAllListener(ReactionRemoveAllListener listener) {
        return delegate.addReactionRemoveAllListener(listener);
    }

    @Override
    public List<ReactionRemoveAllListener> getReactionRemoveAllListeners() {
        return delegate.getReactionRemoveAllListeners();
    }

    @Override
    public ListenerManager<ReactionAddListener> addReactionAddListener(ReactionAddListener listener) {
        return delegate.addReactionAddListener(listener);
    }

    @Override
    public List<ReactionAddListener> getReactionAddListeners() {
        return delegate.getReactionAddListeners();
    }

    @Override
    public ListenerManager<ReactionRemoveListener> addReactionRemoveListener(ReactionRemoveListener listener) {
        return delegate.addReactionRemoveListener(listener);
    }

    @Override
    public List<ReactionRemoveListener> getReactionRemoveListeners() {
        return delegate.getReactionRemoveListeners();
    }

    @Override
    public ListenerManager<MessageEditListener> addMessageEditListener(MessageEditListener listener) {
        return delegate.addMessageEditListener(listener);
    }

    @Override
    public List<MessageEditListener> getMessageEditListeners() {
        return delegate.getMessageEditListeners();
    }

    @Override
    public ListenerManager<CachedMessageUnpinListener> addCachedMessageUnpinListener(CachedMessageUnpinListener listener) {
        return delegate.addCachedMessageUnpinListener(listener);
    }

    @Override
    public List<CachedMessageUnpinListener> getCachedMessageUnpinListeners() {
        return delegate.getCachedMessageUnpinListeners();
    }

    @Override
    public ListenerManager<MessageDeleteListener> addMessageDeleteListener(MessageDeleteListener listener) {
        return delegate.addMessageDeleteListener(listener);
    }

    @Override
    public List<MessageDeleteListener> getMessageDeleteListeners() {
        return delegate.getMessageDeleteListeners();
    }

    @Override
    public ListenerManager<CachedMessagePinListener> addCachedMessagePinListener(CachedMessagePinListener listener) {
        return delegate.addCachedMessagePinListener(listener);
    }

    @Override
    public List<CachedMessagePinListener> getCachedMessagePinListeners() {
        return delegate.getCachedMessagePinListeners();
    }

    @Override
    public <T extends MessageAttachableListener & ObjectAttachableListener> Collection<ListenerManager<T>> addMessageAttachableListener(T listener) {
        return delegate.addMessageAttachableListener(listener);
    }

    @Override
    public <T extends MessageAttachableListener & ObjectAttachableListener> void removeMessageAttachableListener(T listener) {
        delegate.removeMessageAttachableListener(listener);
    }

    @Override
    public <T extends MessageAttachableListener & ObjectAttachableListener> Map<T, List<Class<T>>> getMessageAttachableListeners() {
        return delegate.getMessageAttachableListeners();
    }

    @Override
    public <T extends MessageAttachableListener & ObjectAttachableListener> void removeListener(Class<T> listenerClass, T listener) {
        delegate.removeMessageAttachableListener(listener);
    }
}
