//
// $Id

package com.threerings.msoy.world.data {

import com.threerings.io.ObjectInputStream;

import com.threerings.msoy.data.MemberInfo;

import com.threerings.msoy.item.web.MediaDesc;

public class WorldMemberInfo extends MemberInfo
{
    /** The media that represents our avatar. */
    public var avatar :MediaDesc;

    /** The style of chat bubble to use. */
    public var chatStyle :int;

    /** The style with which the chat bubble pops up. */
    public var chatPopStyle :int;

    // documentation inherited
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        avatar = (ins.readObject() as MediaDesc);
        chatStyle = ins.readShort();
        chatPopStyle = ins.readShort();
    }
}
}
