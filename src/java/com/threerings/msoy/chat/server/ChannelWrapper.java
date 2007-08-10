//
// $Id$

package com.threerings.msoy.chat.server;

import com.samskivert.util.ArrayUtil;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.dobj.MessageListener;

import com.threerings.msoy.chat.data.ChannelMessage;
import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.chat.data.ChatChannelCodes;
import com.threerings.msoy.chat.data.ChatChannelObject;
import com.threerings.msoy.chat.data.ChatterInfo;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.server.MsoyServer;

import static com.threerings.msoy.Log.log;

/**
 * Simple wrapper that packages together channel object, distributed channel object,
 * and various functions for adding and removing channel occupants.
 */
public abstract class ChannelWrapper
    implements MessageListener
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

    // from interface MessageListener
    public void messageReceived (MessageEvent event)
    {
        // please note: this abstract class does not automatically register itself as a listener on
        // the distributed object. subclasses should register themselves if desired.
        if (event.getName().equals(ChatChannelCodes.CHAT_MESSAGE)) {
            Object[] args = event.getArgs();
            if (! (args.length == 1 && args[0] instanceof ChannelMessage)) {
                log.warning("Invalid chat message event [event=" + event + "].");
                return;
            }
            recordChatMessage((ChannelMessage)args[0]);
        }
    }

    /** Updates the peer-local chat storage with the new message. */
    protected void recordChatMessage (ChannelMessage msg)
    {
        // count up old messages to be removed. 
        long now = System.currentTimeMillis();
        int removeCount = 0;
        for (ChannelMessage old : _ccobj.recentMessages) {
            if ((now - old.creationTime) < MAX_RECENT_MESSAGE_AGE) {
                break;
            }
            removeCount++;
        }

        // remove old messages, add the new one
        _ccobj.recentMessages = ArrayUtil.append(
            ArrayUtil.splice(_ccobj.recentMessages, 0, removeCount), msg);
    }

    /** How long a chat message should stay in channel history (in milliseconds). */
    protected static final int MAX_RECENT_MESSAGE_AGE = 5 * 1000;
    
    protected ChatChannel _channel;
    protected ChatChannelManager _mgr;
    protected ChatChannelObject _ccobj;
}
