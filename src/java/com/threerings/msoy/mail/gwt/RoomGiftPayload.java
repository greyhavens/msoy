//
// $Id$

package com.threerings.msoy.mail.gwt;

import com.threerings.msoy.data.all.HashMediaDesc;

/**
 * Contains information on a room gifted from one player to another.
 */
public class RoomGiftPayload extends MailPayload
{
    /** The sceneId being gifted. */
    public int sceneId;

    /** The name of the room. */
    public String name;

    /** This room's canonical thumbnail (may be null). */
    public HashMediaDesc thumbnail;

    /**
     * An empty constructor for deserialization.
     */
    public RoomGiftPayload ()
    {
    }

    /**
     * Create a new {@link RoomGiftPayload} with the supplied configuration.
     */
    public RoomGiftPayload (int sceneId, String name, HashMediaDesc thumbnail)
    {
        this.sceneId = sceneId;
        this.name = name;
        this.thumbnail = thumbnail;
    }

    @Override
    public int getType ()
    {
        return MailPayload.TYPE_ROOM_GIFT;
    }
}
