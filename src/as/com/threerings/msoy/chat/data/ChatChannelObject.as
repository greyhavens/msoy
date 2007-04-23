//
// $Id$

package com.threerings.msoy.chat.data {

import com.threerings.io.ObjectInputStream;

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

    /** The field name of the <code>speakService</code> field. */
    public static const SPEAK_SERVICE :String = "speakService";
    // AUTO-GENERATED: FIELDS END

    /** This channel's metadata (type, name). */
    public var channel :ChatChannel;

    /** Info for all channel participants. */
    public var chatters :DSet/*ChatterInfo*/ = new DSet();

    /** Used to generate speak requests on this place object. */
    public var speakService :SpeakMarshaller;

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        channel = (ins.readObject() as ChatChannel);
        chatters = (ins.readObject() as DSet);
        speakService = (ins.readObject() as SpeakMarshaller);
    }
}
}
