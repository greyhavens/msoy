//
// $Id$

package com.threerings.msoy.chat.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.TypedArray;

import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.DSet;

import com.threerings.crowd.chat.data.SpeakMarshaller;

/**
 * Used to distribute chat messages to all participants in a chat channel.
 */
public class ChatChannelObject extends DObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>channel</code> field. */
    public static const CHANNEL :String = "channel";

    /** The field name of the <code>chatters</code> field. */
    public static const CHATTERS :String = "chatters";

    /** The field name of the <code>recentMessages</code> field. */
    public static const RECENT_MESSAGES :String = "recentMessages";

    /** The field name of the <code>speakService</code> field. */
    public static const SPEAK_SERVICE :String = "speakService";
    // AUTO-GENERATED: FIELDS END

    /** This channel's metadata (type, name). */
    public var channel :ChatChannel;

    /** Info for all channel participants. */
    public var chatters :DSet/*ChatterInfo*/ = new DSet();

    /**
     * Peer storage for recently received ChannelMessage chat message instances.
     * This field has special behavior in a peer environment: its value is not distributed;
     * instead, each peer server will listen for chat messages separately, and update its
     * own local instance of the array.
     * <p> Also, clients will not be notified about changes to this array. The data is intended
     * only to be used when initially subscribing to the distributed object, in order to obtain
     * a snapshot of the history of recent messages. After subscribing, the client will not
     * receive update notifications, instead it should listen for ChannelMessage events
     * and process them as desired.
     */
    public var recentMessages :TypedArray /*ChannelMessage*/ = TypedArray.create(ChannelMessage);
    
    /**
     * Used to generate speak requests on this place object.
     * This field has special behavior in a peer environment: its value is not distributed;
     * instead, each peer server overwrites it with a reference to its own local provider instance.
     */
    public var speakService :SpeakMarshaller;

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        channel = (ins.readObject() as ChatChannel);
        chatters = (ins.readObject() as DSet);
        recentMessages = (ins.readObject() as TypedArray);
        speakService = (ins.readObject() as SpeakMarshaller);
    }
}
}
