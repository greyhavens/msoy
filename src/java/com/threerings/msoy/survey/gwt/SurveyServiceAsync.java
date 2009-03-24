package com.threerings.msoy.survey.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

/** The asynchronous version of {@link SurveyService}. */
public interface SurveyServiceAsync
{
    /** The asynchronous version of {@link SurveyService#getAllSurveys}. */
    void getAllSurveys (AsyncCallback<List<Survey>> callback);

    /** The asynchronous version of {@link SurveyService#getQuestions}. */
    void getQuestions (int surveyId, AsyncCallback<List<SurveyQuestion>> callback);

    /** The asynchronous version of {@link SurveyService#updateQuestion}. */
    void updateQuestion (int surveyId, SurveyQuestion question, AsyncCallback<Void> callback);

    /** The asynchronous version of {@link SurveyService#updateSurvey}. */
    void updateSurvey (Survey survey, AsyncCallback<Void> callback);
}
