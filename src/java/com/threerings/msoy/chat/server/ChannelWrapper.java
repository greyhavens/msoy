//
// $Id$

package com.threerings.msoy.chat.server;

import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.chat.data.ChatChannelObject;
import com.threerings.msoy.chat.data.ChatterInfo;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.server.MsoyServer;

import com.threerings.presents.data.ClientObject;

import static com.threerings.msoy.Log.log;

/**
 * Simple wrapper that packages together channel object, distributed channel object,
 * and various functions for adding and removing channel occupants.
 */
public abstract class ChannelWrapper
{
    public ChannelWrapper (ChatChannelManager mgr, ChatChannel channel)
    {
        _mgr = mgr;
        _channel = channel;
    }

    /** Accessor for the channel variable. */
    public ChatChannel getChannel()
    {
        return _channel;
    }

    /** Accessor for the distributed channel object. */
    public ChatChannelObject getCCObj()
    {
        return _ccobj;
    }

    /** Returns true when the wrapped channel has been created and initialized. */
    public boolean ready ()
    {
        return _ccobj != null;
    }

    /** Initializes the local distributed channel object. When done, calls the continuation. */
    public abstract void initialize (final ChannelCreationContinuation cccont);

    /** Deinitializes the local distributed channel object. */
    public abstract void shutdown ();

    /** Asks the hosting server to add the specified user to the wrapped channel. */
    public abstract void addChatter (ChatterInfo userInfo);

    /** Asks the hosting server to remove the speficied user from the wrapped channel. */
    public abstract void removeChatter (ChatterInfo userInfo);

    /** Does this channel contain the specified chatter? */
    public boolean hasMember (ChatterInfo chatter)
    {
        return ready() && _ccobj.chatters.containsKey(chatter.name);
    }
    
    /** Does this channel contain the specified chatter? */
    public boolean hasMember (ClientObject chatter) 
    { 
        MemberObject who = (MemberObject)chatter;
        return ready() && (who == null || _ccobj.chatters.containsKey(who.memberName));
    }

    protected ChatChannel _channel;
    protected ChatChannelManager _mgr;
    protected ChatChannelObject _ccobj;
}
