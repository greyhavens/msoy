//
// $Id$

package com.threerings.msoy.web.data;

import java.util.List;

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

    /**
     * This members friend list at the time of logon.
     *
     * @gwt.typeArgs <com.threerings.msoy.data.all.FriendEntry>
     */
    public List friends;

    /** The hostname of the game server to which this client should connect. */
    public String gameServerHost;

    /** The port on which this client should connect to the game server. */
    public int gameServerPort;
}
