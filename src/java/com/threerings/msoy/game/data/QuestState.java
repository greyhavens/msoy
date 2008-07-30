//
// $Id$

package com.threerings.msoy.game.data;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.threerings.io.SimpleStreamableObject;
import com.threerings.presents.dobj.DSet;

/**
 * Represents an active quest for a player in the player's active world game.
 */
public class QuestState extends SimpleStreamableObject
    implements IsSerializable, DSet.Entry
{
    public static final int STEP_COMPLETED = -2;
    public static final int STEP_VIRGIN = -1;
    public static final int STEP_FIRST = 1;

    public String questId;

    public int step;

    public String status;

    public int sceneId;

    public QuestState ()
    {
    }

    public QuestState (String questId, int stepId, String status, int sceneId)
    {
        this.questId = questId;
        this.step = stepId;
        this.status = status;
        this.sceneId = sceneId;
    }

    // from DSet.Entry
    public Comparable<?> getKey ()
    {
        return questId;
    }
}
