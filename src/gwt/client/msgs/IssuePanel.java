//
// $Id$

package client.msgs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.fora.gwt.Issue;
import com.threerings.msoy.fora.gwt.IssueService;
import com.threerings.msoy.fora.gwt.IssueServiceAsync;

import client.shell.CShell;
import client.shell.Page;
import client.ui.MsoyUI;
import client.util.Link;
import client.util.ServiceUtil;

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
        setContents(createHeader(_owned ? _mmsgs.myIssueListHeader() :
                    _mmsgs.issueListHeader(), true), issues);
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
        setContents(createHeader(_mmsgs.assignIssueListHeader(), false), issues);
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
            setContents(_mmsgs.viewIssue(issue.description), _ipanel, true);

        } else {
            _issuesvc.loadIssue(CShell.ident, issueId, new AsyncCallback<Issue>() {
                public void onSuccess (Issue issue) {
                    _state = issue.state;
                    _type = issue.type;
                    _ipanel.setIssue(issue, messageId, page);
                    updateTitle(_mmsgs.viewIssue(issue.description));
                }
                public void onFailure (Throwable caught) {
                    MsoyUI.error(_mmsgs.errINotFound());
                }
            });
            setContents(_mmsgs.viewIssue("..."), _ipanel, true);
        }
    }

    public void createIssue ()
    {
        if (_ipanel == null) {
            _ipanel = new EditIssuePanel(this);
        }
        _ipanel.createIssue();
        setContents(_mmsgs.newIssue(), _ipanel, true);
    }

    protected FlexTable createHeader (String title, boolean states)
    {
        SmartTable header = new SmartTable(0, 2);
        header.setWidth("100%");

        int col = 0;
        header.setText(0, col++, _mmsgs.iType());
        ListBox typeBox = new ListBox();
        for (int ii = 0; ii < Issue.TYPE_VALUES.length; ii++) {
            typeBox.addItem(IssueMsgs.typeMsg(Issue.TYPE_VALUES[ii], _mmsgs));
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
            header.setText(0, col++, _mmsgs.iState());
            ListBox stateBox = new ListBox();
            for (int ii = 0; ii < Issue.STATE_VALUES.length; ii++) {
                stateBox.addItem(IssueMsgs.stateMsg(Issue.STATE_VALUES[ii], _mmsgs));
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

        header.setText(0, col++, _mmsgs.iPriority(), 1, "Column");
        header.setText(0, col++, _mmsgs.iCategory(), 1, "Column");
        header.setText(0, col++, _mmsgs.iOwner(), 1, "Column");
        String htext = (_state == Issue.STATE_OPEN) ?
            _mmsgs.iCreator() : _mmsgs.iCloser();
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

    protected static final MsgsMessages _mmsgs = (MsgsMessages)GWT.create(MsgsMessages.class);
    protected static final IssueServiceAsync _issuesvc = (IssueServiceAsync)
        ServiceUtil.bind(GWT.create(IssueService.class), IssueService.ENTRY_POINT);
}
