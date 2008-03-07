//
// $Id$

package client.msgs;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.fora.data.Issue;

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
                    CMsgs.mmsgs.issueListHeader()), issues);
    }

    public void displayOwnedIssues (int type, int state, boolean refresh)
    {
        _owned = true;
        displayIssues(type, state, refresh);
    }

    public void displayIssue (int issueId)
    {
        if (_ipanel == null) {
            _ipanel = new EditIssuePanel(this);
        }
        Issue issue = _imodels.findIssue(issueId);

        if (issue != null) {
            _ipanel.setIssue(issue);
            setContents(CMsgs.mmsgs.viewIssue(issue.description), _ipanel, true);
        } else {
            CMsgs.issuesvc.loadIssue(CMsgs.ident, issueId, new AsyncCallback() {
                public void onSuccess (Object result) {
                    _ipanel.setIssue((Issue)result);
                    updateTitle(CMsgs.mmsgs.viewIssue(((Issue)result).description));
                }
                public void onFailure (Throwable caught) {
                    MsoyUI.error(CMsgs.mmsgs.errINotFound());
                }
            });
            setContents(CMsgs.mmsgs.viewIssue("..."), _ipanel, true);
        }
    }

    protected FlexTable createHeader (String title)
    {
        FlexTable header = new FlexTable();
        header.setCellSpacing(0);
        header.setCellPadding(0);
        header.setWidth("100%");
        header.setText(0, 0, title);
        header.getFlexCellFormatter().setStyleName(0, 0, "Title");
        header.getFlexCellFormatter().setRowSpan(0, 0, 2);

        HorizontalPanel filter = new HorizontalPanel();
        filter.setSpacing(5);
        filter.add(new Label(CMsgs.mmsgs.IType()));
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
        filter.add(typeBox);

        filter.add(new Label(CMsgs.mmsgs.IState()));
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
        filter.add(stateBox);
        header.setWidget(0, 1, filter);
        header.getFlexCellFormatter().setStyleName(0, 1, "Filter");
        header.getFlexCellFormatter().setColSpan(0, 1, 4);

        header.setText(1, 0, CMsgs.mmsgs.IPriority());
        header.getFlexCellFormatter().setStyleName(1, 0, "Column");
        header.setText(1, 1, CMsgs.mmsgs.ICategory());
        header.getFlexCellFormatter().setStyleName(1, 1, "Column");
        header.setText(1, 2, CMsgs.mmsgs.IOwner());
        header.getFlexCellFormatter().setStyleName(1, 2, "Column");
        if (_state == Issue.STATE_OPEN) {
            header.setText(1, 3, CMsgs.mmsgs.ICreator());
        } else {
            header.setText(1, 3, CMsgs.mmsgs.ICloser());
        }
        header.getFlexCellFormatter().setStyleName(1, 3, "Created");

        return header;
    }

    /** Our issue model cache. */
    protected IssueModels _imodels;

    /** Our current state and type being displayed. */
    protected int _state, _type;

    /** If we're only showing owned issues. */
    protected boolean _owned;

    /** The issue viewing/editing panel. */
    protected EditIssuePanel _ipanel;
}
