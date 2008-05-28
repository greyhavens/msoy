//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains a snapshot of the user's data delivered when they validate their session.
 */
public class SessionData implements IsSerializable
{
    public WebCreds creds;

    /** This member's flow at the time of session start. */
    public int flow;

    /** This member's gold at the time of session start. */
    public int gold;

    /** This member's level at the time of session start. */
    public int level;

    /** This member's new mail message count at the time of session start. */
    public int newMailCount;
}
