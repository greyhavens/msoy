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
    public List<Survey> getAllSurveys ()
        throws ServiceException;

    /** Loads the list of questions for a survey. */
    public List<SurveyQuestion> getQuestions (int surveyId)
        throws ServiceException;

    /** Updates an existing survey or inserts a new survey. */
    public void updateSurvey (Survey survey)
        throws ServiceException;

    /** Updates an existing survey question or inserts a new question. */
    public void updateQuestion (int surveyId, SurveyQuestion question)
        throws ServiceException;
}
