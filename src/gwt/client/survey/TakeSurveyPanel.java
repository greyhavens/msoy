package client.survey;

import java.util.ArrayList;
import java.util.List;

import client.ui.MsoyUI;
import client.util.ServiceUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.msoy.survey.gwt.Survey;
import com.threerings.msoy.survey.gwt.SurveyQuestion;
import com.threerings.msoy.survey.gwt.SurveyResponse;
import com.threerings.msoy.survey.gwt.SurveyService;
import com.threerings.msoy.survey.gwt.SurveyServiceAsync;

public class TakeSurveyPanel extends VerticalPanel
{
    public TakeSurveyPanel (int surveyId)
    {
        setStyleName("takeSurvey");
        setSpacing(10);
        _surveySvc.getSurvey(surveyId, new AsyncCallback<Survey>() {
            public void onFailure (Throwable caught) {
                add(MsoyUI.createLabel(_msgs.errSurveyNotLoaded(caught.getMessage()), "error"));
            }
            public void onSuccess (Survey result) {
                setSurvey(result);
            }
        });
    }

    protected void setSurvey (Survey survey)
    {
        _survey = survey;

        add(MsoyUI.createLabel(survey.name, "name"));

        VerticalPanel questions = new VerticalPanel();
        questions.setSpacing(10);
        questions.setWidth("100%");
        questions.setStyleName("questions");

        for (int ii = 0; ii < _survey.questions.length; ++ii) {
            Widget panel = createQuestionPanel(ii + 1, _survey.questions[ii]);
            questions.add(panel);
        }
        add(questions);
    }

    protected Widget createQuestionPanel (int number, SurveyQuestion question)
    {
        String text = _msgs.questionText("" + number, question.text);
        return MsoyUI.createLabel(text, "text");
    }

    public Survey _survey;
    public List<SurveyResponse> _responses = new ArrayList<SurveyResponse>();

    protected static final SurveyServiceAsync _surveySvc = (SurveyServiceAsync)(ServiceUtil.bind(
        GWT.create(SurveyService.class), SurveyService.ENTRY_POINT));
    protected static final SurveyMessages _msgs = GWT.create(SurveyMessages.class);
}
