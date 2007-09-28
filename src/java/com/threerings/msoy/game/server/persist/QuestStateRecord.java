//
// $Id$

package com.threerings.msoy.game.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.expression.ColumnExp;
import com.samskivert.util.StringUtil;

/**
 * Contains the details of a group.
 */
public class QuestStateRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #memberId} field. */
    public static final String MEMBER_ID = "memberId";

    /** The qualified column identifier for the {@link #memberId} field. */
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(QuestStateRecord.class, MEMBER_ID);

    /** The column identifier for the {@link #gameId} field. */
    public static final String AVRG_ID = "avrgId";

    /** The qualified column identifier for the {@link #gameId} field. */
    public static final ColumnExp AVRG_ID_C =
        new ColumnExp(QuestStateRecord.class, AVRG_ID);

    /** The column identifier for the {@link #questId} field. */
    public static final String QUEST_ID = "questId";

    /** The qualified column identifier for the {@link #questId} field. */
    public static final ColumnExp QUEST_ID_C =
        new ColumnExp(QuestStateRecord.class, QUEST_ID);

    /** The column identifier for the {@link #step} field. */
    public static final String STEP = "step";

    /** The qualified column identifier for the {@link #step} field. */
    public static final ColumnExp STEP_C =
        new ColumnExp(QuestStateRecord.class, STEP);
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    @Id
    public int memberId;

    @Id
    public int gameId;

    @Id
    public String questId;

    public int step;

    @Column(length=32)
    public String status;

    public int sceneId;

    public QuestStateRecord ()
    {
    }

    public QuestStateRecord (int gameId, String questId, int step, String status, int sceneId)
    {
        this.gameId = gameId;
        this.questId = questId;
        this.step = step;
        this.status = status;
        this.sceneId = sceneId;
    }

    /**
     * Generates a string representation of this instance.
     */
    @Override
    public String toString ()
    {
        StringBuilder buf = new StringBuilder("[");
        StringUtil.fieldsToString(buf, this);
        return buf.append("]").toString();
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #QuestStateRecord}
     * with the supplied key values.
     */
    public static Key<QuestStateRecord> getKey (int memberId, int avrgId, int questId)
    {
        return new Key<QuestStateRecord>(
                QuestStateRecord.class,
                new String[] { MEMBER_ID, AVRG_ID, QUEST_ID },
                new Comparable[] { memberId, avrgId, questId });
    }
    // AUTO-GENERATED: METHODS END
}
