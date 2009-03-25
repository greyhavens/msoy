//
// $Id$

package client.survey;

import java.util.ArrayList;
import java.util.List;

import client.ui.MsoyUI;
import client.util.ArrayUtil;
import client.util.ClickCallback;
import client.util.Link;
import client.util.ServiceUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedTable;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import com.threerings.msoy.survey.gwt.SurveyMetaData;
import com.threerings.msoy.survey.gwt.SurveyQuestion;
import com.threerings.msoy.survey.gwt.SurveyService;
import com.threerings.msoy.survey.gwt.SurveyServiceAsync;

/**
 * Manages the editing of surveys and their questions.
 */
public class EditSurveyPanel extends VerticalPanel
{
    /** The action string for this panel. */
    public static final String ACTION = "surveys";

    /**
     * Creates a new survey editor.
     */
    public EditSurveyPanel()
    {
    }

    /**
     * Instantiates the requested editor as the contents of this panel. The arguments will indicate
     * whether we show the list of all surveys, the editor for an existing or new survey, or the
     * editor for an existing or new question.
     */
    public void setArgs (Args args)
    {
        boolean survey = args.getArgCount() >= 2;
        boolean question = args.getArgCount() >= 3;

        int surveyId = args.get(1, 0);
        int questionIndex = args.get(2, 0);

        if (!survey) {
            // show survey list
            clear();
            add(new SurveysPanel());

        } else if (!question) {
            if (surveyId == 0) {
                // add a new survey
                clear();
                add(new SurveyPanel());

            } else {
                // edit an existing survey
                clear();
                add(new SurveyPanel(surveyId));
            }
        } else {
            if (questionIndex == -1) {
                // add a new question
                clear();
                add(new QuestionPanel(surveyId));

            } else {
                // edit an existing question
                clear();
                add(new QuestionPanel(surveyId, questionIndex));
            }
        }
    }

    /**
     * Displays a list of surveys and links to edit them.
     */
    protected class SurveysPanel extends VerticalPanel
    {
        /**
         * Creates a new surveys panel.
         */
        public SurveysPanel ()
        {
            // show a loading message and load em up
            add(MsoyUI.createLabel(_msgs.loadingSurveys(), null));
            _cache.getSurveys(new AsyncCallback<List<SurveyMetaData>>() {
                public void onFailure (Throwable caught) {
                    clear();
                    add(MsoyUI.createLabel(_msgs.errSurveysNotLoaded(caught.getMessage()), null));
                }
    
                public void onSuccess (List<SurveyMetaData> result) {
                    clear();
                    init(result);
                }
            });
        }

        protected void init (List<SurveyMetaData> surveys)
        {
            add(MsoyUI.createLabel(_msgs.surveysTitle(), null));
            PagedTable<SurveyMetaData> table = new PagedTable<SurveyMetaData>(10) {
                @Override protected List<Widget> createHeader () {
                    List<Widget> header = new ArrayList<Widget>();
                    header.add(MsoyUI.createLabel(_msgs.surveyNameHeader(), null));
                    header.add(MsoyUI.createLabel("", null));
                    header.add(MsoyUI.createLabel("", null));
                    return header;
                }

                @Override protected List<Widget> createRow (SurveyMetaData item) {
                    List<Widget> row = new ArrayList<Widget>();
                    row.add(MsoyUI.createLabel(item.name, null));
                    row.add(Link.create(_msgs.edit(), Pages.ADMINZ,
                        Args.compose(ACTION, item.surveyId)));
                    row.add(Link.create(_msgs.view(), Pages.ME,
                        Args.compose("survey", item.surveyId)));
                    // TODO: action link to delete a survey
                    return row;
                }

                @Override protected String getEmptyMessage () {
                    return _msgs.noSurveys();
                }
            };
            table.setModel(new SimpleDataModel<SurveyMetaData>(surveys), 0);
            add(table);

            // link to add a new survey (id 0)
            add(Link.create(_msgs.addNew(), Pages.ADMINZ, Args.compose(ACTION, 0)));
        }
    }

    /**
     * Displays a panel to edit an existing or new survey.
     */
    protected class SurveyPanel extends VerticalPanel
    {
        /**
         * Creates a survey panel to edit a new survey.
         */
        public SurveyPanel ()
        {
            init(new SurveyWithQuestions(new SurveyMetaData(), new ArrayList<SurveyQuestion>()));
        }

        /**
         * Creates a survey panel to edit an existing survey.
         */
        public SurveyPanel (int surveyId)
        {
            // show a loading message and load em up
            add(MsoyUI.createLabel(_msgs.loadingSurvey(), null));
            _cache.getQuestions(surveyId, new AsyncCallback<SurveyWithQuestions>() {
                public void onFailure (Throwable caught) {
                    clear();
                    add(MsoyUI.createLabel(_msgs.errSurveyNotLoaded(caught.getMessage()), null));
                }
    
                public void onSuccess (SurveyWithQuestions result) {
                    clear();
                    init(result);
                }
            });
        }

        protected void init (final SurveyWithQuestions result)
        {
            _result = result;

            // metadata fields w/ save button
            SmartTable table = new SmartTable();
            final TextBox name = MsoyUI.createTextBox(_result.survey.name, 80, 40);
            table.setText(0, 0, _msgs.nameLabel());
            table.setWidget(0, 1, name);
            Button save = new Button(_msgs.save());
            new ClickCallback<SurveyMetaData>(save) {
                @Override // from ClickCallback
                protected boolean callService () {
                    _result.survey.name = name.getText();
                    _surveySvc.updateSurvey(_result.survey, this);
                    return true;
                }
                @Override  // from ClickCallback
                protected boolean gotResult (SurveyMetaData result) {
                    MsoyUI.info(_msgs.surveySaveComplete());
                    // let the cache know something changed
                    _cache.surveyUpdated(result);
                    _result.survey = result;
                    return true;
                }
            };
            table.setText(1, 0, "");
            table.setWidget(1, 1, save);
            add(table);

            // list of questions
            add(MsoyUI.createLabel(_msgs.questionsTitle(), null));
            _questions = new PagedTable<SurveyQuestion>(10) {
                @Override protected List<Widget> createHeader () {
                    List<Widget> header = new ArrayList<Widget>();
                    header.add(MsoyUI.createLabel(_msgs.questionTextHeader(), null));
                    header.add(MsoyUI.createLabel(_msgs.questionTypeHeader(), null));
                    header.add(MsoyUI.createLabel("", null));
                    header.add(MsoyUI.createLabel("", null));
                    return header;
                }

                @Override protected List<Widget> createRow (SurveyQuestion item) {
                    // informational columns
                    List<Widget> row = new ArrayList<Widget>();
                    row.add(MsoyUI.createLabel(item.text, null));
                    row.add(MsoyUI.createLabel(getQuestionTypeName(item.type), null));

                    // edit link
                    int index = _result.getQuestionIndex(item);
                    row.add(Link.create(_msgs.edit(), Pages.ADMINZ, Args.compose(ACTION,
                        _result.survey.surveyId, index)));

                    // last column is controls to rearrange or delete questions
                    HorizontalPanel alterBox = new HorizontalPanel();
                    alterBox.setSpacing(2);
                    if (index > 0) {
                        alterBox.add(MsoyUI.createActionLabel(_msgs.moveUp(),
                            new AlterQuestion(index, -1)));
                    }
                    if (index < _result.questions.size() - 1) {
                        alterBox.add(MsoyUI.createActionLabel(_msgs.moveDown(),
                            new AlterQuestion(index, 1)));
                    }
                    alterBox.add(MsoyUI.createActionLabel(_msgs.delete(),
                        new AlterQuestion(index, 0)));
                    row.add(alterBox);
                    return row;
                }

                @Override protected String getEmptyMessage () {
                    return _msgs.noQuestions();
                }
            };
            _questions.setModel(new SimpleDataModel<SurveyQuestion>(_result.questions), 0);
            add(_questions);

            // link to create a new question
            add(Link.create(_msgs.addNewQuestion(), Pages.ADMINZ,
                Args.compose(ACTION, _result.survey.surveyId, -1)));
        }

        /**
         * Listener/Callback for moving a question up or down or deleting it.
         */
        protected class AlterQuestion
            implements ClickListener, AsyncCallback<Void>
        {
            /**
             * Creates a new alteration callback.
             * @param index the index of the question to alter
             * @param direction the direction to move in, 0 means delete
             */
            public AlterQuestion (int index, int direction)
            {
                _index = index;
                _direction = direction;
            }

            public void onClick (Widget sender)
            {
                if (_altering) {
                    return;
                }

                _altering = true;
                if (_direction != 0) {
                    _surveySvc.moveQuestion(
                        _result.survey.surveyId, _index, _index + _direction, this);
                } else {
                    _surveySvc.deleteQuestion(_result.survey.surveyId, _index, this);
                }
            }

            public void onFailure (Throwable caught)
            {
                MsoyUI.error(_msgs.errQuestionNotAltered(caught.getMessage()));
                _altering = false;
            }

            public void onSuccess (Void result)
            {
                _altering = false;
                if (_direction != 0) {
                    // no need to notify the cache here 
                    SurveyQuestion tmp = _result.questions.get(_index);
                    _result.questions.set(_index, _result.questions.get(_index + _direction));
                    _result.questions.set(_index + _direction, tmp);

                } else {
                    _result.questions.remove(_index);
                }
                _questions.displayPage(_questions.getPage(), true);
            }

            protected int _index;
            protected int _direction;
        }

        protected SurveyWithQuestions _result;
        protected PagedTable<SurveyQuestion> _questions;
        protected boolean _altering;
    }

    /**
     * Displays a panel to add a new or edit an existing question.
     */
    protected class QuestionPanel extends VerticalPanel
    {
        /**
         * Creates a question panel to add a new question.
         */
        public QuestionPanel (int surveyId)
        {
            this(surveyId, -1);
        }

        /**
         * Creates a panel to edit an existing question.
         */
        public QuestionPanel (int surveyId, final int questionIndex)
        {
            // show a loading message and load em up
            add(MsoyUI.createLabel(_msgs.loadingSurvey(), null));
            _cache.getQuestions(surveyId, new AsyncCallback<SurveyWithQuestions>() {
                public void onSuccess(SurveyWithQuestions result) {
                    clear();
                    _questionIndex = questionIndex;
                    init(result.survey, result.getQuestion(questionIndex));
                }
                public void onFailure(Throwable caught) {
                    clear();
                    add(MsoyUI.createLabel(_msgs.errSurveyNotLoaded(caught.getMessage()), null));
                }
            });
        }

        protected void init (final SurveyMetaData survey, SurveyQuestion question)
        {
            _survey = survey;
            _question = question;

            add(MsoyUI.createLabel(_question == null ? _msgs.addNewQuestionTitle(survey.name) :
                _msgs.editQuestionTitle(survey.name), null));

            // if we're editing, the type is read only so fill in the rest of the form
            add(_editGrid = new SmartTable());
            _editGrid.setText(0, 0, _msgs.questionTypeLabel());
            if (_question != null) {
                finishInit();
                return;
            }

            // type selector for new questions. once the type is known, fill in the rest
            final ListBox types = new ListBox();
            types.addItem(_msgs.questionSelectType());
            for (SurveyQuestion.Type type : SurveyQuestion.Type.values()) {
                types.addItem(getQuestionTypeName(type));
            }
            types.addChangeListener(new ChangeListener() {
                public void onChange (Widget sender) {
                    int idx = types.getSelectedIndex() - 1;
                    if (idx >= 0 && idx < SurveyQuestion.Type.values().length) {
                        _question = new SurveyQuestion();
                        _question.type = SurveyQuestion.Type.values()[idx];
                        finishInit();
                    }
                }
            });
            _editGrid.setWidget(0, 1, types);
        }

        protected void finishInit ()
        {
            _editGrid.setText(0, 1, getQuestionTypeName(_question.type));

            // the text of the question
            _editGrid.setText(1, 0, _msgs.questionTextLabel());
            _question.text = _question.text != null ? _question.text : "";
            final TextBox text = MsoyUI.createTextBox(_question.text, 80, 40);
            _editGrid.setWidget(1, 1, text);

            // type-specific controls
            TextBox maxValue = null;
            if (_question.type == SurveyQuestion.Type.EXCLUSIVE_CHOICE ||
                _question.type == SurveyQuestion.Type.SUBSET_CHOICE) {

                // existing choices
                _editGrid.setText(2, 0, _msgs.choices());
                refreshChoices();
                _editGrid.setText(3, 0, "");

                // controls to add a new choice
                HorizontalPanel add = new HorizontalPanel();
                final TextBox choice = MsoyUI.createTextBox("", 60, 20);
                add.add(choice);
                add.add(new Button(_msgs.newChoiceButtonLabel(), new ClickListener() {
                    public void onClick (Widget sender) {
                        _question.choices = ArrayUtil.append(
                            _question.choices, choice.getText(), ArrayUtil.STRING_TYPE);
                        refreshChoices();
                    }
                }));
                _editGrid.setWidget(3, 1, add);

            } else if (_question.type == SurveyQuestion.Type.RATING) {
                // maximum rating box
                _editGrid.setText(2, 0, _msgs.maxValue());
                maxValue = MsoyUI.createTextBox(String.valueOf(_question.maxValue), 2, 2);
                _editGrid.setWidget(2, 1, maxValue);
            }

            // save button
            final TextBox fmaxValue = maxValue;
            Button save = new Button(_msgs.save());
            new ClickCallback<SurveyQuestion>(save) {
                protected boolean callService () {
                    _question.text = text.getText();
                    if (fmaxValue != null) {
                        _question.maxValue = Integer.parseInt(fmaxValue.getText());
                    }
                    _surveySvc.updateQuestion(_survey.surveyId, _questionIndex, _question, this);
                    return false;
                }

                protected boolean gotResult (SurveyQuestion result) {
                    // let the cache know we've changed things
                    _cache.questionUpdated(_survey, _questionIndex, result);
                    clear();
                    init(_survey, result);
                    return true;
                }
            };
            add(save);
        }

        protected void refreshChoices ()
        {
            // set up the table of choices for this question. show the choice string and a way to
            // delete it. these should usually be pretty short and not worth the trouble of doing
            // up/down controls.
            SmartTable choices = new SmartTable();
            if (_question.choices == null) {
                _question.choices = new String[]{};
            }
            int row = 0;
            for (String choice : _question.choices) {
                final int frow = row++;
                choices.setText(frow, 0, choice);
                choices.setWidget(frow, 1, MsoyUI.createActionLabel(_msgs.delete(),
                    new ClickListener() {
                        public void onClick (Widget sender) {
                            _question.choices = ArrayUtil.splice(
                                _question.choices, frow, 1, ArrayUtil.STRING_TYPE);
                            refreshChoices();
                        }
                    }));
            }
            _editGrid.setWidget(2, 1, choices);
        }

        protected SurveyMetaData _survey;
        protected SurveyQuestion _question;
        protected int _questionIndex;
        protected SmartTable _editGrid;
    }

    /**
     * Wraps up a survey with its list of questions as this is not convenient to provide from the
     * servlet.
     */
    protected static class SurveyWithQuestions
    {
        /** The survey. */
        public SurveyMetaData survey;

        /** The questions. */
        public List<SurveyQuestion> questions;

        /** Creates a new wrapper of a survey and its questions. */
        public SurveyWithQuestions (SurveyMetaData survey, List<SurveyQuestion> questions)
        {
            this.survey = survey;
            this.questions = questions;
        }

        /**
         * Gets the question of a given index, returning null if the index is out of range.
         */
        public SurveyQuestion getQuestion (int idx)
        {
            if (idx >= 0 && idx < questions.size()) {
                return questions.get(idx);
            }
            return null;
        }

        /**
         * Get the index of a question in the list.
         */
        int getQuestionIndex (SurveyQuestion question) {
            return questions.indexOf(question);
        }
    }

    /**
     * Facilitates getting information from the server. Allows the back button to be used
     * efficiently and works around the server's db collection caching. For example, adding a new
     * question then viewing the survey does not show the new question without this. All methods
     * will query the server unless the requested information is already present.
     */
    protected static class Cache
    {
        /**
         * Gets the list of surveys.
         */
        public void getSurveys (final AsyncCallback<List<SurveyMetaData>> callback)
        {
            if (_surveys != null) {
                callback.onSuccess(_surveys);
                return;
            }

            _surveySvc.getAllSurveys(new AsyncCallback<List<SurveyMetaData>>() {
                public void onSuccess (List<SurveyMetaData> result) {
                    _surveys = result;
                    callback.onSuccess(_surveys);
                }
                public void onFailure (Throwable caught) {
                    callback.onFailure(caught);
                }
            });
        }

        /**
         * Gets a single survey metadata.
         */
        public void getSurvey (final int surveyId, final AsyncCallback<SurveyMetaData> callback)
        {
            getSurveys(new AsyncCallback<List<SurveyMetaData>>() {
                public void onSuccess (List<SurveyMetaData> result) {
                    for (SurveyMetaData survey : _surveys) {
                        if (survey.surveyId == surveyId) {
                            callback.onSuccess(survey);
                            return;
                        }
                    }
                    callback.onSuccess(null);
                }
                public void onFailure(Throwable caught) {
                    callback.onFailure(caught);
                }
            });
        }

        /**
         * Gets a survey's metadata and list of questions.
         */
        public void getQuestions (
            final int surveyId, final AsyncCallback<SurveyWithQuestions> callback)
        {
            getSurvey(surveyId, new AsyncCallback<SurveyMetaData>() {
                SurveyMetaData survey;
                public void onSuccess (SurveyMetaData result) {
                    survey = result;
                    if (_surveyId != null && _surveyId == surveyId) {
                        callback.onSuccess(new SurveyWithQuestions(survey, _questions));
                        return;
                    }

                    _surveySvc.getQuestions(surveyId, new AsyncCallback<List<SurveyQuestion>>() {
                        public void onSuccess (List<SurveyQuestion> questions) {
                            _questions = questions;
                            _surveyId = surveyId;
                            callback.onSuccess(new SurveyWithQuestions(survey, questions));
                        }
                        public void onFailure (Throwable caught) {
                            callback.onFailure(caught);
                        }
                    });
                }
                public void onFailure(Throwable caught) {
                    callback.onFailure(caught);
                }
            });
        }

        /**
         * Updates cached data relevant to the newly updated or inserted survey.
         */
        public void surveyUpdated (SurveyMetaData survey)
        {
            if (_surveys == null) {
                return;
            }

            for (int ii = 0; ii < _surveys.size(); ++ii) {
                if (_surveys.get(ii).surveyId == survey.surveyId) {
                    _surveys.set(ii, survey);
                    return;
                }
            }

            _surveys.add(survey);
        }

        /**
         * Updates cached data relevant to the newly updated or inserted question.
         */
        public void questionUpdated (SurveyMetaData survey, int index, SurveyQuestion question)
        {
            if (_surveyId == null || _surveyId != survey.surveyId) {
                return;
            }

            if (index >= 0) {
                _questions.set(index, question);
            } else {
                _questions.add(question);
            }
        }

        protected List<SurveyMetaData> _surveys;
        protected Integer _surveyId;
        protected List<SurveyQuestion> _questions;
    }

    /**
     * Returns a translated string for a type of question.
     */
    protected static String getQuestionTypeName (SurveyQuestion.Type type)
    {
        switch (type) {
        case BOOLEAN:
            return _msgs.questionTypeBoolean();
        case EXCLUSIVE_CHOICE:
            return _msgs.questionTypeExclusiveChoice();
        case FREE_FORM:
            return _msgs.questionTypeFreeForm();
        case RATING:
            return _msgs.questionTypeRating();
        case SUBSET_CHOICE:
            return _msgs.questionTypeSubset();
        }
        return type.toString();
    }

    /** Our survey and questions cache, used across token changes. */
    protected Cache _cache = new Cache();

    protected static final SurveyServiceAsync _surveySvc = (SurveyServiceAsync)(ServiceUtil.bind(
        GWT.create(SurveyService.class), SurveyService.ENTRY_POINT));
    protected static final SurveyMessages _msgs = GWT.create(SurveyMessages.class);
}
