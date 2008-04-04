//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

public class EmailContact
    implements IsSerializable
{
    /** The contact name. */
    public String name;

    /** The contact email. */
    public String email;
}
