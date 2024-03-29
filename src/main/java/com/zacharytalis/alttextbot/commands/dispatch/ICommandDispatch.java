package com.zacharytalis.alttextbot.commands.dispatch;

import com.zacharytalis.alttextbot.messages.UserCommandMessage;

import java.util.concurrent.CompletableFuture;

public interface ICommandDispatch {
    boolean canDispatch(UserCommandMessage msg);

    CompletableFuture<Void> dispatch(UserCommandMessage msg);
}
