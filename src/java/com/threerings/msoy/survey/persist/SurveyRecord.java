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
    public static final ColumnExp<Integer> SURVEY_ID = colexp(_R, "surveyId");
    public static final ColumnExp<String> NAME = colexp(_R, "name");
    public static final ColumnExp<Date> START = colexp(_R, "start");
    public static final ColumnExp<Date> FINISH = colexp(_R, "finish");
    public static final ColumnExp<Boolean> ENABLED = colexp(_R, "enabled");
    public static final ColumnExp<Integer> MAX_SUBMISSIONS = colexp(_R, "maxSubmissions");
    public static final ColumnExp<Integer> COIN_AWARD = colexp(_R, "coinAward");
    public static final ColumnExp<String> LINKED_PROMO_ID = colexp(_R, "linkedPromoId");
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

    /** After this number of submissions, the survey automatically disables. */
    public int maxSubmissions;

    /** Number of coins awarded to users that submit this survey. */
    public int coinAward;

    /** If non-null, the survey will automatically update some fields of a promotion when saved,
     * and delete the promotion when the survey is disabled. */
    @Column(defaultValue="''")
    public String linkedPromoId;

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 4;

    /**
     * Converts to a runtime record.
     */
    public SurveyMetaData toSurvey ()
    {
        SurveyMetaData s = new SurveyMetaData();
        s.surveyId = surveyId;
        s.name = name;
        s.enabled = enabled;
        s.startDate = start == null ? null : new java.util.Date(start.getTime());
        s.finishDate = finish == null ? null : new java.util.Date(finish.getTime());
        s.maxSubmissions = maxSubmissions;
        s.coinAward = coinAward;
        s.linkedPromoId = linkedPromoId;
        return s;
    }

    /**
     * Converts from a runtime record.
     */
    public void fromSurvey (SurveyMetaData survey)
    {
        name = survey.name;
        surveyId = survey.surveyId;
        enabled = survey.enabled;
        start = survey.startDate == null ? null : new Date(survey.startDate.getTime());
        finish = survey.finishDate == null ? null : new Date(survey.finishDate.getTime());
        maxSubmissions = survey.maxSubmissions;
        coinAward = survey.coinAward;
        linkedPromoId = survey.linkedPromoId;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link SurveyRecord}
     * with the supplied key values.
     */
    public static Key<SurveyRecord> getKey (int surveyId)
    {
        return newKey(_R, surveyId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(SURVEY_ID); }
    // AUTO-GENERATED: METHODS END
}
