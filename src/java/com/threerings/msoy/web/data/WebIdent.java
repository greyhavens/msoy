//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains only the information needed to authenticate a request with the server.
 */
public class WebIdent implements IsSerializable
{
    /** Our member identifier. */
    public int memberId;

    /** Our session token. */
    public String token;

    /** Used when unserializing. */
    public WebIdent ()
    {
    }

    /** Creates an identifier with the supplied information. */
    public WebIdent (int memberId, String token)
    {
        this.memberId = memberId;
        this.token = token;
    }

    /** Generates a string representation of this instance. */
    public String toString ()
    {
        return memberId + ":" + token;
    }
}
