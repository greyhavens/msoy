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
    public static final String AUDIO = "audio";

    /** Indicates that we're choosing video. */
    public static final String VIDEO = "video";

    /** Indicates that we're choosing something that can be visualized in Flash. */
    public static final String FLASH = "flash";

    /** Indicates that we're choosing code (swf, jar or zip). */
    public static final String CODE = "code";

    /** Indicates that we're choosing any old thang. */
    public static final String ANY = "any";

    /** A list of all accepted media types. */
    public static final String[] TYPES = { IMAGE, AUDIO, VIDEO, FLASH, CODE, ANY };

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
        this.type = checkType(type);
    }

    protected static String checkType (String type)
    {
        for (String ctype : TYPES) {
            if (type.equals(ctype)) {
                return ctype;
            }
        }
        throw new IllegalArgumentException("Unknown media type: " + type);
    }
}
