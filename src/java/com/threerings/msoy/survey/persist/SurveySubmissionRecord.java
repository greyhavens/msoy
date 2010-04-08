//
// $Id$

package com.threerings.msoy.survey.persist;

import java.sql.Date;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Records a user having done a survey.
 */
@Entity
public class SurveySubmissionRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<SurveySubmissionRecord> _R = SurveySubmissionRecord.class;
    public static final ColumnExp SURVEY_ID = colexp(_R, "surveyId");
    public static final ColumnExp MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp COMPLETED = colexp(_R, "completed");
    public static final ColumnExp NUM_QUESTIONS = colexp(_R, "numQuestions");
    // AUTO-GENERATED: FIELDS END

    /** Id of the survey filled in. */
    @Id public int surveyId;

    /** Id of the member who filled in the survey. */
    @Id public int memberId;

    /** Date the member filled in the survey. */
    public Date completed;

    /** Number of questions completed. */
    public int numQuestions;

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link SurveySubmissionRecord}
     * with the supplied key values.
     */
    public static Key<SurveySubmissionRecord> getKey (int surveyId, int memberId)
    {
        return newKey(_R, surveyId, memberId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(SURVEY_ID, MEMBER_ID); }
    // AUTO-GENERATED: METHODS END
}
