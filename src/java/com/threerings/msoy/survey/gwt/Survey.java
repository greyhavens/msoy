package com.threerings.msoy.survey.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * A survey for end-user consumption.
 */
public class Survey
    implements IsSerializable
{
    /** The name of the survey. */
    public String name;

    /** The questions. */
    public SurveyQuestion[] questions;
}
