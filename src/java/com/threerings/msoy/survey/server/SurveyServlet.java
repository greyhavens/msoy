//
// $Id$

package com.threerings.msoy.survey.server;

import java.sql.Date;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.samskivert.depot.DuplicateKeyException;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.server.MsoyServiceServlet;

import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.survey.gwt.Survey;
import com.threerings.msoy.survey.gwt.SurveyMetaData;
import com.threerings.msoy.survey.gwt.SurveyQuestion;
import com.threerings.msoy.survey.gwt.SurveyResponse;
import com.threerings.msoy.survey.gwt.SurveyService;
import com.threerings.msoy.survey.persist.SurveyQuestionRecord;
import com.threerings.msoy.survey.persist.SurveyRecord;
import com.threerings.msoy.survey.persist.SurveyRepository;
import com.threerings.msoy.survey.persist.SurveyResponseRecord;
import com.threerings.msoy.survey.persist.SurveySubmissionRecord;

/**
 * Provides survey services.
 */
public class SurveyServlet extends MsoyServiceServlet
    implements SurveyService
{
    // from SurveyService
    public List<SurveyMetaData> getAllSurveys ()
        throws ServiceException
    {
        requireAdminUser();
        List<SurveyMetaData> surveys = Lists.newArrayList();
        for (SurveyRecord surveyRec : _surveyRepo.loadAllSurveys()) {
            surveys.add(surveyRec.toSurvey());
        }
        return surveys;
    }

    // from SurveyService
    public List<SurveyQuestion> getQuestions (int surveyId)
        throws ServiceException
    {
        List<SurveyQuestion> questions = Lists.newArrayList();
        for (SurveyQuestionRecord questionRec : _surveyRepo.loadQuestions(surveyId)) {
            questions.add(questionRec.toSurveyQuestion());
        }
        return questions;
    }

    // from SurveyService
    public Survey getSurvey (int surveyId)
        throws ServiceException
    {
        SurveyRecord surveyRec = _surveyRepo.loadSurvey(surveyId);
        if (surveyRec == null) {
            throw new ServiceException(MsoyCodes.INTERNAL_ERROR);
        }
        List<SurveyQuestionRecord> questions = _surveyRepo.loadQuestions(surveyId);
        Survey survey = new Survey();
        survey.name = surveyRec.name;
        survey.questions = new SurveyQuestion[questions.size()];
        for (int ii = 0; ii < questions.size(); ++ii) {
            survey.questions[ii] = questions.get(ii).toSurveyQuestion();
        }
        return survey;
    }

    // from SurveyService
    public SurveyQuestion updateQuestion (int surveyId, int index, SurveyQuestion question)
        throws ServiceException
    {
        requireAdminUser();
        SurveyRecord survey = _surveyRepo.loadSurvey(surveyId);
        if (survey == null) {
            throw new ServiceException(MsoyCodes.INTERNAL_ERROR);
        }
        SurveyQuestionRecord questionRec = new SurveyQuestionRecord();
        questionRec.fromSurveyQuestion(question);
        questionRec.questionIndex = index;
        questionRec.surveyId = surveyId;
        if (index >= 0) {
            _surveyRepo.updateQuestion(questionRec);

        } else {
            _surveyRepo.insertQuestion(questionRec);
        }
        return questionRec.toSurveyQuestion();
    }

    // from SurveyService
    public SurveyMetaData updateSurvey (SurveyMetaData survey)
        throws ServiceException
    {
        requireAdminUser();
        SurveyRecord surveyRec = new SurveyRecord();
        surveyRec.fromSurvey(survey);
        if (surveyRec.surveyId != 0) {
            _surveyRepo.updateSurvey(surveyRec);
        } else {
            _surveyRepo.insertSurvey(surveyRec);
        }
        return surveyRec.toSurvey();
    }

    // from SurveyService
    public void deleteQuestion (int surveyId, int index)
        throws ServiceException
    {
        requireAdminUser();
        _surveyRepo.deleteQuestion(surveyId, index);
    }

    // from SurveyService
    public void moveQuestion (int surveyId, int index, int newIndex)
        throws ServiceException
    {
        requireAdminUser();
        _surveyRepo.moveQuestion(surveyId, index, newIndex);
    }

    public void submitResponse (int surveyId, List<SurveyResponse> responses)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        SurveyRecord survey = _surveyRepo.loadSurvey(surveyId);
        if (survey == null) {
            throw new ServiceException(MsoyCodes.INTERNAL_ERROR);
        }
        long now = System.currentTimeMillis();
        if (!survey.enabled || (survey.start != null && now < survey.start.getTime()) ||
            (survey.finish != null && now >= survey.finish.getTime() + 24*60*60*1000)) {
            throw new ServiceException("e.survey_inactive");
        }
        SurveySubmissionRecord subRec = new SurveySubmissionRecord();
        subRec.completed = new Date(System.currentTimeMillis());
        subRec.memberId = mrec.memberId;
        subRec.numQuestions = responses.size();
        subRec.surveyId = surveyId;
        try {
            _surveyRepo.insertSubmission(subRec);
        } catch (DuplicateKeyException dke) {
            throw new ServiceException("e.survey_already_completed");
        }
        for (SurveyResponse resp : responses) {
            SurveyResponseRecord responseRec = new SurveyResponseRecord();
            responseRec.surveyId = surveyId;
            responseRec.memberId = mrec.memberId;
            responseRec.questionIndex = resp.questionIndex;
            responseRec.response = resp.response;
            _surveyRepo.insertQuestionResponse(responseRec);
        }
        if (survey.maxSubmissions > 0 && _surveyRepo.countSubmissions(surveyId) >=
            survey.maxSubmissions) {
            survey.enabled = false;
            _surveyRepo.updateSurvey(survey);
        }
    }

    @Inject SurveyRepository _surveyRepo;
}
