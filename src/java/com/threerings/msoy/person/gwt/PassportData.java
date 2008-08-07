//
// $Id$

package com.threerings.msoy.person.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

public class PassportData
    implements IsSerializable
{
    /** The account name for the stamps we're currently displaying **/
    public String stampOwner;
}
