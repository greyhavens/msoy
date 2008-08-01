package com.threerings.msoy.chat.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.presents.server.InvocationManager;

import com.threerings.crowd.chat.server.ChatProvider;

import com.threerings.util.Name;

import com.threerings.msoy.game.server.MsoyGameRegistry;

/**
 * The chat provider handles the server side of the chat-related invocation services.
 */
@Singleton
public class MsoyChatProvider extends ChatProvider
{
    /**
     * Creates and registers this chat provider.
     */
    @Inject public MsoyChatProvider (InvocationManager invmgr)
    {
        super(invmgr);
    }

    /**
     * Initializes this chat provider with a reference to the game registry
     */
    public void init (MsoyGameRegistry gameReg)
    {
        _gameReg = gameReg;
    }

    @Override
    public void broadcast(Name from, String bundle, String msg, boolean attention,
            boolean forward) {
        super.broadcast(from, bundle, msg, attention, forward);

        _gameReg.forwardBroadcast(from, bundle, msg, attention);
    }

    // MsoyGameRegistry cannot be injected due to a circular dependency.
    protected MsoyGameRegistry _gameReg;
}
