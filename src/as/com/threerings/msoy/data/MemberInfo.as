//
// $Id$

package com.threerings.msoy.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.crowd.data.OccupantInfo;

public class MemberInfo extends OccupantInfo
{
    /** The memberId of this occupant. */
    public var memberId :int;

    /** The media that represents our avatar. */
    public var media :MediaData;

    /** The style of chat bubble to use. */
    public var chatStyle :int;

    /** The style with which the chat bubble pops up. */
    public var chatPopStyle :int;

    // documentation inherited
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeInt(memberId);
        out.writeObject(media);
        out.writeShort(chatStyle);
        out.writeShort(chatPopStyle);
    }

    // documentation inherited
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        memberId = ins.readInt();
        media = (ins.readObject() as MediaData);
        chatStyle = ins.readShort();
        chatPopStyle = ins.readShort();
    }
}
}
