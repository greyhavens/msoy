//
// $Id$

package com.threerings.msoy.mail.gwt;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Contains information on an awarded trophy.
 */
public class GameAwardPayload extends MailPayload
{
    /** A type of game award. */
    public static final byte TROPHY = 0;

    /** A type of game award. */
    public static final byte PRIZE = 1;

    /** The id of the game which did the awarding. */
    public int gameId;

    /** The name of the game which did the awarding. */
    public String gameName;

    /** The type of award, e.g. {@link #TROPHY}. */
    public byte awardType;

    /** The name of the award. */
    public String awardName;

    /** The hash of the image associated with the award. */
    public byte[] awardMediaHash;

    /** The mime-type of the image associated with the award. */
    public byte awardMimeType;

    /**
     * An empty constructor for deserialization.
     */
    public GameAwardPayload ()
    {
    }

    /**
     * Creates a new payload with the supplied configuration.
     */
    public GameAwardPayload (int gameId, String gameName, byte awardType, String awardName,
                             MediaDesc awardMedia)
    {
        this.gameId = gameId;
        this.gameName = gameName;
        this.awardName = awardName;
        this.awardMediaHash = awardMedia.hash;
        this.awardMimeType = awardMedia.mimeType;
    }

    @Override // from MailPayload
    public int getType ()
    {
        return MailPayload.TYPE_GAME_AWARD;
    }

    /**
     * Returns the media descriptor for our award.
     */
    public MediaDesc getAwardMedia ()
    {
        return new MediaDesc(awardMediaHash, awardMimeType);
    }
}
