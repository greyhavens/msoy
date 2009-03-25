package com.threerings.msoy.survey.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Records a member's response to a single survey question.
 */
@Entity
public class SurveyResponseRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<SurveyResponseRecord> _R = SurveyResponseRecord.class;
    public static final ColumnExp SUVERY_ID = colexp(_R, "suveryId");
    public static final ColumnExp MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp QUESTION_INDEX = colexp(_R, "questionIndex");
    public static final ColumnExp RESPONSE = colexp(_R, "response");
    // AUTO-GENERATED: FIELDS END

    /** The id of the survey this response is for. */
    @Id public int suveryId;

    /** The id of the member who responded. */
    @Id public int memberId;

    /** The index of the question responded to. */
    @Id public int questionIndex;

    /** The member's entered response to the question. The format depends on the type of
     * question. */
    public String response;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link SurveyResponseRecord}
     * with the supplied key values.
     */
    public static Key<SurveyResponseRecord> getKey (int suveryId, int memberId, int questionIndex)
    {
        return new Key<SurveyResponseRecord>(
                SurveyResponseRecord.class,
                new ColumnExp[] { SUVERY_ID, MEMBER_ID, QUESTION_INDEX },
                new Comparable[] { suveryId, memberId, questionIndex });
    }
    // AUTO-GENERATED: METHODS END
}
