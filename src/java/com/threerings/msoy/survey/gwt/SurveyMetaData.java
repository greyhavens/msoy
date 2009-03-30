//
// $Id$

package com.threerings.msoy.survey.gwt;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Meta-data for a survey.
 */
public class SurveyMetaData
    implements IsSerializable
{
    /** The id for unique identifying this survey. */
    public int surveyId;

    /** The name of this survey. */
    public String name;

    /** Whether this survey is enabled. */
    public boolean enabled;

    /** First day of this server (optional, may be null). */
    public Date startDate;

    /** Last day of this survey (optional, may be null). */
    public Date finishDate;

    /** After this number of submissions, the survey automatically disables. */
    public int maxSubmissions;

    /** Number of coins awarded for submitting this survey. */
    public int coinAward;
}
