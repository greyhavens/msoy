//
// $Id$

package com.threerings.msoy.survey.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.threerings.msoy.web.gwt.ServiceException;

/** Provides operations for viewing, modifying and submitting surveys. */
public interface SurveyService
    extends RemoteService
{
    public static final String ENTRY_POINT = "/survey";

    /** Loads all surveys (meta data only). */
    List<SurveyMetaData> getAllSurveys ()
        throws ServiceException;

    /** Loads the list of questions for a survey. */
    List<SurveyQuestion> getQuestions (int surveyId)
        throws ServiceException;

    /** Updates an existing survey or inserts a new survey. Returns the newly updated or inserted
     * survey. */
    SurveyMetaData updateSurvey (SurveyMetaData survey)
        throws ServiceException;

    /** Updates an existing survey question or inserts a new question. Returns the newly update or
     * inserted question. */
    SurveyQuestion updateQuestion (int surveyId, int index, SurveyQuestion question)
        throws ServiceException;

    /** Moves a question to a new position in the survey. */
    void moveQuestion (int surveyId, int index, int newIndex)
        throws ServiceException;

    /** Removes a question from the survey. */
    void deleteQuestion (int surveyId, int index)
        throws ServiceException;

    /** Gets an end-user survey for filling in. */
    Survey getSurvey (int surveyId)
        throws ServiceException;

    void submitResponse (int surveyId, List<SurveyResponse> responses)
        throws ServiceException;
}
