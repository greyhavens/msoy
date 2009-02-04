//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.util.Name;

import com.threerings.crowd.data.TokenRing;

import com.whirled.game.client.PropertySpaceHelper;
import com.whirled.game.data.PropertySpaceMarshaller;
import com.whirled.game.data.PropertySpaceObject;
import com.whirled.game.data.WhirledPlayerObject;

import com.threerings.msoy.data.MsoyTokenRing;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.data.all.VizMemberName;

/**
 * Contains information on a player logged on to an MSOY Game server.
 */
public class PlayerObject extends WhirledPlayerObject
    implements PropertySpaceObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>memberName</code> field. */
    public static const MEMBER_NAME :String = "memberName";

    /** The field name of the <code>tokens</code> field. */
    public static const TOKENS :String = "tokens";

    /** The field name of the <code>photo</code> field. */
    public static const PHOTO :String = "photo";

    /** The field name of the <code>humanity</code> field. */
    public static const HUMANITY :String = "humanity";

    /** The field name of the <code>visitorInfo</code> field. */
    public static const VISITOR_INFO :String = "visitorInfo";
    // AUTO-GENERATED: FIELDS END

    /** The name and id information for this user. */
    public var memberName :VizMemberName;

    /** The tokens defining the access controls for this user. */
    public var tokens :MsoyTokenRing;

    /** Our current assessment of how likely to be human this member is, in [0, 255]. */
    public var humanity :int;

    /** Player's tracking information. */
    public var visitorInfo :VisitorInfo;

    /** Service for setting properties. */
    public var propertyService :PropertySpaceMarshaller;

    // from BodyObject
    override public function getTokens () :TokenRing
    {
        return tokens;
    }

    // from BodyObject
    override public function getVisibleName () :Name
    {
        return memberName;
    }

    /**
     * Returns this member's unique id.
     */
    public function getMemberId () :int
    {
        return memberName.getMemberId();
    }

    /**
     * Return true if this user is merely a guest.
     */
    public function isGuest () :Boolean
    {
        return memberName.isGuest();
    }

    /**
     * Get the media to use as our headshot.
     */
    public function getHeadShotMedia () :MediaDesc
    {
        return memberName.getPhoto();
    }

    /**
     * Return our assessment of how likely this member is to be human, in [0, 1].
     */
    public function getHumanity () :Number
    {
        return humanity / 255;
    }

    // from PropertySpaceObject
    public function getUserProps () :Object
    {
        return _props;
    }

    // from PropertySpaceObject
    public function getPropService () :PropertySpaceMarshaller
    {
        return propertyService;
    }

    // from BodyObject
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        // first read any regular bits
        readDefaultFields(ins);

        // then user properties
        PropertySpaceHelper.readProperties(this, ins);
    }

    /**
     * Reads the fields written by the default serializer for this instance.
     */
    protected function readDefaultFields (ins :ObjectInputStream) :void
    {
        memberName = VizMemberName(ins.readObject());
        tokens = MsoyTokenRing(ins.readObject());
        humanity = ins.readInt();
        visitorInfo = VisitorInfo(ins.readObject());
        propertyService = PropertySpaceMarshaller(ins.readObject());
    }

    /** The raw properties set by the game. */
    protected var _props :Object = new Object();
}
}
