//
// $Id$

package com.threerings.msoy.world.client {

import com.threerings.msoy.data.all.MemberName;

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
            _media = new StudioMediaDesc(avatarUrl);
            _scale = 1;
        }
    }

    public function setState (state :String) :void
    {
        _state = state;
    }
}
}
