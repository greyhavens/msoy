//
// $Id$

package com.threerings.msoy.bureau.data;

import com.threerings.util.Name;

/**
 * Represents an authenticated window client.
 */
@com.threerings.util.ActionScript(omit=true)
public class WindowAuthName extends Name
{
    public WindowAuthName (String bureauId)
    {
        super(bureauId);
    }

    public WindowAuthName ()
    {
    }
}
