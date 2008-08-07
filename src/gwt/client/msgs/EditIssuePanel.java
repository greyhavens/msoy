//
// $Id$

package client.msgs;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.fora.gwt.ForumMessage;
import com.threerings.msoy.fora.gwt.Issue;
import com.threerings.msoy.fora.gwt.IssueService;
import com.threerings.msoy.fora.gwt.IssueServiceAsync;

import client.shell.Args;
import client.shell.CShell;
import client.shell.Pages;
import client.ui.MsoyUI;
import client.util.ClickCallback;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

/**
 * Displays an issue and allows it to be edited if the user is authorized.
 */
public class EditIssuePanel extends TableFooterPanel
{
    public EditIssuePanel (IssuePanel ipanel)
    {
        _ipanel = ipanel;
        buildPanel();
    }

    public EditIssuePanel (ThreadPanel tpanel, ForumMessage message)
    {
        _tpanel = tpanel;
        _message = message;
        buildPanel();

        createIssue();
        addMessage(message);
    }

    public void createIssue ()
    {
        _newIssue = true;
        _issue = new Issue();
        _issue.creator = new MemberName(CShell.creds.permaName, CShell.creds.getMemberId());
        fillEditPanel();
    }

    public void setIssue (Issue issue)
    {
        setIssue(issue, 0, 0);
    }

    public void setIssue (Issue issue, int messageId, int page)
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

    protected void buildPanel ()
    {
        _table = new SmartTable(0, 5);

        _table.setText(0, 0, _mmsgs.iType());
        _table.setText(1, 0, _mmsgs.iCreator());
        _table.setText(2, 0, _mmsgs.iOwner());
        _table.setText(3, 0, _mmsgs.iDescription());
        _table.setText(4, 0, _mmsgs.iState());
        _table.setText(5, 0, _mmsgs.iPriority());
        _table.setText(6, 0, _mmsgs.iCategory());
        _table.setText(7, 0, _mmsgs.iComment());

        addRow(_table);
    }

    protected void fillViewPanel ()
    {
        _table.setText(0, 1, IssueMsgs.typeMsg(_issue.type, _mmsgs));
        _table.setText(1, 1, _issue.creator.toString());
        _table.setText(2, 1, (_issue.owner == null ?
                    _mmsgs.iNoOwner() : _issue.owner.toString()));
        _table.setText(3, 1, _issue.description);
        _table.setText(4, 1, IssueMsgs.stateMsg(_issue.state, _mmsgs));
        _table.setText(5, 1, IssueMsgs.priorityMsg(_issue.priority, _mmsgs));
        _table.setText(6, 1, IssueMsgs.categoryMsg(_issue.category, _mmsgs));
        _table.setText(7, 1, _issue.closeComment);
        _messagesRow = 8;
        if (_messageId > 0) {
            Button assign = new Button(_mmsgs.assign());
            new ClickCallback<Void>(assign) {
                public boolean callService () {
                    _issuesvc.assignMessage(_issue.issueId, _messageId, this);
                    return true;
                }
                public boolean gotResult (Void result) {
                    Link.go(Pages.WHIRLEDS, Args.compose(new String[] {
                                "t", ""+_message.threadId, ""+_page, ""+_messageId }));
                    return false;
                }
            };
            _table.setWidget(0, _messagesRow++, assign);
        }
    }

    protected void fillEditPanel ()
    {
        int row = 0;
        _table.setWidget(row++, 1, _typeBox = new ListBox());
        for (int ii = 0; ii < Issue.TYPE_VALUES.length; ii++) {
            _typeBox.addItem(IssueMsgs.typeMsg(Issue.TYPE_VALUES[ii], _mmsgs));
            if (_issue.type == Issue.TYPE_VALUES[ii]) {
                _typeBox.setSelectedIndex(ii);
            }
        }
        _table.setText(row++, 1, _issue.creator.toString());
        _table.setWidget(row++, 1, _ownerBox = new ListBox());
        _ownerBox.addItem(_mmsgs.iNoOwner());
        _issuesvc.loadOwners(new MsoyCallback<List<MemberName>>() {
            public void onSuccess (List<MemberName> owners) {
                if (owners != null) {
                    setOwners(owners);
                }
            }
        });
        _table.setWidget(row++, 1, _description = MsoyUI.createTextArea(_issue.description, 50, 3));

        _table.setWidget(row++, 1, _stateBox = new ListBox());
        for (int ii = 0; ii < Issue.STATE_VALUES.length; ii++) {
            _stateBox.addItem(IssueMsgs.stateMsg(Issue.STATE_VALUES[ii], _mmsgs));
            if (_issue.state == Issue.STATE_VALUES[ii]) {
                _stateBox.setSelectedIndex(ii);
            }
        }

        _table.setWidget(row++, 1, _priorityBox = new ListBox());
        for (int ii = 0; ii < Issue.PRIORITY_VALUES.length; ii++) {
            _priorityBox.addItem(IssueMsgs.priorityMsg(Issue.PRIORITY_VALUES[ii], _mmsgs));
            if (_issue.priority == Issue.PRIORITY_VALUES[ii]) {
                _priorityBox.setSelectedIndex(ii);
            }
        }

        _table.setWidget(row++, 1, _categoryBox = new ListBox());
        for (int ii = 0; ii < Issue.CATEGORY_VALUES.length; ii++) {
            _categoryBox.addItem(IssueMsgs.categoryMsg(Issue.CATEGORY_VALUES[ii], _mmsgs));
            if (_issue.category == Issue.CATEGORY_VALUES[ii]) {
                _categoryBox.setSelectedIndex(ii);
            }
        }

        _table.setWidget(row++, 1, _comment = MsoyUI.createTextArea(_issue.closeComment, 50, 3));

        Button left, right;
        if (_tpanel != null) {
            left = new Button(_mmsgs.cancel(), new ClickListener() {
                public void onClick (Widget source) {
                    _tpanel.showMessages();
                }
            });
            right = new Button(_mmsgs.create());
            new ClickCallback<Issue>(right) {
                public boolean callService () {
                    return commitEdit(true, this);
                }
                public boolean gotResult (Issue result) {
                    _message.issueId = result.issueId;
                    _tpanel.showMessages(true);
                    return false;
                }
            };

        } else {
            left = new Button(_mmsgs.cancel(), new ClickListener() {
                public void onClick (Widget source) {
                    _ipanel.redisplayIssues();
                }
            });
            right = new Button(_newIssue ? _mmsgs.create() : _mmsgs.update());
            new ClickCallback<Issue>(right) {
                public boolean callService () {
                    return commitEdit(_newIssue, this);
                }
                public boolean gotResult (Issue result) {
                    _ipanel.redisplayIssues();
                    return false;
                }
            };
        }
        _table.getFlexCellFormatter().setHorizontalAlignment(row, 0, HasAlignment.ALIGN_RIGHT);
        _table.setWidget(row++, 0, MsoyUI.createButtonPair(left, right), 2, null);

        _messagesRow = row;
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
        for (ForumMessage message : messages) {
            addMessage(message);
        }
        if (_messageId > 0) {
            _message = messages.get(0);
        }
    }

    protected void addMessage (ForumMessage message)
    {
        _table.getFlexCellFormatter().setColSpan(_messagesRow, 0, 2);
        _table.setWidget(_messagesRow++, 0, new IssueMessagePanel(message));
    }

    protected boolean commitEdit (boolean create, ClickCallback<Issue> callback)
    {
        _issue.owner = (_ownerBox.getSelectedIndex() > 0) ?
                        _ownerNames.get(_ownerBox.getSelectedIndex() - 1) : null;
        _issue.description = _description.getText();
        if (_issue.description.length() == 0) {
            MsoyUI.error(_mmsgs.errINoDescription());
            return false;
        } else if (_issue.description.length() > Issue.MAX_DESC_LENGTH) {
            MsoyUI.error(_mmsgs.errIDescLong());
            return false;
        }
        _issue.state = Issue.STATE_VALUES[_stateBox.getSelectedIndex()];
        _issue.priority = Issue.PRIORITY_VALUES[_priorityBox.getSelectedIndex()];
        _issue.type = Issue.TYPE_VALUES[_typeBox.getSelectedIndex()];
        _issue.category = Issue.CATEGORY_VALUES[_categoryBox.getSelectedIndex()];
        if (_issue.state != Issue.STATE_OPEN) {
            _issue.closeComment = _comment.getText();
            if (_issue.closeComment.length() == 0) {
                MsoyUI.error(_mmsgs.errINoComment());
                return false;
            } else if (_issue.closeComment.length() > Issue.MAX_COMMENT_LENGTH) {
                MsoyUI.error(_mmsgs.errICommentLong());
                return false;
            } else if (_issue.owner == null ||
                    _issue.owner.getMemberId() != CShell.creds.getMemberId()) {
                MsoyUI.error(_mmsgs.errICloseOwner());
                return false;
            }
        }

        if (create) {
            _issuesvc.createIssue(_issue, (_message == null ? 0 : _message.messageId), callback);
        } else {
            _issuesvc.updateIssue(_issue, callback);
        }
        return true;
    }

    protected IssuePanel _ipanel;
    protected Issue _issue;
    protected ThreadPanel _tpanel;
    protected ForumMessage _message;
    protected int _messageId, _page;
    protected boolean _newIssue;

    protected SmartTable _table;
    protected ListBox _typeBox;
    protected ListBox _ownerBox;
    protected TextArea _description;
    protected ListBox _stateBox;
    protected ListBox _priorityBox;
    protected ListBox _categoryBox;
    protected TextArea _comment;
    protected List<MemberName> _ownerNames;
    protected Hyperlink _threadLink;

    protected int _messagesRow;

    protected static final MsgsMessages _mmsgs = (MsgsMessages)GWT.create(MsgsMessages.class);
    protected static final IssueServiceAsync _issuesvc = (IssueServiceAsync)
        ServiceUtil.bind(GWT.create(IssueService.class), IssueService.ENTRY_POINT);
}
