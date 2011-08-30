//
// $Id$

package com.threerings.msoy.chat.client {

import com.threerings.crowd.chat.client.CurseFilter;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.Prefs;

public class CurseFilter extends com.threerings.crowd.chat.client.CurseFilter
{
    public function CurseFilter (ctx :MsoyContext)
    {
        super(Msgs.CHAT.get("x.cursewords"), Msgs.CHAT.get("x.stopwords"));
    }

    override public function getFilterMode () :int
    {
        return Prefs.getChatFilterLevel();
    }
}
}
