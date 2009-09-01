//
// $Id$

package client.issues;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.fora.gwt.ForumMessage;
import com.threerings.msoy.fora.gwt.Issue;
import com.threerings.msoy.fora.gwt.IssueService;
import com.threerings.msoy.fora.gwt.IssueServiceAsync;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.CShell;
import client.ui.LimitedTextArea;
import client.ui.MsoyUI;
import client.util.ClickCallback;
import client.util.Link;
import client.util.InfoCallback;
import client.util.NaviUtil;
import client.util.StringUtil;

/**
 * Displays an issue and allows it to be edited if the user is authorized.
 */
public class EditIssuePanel extends SmartTable
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
        super("editIssuePanel", 0, 5);
        _imodels = imodels;
    }

    protected void createIssue (final int messageId)
    {
        _newIssue = true;
        _issue = new Issue();
        _issue.creator = new MemberName(CShell.creds.permaName, CShell.creds.getMemberId());
        fillEditPanel();

        if (messageId != 0) {
            _issuesvc.loadMessage(messageId, new InfoCallback<ForumMessage>() {
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
            _issuesvc.loadIssue(issueId, new InfoCallback<Issue>() {
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
        _issuesvc.loadMessages(_issue.issueId, messageId, new InfoCallback<List<ForumMessage>>() {
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
        setText(row++, 0, _issue.summary, 2, "Title");
        setText(row++, 0, IssueMsgs.typeMsg(_issue.type), 2);

        setText(row, 0, _msgs.iCreator(), 1, "Label");
        setText(row++, 1, ""+_issue.creator);

        setText(row, 0, _msgs.iState(), 1, "Label");
        setText(row++, 1, IssueMsgs.stateMsg(_issue.state));

        setText(row, 0, _msgs.iOwner(), 1, "Label");
        setText(row++, 1, (_issue.owner == null) ?
                       _msgs.iNoOwner() : _issue.owner.toString());

        setText(row, 0, _msgs.iPriority(), 1, "Label");
        setText(row++, 1, IssueMsgs.priorityMsg(_issue.priority));

//         setText(row, 0, _msgs.iCategory(), 1, "Label");
//         setText(row++, 1, IssueMsgs.categoryMsg(_issue.category));

        if (!StringUtil.isBlank(_issue.description)) {
            setText(row++, 0, _msgs.iDescription(), 3, "SubTitle");
            setText(row++, 0, _issue.description, 3);
        }
        if (_issue.state != Issue.STATE_OPEN) {
            setText(row++, 0, _msgs.iComment(), 3, "SubTitle");
            setText(row++, 0, StringUtil.getOr(_issue.closeComment, _msgs.noComment()), 3);

            if (CShell.isSupport()) {
                Button reopen = new Button(_msgs.iReopen());
                new ClickCallback<Void>(reopen) {
                    @Override protected boolean callService () {
                        String newDescription = _issue.description;
                        if (_issue.closeComment != null && !_issue.closeComment.trim().isEmpty()) {
                            newDescription += "\n" + _msgs.oldCloseComment(_issue.closeComment);
                        }
                        if (_issue.owner == null) {
                            // reopening an unowned issue (weird) makes it yours
                            _issue.owner = CShell.creds.name;
                        }
                        _issuesvc.reopenIssue(_issue.issueId, newDescription, this);
                        return true;
                    }
                    @Override protected boolean gotResult (Void nothing) {
                        if (_imodels != null) {
                            _imodels.flush();
                        }
                        History.back();
                        return false;
                    }
                };
                setWidget(row++, 0, reopen);
            }

        }

        if (_messageId > 0) {
            Button assign = new Button(_msgs.assign());
            new ClickCallback<Void>(assign) {
                @Override protected boolean callService () {
                    _issuesvc.assignMessage(_issue.issueId, _messageId, this);
                    return true;
                }
                @Override protected boolean gotResult (Void result) {
                    Link.go(Pages.GROUPS, "t", _threadId, _page, _messageId);
                    return false;
                }
            };
            setWidget(row++, 2, assign);
        }
    }

    protected void fillEditPanel ()
    {
        int row = 0;
        if (!StringUtil.isBlank(_issue.summary)) {
            setText(row++, 0, _issue.summary, 2, "Title");
        }

        setText(row, 0, _msgs.iCreator(), 1, "Label");
        setText(row++, 1, _issue.creator.toString());

        setText(row, 0, _msgs.iType(), 1, "Label");
        setWidget(row++, 1, _typeBox = new ListBox());
        for (int ii = 0; ii < Issue.TYPE_VALUES.length; ii++) {
            _typeBox.addItem(IssueMsgs.typeMsg(Issue.TYPE_VALUES[ii]));
            if (_issue.type == Issue.TYPE_VALUES[ii]) {
                _typeBox.setSelectedIndex(ii);
            }
        }

        setText(row, 0, _msgs.iOwner(), 1, "Label");
        setWidget(row++, 1, _ownerBox = new ListBox());
        _ownerBox.addItem(_msgs.iNoOwner());
        if (_issue.owner != null) {
            List<MemberName> tempNames = new ArrayList<MemberName>();
            tempNames.add(_issue.owner);
            setOwners(tempNames);
        }
        _issuesvc.loadOwners(new InfoCallback<List<MemberName>>() {
            public void onSuccess (List<MemberName> owners) {
                if (owners != null) {
                    setOwners(owners);
                }
            }
        });

        setText(row, 0, _msgs.iPriority(), 1, "Label");
        setWidget(row++, 1, _priorityBox = new ListBox());
        for (int ii = 0; ii < Issue.PRIORITY_VALUES.length; ii++) {
            _priorityBox.addItem(IssueMsgs.priorityMsg(Issue.PRIORITY_VALUES[ii]));
            if (_issue.priority == Issue.PRIORITY_VALUES[ii]) {
                _priorityBox.setSelectedIndex(ii);
            }
        }

//         setText(row, 0, _msgs.iCategory(), 1, "Label");
//         setWidget(row++, 1, _categoryBox = new ListBox());
//         for (int ii = 0; ii < Issue.CATEGORY_VALUES.length; ii++) {
//             _categoryBox.addItem(IssueMsgs.categoryMsg(Issue.CATEGORY_VALUES[ii]));
//             if (_issue.category == Issue.CATEGORY_VALUES[ii]) {
//                 _categoryBox.setSelectedIndex(ii);
//             }
//         }

        _summary = MsoyUI.createTextBox(_issue.summary, Issue.MAX_SUMMARY_LENGTH, -1);
        setText(row, 0, _msgs.iSummary(), 1, "Label");
        setWidget(row++, 1, _summary);

        setText(row, 0, _msgs.iDescription(), 1, "Label");
        _description = new LimitedTextArea(Issue.MAX_DESC_LENGTH, -1, 15);
        _description.setText(_issue.description);
        setWidget(row++, 1, _description);

        _comment = new LimitedTextArea(Issue.MAX_COMMENT_LENGTH, -1, 3);
        if (_issue.issueId != 0) {
            setText(row, 0, _msgs.iComment(), 1, "Label");
            setWidget(row++, 1, _comment);
            _comment.setText(_issue.closeComment);
        }

        HorizontalPanel qbuts = new HorizontalPanel();
        qbuts.add(new Button(_msgs.cancel(), NaviUtil.onGoBack()));
        qbuts.add(WidgetUtil.makeShim(5, 5));
        Button submit = new Button(_newIssue ? _msgs.create() : _msgs.update());
        new CommitCallback(submit);
        qbuts.add(submit);
        if (_issue.issueId != 0) {
            qbuts.add(WidgetUtil.makeShim(20, 5));
            for (final byte state : Issue.STATE_VALUES) {
                if (state == Issue.STATE_OPEN) {
                    continue;
                }
                qbuts.add(WidgetUtil.makeShim(5, 5));
                Button quickClose = new Button(IssueMsgs.stateMsg(state));
                new CommitCallback(quickClose) {
                    protected byte getState () {
                        return state;
                    }
                };
                qbuts.add(quickClose);
            }
        }
        getFlexCellFormatter().setHorizontalAlignment(row, 0, HasAlignment.ALIGN_RIGHT);
        setWidget(row++, 0, qbuts, 2);
    }

    protected void setOwners (List<MemberName> owners)
    {
        while (_ownerBox.getItemCount() > 1) {
            _ownerBox.removeItem(1);
        }

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
        if (messages.size() > 0) {
            addText(_msgs.assocMessages(), 3, "SubTitle");
        }
        for (ForumMessage message : messages) {
            addMessage(message);
        }
        if (_messageId > 0) {
            _threadId = messages.get(0).threadId;
        }
    }

    protected void addMessage (ForumMessage message)
    {
        addWidget(new IssueMessagePanel(message), 3);
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
//             _issue.category = Issue.CATEGORY_VALUES[_categoryBox.getSelectedIndex()];
            if (_ownerBox.getSelectedIndex() > 0) {
                _issue.owner = _ownerNames.get(_ownerBox.getSelectedIndex() - 1);
            } else if (_issue.owner != null) {
                // if issue was previously assigned, this is a deliberately unassignment
                _issue.owner = null;
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
            return Issue.STATE_OPEN;
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
    protected ListBox _priorityBox;
//     protected ListBox _categoryBox;
    protected LimitedTextArea _comment;
    protected List<MemberName> _ownerNames;
    protected Hyperlink _threadLink;

    protected static final IssuesMessages _msgs = (IssuesMessages)GWT.create(IssuesMessages.class);
    protected static final IssueServiceAsync _issuesvc = GWT.create(IssueService.class);
}
