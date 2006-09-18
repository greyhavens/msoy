//
// $Id$

package com.threerings.msoy.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.msoy.item.web.MediaDesc;

public class MemberInfo extends OccupantInfo
{
    /** The media that represents our avatar. */
    public var media :MediaDesc;

    /** The style of chat bubble to use. */
    public var chatStyle :int;

    /** The style with which the chat bubble pops up. */
    public var chatPopStyle :int;

    /**
     * Get the member id for this user, or -1 if they're a guest.
     */
    public function getMemberId () :int
    {
        return (username as MemberName).getMemberId();
    }

    // documentation inherited
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeObject(media);
        out.writeShort(chatStyle);
        out.writeShort(chatPopStyle);
    }

    // documentation inherited
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        media = (ins.readObject() as MediaDesc);
        chatStyle = ins.readShort();
        chatPopStyle = ins.readShort();
    }
}
}
