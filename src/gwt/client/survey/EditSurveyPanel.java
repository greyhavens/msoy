package client.survey;

import java.util.ArrayList;
import java.util.List;

import client.ui.MsoyUI;
import client.util.ClickCallback;
import client.util.Link;
import client.util.ServiceUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.PagedTable;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.util.SimpleDataModel;
import com.threerings.msoy.survey.gwt.Survey;
import com.threerings.msoy.survey.gwt.SurveyQuestion;
import com.threerings.msoy.survey.gwt.SurveyService;
import com.threerings.msoy.survey.gwt.SurveyServiceAsync;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

public class EditSurveyPanel extends VerticalPanel
{
    public EditSurveyPanel()
    {
    }

    public void setArgs (Args args)
    {
        int surveyId = args.get(1, -1);
        int questionId = args.get(2, -1);

        if (surveyId == -1) { // show survey list
            if (_surveys != null) {
                showSurveys();
                return;
            }
            clear();
            add(MsoyUI.createLabel(_msgs.loadingSurveys(), null));
            _surveySvc.getAllSurveys(new AsyncCallback<List<Survey>>() {
                public void onFailure (Throwable caught) {
                    clear();
                    add(MsoyUI.createLabel(_msgs.errSurveysNotLoaded(caught.getMessage()), null));
                }
    
                public void onSuccess (List<Survey> result) {
                    _surveys = result;
                    showSurveys();
                }
            });
            return;

        } else if (questionId == -1 && surveyId == 0) { // add a new one
            _survey = new Survey();
            _survey.name = _msgs.defaultSurveyName();
            _questions = new ArrayList<SurveyQuestion>();
            showSurvey();

        } else if (questionId == -1) { // edit an existing one
            if (!surveyRequested(surveyId)) {
                return;
            }

            if (_questions != null) {
                showSurvey();
                return;
            }

            add(MsoyUI.createLabel(_msgs.loadingSurvey(), null));
            _surveySvc.getQuestions(surveyId, new AsyncCallback<List<SurveyQuestion>> () {
                public void onFailure (Throwable caught) {
                    clear();
                    add(MsoyUI.createLabel(
                        _msgs.errSurveyNotLoaded(caught.getMessage()), null));
                }

                public void onSuccess (List<SurveyQuestion> result) {
                    _questions = result;
                    showSurvey();
                }
            });

        } else if (questionId == 0) { // add a new question
            if (!surveyRequested(surveyId)) {
                return;
            }

            if (_questions == null) {
                Link.go(Pages.ADMINZ, Args.compose(ACTION, surveyId));
                return;
            }

            add(MsoyUI.createLabel(_msgs.loadingSurvey(), null));
            _surveySvc.getQuestions(surveyId, new AsyncCallback<List<SurveyQuestion>> () {
                public void onFailure (Throwable caught) {
                    clear();
                    add(MsoyUI.createLabel(
                        _msgs.errSurveyNotLoaded(caught.getMessage()), null));
                }

                public void onSuccess (List<SurveyQuestion> result) {
                    _questions = result;
                    showSurvey();
                }
            });
        }
    }

    protected boolean surveyRequested (int surveyId)
    {
        if (_surveys == null) {
            Link.go(Pages.ADMINZ, ACTION);
            return false;
        }

        if (_survey == null || _questions == null || _survey.surveyId != surveyId) {
            _survey = null;
            _questions = null;
            for (Survey survey : _surveys) {
                if (survey.surveyId == surveyId) {
                    _survey = survey;
                    break;
                }
            }
            if (_survey == null) {
                clear();
                add(MsoyUI.createLabel(_msgs.surveyNotFound(), null));
                return false;
            }
        }
        return true;
    }

    protected void showSurveys ()
    {
        clear();
        add(Link.create(_msgs.addNew(), Pages.ADMINZ, Args.compose(ACTION, 0)));
        PagedTable<Survey> grid = new PagedTable<Survey>(10) {
            @Override protected List<Widget> createHeader () {
                List<Widget> header = new ArrayList<Widget>();
                header.add(MsoyUI.createLabel(_msgs.surveyNameHeader(), null));
                header.add(MsoyUI.createLabel("", null));
                return header;
            }

            @Override protected List<Widget> createRow (Survey item) {
                List<Widget> row = new ArrayList<Widget>();
                row.add(MsoyUI.createLabel(item.name, null));
                row.add(Link.create(_msgs.editSurvey(), Pages.ADMINZ,
                    Args.compose(ACTION, item.surveyId)));
                return row;
            }

            @Override protected String getEmptyMessage () {
                return _msgs.noSurveys();
            }
        };
        grid.setModel(new SimpleDataModel<Survey>(_surveys), 0);
    }

    protected void showSurvey ()
    {
        clear();
        //add(MsoyUI.createTextBox(_survey, maxLength, visibleLength))
    }

    protected List<Survey> _surveys;
    protected Survey _survey;
    protected List<SurveyQuestion> _questions;

    protected static final String ACTION = "surveys";
    protected static final SurveyServiceAsync _surveySvc = (SurveyServiceAsync)(ServiceUtil.bind(
        GWT.create(SurveyService.class), SurveyService.ENTRY_POINT));
    protected static final SurveyMessages _msgs = GWT.create(SurveyMessages.class);
}
