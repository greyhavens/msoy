//
// $Id$

package client.survey;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedTable;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.util.PagedResult;

import com.threerings.msoy.survey.gwt.SurveyQuestion;
import com.threerings.msoy.survey.gwt.SurveyService.ResponseSummary;
import com.threerings.msoy.survey.gwt.SurveyService.SubmissionSummary;
import com.threerings.msoy.survey.gwt.SurveyService;
import com.threerings.msoy.survey.gwt.SurveyServiceAsync;

import client.ui.MsoyUI;
import client.util.MsoyPagedServiceDataModel;

public class ViewSurveyResultsPanel extends VerticalPanel
{
    public ViewSurveyResultsPanel (int surveyId)
    {
        _surveyId = surveyId;
        setStyleName("viewSurveyResults");
        setSpacing(10);
        setWidth("100%");
        _surveySvc.getSubmissionSummary(surveyId, new AsyncCallback<SubmissionSummary>() {
            public void onFailure (Throwable caught) {
                add(MsoyUI.createLabel(_msgs.errSurveyNotLoaded(caught.getMessage()), "error"));
            }
            public void onSuccess (SubmissionSummary result) {
                init(result);
            }
        });
    }

    protected void init (SubmissionSummary summary)
    {
        _summary = summary;

        add(MsoyUI.createLabel(_msgs.viewResultTitle(summary.metaData.name,
            String.valueOf(summary.total)), "name"));

        SmartTable questions = new SmartTable(3, 0);
        questions.setWidth("100%");
        questions.setStyleName("questions");
        add(questions);

        for (int ii = 0; ii < _summary.questions.length; ++ii) {
            SurveyQuestion q = _summary.questions[ii];
            questions.setText(ii * 2, 0, (ii + 1) + ".", 1, "number");
            questions.setText(ii * 2, 1, q.text, 1, "text");
            questions.setText(ii * 2, 2, q.optional ? _msgs.optional() : "", 1, "optional");

            Widget response = new ResponseSummaryWidget(q, _summary.responses[ii], ii);
            questions.setText(ii * 2 + 1, 0, "");
            questions.setWidget(ii * 2 + 1, 1, response, 2, "summary");
            questions.getRowFormatter().setStyleName(ii * 2, "question");
            questions.getRowFormatter().setStyleName(ii * 2 + 1, "summary");
        }
    }

    protected class ResponseSummaryWidget extends SmartTable
    {
        public ResponseSummaryWidget (SurveyQuestion question, ResponseSummary summary,
            final int questionIndex)
        {
            super(2, 0);
            setStyleName("responses");
            String[] answers = question.getEncodedChoices();
            switch (question.type) {
            case EXCLUSIVE_CHOICE:
            case SUBSET_CHOICE:
                answers = question.choices;
                break;
            case BOOLEAN:
                answers = new String[] {_msgs.falseLabel(), _msgs.trueLabel()};
                break;
            }
            if (question.type == SurveyQuestion.Type.FREE_FORM) {
                Button view = new Button(_msgs.viewResultFreeFormResponses(), new ClickHandler() {
                    public void onClick (ClickEvent event) {
                        setWidget(0, 0, new FreeFormResponseTable(questionIndex));
                    }
                });
                if (summary.others > 0) {
                    setWidget(0, 0, view);
                } else {
                    setText(0, 0, _msgs.viewResultNoResponses());
                }

            } else {
                int row = 0;
                setText(row, 0, _msgs.totalResponseLabel(), 1, "label");
                setText(row, 1, String.valueOf(summary.total), 1, "count");
                getRowFormatter().setStyleName(row++, "total");
                for (int ii = 0; ii < answers.length; ++ii) {
                    setText(row, 0, answers[ii], 1, "answer");
                    setText(row++, 1, String.valueOf(summary.numberChosen[ii]), 1, "count");
                }
                if (summary.others != 0) {
                    setText(row, 0, _msgs.otherResponseLabel(), 1, "label");
                    setText(row++, 1, String.valueOf(summary.others), 1, "count");
                }
            }
        }
    }

    protected class FreeFormResponseTable extends PagedTable<String>
    {
        public FreeFormResponseTable (int questionIndex)
        {
            super(20);
            _questionIndex = questionIndex;
            addStyleName("freeFormResponsesTable");
            setModel(new MsoyPagedServiceDataModel<String, PagedResult<String>>() {
                protected void callFetchService (int start, int count, boolean needCount,
                    AsyncCallback<PagedResult<String>> callback) {
                    _surveySvc.getFreeFormResponses(
                        _surveyId, _questionIndex, needCount, start, count, callback);
                }
            }, 0);
        }

        protected List<Widget> createRow (String item)
        {
            List<Widget> row = Lists.newArrayList();
            row.add(MsoyUI.createLabel(item, "freeFormResponse"));
            return row;
        }

        protected String getEmptyMessage ()
        {
            return "";
        }

        protected int _questionIndex;
    }

    protected int _surveyId;
    protected SubmissionSummary _summary;
    protected List<SurveyQuestion> _questions;

    protected static final SurveyServiceAsync _surveySvc = GWT.create(SurveyService.class);
    protected static final SurveyMessages _msgs = GWT.create(SurveyMessages.class);
}
