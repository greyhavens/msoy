package client.adminz;

import java.util.Date;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import org.gwt.advanced.client.ui.widget.DatePicker;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.AdminServiceAsync;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.web.gwt.Contest;

import client.item.ImageChooserPopup;
import client.ui.ContestBox;
import client.ui.LimitedTextArea;
import client.ui.MsoyUI;
import client.ui.TongueBox;
import client.util.ClickCallback;
import client.util.DateUtil;
import client.util.MediaUtil;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

/**
 * Displays all contests registered with the system, allows adding, editing and deletion.
 */
public class ContestsEditor extends FlowPanel
{
    public ContestsEditor ()
    {
        setStyleName("contestsEditor");
        add(MsoyUI.createLabel(_msgs.contestsLoading(), null));

        _adminsvc.loadContests(new MsoyCallback<List<Contest>>() {
            public void onSuccess (List<Contest> contests) {
                init(contests);
            }
        });
    }

    protected void init (List<Contest> contests)
    {
        clear();

        final SmartTable create = new SmartTable("Create", 0, 10);
        int row = 0;
        create.setHTML(row++, 0, _msgs.contestsIntro(), 2, "Intro");
        create.setText(row, 0, _msgs.contestsId());
        create.setWidget(row++, 1, _contestId = MsoyUI.createTextBox("", 40, 40));

        create.setText(row, 0, _msgs.contestsStarts());
        create.setWidget(row++, 1, _startsContainer = new SimplePanel());

        create.setText(row, 0, _msgs.contestsEnds());
        create.setWidget(row++, 1, _endsContainer = new SimplePanel());

        create.setText(row, 0, _msgs.contestsIcon());
        create.setWidget(row++, 1, new Button(_msgs.contestsChange(), new ClickListener() {
            public void onClick (Widget source) {
                ImageChooserPopup.displayImageChooser(true, new MsoyCallback<MediaDesc>() {
                    public void onSuccess (MediaDesc photo) {
                        if (photo != null) {
                            _contestIcon = photo;
                            _previewContainer.setWidget(new ContestBox(createContest()));
                        }
                    }
                });
            }
        }));

        KeyboardListenerAdapter updateOnKeyPress = new KeyboardListenerAdapter() {
            public void onKeyPress (Widget sender, char keyCode, int modifiers) {
                _previewContainer.setWidget(new ContestBox(createContest()));
            }
        };

        create.setText(row, 0, _msgs.contestsName());
        create.setWidget(row++, 1, _name = new LimitedTextArea(255, 60, 2));
        _name.getTextArea().addKeyboardListener(updateOnKeyPress);

        create.setText(row, 0, _msgs.contestsBlurb());
        create.setWidget(row++, 1, _blurb = new LimitedTextArea(Contest.MAX_BLURB_LENGTH, 60, 5));
        _blurb.getTextArea().addKeyboardListener(updateOnKeyPress);

        create.setText(row, 0, _msgs.contestsStatus());
        create.setWidget(row++, 1, _status = new LimitedTextArea(255, 60, 2));
        _status.getTextArea().addKeyboardListener(updateOnKeyPress);

        create.setText(row, 0, _msgs.contestsPrizes());
        create.setWidget(row++, 1,
            _prizes = new LimitedTextArea(Contest.MAX_PRIZES_LENGTH, 60, 4));
        _prizes.getTextArea().addKeyboardListener(updateOnKeyPress);

        create.setText(row, 0, _msgs.contestsPastBlurb());
        create.setWidget(row++, 1, _pastBlurb = new LimitedTextArea(Contest.MAX_PASTBLURB_LENGTH,
            60, 4));
        _pastBlurb.getTextArea().addKeyboardListener(updateOnKeyPress);

        create.setText(row++, 0, _msgs.promoPreview());
        create.setWidget(row++, 0, _previewContainer = new SimplePanel(), 2, null);

        Button editButton = new Button(_msgs.contestsClearButton(), new ClickListener() {
            public void onClick (Widget sender) {
                setFormDefaults();
            }
        });
        _saveButton = new Button("", new ClickListener() {
            public void onClick (Widget sender) {
                publishContest(createContest());
            }
        });
        create.setWidget(row, 0, MsoyUI.createButtonPair(editButton, _saveButton), 2, null);
        create.getFlexCellFormatter().setHorizontalAlignment(row, 0, HasAlignment.ALIGN_RIGHT);

        // set default dates and button text
        setFormDefaults();
        add(new TongueBox(_msgs.contestsCreate(), create));

        // set up the list header
        int col = 0;
        _contestTable.setText(0, col++, _msgs.contestsId(), 1, "Header");
        _contestTable.setText(0, col++, _msgs.contestsIcon(), 1, "Header");
        _contestTable.setText(0, col++, _msgs.contestsName(), 1, "Header");
        _contestTable.setText(0, col++, _msgs.contestsStarts(), 1, "Header");
        _contestTable.setText(0, col++, _msgs.contestsEnds(), 1, "Header");

        // add the list contests
        for (Contest contest : contests) {
            setContestRow(_contestTable, contest, _contestTable.getRowCount());
        }
        add(new TongueBox(_msgs.contestsTitle(), _contestTable));
    }

    /**
     * Clear form text fields, default dates and set save button text to "Add"
     */
    protected void setFormDefaults ()
    {
        _contestId.setEnabled(true);
        _contestId.setText("");
        _contestIcon = null;

        // create new DatePickers because we can't set the time on existing ones (???)
        _startsContainer.setWidget(_starts = new DatePicker(new Date()));
        _starts.setTimeVisible(true);
        _starts.display();
        _endsContainer.setWidget(_ends = new DatePicker(DateUtil.toDate(THE_FUTURE)));
        _ends.setTimeVisible(true);
        _ends.display();

        _name.setText("");
        _blurb.setText("");
        _status.setText("");
        _prizes.setText("");
        _pastBlurb.setText("");
        _previewContainer.setWidget(new ContestBox(createContest()));

        _editingRow = -1;
        _saveButton.setText(_msgs.contestsAddButton());
    }

    /**
     * Add a contest to the list of contests or replace one already there
     */
    protected void setContestRow (SmartTable ptable, final Contest contest, final int row)
    {
        int col = 0;
        ptable.setText(row, col++, contest.contestId);
        if (contest.icon != null) {
            ptable.setWidget(row, col++, MediaUtil.createMediaView(
                contest.icon, MediaDesc.HALF_THUMBNAIL_SIZE));
        } else {
            ptable.setText(row, col++, "");
        }
        ptable.setWidget(row, col++, MsoyUI.createHTML(contest.name, null));
        ptable.setText(row, col++, MsoyUI.formatDateTime(contest.starts));
        ptable.setText(row, col++, MsoyUI.formatDateTime(contest.ends));

        Button edit = MsoyUI.createTinyButton("E", new ClickListener() {
            public void onClick (Widget sender) {
                _editingRow = row;
                editContest(contest);
            }
        });
        edit.setTitle(_msgs.contestsEditTip());
        edit.setWidth("14px");
        edit.setHeight("14px");
        ptable.setWidget(row, col++, edit);

        PushButton delete = MsoyUI.createCloseButton(null);
        delete.setTitle(_msgs.contestsDeleteTip());
        ptable.setWidget(row, col++, delete);
        new ClickCallback<Void>(delete, _msgs.contestsDeleteConfirm()) {
            protected boolean callService () {
                _adminsvc.deleteContest(contest.contestId, this);
                return true;
            }
            protected boolean gotResult (Void result) {
                _contestTable.removeRow(row);
                return false;
            }
        };
    }

    /**
     * Set the contest currently being edited.
     */
    protected void editContest (Contest contest)
    {
        _contestId.setText(contest.contestId);
        _contestId.setEnabled(false);
        _contestIcon = contest.icon;

        // create new DatePickers because we can't set the time on existing ones (???)
        _startsContainer.setWidget(_starts = new DatePicker(contest.starts));
        _starts.setTimeVisible(true);
        _starts.display();
        _endsContainer.setWidget(_ends = new DatePicker(contest.ends));
        _ends.setTimeVisible(true);
        _ends.display();

        _name.setText(contest.name);
        _blurb.setText(contest.blurb);
        _status.setText(contest.status);
        _prizes.setText(contest.prizes);
        _pastBlurb.setText(contest.pastBlurb);

        _previewContainer.setWidget(new ContestBox(createContest()));
        _saveButton.setText(_msgs.contestsUpdateButton());
    }

    /**
     * Build a new Contest object from text field contents
     */
    protected Contest createContest ()
    {
        Contest contest = new Contest();
        contest.contestId = _contestId.getText().trim();
        contest.name = _name.getText().trim();
        contest.blurb = _blurb.getText().trim();
        contest.status = _status.getText().trim();
        contest.prizes = _prizes.getText().trim();
        contest.pastBlurb = _pastBlurb.getText().trim();
        contest.icon = _contestIcon;
        contest.starts = _starts.getDate();
        contest.ends = _ends.getDate();
        return contest;
    }

    /**
     * Save a new contest, or update an old one. Any existing contest with the same conestId will
     * be overwritten.
     */
    protected void publishContest (final Contest contest)
    {
        if (contest.contestId.length() == 0) {
            return;
        }

        // creating a new contest
        if (_editingRow == -1) {
            System.out.println("creating contest: " + contest.contestId);
            _adminsvc.addContest(contest, new MsoyCallback<Void>() {
                public void onSuccess (Void result) {
                    setContestRow(_contestTable, contest, _contestTable.getRowCount());
                    setFormDefaults();
                }});

        // updating an existing contest
        } else {
            System.out.println("updating contest: " + contest.contestId);
            _adminsvc.updateContest(contest, new MsoyCallback<Void>() {
                public void onSuccess (Void result) {
                    setContestRow(_contestTable, contest, _editingRow);
                    setFormDefaults();
                }});
        }
    }

    protected SmartTable _contestTable = new SmartTable("Contests", 0, 10);

    protected TextBox _contestId;
    protected SimplePanel _startsContainer, _endsContainer;
    protected DatePicker _starts, _ends;
    protected LimitedTextArea _name, _blurb, _status, _prizes, _pastBlurb;
    protected MediaDesc _contestIcon;
    protected SimplePanel _previewContainer;
    protected Button _saveButton;

    /** The row currently being edited, or -1 */
    int _editingRow = -1;

    protected static final AdminMessages _msgs = GWT.create(AdminMessages.class);
    protected static final AdminServiceAsync _adminsvc = (AdminServiceAsync)
        ServiceUtil.bind(GWT.create(AdminService.class), AdminService.ENTRY_POINT);

    protected static final int[] THE_FUTURE = { 2099, 0, 1 };
}
