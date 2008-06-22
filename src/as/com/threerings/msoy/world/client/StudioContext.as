//
// $Id$

package com.threerings.msoy.world.client {

import com.threerings.whirled.client.SceneDirector;

import com.threerings.msoy.data.all.MemberName;

public class StudioContext extends WorldContext
{
    public function StudioContext (client :StudioClient)
    {
        super(client);
        _name = new MemberName("It's-a me!", 0);
    }

    override public function getMyName () :MemberName
    {
        return _name;
    }

    override protected function createWorldDirectors () :void
    {
        // suppress super
    }

    /** Our name in the studio. */
    protected var _name :MemberName;
}
}
