//
// $Id$

package com.threerings.msoy.survey.persist;

import java.sql.Date;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.GenerationType;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;
import com.threerings.msoy.survey.gwt.SurveyMetaData;

/**
 * Top-level metadata for a survey entered into the survey system.
 * TODO: add fields for limiting who we show the survey to (or maybe a separate SurveyConditions
 * table)
 */
@Entity
public class SurveyRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<SurveyRecord> _R = SurveyRecord.class;
    public static final ColumnExp SURVEY_ID = colexp(_R, "surveyId");
    public static final ColumnExp NAME = colexp(_R, "name");
    public static final ColumnExp START = colexp(_R, "start");
    public static final ColumnExp FINISH = colexp(_R, "finish");
    public static final ColumnExp ENABLED = colexp(_R, "enabled");
    // AUTO-GENERATED: FIELDS END

    /** Unique integer key of this survey. */
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int surveyId;

    /** Name for this survey. */
    public String name;

    /** The date this survey started or will start. */
    @Column(nullable=true)
    public Date start;

    /** The date this survey finished or will finish. */
    @Column(nullable=true)
    public Date finish;

    /** Whether this survey is currently enabled. */
    public boolean enabled;

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /**
     * Converts to a runtime record.
     */
    public SurveyMetaData toSurvey ()
    {
        SurveyMetaData s = new SurveyMetaData();
        s.surveyId = surveyId;
        s.name = name;
        return s;
    }

    /**
     * Converts from a runtime record.
     */
    public void fromSurvey (SurveyMetaData survey)
    {
        name = survey.name;
        surveyId = survey.surveyId;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link SurveyRecord}
     * with the supplied key values.
     */
    public static Key<SurveyRecord> getKey (int surveyId)
    {
        return new Key<SurveyRecord>(
                SurveyRecord.class,
                new ColumnExp[] { SURVEY_ID },
                new Comparable[] { surveyId });
    }
    // AUTO-GENERATED: METHODS END
}
