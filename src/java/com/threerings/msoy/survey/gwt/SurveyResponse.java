package com.threerings.msoy.survey.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

public class SurveyResponse
    implements IsSerializable
{
    public int questionIndex;
    public String response;
}
