//
// $Id$

package client.msgs;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.fora.gwt.Issue;

import client.shell.Page;
import client.util.Link;
import client.util.MsoyUI;

/**
 * Displays issues.
 */
public class IssuePanel extends TitledListPanel
{
    public IssuePanel (IssueModels imodels)
    {
        _imodels = imodels;
    }

    public void displayIssues (int type, int state, boolean refresh)
    {
        _type = type;
        _state = state;
        displayIssues(refresh);
    }

    public void displayIssues (boolean refresh)
    {
        IssueListPanel issues = new IssueListPanel(this);
        issues.displayIssues(_type, _state, _owned, _imodels, refresh);
        setContents(createHeader(_owned ? CMsgs.mmsgs.myIssueListHeader() :
                    CMsgs.mmsgs.issueListHeader(), true), issues);
    }

    public void displayOwnedIssues (int type, int state, boolean refresh)
    {
        _owned = true;
        displayIssues(type, state, refresh);
    }

    public void displayAssignIssues (int type, int messageId, int page)
    {
        _type = type;
        _state = Issue.STATE_OPEN;
        IssueListPanel issues = new IssueListPanel(this);
        issues.displayAssignIssues(_type, _state, _imodels, messageId, page);
        setContents(createHeader(CMsgs.mmsgs.assignIssueListHeader(), false), issues);
    }

    public void redisplayIssues ()
    {
        Link.go(Page.WHIRLEDS, (_owned ? "owned_" : "b_") + _type + "_" + _state);
    }

    public void displayIssue (int issueId, int owned)
    {
        displayIssue(issueId, owned, 0, 0);
    }

    public void displayIssue (int issueId, int owned, final int messageId, final int page)
    {
        if (_ipanel == null) {
            _ipanel = new EditIssuePanel(this);
        }
        _owned = owned > 0;

        Issue issue = _imodels.findIssue(issueId);
        if (issue != null) {
            _state = issue.state;
            _type = issue.type;
            _ipanel.setIssue(issue, messageId, page);
            setContents(CMsgs.mmsgs.viewIssue(issue.description), _ipanel, true);

        } else {
            CMsgs.issuesvc.loadIssue(CMsgs.ident, issueId, new AsyncCallback<Issue>() {
                public void onSuccess (Issue issue) {
                    _state = issue.state;
                    _type = issue.type;
                    _ipanel.setIssue(issue, messageId, page);
                    updateTitle(CMsgs.mmsgs.viewIssue(issue.description));
                }
                public void onFailure (Throwable caught) {
                    MsoyUI.error(CMsgs.mmsgs.errINotFound());
                }
            });
            setContents(CMsgs.mmsgs.viewIssue("..."), _ipanel, true);
        }
    }

    public void createIssue ()
    {
        if (_ipanel == null) {
            _ipanel = new EditIssuePanel(this);
        }
        _ipanel.createIssue();
        setContents(CMsgs.mmsgs.newIssue(), _ipanel, true);
    }

    protected FlexTable createHeader (String title, boolean states)
    {
        SmartTable header = new SmartTable(0, 2);
        header.setWidth("100%");

        int col = 0;
        header.setText(0, col++, CMsgs.mmsgs.iType());
        ListBox typeBox = new ListBox();
        for (int ii = 0; ii < Issue.TYPE_VALUES.length; ii++) {
            typeBox.addItem(IssueMsgs.typeMsg(Issue.TYPE_VALUES[ii], CMsgs.mmsgs));
            if (Issue.TYPE_VALUES[ii] == _type) {
                typeBox.setSelectedIndex(ii);
            }
        }
        typeBox.addChangeListener(new ChangeListener() {
            public void onChange (Widget sender) {
                displayIssues(
                    Issue.TYPE_VALUES[((ListBox)sender).getSelectedIndex()], _state, false);
            }
        });
        header.setWidget(0, col++, typeBox);

        if (states) {
            header.setText(0, col++, CMsgs.mmsgs.iState());
            ListBox stateBox = new ListBox();
            for (int ii = 0; ii < Issue.STATE_VALUES.length; ii++) {
                stateBox.addItem(IssueMsgs.stateMsg(Issue.STATE_VALUES[ii], CMsgs.mmsgs));
                if (Issue.STATE_VALUES[ii] == _state) {
                    stateBox.setSelectedIndex(ii);
                }
            }
            stateBox.addChangeListener(new ChangeListener() {
                public void onChange (Widget sender) {
                    displayIssues(
                        _type, Issue.STATE_VALUES[((ListBox)sender).getSelectedIndex()], false);
                }
            });
            header.setWidget(0, col++, stateBox);
        }

        header.setText(0, col++, CMsgs.mmsgs.iPriority(), 1, "Column");
        header.setText(0, col++, CMsgs.mmsgs.iCategory(), 1, "Column");
        header.setText(0, col++, CMsgs.mmsgs.iOwner(), 1, "Column");
        String htext = (_state == Issue.STATE_OPEN) ?
            CMsgs.mmsgs.iCreator() : CMsgs.mmsgs.iCloser();
        header.setText(0, col++, htext, 1, "Created");

        return header;
    }

    /** Our issue model cache. */
    protected IssueModels _imodels;

    /** Our current state and type being displayed. */
    protected int _state, _type, _page;

    /** If we're only showing owned issues. */
    protected boolean _owned;

    /** The issue viewing/editing panel. */
    protected EditIssuePanel _ipanel;
}
