//
// $Id$

package com.threerings.msoy.game.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.samskivert.util.ByteEnum;
import com.threerings.msoy.data.all.MediaDesc;

/**
 * A thumbnail used by a game, currently just for facebook feed posts.
 */
public class GameThumbnail
    implements IsSerializable
{
    /** The types of thumbnails. */
    public enum Type
        implements ByteEnum
    {
        GAME_STORY(0, 1), TROPHY(1, 2), CHALLENGE(2, 3), LEVELUP(3, 3);

        /** Number of thumbnails required for this type (per variant). */
        public int count;

        public byte toByte ()
        {
            return _value;
        }

        Type (int value, int count) {
            _value = (byte)value;
            this.count = count;
        };
        protected byte _value;
    }

    /** The type of thumbnail. */
    public Type type;

    /** The variant. Only thumbnails with the same variant may be used together. */
    public String variant;

    /** The position. When using multiple thumbnails at once, they will be ordered by pos. */
    public byte pos;

    /** The image media for the thumbnail. */
    public MediaDesc media;

    /**
     * For deserializing.
     */
    public GameThumbnail ()
    {
    }

    /**
     * Creates a new thumbnail with the given media.
     */
    public GameThumbnail (MediaDesc media, Type type, String variant, byte pos)
    {
        this.media = media;
        this.type = type;
        this.variant = variant;
        this.pos = pos;
    }
}
