//
// $Id$

package com.threerings.msoy.facebook.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Describes the parameters necessary for sending Facebook usage data messages to Kontagent, for
 * use by the client application editor.
 */
public class KontagentInfo
    implements IsSerializable
{
    /** The length of a Kontagent api key. */
    public static final int KEY_LENGTH = 32;

    /** The length of a Kontagent api secrect. */
    public static final int SECRET_LENGTH = 32;

    /** The key used to access the message folder. */
    public String apiKey;

    /** The secret used to hash message parameters. */
    public String apiSecret;

    /**
     * Creates a new Kontagent info for deserializing.
     */
    public KontagentInfo ()
    {
    }

    /**
     * Creates a new Kontagent info with the given key and secret.
     */
    public KontagentInfo (String apiKey, String apiSecret)
    {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }
}
