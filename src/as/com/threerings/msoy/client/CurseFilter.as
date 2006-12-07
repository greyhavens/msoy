//
// $Id$

package com.threerings.msoy.client {

import com.threerings.crowd.chat.client.CurseFilter;

public class CurseFilter extends com.threerings.crowd.chat.client.CurseFilter
{
    public function CurseFilter (ctx :MsoyContext)
    {
        super(Msgs.GENERAL.get("x.cursewords"), Msgs.GENERAL.get("x.stopwords"));
    }

    override public function getFilterMode () :int
    {
        return VERNACULAR;
    }
}
}
