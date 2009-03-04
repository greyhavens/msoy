//
// $Id$

package client.issues;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.fora.gwt.ForumMessage;
import com.threerings.msoy.fora.gwt.Issue;
import com.threerings.msoy.fora.gwt.IssueService;
import com.threerings.msoy.fora.gwt.IssueServiceAsync;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.msgs.TableFooterPanel;
import client.msgs.ThreadPanel;
import client.shell.CShell;
import client.ui.LimitedTextArea;
import client.ui.MsoyUI;
import client.util.ClickCallback;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.NaviUtil;
import client.util.ServiceUtil;
import client.util.StringUtil;

/**
 * Displays an issue and allows it to be edited if the user is authorized.
 */
public class EditIssuePanel extends TableFooterPanel
{
    public static EditIssuePanel newForCreate (IssueModels models, int messageId)
    {
        EditIssuePanel panel = new EditIssuePanel(models);
        panel.createIssue(messageId);
        return panel;
    }

    public static EditIssuePanel newForEdit (IssueModels models, int issueId)
    {
        EditIssuePanel panel = new EditIssuePanel(models);
        panel.displayIssue(issueId);
        return panel;
    }

    public EditIssuePanel (IssueModels imodels)
    {
        setStyleName("editIssuePanel");
        _imodels = imodels;
        addRow(_table = new SmartTable(0, 5));
    }

    protected void createIssue (final int messageId)
    {
        _newIssue = true;
        _issue = new Issue();
        _issue.creator = new MemberName(CShell.creds.permaName, CShell.creds.getMemberId());
        fillEditPanel();

        if (messageId != 0) {
            _issuesvc.loadMessage(messageId, new MsoyCallback<ForumMessage>() {
                public void onSuccess (ForumMessage msg) {
                    if (msg != null) {
                        _messageId = messageId;
                        addMessage(msg);
                    }
                }
            });
        }
    }

    protected void displayIssue (int issueId)
    {
        Issue issue = _imodels.findIssue(issueId);
        if (issue != null) {
            setIssue(issue, 0, 0);

        } else {
            _issuesvc.loadIssue(issueId, new MsoyCallback<Issue>() {
                public void onSuccess (Issue issue) {
                    if (issue == null) {
                        MsoyUI.error(_msgs.errINotFound());
                    } else {
                        setIssue(issue, 0, 0);
                    }
                }
            });
        }
    }

    protected void setIssue (Issue issue, int messageId, int page)
    {
        _issue = issue;
        _messageId = messageId;
        _page = page;
        if (CShell.isSupport() && _issue.state == Issue.STATE_OPEN && messageId == 0) {
            fillEditPanel();
        } else {
            fillViewPanel();
        }
        _issuesvc.loadMessages(_issue.issueId, messageId, new MsoyCallback<List<ForumMessage>>() {
            public void onSuccess (List<ForumMessage> messages) {
                if (messages != null) {
                    setMessages(messages);
                }
            }
        });
    }

    protected void fillViewPanel ()
    {
        int row = 0;
        _table.setText(row, 0, _issue.summary, 2, "Title");
        _table.setText(row++, 1, IssueMsgs.typeMsg(_issue.type, _msgs), 1, null);

        _table.setText(row, 0, _msgs.iCreator() + ": " + _issue.creator);
        String otxt = (_issue.owner == null ? _msgs.iNoOwner() : _issue.owner.toString());
        _table.setText(row++, 1, _msgs.iOwner() + ": " + otxt);

        _table.setText(row, 0, _msgs.iState() + ": " + IssueMsgs.stateMsg(_issue.state, _msgs));
        _table.setText(
            row, 1, _msgs.iPriority() + ": " + IssueMsgs.priorityMsg(_issue.priority, _msgs));
        _table.setText(
            row++, 2, _msgs.iCategory() + ": " + IssueMsgs.categoryMsg(_issue.category, _msgs));

        if (!StringUtil.isBlank(_issue.description)) {
            _table.setText(row++, 0, _msgs.iDescription(), 3, "SubTitle");
            _table.setText(row++, 0, _issue.description, 3, null);
        }
        if (_issue.state != Issue.STATE_OPEN) {
            _table.setText(row++, 0, _msgs.iComment(), 3, "SubTitle");
            _table.setText(row++, 0, _issue.closeComment, 3, null);
        }

        if (_messageId > 0) {
            Button assign = new Button(_msgs.assign());
            new ClickCallback<Void>(assign) {
                @Override protected boolean callService () {
                    _issuesvc.assignMessage(_issue.issueId, _messageId, this);
                    return true;
                }
                @Override protected boolean gotResult (Void result) {
                    Link.go(Pages.GROUPS, Args.compose("t", _threadId, _page, _messageId));
                    return false;
                }
            };
            _table.setWidget(row++, 2, assign);
        }
    }

    protected void fillEditPanel ()
    {
        int row = 0;
        _table.setText(row, 0, _msgs.iType(), 1, "Label");
        _table.setWidget(row++, 1, _typeBox = new ListBox());
        for (int ii = 0; ii < Issue.TYPE_VALUES.length; ii++) {
            _typeBox.addItem(IssueMsgs.typeMsg(Issue.TYPE_VALUES[ii], _msgs));
            if (_issue.type == Issue.TYPE_VALUES[ii]) {
                _typeBox.setSelectedIndex(ii);
            }
        }

        _table.setText(row, 0, _msgs.iCreator(), 1, "Label");
        _table.setText(row++, 1, _issue.creator.toString());

        _table.setText(row, 0, _msgs.iOwner(), 1, "Label");
        _table.setWidget(row++, 1, _ownerBox = new ListBox());
        _ownerBox.addItem(_msgs.iNoOwner());
        _issuesvc.loadOwners(new MsoyCallback<List<MemberName>>() {
            public void onSuccess (List<MemberName> owners) {
                if (owners != null) {
                    setOwners(owners);
                }
            }
        });

        HorizontalPanel sbits = new HorizontalPanel();
        sbits.add(_stateBox = new ListBox());
        for (int ii = 0; ii < Issue.STATE_VALUES.length; ii++) {
            _stateBox.addItem(IssueMsgs.stateMsg(Issue.STATE_VALUES[ii], _msgs));
            if (_issue.state == Issue.STATE_VALUES[ii]) {
                _stateBox.setSelectedIndex(ii);
            }
        }
        if (_issue.issueId != 0) {
            sbits.add(WidgetUtil.makeShim(5, 5));
            Button closeFixed = new Button(_msgs.iCloseFixed());
            new CommitCallback(closeFixed) {
                protected byte getState () {
                    return Issue.STATE_RESOLVED;
                }
            };
            sbits.add(closeFixed);
            sbits.add(WidgetUtil.makeShim(5, 5));
            Button closeIgnored = new Button(_msgs.iCloseIgnored());
            new CommitCallback(closeIgnored) {
                protected byte getState () {
                    return Issue.STATE_IGNORED;
                }
            };
            sbits.add(closeIgnored);
        }
        _table.setText(row, 0, _msgs.iState(), 1, "Label");
        _table.setWidget(row++, 1, sbits);

        _table.setText(row, 0, _msgs.iPriority(), 1, "Label");
        _table.setWidget(row++, 1, _priorityBox = new ListBox());
        for (int ii = 0; ii < Issue.PRIORITY_VALUES.length; ii++) {
            _priorityBox.addItem(IssueMsgs.priorityMsg(Issue.PRIORITY_VALUES[ii], _msgs));
            if (_issue.priority == Issue.PRIORITY_VALUES[ii]) {
                _priorityBox.setSelectedIndex(ii);
            }
        }

        _table.setText(row, 0, _msgs.iCategory(), 1, "Label");
        _table.setWidget(row++, 1, _categoryBox = new ListBox());
        for (int ii = 0; ii < Issue.CATEGORY_VALUES.length; ii++) {
            _categoryBox.addItem(IssueMsgs.categoryMsg(Issue.CATEGORY_VALUES[ii], _msgs));
            if (_issue.category == Issue.CATEGORY_VALUES[ii]) {
                _categoryBox.setSelectedIndex(ii);
            }
        }

        _summary = MsoyUI.createTextBox(_issue.summary, Issue.MAX_SUMMARY_LENGTH, 60);
        _table.setText(row, 0, _msgs.iSummary(), 1, "Label");
        _table.setWidget(row++, 1, _summary);

        _table.setText(row, 0, _msgs.iDescription(), 1, "Label");
        _description = new LimitedTextArea(Issue.MAX_DESC_LENGTH, 60, 3);
        _description.setText(_issue.description);
        _table.setWidget(row++, 1, _description);

        _comment = new LimitedTextArea(Issue.MAX_COMMENT_LENGTH, 60, 3);
        if (_issue.issueId != 0) {
            _table.setText(row, 0, _msgs.iComment(), 1, "Label");
            _table.setWidget(row++, 1, _comment);
            _comment.setText(_issue.closeComment);
        }

        Button cancel = new Button(_msgs.cancel(), NaviUtil.onGoBack());
        Button submit = new Button(_newIssue ? _msgs.create() : _msgs.update());
        new CommitCallback(submit);
        _table.getFlexCellFormatter().setHorizontalAlignment(row, 0, HasAlignment.ALIGN_RIGHT);
        _table.setWidget(row++, 0, MsoyUI.createButtonPair(cancel, submit), 2, null);
    }

    protected void setOwners (List<MemberName> owners)
    {
        _ownerNames = owners;
        for (int ii = 0, nn = owners.size(); ii < nn; ii++) {
            MemberName name = owners.get(ii);
            _ownerBox.addItem(name.toString());
            if (name.equals(_issue.owner)) {
                _ownerBox.setSelectedIndex(ii + 1);
            }
        }
    }

    protected void setMessages (List<ForumMessage> messages)
    {
        _table.addText(_msgs.assocMessages(), 3, "SubTitle");
        for (ForumMessage message : messages) {
            addMessage(message);
        }
        if (_messageId > 0) {
            _threadId = messages.get(0).threadId;
        }
    }

    protected void addMessage (ForumMessage message)
    {
        _table.addWidget(new IssueMessagePanel(message), 3, null);
    }

    protected class CommitCallback extends ClickCallback<Issue>
    {
        public CommitCallback (Button button) {
            super(button);
        }

        @Override protected boolean callService () {
            _issue.summary = _summary.getText().trim();
            if (_issue.summary.length() == 0) {
                MsoyUI.error(_msgs.errINoSummary());
                return false;
            }

            _issue.description = _description.getText().trim();
            _issue.state = getState();
            _issue.priority = Issue.PRIORITY_VALUES[_priorityBox.getSelectedIndex()];
            _issue.type = Issue.TYPE_VALUES[_typeBox.getSelectedIndex()];
            _issue.category = Issue.CATEGORY_VALUES[_categoryBox.getSelectedIndex()];
            if (_ownerBox.getSelectedIndex() > 0) {
                _issue.owner = _ownerNames.get(_ownerBox.getSelectedIndex() - 1);
            } else if (_issue.issueId != 0) {
                // updating an ownerless issue makes it yours
                _issue.owner = CShell.creds.name;
            }

            if (_issue.state != Issue.STATE_OPEN) {
                _issue.closeComment = _comment.getText();
            }

            if (_issue.issueId == 0) {
                _issuesvc.createIssue(_issue, _messageId, this);
            } else {
                _issuesvc.updateIssue(_issue, this);
            }
            return true;
        }

        @Override protected boolean gotResult (Issue result) {
            if (_imodels != null) {
                _imodels.flush();
            }
            History.back();
            return false;
        }

        protected byte getState () {
            return Issue.STATE_VALUES[_stateBox.getSelectedIndex()];
        }
    }

    protected IssueModels _imodels;
    protected Issue _issue;
    protected int _threadId, _messageId, _page;
    protected boolean _newIssue;

    protected SmartTable _table;
    protected ListBox _typeBox;
    protected ListBox _ownerBox;
    protected TextBox _summary;
    protected LimitedTextArea _description;
    protected ListBox _stateBox;
    protected ListBox _priorityBox;
    protected ListBox _categoryBox;
    protected LimitedTextArea _comment;
    protected List<MemberName> _ownerNames;
    protected Hyperlink _threadLink;

    protected static final IssuesMessages _msgs = (IssuesMessages)GWT.create(IssuesMessages.class);
    protected static final IssueServiceAsync _issuesvc = (IssueServiceAsync)
        ServiceUtil.bind(GWT.create(IssueService.class), IssueService.ENTRY_POINT);
}
