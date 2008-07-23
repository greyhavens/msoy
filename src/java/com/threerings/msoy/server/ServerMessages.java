//
// $Id$

package com.threerings.msoy.server;

import com.google.inject.Singleton;

import com.threerings.util.MessageManager;

/**
 * Provides a mechanism for translating strings on the server.
 *
 * <em>Note:</em> avoid using this if at all possible. Delay translation to the client so that we
 * can properly react to the client's locale. Translating on the server means that we treat all
 * clients as if they are using the default locale of the server.
 */
@Singleton
public class ServerMessages extends MessageManager
{
    public ServerMessages ()
    {
        super("rsrc.i18n");
    }
}
