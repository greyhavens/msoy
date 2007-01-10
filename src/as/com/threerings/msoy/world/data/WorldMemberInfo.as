//
// $Id

package com.threerings.msoy.world.data {

import com.threerings.io.ObjectInputStream;

import com.threerings.msoy.data.MemberInfo;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.item.web.MediaDesc;

public class WorldMemberInfo extends MemberInfo
{
    /** The media that represents our avatar. */
    public var avatar :MediaDesc;

    /** The style of chat bubble to use. */
    public var chatStyle :int;

    /** The style with which the chat bubble pops up. */
    public var chatPopStyle :int;

    /**
     * Returns an item identifier for this occupant's avatar item. If this is a stock avatar, this
     * will be an OCCUPANT identifier which will use their body oid instead of their avatar item id
     * as the item id.
     */
    public function getAvatarIdent () :ItemIdent
    {
        return avatarId == 0 ? new ItemIdent(Item.OCCUPANT, bodyOid) :
            new ItemIdent(Item.AVATAR, avatarId);
    }

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
