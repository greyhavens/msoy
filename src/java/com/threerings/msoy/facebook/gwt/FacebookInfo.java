//
// $Id$

package com.threerings.msoy.facebook.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains information on a game's Facebook app.
 */
public class FacebookInfo implements IsSerializable
{
    /** The length of the Facebook API key. */
    public static final int KEY_LENGTH = 32;

    /** The length of the Facebook app secret. */
    public static final int SECRET_LENGTH = 32;

    /** The length of the Facebbok canvas name. */
    public static final int CANVAS_NAME_LENGTH = 40;

    /** The game for which we contain metadata, or 0 if this is for an application. */
    public int gameId;

    /** The app for which we contain metadata, or 0 if this is for a game. */
    public int appId;

    /** The Facebook API key for this game's Facebook app. */
    public String apiKey;

    /** The Facebook app secret for this game's Facebook app. */
    public String appSecret;

    /** The id of the application's facebook user (for profile page). */
    public long fbUid;

    /** If true Whirled won't display any chrome when in Facebook App mode. */
    public boolean chromeless;

    /** The Facebook canvas name. */
    public String canvasName;
}
