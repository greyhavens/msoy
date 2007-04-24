//
// $Id$

package com.threerings.msoy.client {

import com.threerings.crowd.chat.client.CurseFilter;

public class CurseFilter extends com.threerings.crowd.chat.client.CurseFilter
{
    public function CurseFilter (ctx :BaseContext)
    {
        super(Msgs.CHAT.get("x.cursewords"), Msgs.CHAT.get("x.stopwords"));
    }

    override public function getFilterMode () :int
    {
        return Prefs.getChatFilterLevel();
    }
}
}
