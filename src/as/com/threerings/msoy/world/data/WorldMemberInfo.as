//
// $Id$

package com.threerings.msoy.world.data {

import com.threerings.io.ObjectInputStream;

import com.threerings.msoy.data.MemberInfo;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.MediaDesc;

import com.threerings.msoy.game.data.GameSummary;

/**
 * Contains extra information for a member occupant when they are in the virtual world.
 */
public class WorldMemberInfo extends MemberInfo
    implements WorldOccupantInfo
{
    /** The current state of the member's avatar. */
    public var state :String;

    /** The game summary for the user's currently pending game. */
    public var game :GameSummary;

    /** True of this member is only viewing the scene and should not be rendered in it. */
    public var viewOnly :Boolean;

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
    public function getScale () :Number
    {
        return _scale;
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
        game = (ins.readObject() as GameSummary);
        viewOnly = ins.readBoolean();
        _media = (ins.readObject() as MediaDesc);
        _scale = ins.readFloat();
    }

    /**
     * Update the scale.
     *
     * This is sorta a hack. This method currently only exists on the actionscript side.
     * We update the scale immediately when someone is futzing with the scale in the
     * avatarviewer.
     */
    public function setScale (newScale :Number) :void
    {
        _scale = newScale;
    }

    /** The media that represents this occupant. */
    protected var _media :MediaDesc;

    /** The scaling factor of the media. */
    protected var _scale :Number;
}
}
