//
// $Id$

package com.threerings.msoy.chat.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.util.Long;
import com.threerings.util.Name;

import com.threerings.crowd.chat.data.UserMessage;

/**
 * A custom chat message used on chat channels.
 */
public class ChannelMessage extends UserMessage
{
    /** Creation timestamp on this message (set by server hosting the channel). */
    public var creationTime :Long;

    /** Default constructor. */
    public function ChannelMessage (speaker :Name = null, message :String = null, mode :int = 0)
    {
        super(speaker, null, message, mode);
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        creationTime = (ins.readField(Long) as Long);
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeField(creationTime);
    }
}
}
