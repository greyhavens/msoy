//
// $Id$

package com.threerings.msoy.survey.gwt;

import java.util.List;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.threerings.gwt.util.PagedResult;

/**
 * Provides the asynchronous version of {@link SurveyService}.
 */
public interface SurveyServiceAsync
{
    /**
     * The async version of {@link SurveyService#getAllSurveys}.
     */
    void getAllSurveys (AsyncCallback<List<SurveyMetaData>> callback);

    /**
     * The async version of {@link SurveyService#getQuestions}.
     */
    void getQuestions (int surveyId, AsyncCallback<List<SurveyQuestion>> callback);

    /**
     * The async version of {@link SurveyService#updateSurvey}.
     */
    void updateSurvey (SurveyMetaData survey, AsyncCallback<SurveyMetaData> callback);

    /**
     * The async version of {@link SurveyService#updateQuestion}.
     */
    void updateQuestion (int surveyId, int index, SurveyQuestion question, AsyncCallback<SurveyQuestion> callback);

    /**
     * The async version of {@link SurveyService#moveQuestion}.
     */
    void moveQuestion (int surveyId, int index, int newIndex, AsyncCallback<Void> callback);

    /**
     * The async version of {@link SurveyService#deleteQuestion}.
     */
    void deleteQuestion (int surveyId, int index, AsyncCallback<Void> callback);

    /**
     * The async version of {@link SurveyService#getSurvey}.
     */
    void getSurvey (int surveyId, AsyncCallback<Survey> callback);

    /**
     * The async version of {@link SurveyService#submitResponse}.
     */
    void submitResponse (int surveyId, List<SurveyResponse> responses, AsyncCallback<Void> callback);

    /**
     * The async version of {@link SurveyService#getSubmissionSummary}.
     */
    void getSubmissionSummary (int surveyId, AsyncCallback<SurveyService.SubmissionSummary> callback);

    /**
     * The async version of {@link SurveyService#getFreeFormResponses}.
     */
    void getFreeFormResponses (int surveyId, int index, boolean needCount, int start, int count, AsyncCallback<PagedResult<String>> callback);
}
