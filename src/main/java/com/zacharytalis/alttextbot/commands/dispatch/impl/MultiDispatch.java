package com.zacharytalis.alttextbot.commands.dispatch.impl;

import com.zacharytalis.alttextbot.commands.dispatch.ICommandDispatch;
import com.zacharytalis.alttextbot.messages.UserCommandMessage;
import com.zacharytalis.alttextbot.utils.Toolbox;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MultiDispatch implements ICommandDispatch {
    private final List<ICommandDispatch> dispatchers = new LinkedList<>();

    public MultiDispatch(ICommandDispatch... dispatchers) {
        this.dispatchers.addAll(Arrays.asList(dispatchers));
    }

    @Override
    public boolean canDispatch(UserCommandMessage msg) {
        return this.dispatchers.stream().anyMatch(dispatcher -> dispatcher.canDispatch(msg));
    }

    @Override
    public CompletableFuture<Void> dispatch(UserCommandMessage msg) {
        for (var dispatcher : this.dispatchers)
            if (dispatcher.canDispatch(msg)) {
                return dispatcher.dispatch(msg);
            }

        return Toolbox.nullFuture();
    }
}
