//
// $Id$

package com.threerings.msoy.web.data;

/**
 * Contains information on an awarded trophy.
 */
public class TrophyAwardPayload extends MailPayload
{
    /** The id of the game for which the trophy was awarded. */
    public int gameId;

    /** The name of the game for which the trophy was awarded. */
    public String gameName;

    /** The name of the trophy that was awarded. */
    public String trophyName;

    /** The hash of the image associated with the awarded trophy. */
    public byte[] trophyMedia;

    /** The mime-type of the image associated with the awarded trophy. */
    public byte trophyMimeType;

    /**
     * An empty constructor for deserialization.
     */
    public TrophyAwardPayload ()
    {
    }

    /**
     * Creates a new payload with the supplied configuration.
     */
    public TrophyAwardPayload (int gameId, String gameName, String trophyName,
                               byte[] trophyMedia, byte trophyMimeType)
    {
        this.gameId = gameId;
        this.gameName = gameName;
        this.trophyName = trophyName;
        this.trophyMedia = trophyMedia;
        this.trophyMimeType = trophyMimeType;
    }

    // @Override // from MailPayload
    public int getType ()
    {
        return MailPayload.TYPE_TROPHY_AWARD;
    }
}
