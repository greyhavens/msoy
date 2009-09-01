//
// $Id$

package client.survey;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.survey.gwt.Survey;
import com.threerings.msoy.survey.gwt.SurveyQuestion;
import com.threerings.msoy.survey.gwt.SurveyResponse;
import com.threerings.msoy.survey.gwt.SurveyService;
import com.threerings.msoy.survey.gwt.SurveyServiceAsync;

import client.ui.LimitedTextArea;
import client.ui.MsoyUI;
import client.util.ClickCallback;

public class TakeSurveyPanel extends VerticalPanel
{
    public TakeSurveyPanel (int surveyId)
    {
        _surveyId = surveyId;
        setStyleName("takeSurvey");
        setSpacing(10);
        setWidth("100%");
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

        SmartTable questions = new SmartTable(3, 0);
        questions.setWidth("100%");
        questions.setStyleName("questions");
        add(questions);

        _questions = new QuestionUI[_survey.questions.length];
        for (int ii = 0; ii < _survey.questions.length; ++ii) {
            SurveyQuestion q = _survey.questions[ii];
            questions.setText(ii * 2, 0, (ii + 1) + ".", 1, "number");
            questions.setText(ii * 2, 1, q.text, 1, "text");
            questions.setText(ii * 2, 2, q.optional ? _msgs.optional() : "", 1, "optional");

            switch (q.type) {
            case BOOLEAN:
                _questions[ii] = new TrueFalse();
                break;
            case EXCLUSIVE_CHOICE:
                _questions[ii] = new ExclusiveChoice();
                break;
            case SUBSET_CHOICE:
                _questions[ii] = new SubsetChoice();
                break;
            case RATING:
                _questions[ii] = new Rating();
                break;
            case FREE_FORM:
                _questions[ii] = new Essay();
                break;
            }
            questions.setText(ii * 2 + 1, 0, "");
            if (_questions[ii] != null) {
                questions.setWidget(ii * 2 + 1, 1, _questions[ii].makeWidget(ii, q), 2, "ui");
            }
            questions.getRowFormatter().setStyleName(ii * 2, "question");
            questions.getRowFormatter().setStyleName(ii * 2 + 1, "response");
        }

        Button done = new Button(_msgs.doneLabel());
        new ClickCallback<Void>(done) {
            protected boolean callService () {
                if (!gatherResponses()) {
                    return false;
                }
                _surveySvc.submitResponse(_surveyId, _responses, this);
                return true;
            }
            protected boolean gotResult (Void result) {
                MsoyUI.info(_msgs.thankYou());
                return true;
            }
        };
        add(done);
    }

    protected boolean gatherResponses ()
    {
        _responses.clear();
        for (QuestionUI qui : _questions) {
            if (qui == null) {
                continue;
            }

            SurveyResponse resp = new SurveyResponse();
            resp.questionIndex = qui.getIndex();
            resp.response = qui.getResponse();

            if (resp.response != null) {
                _responses.add(resp);

            } else if (!_survey.questions[resp.questionIndex].optional) {
                MsoyUI.error(_msgs.errAnswerRequired(String.valueOf(resp.questionIndex + 1)));
                return false;
            }
        }
        if (_responses.size() == 0) {
            MsoyUI.error(_msgs.errNoResponses());
            return false;
        }
        return true;
    }

    protected abstract class QuestionUI
    {
        public Widget makeWidget (int idx, SurveyQuestion question)
        {
            _index = idx;
            _question = question;
            _responseChoices = _question.getEncodedChoices();
            return makeWidget();
        }

        public int getIndex ()
        {
            return _index;
        }

        protected abstract Widget makeWidget ();
        protected abstract String getResponse();

        protected int _index;
        protected SurveyQuestion _question;
        protected String[] _responseChoices;
    }

    protected class TrueFalse extends QuestionUI
    {
        protected Widget makeWidget ()
        {
            // buttons for true and false
            _trueButton = new RadioButton("Q" + _index, _msgs.trueLabel());
            _falseButton = new RadioButton("Q" + _index, _msgs.falseLabel());

            // make the panel
            VerticalPanel panel = new VerticalPanel();
            panel.setStyleName("trueFalse");
            panel.add(_trueButton);
            panel.add(_falseButton);
            return panel;
        }

        protected String getResponse ()
        {
            if (_trueButton.getValue()) {
                return _responseChoices[1];
            }
            if (_falseButton.getValue()) {
                return _responseChoices[0];
            }
            return null;
        }

        protected RadioButton _trueButton, _falseButton;
    }

    protected class ExclusiveChoice extends QuestionUI
    {
        protected Widget makeWidget ()
        {
            // one button per choice
            _buttons = new RadioButton[_question.choices.length];

            // create all the buttons and add them to the panel
            VerticalPanel panel = new VerticalPanel();
            for (int ii = 0; ii < _question.choices.length; ++ii) {
                panel.add(_buttons[ii] = new RadioButton("Q" + _index, _question.choices[ii]));
            }
            panel.setStyleName("exclusive");
            return panel;
        }

        protected String getResponse ()
        {
            for (int ii = 0; ii < _buttons.length; ++ii) {
                if (_buttons[ii].getValue()) {
                    return _responseChoices[ii];
                }
            }
            return null;
        }

        protected RadioButton[] _buttons;
    }

    protected class SubsetChoice extends QuestionUI
    {
        protected Widget makeWidget ()
        {
            // one button per choice
            _buttons = new CheckBox[_question.choices.length];

            // create all the buttons and add them to the panel
            VerticalPanel panel = new VerticalPanel();
            for (int ii = 0; ii < _question.choices.length; ++ii) {
                panel.add(_buttons[ii] = new CheckBox(_question.choices[ii]));
            }
            panel.setStyleName("subset");
            return panel;
        }

        protected String getResponse ()
        {
            StringBuilder response = new StringBuilder();
            for (int ii = 0; ii < _buttons.length; ++ii) {
                if (_buttons[ii].getValue()) {
                    if (response.length() > 0) {
                        response.append(",");
                    }
                    response.append(_responseChoices[ii]);
                }
            }
            if (response.length() > 0) {
                return response.toString();
            }
            return null;
        }

        protected CheckBox[] _buttons;
    }

    protected class Rating extends QuestionUI
    {
        protected Widget makeWidget ()
        {
            // one button per rating value
            _buttons = new RadioButton[_question.maxValue];

            // create all the buttons and add them to the panel
            HorizontalPanel panel = new HorizontalPanel();
            panel.add(MsoyUI.createLabel("1", "value"));
            for (int ii = 0; ii < _question.maxValue; ++ii) {
                panel.add(_buttons[ii] = new RadioButton("Q" + _index));
            }
            panel.add(MsoyUI.createLabel("" + _question.maxValue, "value"));
            panel.setStyleName("rating");
            panel.setSpacing(10);
            return panel;
        }

        protected String getResponse ()
        {
            for (int ii = 0; ii < _buttons.length; ++ii) {
                if (_buttons[ii].getValue()) {
                    return _responseChoices[ii];
                }
            }
            return null;
        }

        protected RadioButton[] _buttons;
    }

    protected class Essay extends QuestionUI
    {
        protected Widget makeWidget ()
        {
            return _text = new LimitedTextArea(SurveyResponse.MAX_LENGTH, 80, 4);
        }

        protected String getResponse ()
        {
            String resp = _text.getText();
            return resp.length() == 0 ? null : resp;
        }

        protected LimitedTextArea _text;
    }

    public int _surveyId;
    public Survey _survey;
    public QuestionUI _questions[];
    public List<SurveyResponse> _responses = new ArrayList<SurveyResponse>();

    protected static final SurveyServiceAsync _surveySvc = GWT.create(SurveyService.class);
    protected static final SurveyMessages _msgs = GWT.create(SurveyMessages.class);
}
