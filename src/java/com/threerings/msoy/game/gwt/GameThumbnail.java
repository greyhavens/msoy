//
// $Id$

package com.threerings.msoy.game.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.threerings.msoy.data.all.MediaDesc;

/**
 * A thumbnail used by a game, currently just for facebook feed posts.
 */
public class GameThumbnail
    implements IsSerializable
{
    /** Maximum number of thumbnails allowed to be associated with a game. */
    public static final int MAX_COUNT = 3;

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
    public GameThumbnail (MediaDesc media)
    {
        this.media = media;
    }
}
