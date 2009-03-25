//
// $Id$

package com.threerings.msoy.survey.server;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.server.MsoyServiceServlet;

import com.threerings.msoy.survey.gwt.SurveyMetaData;
import com.threerings.msoy.survey.gwt.SurveyQuestion;
import com.threerings.msoy.survey.gwt.SurveyService;
import com.threerings.msoy.survey.persist.SurveyQuestionRecord;
import com.threerings.msoy.survey.persist.SurveyRecord;
import com.threerings.msoy.survey.persist.SurveyRepository;

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

    @Inject SurveyRepository _surveyRepo;
}
