//
// $Id$

package com.threerings.msoy.mchooser;

/**
 * A class that provides configuration information to our media chooser.
 */
public class Config
{
    /** Indicates that we're choosing an image. */
    public static final String IMAGE = "image";

    /** Indicates that we're choosing audio. */
    public static final String AUDIO = "image";

    /** The URL of the server of which we make servlet requests. */
    public final String serverURL;

    /** The id of the media we're choosing. */
    public final String mediaId;

    /** The choosing member's authentication information. */
    public final String authToken;

    /** The type of media we're choosing. */
    public final String type;

    public Config (String serverURL, String mediaId, String authToken, String type)
    {
        this.serverURL = serverURL;
        this.mediaId = mediaId;
        this.authToken = authToken;

        // we'd use an enum here but it results in PITA with Proguard and Retroweaver
        if (type.equals(IMAGE)) {
            this.type = IMAGE;
        } else if (type.equals(AUDIO)) {
            this.type = AUDIO;
        } else {
            throw new IllegalArgumentException("Unknown media type: " + type);
        }
    }
}
