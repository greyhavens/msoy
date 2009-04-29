//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.util.Name;

import com.threerings.parlor.game.data.UserIdentifier;

import com.threerings.msoy.data.all.MemberName;

public class MsoyUserIdentifier
    implements UserIdentifier
{
    public function getUserId (name :Name) :int
    {
        return MemberName(name).getMemberId();
    }
}
}
