//
// $Id

package com.threerings.msoy.world.data {

import com.threerings.io.ObjectInputStream;

import com.threerings.msoy.data.MemberInfo;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.item.web.MediaDesc;

import com.threerings.msoy.game.data.GameSummary;

/**
 * Contains extra information for a member occupant when they are in the virtual world.
 */
public class WorldMemberInfo extends MemberInfo
    implements WorldOccupantInfo
{
    /** The current state of the member's avatar. */
    public var state :String;

    /** The game summary info for which to display a game icon on this member's avatar */
    public var currentGame :GameSummary;

//    /** The style of chat bubble to use. */
//    public var chatStyle :int;
//
//    /** The style with which the chat bubble pops up. */
//    public var chatPopStyle :int;

    // from interface WorldOccupantInfo
    public function getMedia () :MediaDesc
    {
        return _media;
    }

    // from interface WorldOccupantInfo
    public function getState () :String
    {
        return state;
    }

    // documentation inherited
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
//        chatStyle = ins.readShort();
//        chatPopStyle = ins.readShort();
        state = (ins.readField(String) as String);
        currentGame = (ins.readObject() as GameSummary);
        _media = (ins.readObject() as MediaDesc);
    }

    /** The media that represents this occupant. */
    protected var _media :MediaDesc;
}
}
