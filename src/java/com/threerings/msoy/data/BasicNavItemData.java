//
// $Id$

package com.threerings.msoy.data;

import com.threerings.msoy.data.all.NavItemData;

/**
 * Standard implementation of {@link NavItemData} that uses an integer ID to identify the
 * action the user should take and a string Name that will be displayed directly to the user.
 *
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class BasicNavItemData
    implements NavItemData
{

    public BasicNavItemData ()
    {
        // For serialization
    }

    public BasicNavItemData (int id, String name)
    {
        _id = id;
        _name = name;
    }

    public int getId ()
    {
        return _id;
    }

    public String getName ()
    {
        return _name;
    }

    protected int _id;
    protected String _name;
}
