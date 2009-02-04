//
// $Id$

package com.threerings.msoy.data.all {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.util.Comparable;
import com.threerings.util.Hashable;
import com.threerings.util.Name;

/**
 * Contains a group name and group id in one handy object.
 */
public class GroupName extends Name
    implements Comparable, Hashable
{
    /** A sort function for sorting Names by their display portion, case insensitively.  */
    public static const BY_DISPLAY_NAME :Function = function (n1 :Name, n2 :Name) :int {
        var val :int = n1.toString().toLowerCase().localeCompare(n2.toString().toLowerCase());
        // massage the value into something that a Sort can handle
        return (val > 0) ? 1 : ((val == 0) ? 0 : -1);
    };

    /** The maximum length of a group name */
    public static const LENGTH_MAX :int = 24;

    /** The minimum length of a group name */
    public static const LENGTH_MIN :int = 3;

    /**
     * Creates a group name that can be used as a key for a DSet lookup or whereever else one might
     * need to use a {@link GroupName} instance as a key but do not have the (unneeded) group name.
     */
    public static function makeKey (groupId :int) :GroupName
    {
        return new GroupName(null, groupId);
    }

    public function GroupName (name :String = null, groupId :int = 0)
    {
        super(name);
        _groupId = groupId;
    }

    /**
     * Returns the id of this group.
     */
    public function getGroupId () :int
    {
        return _groupId;
    }

    // from Hashable (by way of Name)
    override public function hashCode () :int
    {
        return _groupId;
    }

    // from Comparable (by way of Name)
    override public function compareTo (other :Object) :int
    {
        var that :GroupName = (other as GroupName);
        return this._groupId - that._groupId;
    }

    // from Equalable (by way of Hashable by way of Name)
    override public function equals (other :Object) :Boolean
    {
        return (other is GroupName) && ((other as GroupName)._groupId == _groupId);
    }

    // from Streamable (by way of Name)
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        _groupId = ins.readInt();
    }

    // from Streamable (by way of Name)
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeInt(_groupId);
    }

    /** The group's id. */
    protected var _groupId :int;
}
}
