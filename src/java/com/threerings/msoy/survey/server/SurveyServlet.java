package com.threerings.msoy.survey.server;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.survey.gwt.Survey;
import com.threerings.msoy.survey.gwt.SurveyQuestion;
import com.threerings.msoy.survey.gwt.SurveyService;
import com.threerings.msoy.survey.persist.SurveyQuestionRecord;
import com.threerings.msoy.survey.persist.SurveyRecord;
import com.threerings.msoy.survey.persist.SurveyRepository;
import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.server.MsoyServiceServlet;

/**
 * Provides survey services.
 */
public class SurveyServlet extends MsoyServiceServlet
    implements SurveyService
{
    // from SurveyService
    public List<Survey> getAllSurveys ()
    {
        List<Survey> surveys = Lists.newArrayList();
        for (SurveyRecord surveyRec : _surveyRepo.loadAllSurveys()) {
            surveys.add(surveyRec.toSurvey());
        }
        return surveys;
    }

    // from SurveyService
    public List<SurveyQuestion> getQuestions (int surveyId)
    {
        List<SurveyQuestion> questions = Lists.newArrayList();
        for (SurveyQuestionRecord questionRec : _surveyRepo.loadQuestions(surveyId)) {
            questions.add(questionRec.toSurveyQuestion());
        }
        return questions;
    }

    // from SurveyService
    public void updateQuestion (int surveyId, SurveyQuestion question)
        throws ServiceException
    {
        requireAdminUser();
        if (surveyId != 0) {
            SurveyRecord survey = _surveyRepo.loadSurvey(surveyId);
            if (survey == null) {
                throw new ServiceException(MsoyCodes.INTERNAL_ERROR);
            }
        }
        SurveyQuestionRecord questionRec = new SurveyQuestionRecord();
        questionRec.surveyId = surveyId;
        questionRec.fromSurveyQuestion(question);
        if (surveyId == 0) {
            //_surveyRepo.insertQuestion(questionRec);

        } else {
            //_surveyRepo.updateQuestion(questionRec);
        }
    }

    // from SurveyService
    public void updateSurvey (Survey survey)
        throws ServiceException
    {
        requireAdminUser();
        SurveyRecord surveyRec = new SurveyRecord();
        surveyRec.fromSurvey(survey);
        if (surveyRec.surveyId != 0) {
            //_surveyRepo.updateSurvey(surveyRec);
        } else {
            //_surveyRepo.insertSurvey(surveyRec);
        }
    }

    @Inject SurveyRepository _surveyRepo;
}
