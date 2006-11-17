//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.io.ObjectInputStream;

import com.threerings.msoy.data.MemberInfo;

import com.threerings.msoy.item.web.MediaDesc;

public class GameMemberInfo extends MemberInfo
{
    /** The media of the user's headshot (part of their avatar). */
    public var headShot :MediaDesc

    // documentation inherited
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        headShot = (ins.readObject() as MediaDesc);
    }
}
}
