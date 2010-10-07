//
// $Id$

package com.threerings.msoy.survey.server;

import java.sql.Date;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.samskivert.depot.DuplicateKeyException;

import com.threerings.web.gwt.ServiceException;
import com.threerings.gwt.util.PagedResult;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.UserAction;

import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.PromotionRecord;
import com.threerings.msoy.server.persist.PromotionRepository;

import com.threerings.msoy.web.gwt.Promotion;
import com.threerings.msoy.web.server.MsoyServiceServlet;

import com.threerings.msoy.money.server.MoneyLogic;

import com.threerings.msoy.survey.gwt.Survey;
import com.threerings.msoy.survey.gwt.SurveyMetaData;
import com.threerings.msoy.survey.gwt.SurveyQuestion;
import com.threerings.msoy.survey.gwt.SurveyQuestion.Type;
import com.threerings.msoy.survey.gwt.SurveyResponse;
import com.threerings.msoy.survey.gwt.SurveyService;

import com.threerings.msoy.survey.persist.SurveyQuestionRecord;
import com.threerings.msoy.survey.persist.SurveyRecord;
import com.threerings.msoy.survey.persist.SurveyRepository;
import com.threerings.msoy.survey.persist.SurveyResponseRecord;
import com.threerings.msoy.survey.persist.SurveySubmissionRecord;

import static com.threerings.msoy.Log.log;

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
        // check authorization
        MemberRecord mrec = requireAuthedUser();

        // check survey exists and is enabled
        SurveyRecord survey = _surveyRepo.loadSurvey(surveyId);
        if (survey == null) {
            throw new ServiceException(MsoyCodes.INTERNAL_ERROR);
        }
        long now = System.currentTimeMillis();
        if (!survey.enabled || (survey.start != null && now < survey.start.getTime()) ||
            (survey.finish != null && now >= survey.finish.getTime() + 24*60*60*1000)) {
            throw new ServiceException("e.survey_inactive");
        }

        // check required questions have been answered
        List<SurveyQuestionRecord> questions = _surveyRepo.loadQuestions(surveyId);
        for (int ii = 0; ii < questions.size(); ++ii) {
            if (!questions.get(ii).optional) {
                boolean ok = false;
                for (SurveyResponse resp : responses) {
                    if (resp.questionIndex == ii) {
                        ok = true;
                    }
                }
                if (!ok) {
                    log.warning("Incomplete survey submitted", "surveyId", surveyId,
                        "memberId", mrec.memberId, "responses", responses);
                    throw new ServiceException(MsoyCodes.INTERNAL_ERROR);
                }
            }
        }

        // create submission
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

        // award coins if applicable
        if (survey.coinAward > 0) {
            _moneyLogic.awardCoins(mrec.memberId, survey.coinAward, true,
                UserAction.completedSurvey(mrec.memberId, survey.name, surveyId));
        }

        // record responses
        // TODO: validate?
        for (SurveyResponse resp : responses) {
            SurveyResponseRecord responseRec = new SurveyResponseRecord();
            responseRec.surveyId = surveyId;
            responseRec.memberId = mrec.memberId;
            responseRec.questionIndex = resp.questionIndex;
            responseRec.response = resp.response;
            _surveyRepo.insertQuestionResponse(responseRec);
        }

        // auto-disable if maximum submissions has been reached
        if (survey.maxSubmissions > 0 && _surveyRepo.countSubmissions(surveyId) >=
            survey.maxSubmissions) {
            survey.enabled = false;
            _surveyRepo.updateSurvey(survey);

            // kill the linked promotion too
            deactivatePromotion(survey);
        }
    }

    // from StatusService
    public SubmissionSummary getSubmissionSummary (int surveyId)
        throws ServiceException
    {
        requireAdminUser();

        // load survey and questions
        SurveyRecord survey = _surveyRepo.loadSurvey(surveyId);
        List<SurveyQuestionRecord> questions = _surveyRepo.loadQuestions(surveyId);
        if (survey == null) {
            throw new ServiceException(MsoyCodes.INTERNAL_ERROR);
        }

        // fill in survey fields and initialize per-question fields
        SubmissionSummary summary = new SubmissionSummary();
        summary.metaData = survey.toSurvey();
        summary.total = _surveyRepo.countSubmissions(surveyId);
        summary.responses = new ResponseSummary[questions.size()];
        summary.questions = new SurveyQuestion[questions.size()];
        String[][] answers = new String[summary.questions.length][];
        for (int ii = 0; ii < summary.questions.length; ++ii) {
            summary.questions[ii] = questions.get(ii).toSurveyQuestion();
            answers[ii] = summary.questions[ii].getEncodedChoices();
            summary.responses[ii] = new ResponseSummary();
            summary.responses[ii].numberChosen = new int[answers[ii].length];
        }

        // accumulate all responses
        for (SurveyResponseRecord response : _surveyRepo.loadResponses(surveyId)) {
            int qidx = response.questionIndex;
            if (qidx < 0 || qidx >= summary.questions.length) {
                continue;
            }
            summary.responses[qidx].total++;
            SurveyQuestion question = summary.questions[qidx];
            ResponseSummary responseSummary = summary.responses[qidx];
            if (question.type == Type.SUBSET_CHOICE) {
                for (String resp : response.response.split(",")) {
                    accumulate(answers[qidx], resp, responseSummary);
                }
            } else {
                accumulate(answers[qidx], response.response, responseSummary);
            }
        }

        return summary;
    }

    // from StatusService
    public PagedResult<String> getFreeFormResponses (
        int surveyId, int index, boolean needCount, int start, int count)
        throws ServiceException
    {
        requireAdminUser();

        // get the total if requested
        PagedResult<String> result = new PagedResult<String>();
        if (needCount) {
            result.total = _surveyRepo.countResponses(surveyId, index);
        }

        // get one page of results
        result.page = Lists.newArrayListWithCapacity(count);
        for (SurveyResponseRecord response :
            _surveyRepo.loadResponses(surveyId, index, start, count)) {
            result.page.add(response.response);
        }

        return result;
    }

    protected void deactivatePromotion (SurveyRecord survey)
    {
        if (survey.linkedPromoId.length() == 0) {
            return;
        }

        PromotionRecord prec = _promoRepo.loadPromotion(survey.linkedPromoId);
        if (prec == null) {
            return;
        }

        // just set the end date to an hour ago. suppress exceptions since this is probably
        // happening in response to a user action and they will get confused otherwise
        try {
            Promotion promo = prec.toPromotion();
            promo.ends = new java.util.Date(System.currentTimeMillis() - 60*60*1000L);
            _promoRepo.updatePromotion(promo);

        } catch (Exception e) {
            log.warning("Could not save promotion when deactivating a survey",
                "surveyId", survey.surveyId, e);
        }
    }

    protected static void accumulate (String[] answers, String response, ResponseSummary summary)
    {
        for (int ii = 0; ii < answers.length; ++ii) {
            if (answers[ii].equals(response)) {
                summary.numberChosen[ii]++;
                return;
            }
        }
        summary.others++;
    }

    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected PromotionRepository _promoRepo;
    @Inject protected SurveyRepository _surveyRepo;
}
