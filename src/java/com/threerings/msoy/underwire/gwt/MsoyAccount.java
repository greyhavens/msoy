//
// $Id$

package com.threerings.msoy.underwire.gwt;

import com.threerings.underwire.web.data.Account;

public class MsoyAccount extends Account
{
    /** Whether this user is a greeter (user option or may be set by support). */
    public boolean greeter;
    
    /** whether this user is a troublemaker (flagged by support). */
    public boolean troublemaker;
}
