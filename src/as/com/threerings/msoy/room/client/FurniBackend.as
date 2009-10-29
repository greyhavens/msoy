//
// $Id$

package com.threerings.msoy.room.client {

import com.threerings.msoy.client.DeploymentConfig;

public class FurniBackend extends EntityBackend
{
    override protected function populateControlProperties (o :Object) :void
    {
        super.populateControlProperties(o);

        o["showPage_v1"] = showPage_v1;
    }

    protected function showPage_v1 (token :String) :Boolean
    {
        // handleViewUrl will do the "right thing"
        _ctx.getMsoyController().handleViewUrl(DeploymentConfig.serverURL + "#" + token);
        return true;
    }
}
}
