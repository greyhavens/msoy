//
// $Id$

package client.msgs;

import java.util.List;

import client.util.ClickCallback;
import client.util.MsoyUI;

import client.shell.Page;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.fora.data.ForumMessage;
import com.threerings.msoy.fora.data.ForumThread;
import com.threerings.msoy.fora.data.Issue;

import client.shell.Application;
import client.shell.Page;

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
        _issue = new Issue();
        _issue.creator = new MemberName(CMsgs.creds.permaName, CMsgs.creds.getMemberId());
        fillEditPanel();
        addMessage(message);
    }

    public void setIssue (Issue issue)
    {
        _issue = issue;
        if (CMsgs.isAdmin() && _issue.state == Issue.STATE_OPEN) {
            fillEditPanel();
        } else {
            fillViewPanel();
        }
        CMsgs.issuesvc.loadMessages(CMsgs.ident, _issue.issueId, new AsyncCallback() {
            public void onSuccess (Object result) {
                if (result != null) {
                    setMessages((List)result);
                }
            }
            public void onFailure (Throwable caught) {
                MsoyUI.error(CMsgs.serverError(caught));
            }
        });
    }

    protected void buildPanel ()
    {
        _table = new SmartTable(0, 5);

        _table.setText(0, 0, CMsgs.mmsgs.IType());
        _table.setText(1, 0, CMsgs.mmsgs.ICreator());
        _table.setText(2, 0, CMsgs.mmsgs.IOwner());
        _table.setText(3, 0, CMsgs.mmsgs.IDescription());
        _table.setText(4, 0, CMsgs.mmsgs.IState());
        _table.setText(5, 0, CMsgs.mmsgs.IPriority());
        _table.setText(6, 0, CMsgs.mmsgs.ICategory());
        _table.setText(7, 0, CMsgs.mmsgs.IComment());

        addRow(_table);
    }

    protected void fillViewPanel ()
    {
        int row = 0;
        _table.setText(0, 1, IssueMsgs.typeMsg(_issue.type, CMsgs.mmsgs));
        _table.setText(1, 1, _issue.creator.toString());
        _table.setText(2, 1, (_issue.owner == null ?
                    CMsgs.mmsgs.INoOwner() : _issue.owner.toString()));
        _table.setText(3, 1, _issue.description);
        _table.setText(4, 1, IssueMsgs.stateMsg(_issue.state, CMsgs.mmsgs));
        _table.setText(5, 1, IssueMsgs.priorityMsg(_issue.priority, CMsgs.mmsgs));
        _table.setText(6, 1, IssueMsgs.categoryMsg(_issue.category, CMsgs.mmsgs));
        _table.setText(7, 1, _issue.closeComment);
        _messagesRow = 8;
    }

    protected void fillEditPanel ()
    {
        int row = 0;
        _table.setWidget(row++, 1, _typeBox = new ListBox());
        for (int ii = 0; ii < Issue.TYPE_VALUES.length; ii++) {
            _typeBox.addItem(IssueMsgs.typeMsg(Issue.TYPE_VALUES[ii], CMsgs.mmsgs));
            if (_issue.type == Issue.TYPE_VALUES[ii]) {
                _typeBox.setSelectedIndex(ii);
            }
        }
        _table.setText(row++, 1, _issue.creator.toString());
        _table.setWidget(row++, 1, _ownerBox = new ListBox());
        _ownerBox.addItem(CMsgs.mmsgs.INoOwner());
        CMsgs.issuesvc.loadOwners(CMsgs.ident, new AsyncCallback() {
            public void onSuccess (Object result) {
                if (result != null) {
                    setOwners((List)result);
                }
            }
            public void onFailure (Throwable caught) {
                MsoyUI.error(CMsgs.serverError(caught));
            }
        });
        _description = MsoyUI.createTextBox(_issue.description, Issue.MAX_DESC_LENGTH, 30);
        _table.setWidget(row++, 1, _description);

        _table.setWidget(row++, 1, _stateBox = new ListBox());
        for (int ii = 0; ii < Issue.STATE_VALUES.length; ii++) {
            _stateBox.addItem(IssueMsgs.stateMsg(Issue.STATE_VALUES[ii], CMsgs.mmsgs));
            if (_issue.state == Issue.STATE_VALUES[ii]) {
                _stateBox.setSelectedIndex(ii);
            }
        }

        _table.setWidget(row++, 1, _priorityBox = new ListBox());
        for (int ii = 0; ii < Issue.PRIORITY_VALUES.length; ii++) {
            _priorityBox.addItem(IssueMsgs.priorityMsg(Issue.PRIORITY_VALUES[ii], CMsgs.mmsgs));
            if (_issue.priority == Issue.PRIORITY_VALUES[ii]) {
                _priorityBox.setSelectedIndex(ii);
            }
        }

        _table.setWidget(row++, 1, _categoryBox = new ListBox());
        for (int ii = 0; ii < Issue.CATEGORY_VALUES.length; ii++) {
            _categoryBox.addItem(IssueMsgs.categoryMsg(Issue.CATEGORY_VALUES[ii], CMsgs.mmsgs));
            if (_issue.category == Issue.CATEGORY_VALUES[ii]) {
                _categoryBox.setSelectedIndex(ii);
            }
        }

        _comment = MsoyUI.createTextBox(_issue.closeComment, Issue.MAX_COMMENT_LENGTH, 30);
        _table.setWidget(row++, 1, _comment);

        HorizontalPanel buttons = new HorizontalPanel();
        buttons.setSpacing(5);
        _table.getFlexCellFormatter().setColSpan(row, 0, 2);
        _table.getFlexCellFormatter().setHorizontalAlignment(row, 0, HasAlignment.ALIGN_RIGHT);
        _table.setWidget(row++, 0, buttons);

        if (_tpanel != null) {
            buttons.add(new Button(CMsgs.mmsgs.cancel(), new ClickListener() {
                public void onClick (Widget source) {
                    _tpanel.showMessages();
                }
            }));
            Button create = new Button(CMsgs.mmsgs.create());
            new ClickCallback(create) {
                public boolean callService () {
                    return commitEdit(true, this);
                }
                public boolean gotResult (Object result) {
                    _message.issueId = ((Issue)result).issueId;
                    _tpanel.showMessages(true);
                    return false;
                }
            };
            buttons.add(create);
        } else {
            Button update = new Button(CMsgs.mmsgs.update());
            new ClickCallback(update) {
                public boolean callService () {
                    return commitEdit(false, this);
                }
                public boolean gotResult (Object result) {
                    _ipanel.displayIssues(true);
                    return false;
                }
            };
            buttons.add(update);
        }
        _messagesRow = row;
    }

    protected void setOwners (List owners)
    {
        _ownerNames = owners;
        for (int ii = 0, nn = owners.size(); ii < nn; ii++) {
            MemberName name = (MemberName)owners.get(ii);
            _ownerBox.addItem(name.toString());
            if (name.equals(_issue.owner)) {
                _ownerBox.setSelectedIndex(ii);
            }
        }
    }

    protected void setMessages (List messages)
    {
        for (int ii = 0, nn = messages.size(); ii < nn; ii++) {
            addMessage((ForumMessage)messages.get(ii));
        }
    }

    protected void addMessage (ForumMessage message)
    {
        _table.getFlexCellFormatter().setColSpan(_messagesRow, 0, 2);
        _table.setWidget(_messagesRow++, 0, new SimpleMessagePanel(message));
    }

    protected boolean commitEdit (boolean create, ClickCallback callback)
    {
        _issue.owner = (_ownerBox.getSelectedIndex() > 0) ?
            (MemberName)_ownerNames.get(_ownerBox.getSelectedIndex() - 1) : null;
        _issue.description = _description.getText();
        if (_issue.description.length() == 0) {
            MsoyUI.error(CMsgs.mmsgs.errINoDescription());
            return false;
        }
        _issue.state = Issue.STATE_VALUES[_stateBox.getSelectedIndex()];
        _issue.priority = Issue.PRIORITY_VALUES[_priorityBox.getSelectedIndex()];
        _issue.type = Issue.TYPE_VALUES[_typeBox.getSelectedIndex()];
        _issue.category = Issue.CATEGORY_VALUES[_categoryBox.getSelectedIndex()];
        if (_issue.state != Issue.STATE_OPEN) {
            _issue.closeComment = _comment.getText();
            if (_issue.closeComment.length() == 0) {
                MsoyUI.error(CMsgs.mmsgs.errINoComment());
                return false;
            } else if (_issue.owner == null ||
                    _issue.owner.getMemberId() != CMsgs.creds.getMemberId()) {
                MsoyUI.error(CMsgs.mmsgs.errICloseOwner());
                return false;
            }
        }

        if (create) {
            CMsgs.issuesvc.createIssue(CMsgs.ident, _issue, _message.messageId, callback);
        } else {
            CMsgs.issuesvc.updateIssue(CMsgs.ident, _issue, callback);
        }
        return true;
    }

    protected IssuePanel _ipanel;
    protected Issue _issue;
    protected ThreadPanel _tpanel;
    protected ForumMessage _message;

    protected SmartTable _table;
    protected ListBox _typeBox;
    protected ListBox _ownerBox;
    protected TextBox _description;
    protected ListBox _stateBox;
    protected ListBox _priorityBox;
    protected ListBox _categoryBox;
    protected TextBox _comment;
    protected List _ownerNames;
    protected Hyperlink _threadLink;

    protected int _messagesRow;
}
