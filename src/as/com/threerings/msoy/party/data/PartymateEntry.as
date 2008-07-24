package com.threerings.msoy.party.data {

import com.threerings.msoy.data.all.PeerEntry;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.data.VizMemberName;

public class PartymateEntry
    implements PeerEntry
{
    public var name :VizMemberName;

    public function getName () :MemberName
    {
        return name;
    }

    public function getMemberId () :int
    {
        return name.getMemberId();
    }

    public function getPhoto () :MediaDesc
    {
        return name.getPhoto();
    }

    public function hashCode () :int
    {
        return getMemberId();
    }

    public function compareTo (other :Object) :int
    {
        var that :FriendEntry = (other as PartymateEntry);
        return MemberName.BY_DISPLAY_NAME(this.name, that.name);
    }
}

}
