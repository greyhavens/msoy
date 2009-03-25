//
// $Id$

package com.threerings.msoy.survey.gwt;

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
}
