//
// $Id$

package com.threerings.msoy.survey.persist;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.CountRecord;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.Ops;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.SchemaMigration;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.QueryClause;
import com.samskivert.depot.clause.Where;

import com.threerings.presents.annotation.BlockingThread;

/**
 * Handles database interactions for the survey package.
 */
@Singleton @BlockingThread
public class SurveyRepository extends DepotRepository
{
    /**
     * Creates a new survey repository.
     */
    @Inject public SurveyRepository (PersistenceContext context)
    {
        super(context);

        context.registerMigration(SurveyResponseRecord.class,
            new SchemaMigration.Drop(3, "suveryId"));
        context.registerMigration(SurveyResponseRecord.class,
            new SchemaMigration.Retype(4, SurveyResponseRecord.RESPONSE));
        context.registerMigration(SurveyQuestionRecord.class,
            new SchemaMigration.Retype(3, SurveyQuestionRecord.DESCRIPTOR));
    }

    /**
     * Loads a survey record with the given id.
     */
    public SurveyRecord loadSurvey (int surveyId)
    {
        return load(SurveyRecord.getKey(surveyId));
    }

    /**
     * Loads all stored survey records.
     */
    public List<SurveyRecord> loadAllSurveys ()
    {
        return findAll(SurveyRecord.class);
    }

    /**
     * Loads all question records associated with a survey, sorted by order.
     */
    public List<SurveyQuestionRecord> loadQuestions (int surveyId)
    {
        List<QueryClause> clauses = Lists.newArrayList();
        clauses.add(new Where(SurveyQuestionRecord.SURVEY_ID, surveyId));
        clauses.add(OrderBy.ascending(SurveyQuestionRecord.QUESTION_INDEX));
        return findAll(SurveyQuestionRecord.class, clauses);
    }

    /**
     * Inserts a new survey.
     */
    public void insertSurvey (SurveyRecord surveyRec)
    {
        Preconditions.checkArgument(surveyRec.surveyId == 0);
        insert(surveyRec);
    }

    /**
     * Updates an existing survey.
     */
    public void updateSurvey (SurveyRecord surveyRec)
    {
        Preconditions.checkArgument(surveyRec.surveyId != 0);
        update(surveyRec);
    }

    /**
     * Inserts a new survey question.
     */
    public void insertQuestion (SurveyQuestionRecord question)
    {
        Preconditions.checkArgument(question.surveyId != 0);
        Preconditions.checkArgument(question.questionIndex == -1);
        question.questionIndex = countQuestions(question.surveyId);
        insert(question);
    }

    /**
     * Updates an existing survey question. Note that the index is part of the primary key, so
     * {@link #moveQuestion} must be used to update that.
     */
    public void updateQuestion (SurveyQuestionRecord question)
    {
        Preconditions.checkArgument(question.surveyId != 0);
        Preconditions.checkArgument(question.questionIndex >= 0);
        int size = countQuestions(question.surveyId);
        if (question.questionIndex >= size) {
            throw new RuntimeException("Question index " + question.questionIndex +
                " out of range (" + size + ")" + " for survey " + question.surveyId);
        }
        update(question);
    }

    /**
     * Removes a question from a survey. Takes care of decrementing all subsequent question indices.
     */
    public void deleteQuestion (int surveyId, int questionIndex)
    {
        Preconditions.checkArgument(surveyId != 0);
        Preconditions.checkArgument(questionIndex >= 0);
        int size = countQuestions(surveyId);
        if (questionIndex >= size) {
            throw new RuntimeException("Question index " + questionIndex +
                " out of range (" + size + ")" + " for survey " + surveyId);
        }
        delete(SurveyQuestionRecord.getKey(surveyId, questionIndex));
        for (int ii = questionIndex + 1; ii < size; ++ii) {
            updateQuestionIndex(surveyId, ii, ii - 1);
        }
    }

    /**
     * Moves a question to a new position in a survey. Takes care of incrementing or decrementing
     * intervening indices.
     */
    public void moveQuestion(int surveyId, int index, int newIndex)
    {
        Preconditions.checkArgument(surveyId != 0);
        Preconditions.checkArgument(index >= 0);
        Preconditions.checkArgument(newIndex >= 0);
        int size = countQuestions(surveyId);
        if (index >= size) {
            throw new RuntimeException("Question index " + index +
                " out of range (" + size + ")" + " for survey " + surveyId);
        }
        if (newIndex >= size) {
            throw new RuntimeException("Question index " + index +
                " out of range (" + size + ")" + " for survey " + surveyId);
        }
        if (index == newIndex) {
            return;
        }
        updateQuestionIndex(surveyId, index, -1);
        int offset = index < newIndex ? -1 : 1;
        for (int ii = newIndex; ii != index; ii += offset) {
            updateQuestionIndex(surveyId, ii, ii + offset);
        }
        updateQuestionIndex(surveyId, -1, newIndex);
    }

    /**
     * Returns the number of questions in a survey.
     */
    public int countQuestions (int surveyId)
    {
        return load(CountRecord.class, new Where(SurveyQuestionRecord.SURVEY_ID, surveyId),
                    new FromOverride(SurveyQuestionRecord.class)).count;
    }

    /**
     * Inserts the meta data for a user's response to a survey.
     */
    public void insertSubmission (SurveySubmissionRecord submitRec)
    {
        insert(submitRec);
    }

    /**
     * Returns the number of submissions for a survey.
     */
    public int countSubmissions (int surveyId)
    {
        return load(CountRecord.class, new Where(SurveySubmissionRecord.SURVEY_ID, surveyId),
            new FromOverride(SurveySubmissionRecord.class)).count;
    }

    /**
     * Insert's a user's response to an individual question.
     */
    public void insertQuestionResponse(SurveyResponseRecord responseRec)
    {
        insert(responseRec);
    }

    /**
     * Loads all the responses to a given survey.
     */
    public List<SurveyResponseRecord> loadResponses (int surveyId)
    {
        return findAll(SurveyResponseRecord.class, new Where(
            SurveyResponseRecord.SURVEY_ID, surveyId));
    }

    /**
     * Count the number of responses to a specific question.
     */
    public int countResponses (int surveyId, int questionIndex)
    {
        List<QueryClause> clauses = getResponseClauses(surveyId, questionIndex);
        clauses.add(new FromOverride(SurveyResponseRecord.class));
        return load(CountRecord.class, clauses.toArray(new QueryClause[clauses.size()])).count;
    }

    /**
     * Loads all the responses to a given survey.
     */
    public List<SurveyResponseRecord> loadResponses (
        int surveyId, int questionIndex, int start, int count)
    {
        List<QueryClause> clauses = getResponseClauses(surveyId, questionIndex);
        clauses.add(new Limit(start, count));
        return findAll(SurveyResponseRecord.class, clauses);
    }

    /**
     * Utility method to update only the index column of a question.
     */
    protected void updateQuestionIndex (int surveyId, int index, int newIndex)
    {
        updatePartial(SurveyQuestionRecord.getKey(surveyId, index),
            SurveyQuestionRecord.QUESTION_INDEX, newIndex);
    }

    protected List<QueryClause> getResponseClauses (int surveyId, int questionIndex)
    {
        List<QueryClause> clauses = Lists.newArrayList();
        clauses.add(new Where(
                        Ops.and(SurveyResponseRecord.SURVEY_ID.eq(surveyId),
                                SurveyResponseRecord.QUESTION_INDEX.eq(questionIndex))));
        return clauses;
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(SurveyRecord.class);
        classes.add(SurveyQuestionRecord.class);
        classes.add(SurveySubmissionRecord.class);
        classes.add(SurveyResponseRecord.class);
    }
}
