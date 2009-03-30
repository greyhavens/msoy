//
// $Id$

package com.threerings.msoy.survey.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;
import com.threerings.msoy.web.gwt.ServiceException;

/** Provides operations for viewing, modifying and submitting surveys. */
public interface SurveyService
    extends RemoteService
{
    public static final String ENTRY_POINT = "/survey";

    /**
     * A summary of all responses to a single question. The interpretation of the fields depends on
     * the type of question being summarized.
     */
    public static class ResponseSummary
        implements IsSerializable
    {
        /** Number of people that selected each choice. */
        public int[] numberChosen;

        /** Number of responses that didn't match any choice. */
        public int others;

        /** Number of responses received for this question. */
        public int total;
    }

    /**
     * A summary of all submissions to a survey.
     */
    public static class SubmissionSummary
        implements IsSerializable
    {
        /** The survey meta data. */
        public SurveyMetaData metaData;

        /** Total number of submissions. */
        public int total;

        /** The survey questions. */
        public SurveyQuestion[] questions;

        /** Summaries of the responses to the questions. */
        public ResponseSummary[] responses;
    }

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

    /** Submits a response to a survey. Since questions may be marked as optional, the responses
     * may not be one per question. */
    void submitResponse (int surveyId, List<SurveyResponse> responses)
        throws ServiceException;

    /** Aggregates and returns the results of a survey. */
    SubmissionSummary getSubmissionSummary (int surveyId)
        throws ServiceException;
}
