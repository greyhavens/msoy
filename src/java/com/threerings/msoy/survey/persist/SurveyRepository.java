package com.threerings.msoy.survey.persist;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
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
    }

    /**
     * Loads a survey record with the given id.
     */
    public SurveyRecord loadSurvey (int surveyId)
    {
        return load(SurveyRecord.class, surveyId);
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
        List<QueryClause> clauses = new ArrayList<QueryClause>();
        clauses.add(new Where(SurveyQuestionRecord.SURVEY_ID, surveyId));
        clauses.add(OrderBy.ascending(SurveyQuestionRecord.QUESTION_ORDER));
        return findAll(SurveyQuestionRecord.class, clauses);
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(SurveyRecord.class);
        classes.add(SurveyQuestionRecord.class);
    }
}
