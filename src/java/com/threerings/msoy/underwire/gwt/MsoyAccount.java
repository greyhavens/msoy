//
// $Id$

package com.threerings.msoy.underwire.gwt;

import com.threerings.underwire.web.data.Account;

public class MsoyAccount extends Account
{
    public enum SocialStatus
    {
        NORMAL, GREETER, TROUBLEMAKER;
    }

    /** The social standing of this user. */
    public SocialStatus status;
}
