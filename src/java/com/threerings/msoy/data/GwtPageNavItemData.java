//
// $Id$

package com.threerings.msoy.data;

import com.threerings.msoy.data.all.NavItemData;

@com.threerings.util.ActionScript(omit=true)
public class GwtPageNavItemData
    implements NavItemData
{
    public GwtPageNavItemData (String name, String page, String args)
    {
        _name = name;
        _page = page;
        _args = args;
    }

    protected String _name;
    protected String _page;
    protected String _args;
}
