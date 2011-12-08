//
// $Id$

package com.threerings.msoy.web.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

public class Embedding
    implements IsSerializable
{
    /** The name of the token argument used to communicate the embedding. */
    public static final String ARG_NAME = "emb";

    /**
     * Remove and return the embedding from the given arguments.
     */
    public static Embedding extract (Args args)
    {
        String value = args.extractParameter(ARG_NAME);
        if (value != null && value.equals("fb")) {
            // handle the old "fb" mode in case it crept into anyone's bookmarks
            // TODO: we may need to ditch this when application awareness is complete
            return new Embedding(ClientMode.FB_GAMES, 1);

        } else if (value != null) {
            for (ClientMode mode : ClientMode.values()) {
                if (mode != ClientMode.UNSPECIFIED && value.startsWith(mode.code)) {
                    String appId = value.substring(mode.code.length());
                    return new Embedding(mode, Integer.parseInt(appId));
                }
            }
        }
        return new Embedding(ClientMode.UNSPECIFIED, 1);
    }

    /**
     * Creates the embedding encoded in the string array.
     * @see #flatten()
     */
    public static Embedding unflatten (String[] strs)
    {
        return new Embedding(ClientMode.valueOf(strs[0]), Integer.parseInt(strs[1]));
    }

    /** The client mode for this embedding. */
    public ClientMode mode;

    /** The application of this embedding. */
    public int appId;

    /**
     * Creates a new embedding for the given mode and application.
     */
    public Embedding (ClientMode mode, int appId)
    {
        this.mode = mode;
        this.appId = appId;
    }

    public Embedding ()
    {
    }

    /**
     * Combine the client mode and application for inclusion in a token.
     */
    public Args compose ()
    {
        // NOTE we cannot use e.g. emb_r_43, so just concatenate
        return Args.compose(ARG_NAME, mode.code + appId);
    }

    /**
     * Encodes the embedding into a string array.
     * @see #unflatten()
     */
    public String[] flatten ()
    {
        return new String[] {mode.toString(), String.valueOf(appId)};
    }

    public String toString ()
    {
        return "(mode=" + mode + ", appId=" + appId + ")";
    }
}
