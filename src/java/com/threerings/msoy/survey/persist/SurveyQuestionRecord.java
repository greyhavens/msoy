//
// $Id$

package com.threerings.msoy.survey.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.util.StringUtil;
import com.threerings.msoy.survey.gwt.SurveyQuestion;

/**
 * A single question that appears on a survey.
 */
public class SurveyQuestionRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<SurveyQuestionRecord> _R = SurveyQuestionRecord.class;
    public static final ColumnExp SURVEY_ID = colexp(_R, "surveyId");
    public static final ColumnExp QUESTION_INDEX = colexp(_R, "questionIndex");
    public static final ColumnExp QUESTION_TYPE = colexp(_R, "questionType");
    public static final ColumnExp TEXT = colexp(_R, "text");
    public static final ColumnExp DESCRIPTOR = colexp(_R, "descriptor");
    // AUTO-GENERATED: FIELDS END

    /** The id of the survey we belong to. */
    @Id public int surveyId;

    /** The order of this question in the containing survey. */
    @Id public int questionIndex;

    /** The type of question. */
    public SurveyQuestion.Type questionType;

    /** The text of the question. */
    public String text;

    /** The format of the expected reply, if applicable. */
    @Column(nullable=true) public String descriptor;

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /**
     * Converts to a runtime record.
     */
    public SurveyQuestion toSurveyQuestion ()
    {
        SurveyQuestion sq = new SurveyQuestion();
        sq.type = questionType;
        sq.text = text;
        switch (questionType) {
        case EXCLUSIVE_CHOICE:
        case SUBSET_CHOICE:
            sq.choices = descriptor.split(",");
            break;
        case RATING:
            sq.maxValue = Integer.parseInt(descriptor);
            break;
        }
        return sq;
    }

    /**
     * Converts from a runtime record. NOTE: the caller must fill in the survey id field when using
     * this method to store a user-defined survey question.
     */
    public void fromSurveyQuestion (SurveyQuestion question)
    {
        questionType = question.type;
        text = question.text;
        switch (questionType) {
        case EXCLUSIVE_CHOICE:
        case SUBSET_CHOICE:
            descriptor = StringUtil.join(question.choices, ",");
            break;
        case RATING:
            descriptor = String.valueOf(question.maxValue);
            break;
        default:
            descriptor = null;
            break;
        }
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link SurveyQuestionRecord}
     * with the supplied key values.
     */
    public static Key<SurveyQuestionRecord> getKey (int surveyId, int questionIndex)
    {
        return new Key<SurveyQuestionRecord>(
                SurveyQuestionRecord.class,
                new ColumnExp[] { SURVEY_ID, QUESTION_INDEX },
                new Comparable[] { surveyId, questionIndex });
    }
    // AUTO-GENERATED: METHODS END
}
