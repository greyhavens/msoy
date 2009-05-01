//
// $Id$

package com.threerings.msoy.survey.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * A response to a single question on a survey.
 */
public class SurveyResponse
    implements IsSerializable
{
    /**
     * The maximum response string length for free form questions. NOTE: if this is increased, a
     * column retype migration must be done on <code>SurveyResponseRecord</code>.
     */
    public static final int MAX_LENGTH = 4096;

    /** The index of the question this is a response to. */
    public int questionIndex;

    /**
     * The encoded response to the question. For free form type questions, this is just what the
     * user typed in. For all others, one or more selections from
     * {@link SurveyQuestion#getEncodedChoices()}.
     * */
    public String response;
}
