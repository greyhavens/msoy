//
// $Id$

package client.issues;

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

import client.msgs.TitledListPanel;
import client.shell.CShell;
import client.ui.MsoyUI;
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
        setContents(createHeader(_owned ? _msgs.myIssueListHeader() :
                    _msgs.issueListHeader(), true), issues);
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
        setContents(createHeader(_msgs.assignIssueListHeader(), false), issues);
    }

    public void issueUpdated (boolean newIssue, Issue issue)
    {
        CShell.log("Issue updated " + newIssue + " " + issue);
        // since only admins update issues, we just flush our cached data when issues are updated
        // and reload everything from the database
        _imodels.flush();
        redisplayIssues();
    }

    public void redisplayIssues ()
    {
        if (_owned) {
            displayOwnedIssues(_type, _state, false);
        } else {
            displayIssues(_type, _state, false);
        }
        // Link.go(Pages.GROUPS, (_owned ? "owned_" : "b_") + _type + "_" + _state);
    }

    protected FlexTable createHeader (String title, boolean states)
    {
        SmartTable header = new SmartTable(0, 2);
        header.setWidth("100%");

        int col = 0;
        header.setText(0, col++, _msgs.iType());
        ListBox typeBox = new ListBox();
        for (int ii = 0; ii < Issue.TYPE_VALUES.length; ii++) {
            typeBox.addItem(IssueMsgs.typeMsg(Issue.TYPE_VALUES[ii], _msgs));
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
            header.setText(0, col++, _msgs.iState());
            ListBox stateBox = new ListBox();
            for (int ii = 0; ii < Issue.STATE_VALUES.length; ii++) {
                stateBox.addItem(IssueMsgs.stateMsg(Issue.STATE_VALUES[ii], _msgs));
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

        header.setText(0, col++, _msgs.iPriority(), 1, "Column");
        header.setText(0, col++, _msgs.iCategory(), 1, "Column");
        header.setText(0, col++, _msgs.iOwner(), 1, "Column");
        String htext = (_state == Issue.STATE_OPEN) ? _msgs.iCreated() : _msgs.iClosed();
        header.setText(0, col++, htext, 1, "Created");

        return header;
    }

    /** Our issue model cache. */
    protected IssueModels _imodels;

    /** Our current state and type being displayed. */
    protected int _state, _type, _page;

    /** If we're only showing owned issues. */
    protected boolean _owned;

    protected static final IssuesMessages _msgs = (IssuesMessages)GWT.create(IssuesMessages.class);
    protected static final IssueServiceAsync _issuesvc = (IssueServiceAsync)
        ServiceUtil.bind(GWT.create(IssueService.class), IssueService.ENTRY_POINT);
}
