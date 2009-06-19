//
// $Id$

package com.threerings.msoy.survey.persist;

import java.util.List;

import com.google.common.collect.Lists;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.survey.gwt.SurveyQuestion;

/**
 * A single question that appears on a survey.
 */
@Entity
public class SurveyQuestionRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<SurveyQuestionRecord> _R = SurveyQuestionRecord.class;
    public static final ColumnExp SURVEY_ID = colexp(_R, "surveyId");
    public static final ColumnExp QUESTION_INDEX = colexp(_R, "questionIndex");
    public static final ColumnExp QUESTION_TYPE = colexp(_R, "questionType");
    public static final ColumnExp TEXT = colexp(_R, "text");
    public static final ColumnExp DESCRIPTOR = colexp(_R, "descriptor");
    public static final ColumnExp OPTIONAL = colexp(_R, "optional");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 3;

    /** The id of the survey we belong to. */
    @Id public int surveyId;

    /** The order of this question in the containing survey. */
    @Id public int questionIndex;

    /** The type of question. */
    public SurveyQuestion.Type questionType;

    /** The text of the question. */
    public String text;

    /** The format of the expected reply, if applicable. */
    @Column(nullable=true, length=4096) public String descriptor;

    /** Whether this question is required for a submission. */
    @Column(defaultValue="false") public boolean optional;

    /**
     * Converts to a runtime record.
     */
    public SurveyQuestion toSurveyQuestion ()
    {
        SurveyQuestion sq = new SurveyQuestion();
        sq.type = questionType;
        sq.text = text;
        sq.optional = optional;
        switch (questionType) {
        case EXCLUSIVE_CHOICE:
        case SUBSET_CHOICE:
            sq.choices = decodeList(descriptor);
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
        optional = question.optional;
        switch (questionType) {
        case EXCLUSIVE_CHOICE:
        case SUBSET_CHOICE:
            descriptor = encodeList(question.choices);
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

    protected static String encodeList (String[] list)
    {
        StringBuilder desc = new StringBuilder();
        for (String str : list) {
            if (desc.length() > 0) {
                desc.append(",");
            }
            str = str.replace("\\", "\\\\");
            str = str.replace(",", "\\,");
            desc.append(str);
        }
        return desc.toString();
    }

    protected static String[] decodeList (String desc)
    {
        if (desc.length() == 0) {
            return new String[0];
        }

        // Mini state machine to decode a comma separated list where list items can have commas.
        // Item commas are escaped as "\," - backslashes as "\\".
        List<String> strings = Lists.newArrayList();
        StringBuilder current = new StringBuilder();
        int state = 0;
        for (int ii = 0, ll = desc.length(); ii < ll; ++ii) {
            char c = desc.charAt(ii);
            if (state == 1) {
                current.append(c);
                state = 0;

            } else if (c == '\\') {
                state = 1;

            } else if (c == ',') {
                strings.add(current.toString());
                current.setLength(0);

            } else {
                current.append(c);
            }
        }
        strings.add(current.toString());
        return strings.toArray(new String[strings.size()]);
    }
}
