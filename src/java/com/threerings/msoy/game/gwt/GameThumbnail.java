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
    public static final int COUNT = 3;

    /** The types of thumbnails. */
    public enum Type
        implements ByteEnum
    {
        // TODO: get rid of the OBSOLETE type after a release and existing instances are removed
        OBSOLETE(0), TROPHY(1), CHALLENGE(2), LEVELUP(3, false);

        /** Whether this type of thumbnail can be overridden by a game. */
        public boolean gameOverridable;

        public byte toByte ()
        {
            return _value;
        }

        Type (int value) {
            this(value, true);
        };

        Type (int value, boolean gameOverridable) {
            _value = (byte)value;
            this.gameOverridable = gameOverridable;
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
