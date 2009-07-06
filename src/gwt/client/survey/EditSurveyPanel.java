//
// $Id$

package client.survey;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gwt.core.client.GWT;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedTable;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.util.DateUtil;
import com.threerings.gwt.util.ServiceUtil;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import com.threerings.msoy.survey.gwt.SurveyMetaData;
import com.threerings.msoy.survey.gwt.SurveyQuestion;
import com.threerings.msoy.survey.gwt.SurveyService;
import com.threerings.msoy.survey.gwt.SurveyServiceAsync;

import client.ui.DateFields;
import client.ui.MsoyUI;
import client.util.ArrayUtil;
import client.util.ClickCallback;
import client.util.Link;

/**
 * Manages the editing of surveys and their questions.
 */
public class EditSurveyPanel extends VerticalPanel
{
    /**
     * Creates a new survey editor.
     */
    public EditSurveyPanel()
    {
        setStyleName("editSurveys");
        setWidth("100%");
    }

    /**
     * Instantiates the requested editor as the contents of this panel. The arguments will indicate
     * whether we show the list of all surveys, the editor for an existing or new survey, or the
     * editor for an existing or new question.
     */
    public void setArgs (Args args)
    {
        if (args.getArgCount() < 3) {
            // show survey list
            clear();
            add(new SurveysPanel());
            return;

        }

        int surveyId = args.get(2, 0);
        if (args.getArgCount() < 4) {
            if (surveyId == 0) {
                // add a new survey
                clear();
                add(new SurveyPanel());

            } else {
                // edit an existing survey
                clear();
                add(new SurveyPanel(surveyId));
            }
            return;
        }

        int questionIndex = args.get(3, 0);
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
            setStyleName("surveys");
            setWidth("100%");

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
                    header.add(MsoyUI.createLabel(_msgs.surveyLinksHeader(), null));
                    return header;
                }

                @Override protected List<Widget> createRow (SurveyMetaData item) {
                    List<Widget> row = new ArrayList<Widget>();
                    row.add(MsoyUI.createLabel(item.name, null));
                    HorizontalPanel links = new HorizontalPanel();
                    links.setSpacing(5);
                    links.add(Link.create(_msgs.edit(), Pages.ADMINZ,
                                          "survey", "e", item.surveyId));
                    links.add(Link.create(_msgs.results(), Pages.ADMINZ,
                                          "survey", "r", item.surveyId));
                    links.add(Link.create(_msgs.view(), Pages.ME, "survey", item.surveyId));
                    row.add(links);
                    // TODO: action link to delete a survey
                    return row;
                }

                @Override protected String getEmptyMessage () {
                    return _msgs.noSurveys();
                }

                @Override protected SmartTable createContents (
                    int start, int count, List<SurveyMetaData> list) {
                    SmartTable table = super.createContents(start, count, list);
                    table.setCellPadding(3);
                    table.setCellSpacing(1);
                    return table;
                }
            };

            table.setModel(new SimpleDataModel<SurveyMetaData>(surveys), 0);
            table.addStyleName("table");
            table.setWidth("100%");
            add(table);

            // link to add a new survey (id 0)
            add(Link.create(_msgs.addNew(), "addNew", Pages.ADMINZ, "survey", "e", 0));
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
            setWidth("100%");
            setStyleName("survey");

            init(new SurveyWithQuestions(new SurveyMetaData(), new ArrayList<SurveyQuestion>()));
        }

        /**
         * Creates a survey panel to edit an existing survey.
         */
        public SurveyPanel (int surveyId)
        {
            setWidth("100%");
            setStyleName("survey");

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
            SmartTable table = new SmartTable(0, 5);
            table.setWidth("100%");
            table.setStyleName("fields");
            int row = 0;

            _name = MsoyUI.createTextBox(_result.survey.name, 80, 40);
            table.setText(row, 0, _msgs.nameLabel(), 1, "label");
            table.setWidget(row++, 1, _name);

            _coinAward = MsoyUI.createTextBox(
                String.valueOf(_result.survey.coinAward), 6, 6);
            table.setText(row, 0, _msgs.coinAwardLabel(), 1, "label");
            table.setWidget(row++, 1, _coinAward);

            _maxSubmissions = MsoyUI.createTextBox(
                String.valueOf(_result.survey.maxSubmissions), 6, 6);
            table.setText(row, 0, _msgs.maxSubmissionsLabel(), 1, "label");
            table.setWidget(row++, 1, _maxSubmissions);

            _enabled = new CheckBox();
            _enabled.setValue(_result.survey.enabled);
            table.setText(row, 0, _msgs.enabledLabel(), 1, "label");
            table.setWidget(row++, 1, _enabled);

            _start = new OptionalDate(_result.survey.startDate);
            table.setText(row, 0, _msgs.startLabel(), 1, "label");
            table.setWidget(row++, 1, _start);

            _finish = new OptionalDate(_result.survey.finishDate);
            table.setText(row, 0, _msgs.finishLabel(), 1, "label");
            table.setWidget(row++, 1, _finish);

            _promoId = MsoyUI.createTextBox(_result.survey.linkedPromoId, 20, 20);
            table.setText(row, 0, _msgs.promoIdLabel(), 1, "label");
            table.setWidget(row++, 1, _promoId);

            Button save = new Button(_msgs.save());
            new ClickCallback<SurveyMetaData>(save) {
                @Override // from ClickCallback
                protected boolean callService () {
                    _result.survey.name = _name.getText();
                    _result.survey.enabled = _enabled.getValue();
                    _result.survey.startDate = _start.getDate();
                    _result.survey.finishDate = _finish.getDate();
                    _result.survey.maxSubmissions = Integer.parseInt(_maxSubmissions.getText());
                    _result.survey.coinAward = Integer.parseInt(_coinAward.getText());
                    _result.survey.linkedPromoId = _promoId.getText();
                    _surveySvc.updateSurvey(_result.survey, this);
                    return true;
                }
                @Override  // from ClickCallback
                protected boolean gotResult (SurveyMetaData result) {
                    MsoyUI.info(_msgs.surveySaveComplete());
                    // let the cache know something changed
                    _cache.surveyUpdated(result);
                    // reload if this is a brand new survey. otherwise just set it
                    if (_result.survey.surveyId == 0) {
                        Link.replace(Pages.ADMINZ, "survey", "e", result.surveyId);
                    } else {
                        _result.survey = result;
                    }
                    return true;
                }
            };
            table.setText(row, 0, "");
            table.setWidget(row++, 1, save);
            add(table);

            if (_result.survey.surveyId != 0) {
                // list of questions
                add(MsoyUI.createLabel(_msgs.questionsTitle(), null));
                _questions = new QuestionsSummary(_result);
                _questions.setWidth("100%");
                _questions.addStyleName("questions");
                add(_questions);

                // link to create a new question
                add(Link.create(_msgs.addNewQuestion(), "addNew", Pages.ADMINZ,
                    "survey", "e", _result.survey.surveyId, -1));
            }
        }

        protected SurveyWithQuestions _result;
        protected PagedTable<SurveyQuestion> _questions;

        protected TextBox _name, _coinAward, _maxSubmissions, _promoId;
        protected CheckBox _enabled;
        protected OptionalDate _start, _finish;
    }

    /**
     * Table to show a summary of the questions in the survey being edited.
     */
    protected class QuestionsSummary extends PagedTable<SurveyQuestion>
    {
        public QuestionsSummary (SurveyWithQuestions result)
        {
            super(10);
            _result = result;
            setModel(new SimpleDataModel<SurveyQuestion>(_result.questions), 0);
        }

        @Override protected List<Widget> createHeader ()
        {
            List<Widget> header = new ArrayList<Widget>();
            header.add(MsoyUI.createLabel(_msgs.questionTextHeader(), null));
            header.add(MsoyUI.createLabel(_msgs.questionTypeHeader(), null));
            header.add(MsoyUI.createLabel("", null));
            header.add(MsoyUI.createLabel("", null));
            return header;
        }

        @Override protected List<Widget> createRow (SurveyQuestion item)
        {
            // informational columns
            List<Widget> row = new ArrayList<Widget>();
            row.add(MsoyUI.createLabel(item.text, null));
            row.add(MsoyUI.createLabel(getQuestionTypeName(item.type), null));

            // edit link
            int index = _result.getQuestionIndex(item);
            row.add(Link.create(_msgs.edit(), Pages.ADMINZ,
                                "survey", "e", _result.survey.surveyId, index));

            // last column is controls to rearrange or delete questions
            HorizontalPanel alterBox = new HorizontalPanel();
            alterBox.setSpacing(2);
            Button button;
            alterBox.add(button = new Button(_msgs.moveUp(), new AlterQuestion(index, -1)));
            button.setEnabled(index > 0);
            alterBox.add(button = new Button(_msgs.moveDown(), new AlterQuestion(index, 1)));
            button.setEnabled(index < _result.questions.size() - 1);
            alterBox.add(new Button(_msgs.delete(), new AlterQuestion(index, 0)));
            row.add(alterBox);
            return row;
        }

        @Override protected String getEmptyMessage ()
        {
            return _msgs.noQuestions();
        }

        @Override protected SmartTable createContents (
            int start, int count, List<SurveyQuestion> list)
        {
            SmartTable table = super.createContents(start, count, list);
            table.setCellPadding(3);
            table.setCellSpacing(1);
            return table;
        }

        /**
         * Listener/Callback for moving a question up or down or deleting it.
         */
        protected class AlterQuestion
            implements ClickHandler, AsyncCallback<Void>
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

            public void onClick (ClickEvent e)
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
                displayPage(getPage(), true);
            }

            protected int _index;
            protected int _direction;
        }

        protected SurveyWithQuestions _result;
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
            setStyleName("question");
            setWidth("100%");
            setSpacing(10);

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
            _choices = _question == null ? null : _question.choices;

            add(MsoyUI.createLabel(_question == null ? _msgs.addNewQuestionTitle(survey.name) :
                _msgs.editQuestionTitle(survey.name), null));

            // if we're editing, the type is read only so fill in the rest of the form
            add(_editGrid = new SmartTable(0, 10));
            _editGrid.setStyleName("fieldsTable");
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
            types.addChangeHandler(new ChangeHandler() {
                public void onChange (ChangeEvent sender) {
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

            _editGrid.setText(2, 0, _msgs.questionOptionalLabel());
            final CheckBox optional = new CheckBox();
            _editGrid.setWidget(2, 1, optional);
            optional.setValue(_question.optional);

            _typeStartRow = 3;

            // type-specific controls
            TextBox maxValue = null;
            if (_question.type == SurveyQuestion.Type.EXCLUSIVE_CHOICE ||
                _question.type == SurveyQuestion.Type.SUBSET_CHOICE) {

                // existing choices
                _editGrid.setText(_typeStartRow, 0, _msgs.questionChoicesLabel(), 1, "choicesLabel");
                refreshChoices();
                _editGrid.setText(_typeStartRow + 1, 0, "");

                // controls to add a new choice
                HorizontalPanel add = new HorizontalPanel();
                final TextBox choice = MsoyUI.createTextBox("", 60, 20);
                add.add(choice);
                add.add(new Button(_msgs.newChoiceButtonLabel(), new ClickHandler() {
                    public void onClick (ClickEvent e) {
                        _choices = ArrayUtil.append(
                            _choices, choice.getText(), ArrayUtil.STRING_TYPE);
                        choice.setText("");
                        refreshChoices();
                    }
                }));
                _editGrid.setWidget(_typeStartRow + 1, 1, add);

            } else if (_question.type == SurveyQuestion.Type.RATING) {
                // maximum rating box
                _editGrid.setText(_typeStartRow, 0, _msgs.questionMaxValueLabel());
                maxValue = MsoyUI.createTextBox(String.valueOf(_question.maxValue), 2, 2);
                _editGrid.setWidget(_typeStartRow, 1, maxValue);
            }

            // save button
            final TextBox fmaxValue = maxValue;
            Button save = new Button(_msgs.save());
            new ClickCallback<SurveyQuestion>(save) {
                protected boolean callService () {
                    _question.text = text.getText();
                    _question.choices = _choices;
                    _question.optional = optional.getValue();
                    if (fmaxValue != null) {
                        _question.maxValue = Integer.parseInt(fmaxValue.getText());
                    }
                    _surveySvc.updateQuestion(_survey.surveyId, _questionIndex, _question, this);
                    return false;
                }

                protected boolean gotResult (SurveyQuestion result) {
                    // let the cache know we've changed things
                    _cache.questionUpdated(_survey, _questionIndex, result);
                    Link.replace(Pages.ADMINZ, "survey", "e", _survey.surveyId);
                    return true;
                }
            };
            add(save);
        }

        protected void refreshChoices ()
        {
            if (_choices == null) {
                _choices = new String[]{};
            }

            // set up the table of choices for this question. show the choice string and a way to
            // delete it. these should usually be pretty short and not worth the trouble of doing
            // up/down controls.
            SmartTable choices = new SmartTable(3, 1);
            choices.setWidth("100%");
            choices.setStyleName("choices");
            int row = 0;
            for (int ii = 0; ii < _choices.length; ++ii) {
                final int frow = row++;
                choices.setText(frow, 0, String.valueOf(ii + 1) + ".", 1, "number");
                choices.setText(frow, 1, _choices[ii], 1, "choice");
                choices.setWidget(frow, 2, MsoyUI.createActionLabel(_msgs.delete(),
                    new ClickHandler() {
                        public void onClick (ClickEvent e) {
                            _choices = ArrayUtil.splice(_choices, frow, 1, ArrayUtil.STRING_TYPE);
                            refreshChoices();
                        }
                    }), 1, "delete");
                choices.getRowFormatter().addStyleName(frow, "row" + ii % 2);
            }
            _editGrid.setWidget(_typeStartRow, 1, choices);
        }

        protected SurveyMetaData _survey;
        protected SurveyQuestion _question;
        protected String[] _choices;
        protected int _questionIndex;
        protected SmartTable _editGrid;
        protected int _typeStartRow;
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

    protected static class OptionalDate extends HorizontalPanel
    {
        public OptionalDate (Date initialValue)
        {
            CheckBox set = new CheckBox();
            add(set);
            set.addClickHandler(new ClickHandler() {
                public void onClick (ClickEvent e) {
                    _date.setVisible(((CheckBox)e.getSource()).getValue());
                }
            });
            add(_date = new DateFields(-50, 1));
            set.setValue(initialValue != null);
            _date.setVisible(initialValue != null);
            _date.setDate(DateUtil.toDateVec(
                initialValue != null ? initialValue : new Date()));
        }

        public Date getDate ()
        {
            if (_date.isVisible()) {
                return DateUtil.toDate(_date.getDate());
            }
            return null;
        }

        protected DateFields _date;
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
