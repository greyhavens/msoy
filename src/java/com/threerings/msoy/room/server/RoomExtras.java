//
// $Id: MsoySceneRepository.java 13650 2008-12-04 18:25:53Z zell $

package com.threerings.msoy.room.server;

import java.util.List;

import com.threerings.msoy.room.server.persist.MemoryRecord;

/**
 * A class to hold additional scene data resolved in MsoySceneRepository.
 */
public class RoomExtras
{
    /** The startup memory records for the furni in this room. */
    public List<MemoryRecord> memories;
}
