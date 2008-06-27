//
// $Id$

package com.threerings.msoy.world.client {

import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;

import com.threerings.msoy.world.data.MemberInfo;

/**
 * A helper class for RoomStudioView.
 */
public class StudioMemberInfo extends MemberInfo
{
    public function StudioMemberInfo (ctx :StudioContext = null, avatarUrl :String = null)
    {
        if (ctx != null) {
            username = ctx.getMyName();
            _ident = new ItemIdent(Item.OCCUPANT, RoomStudioView.MEMBER_ID);
            _scale = 1;
            _media = new StudioMediaDesc(avatarUrl);
        }
    }
}
}
