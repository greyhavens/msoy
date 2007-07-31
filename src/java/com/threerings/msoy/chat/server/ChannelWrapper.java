//
// $Id$

package com.threerings.msoy.chat.server;

import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.chat.data.ChatChannelObject;
import com.threerings.msoy.chat.data.ChatterInfo;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.server.MsoyServer;

import com.threerings.crowd.chat.data.SpeakMarshaller;
import com.threerings.crowd.chat.server.SpeakDispatcher;
import com.threerings.crowd.chat.server.SpeakProvider;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.SetAdapter;

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
    public boolean hasMember (ChatterInfo userInfo)
    {
        return ready() && _ccobj.chatters.containsKey(userInfo.name);
    }
    
    /** Links a newly created distributed channel object to the channel definition. */
    protected static void initializeCCObj (final ChatChannelObject ccobj, ChatChannel channel)
    {
        ccobj.channel = channel;
        SpeakProvider.SpeakerValidator validator = new SpeakProvider.SpeakerValidator() {
            public boolean isValidSpeaker (DObject speakObj, ClientObject speaker, byte mode) {
                MemberObject who = (MemberObject)speaker;
                return (who == null || ccobj.chatters.containsKey(who.memberName));
            }
        };
        SpeakDispatcher sd = new SpeakDispatcher(new SpeakProvider(ccobj, validator));
        ccobj.setSpeakService((SpeakMarshaller)MsoyServer.invmgr.registerDispatcher(sd));
    }

    /** Clears any initialization performed on the distributed channel object, and
     *  unregisters the listener. */
    protected static void deinitializeCCObj (ChatChannelObject ccobj)
    {
        if (ccobj == null) {
            log.warning("Deinitializing null channel object!"); // something went horribly wrong
            return;
        }

        MsoyServer.invmgr.clearDispatcher(ccobj.speakService);
    }

    protected ChatChannel _channel;
    protected ChatChannelManager _mgr;
    protected ChatChannelObject _ccobj;
}
