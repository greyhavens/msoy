//
// $Id$

package com.threerings.msoy.money.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains all of the information necessary to display an entry in the bling cash out list.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class CashOutEntry
    implements IsSerializable
{
    public /* final */ int memberId;
    public /* final */ String displayName;
    public /* final */ boolean charity;
    public /* final */ CashOutInfo cashOutInfo;
    public /* final */ String emailAddress;
    
    public CashOutEntry (int memberId, String displayName, CashOutInfo info, String emailAddress,
        boolean charity)
    {
        this.memberId = memberId;
        this.displayName = displayName;
        this.cashOutInfo = info;
        this.emailAddress = emailAddress;
        this.charity = charity;
    }
    
    /** For serialization purposes. */
    public CashOutEntry () { }
}
