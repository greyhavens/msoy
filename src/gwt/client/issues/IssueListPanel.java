//
// $Id$

package client.issues;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;

import com.threerings.msoy.fora.gwt.Issue;
import com.threerings.msoy.web.gwt.Pages;

import client.images.msgs.MsgsImages;
import client.shell.CShell;
import client.ui.MsoyUI;
import client.util.Link;

/**
 * Displays a list of issues.
 */
public class IssueListPanel extends PagedGrid<Issue>
{
    public IssueListPanel (boolean open, boolean owned, IssueModels imodels)
    {
        super(open ? OPEN_ISSUES_PER_PAGE : CLOSED_ISSUES_PER_PAGE, 1, NAV_ON_BOTTOM);
        setWidth("100%");

        _open = open;
        _owned = owned;
        _imodels = imodels;
        _linkPrefix = "i_";
        _create.setVisible(CShell.isSupport() && open);

        refreshModel();
    }

//     public void displayAssignIssues (boolean open, IssueModels imodels, int messageId, int page)
//     {
//         _linkPrefix = "a_" + messageId + "_" + page + "_";
//         setModel(imodels.getIssues(open, false), 0);
//     }

    protected void refreshModel ()
    {
        if (_owned) {
            setModel(_imodels.getOwnedIssues(_open, false), 0);
        } else {
            setModel(_imodels.getIssues(_open, false), 0);
        }
    }

    @Override // from PagedGrid
    protected Widget createWidget (Issue item)
    {
        return new IssueSummaryPanel(item);
    }

    @Override // from PagedGrid
    protected Widget createEmptyContents ()
    {
        return MsoyUI.createHTML(_msgs.noIssues(), "Empty");
    }

    @Override // from PagedGrid
    protected String getEmptyMessage ()
    {
        return _msgs.noIssues();
    }

    @Override // from PagedGrid
    protected boolean displayNavi (int items)
    {
        return true; // we always show our navigation for consistency
    }

    @Override // from PagedGrid
    protected void addCustomControls (FlexTable controls)
    {
        super.addCustomControls(controls);

        // add a button for refreshing the issue list
        _refresh = new Button(_msgs.ilpRefresh(), new ClickListener() {
            public void onClick (Widget sender) {
                refreshModel();
            }
        });
        controls.setWidget(0, 0, _refresh);

        // add a button for creating a new issue (might later get hidden)
        _create = new Button(_msgs.newIssue(), Link.createListener(Pages.ISSUES, "create"));
        controls.setWidget(0, 1, _create);
    }

    @Override // from PagedGrid
    protected void displayResults (int start, int count, List<Issue> list)
    {
        super.displayResults(start, count, list);
        _refresh.setVisible(true);
        _refresh.setEnabled(true);
    }

    protected class IssueSummaryPanel extends FlexTable
    {
        public IssueSummaryPanel (final Issue issue)
        {
            setStyleName("issueSummaryPanel");
            setCellPadding(0);
            setCellSpacing(0);

            addStyleName("ispPriority" + issue.priority);

            int col = 0;
            setWidget(0, col, (issue.type == Issue.TYPE_BUG) ?
                      _mimgs.new_issue().createImage() : // a little bug
                      _mimgs.edit_post().createImage()); // a pencil
            getFlexCellFormatter().setStyleName(0, col++, "Type");

            String summary = issue.summary;
            // TEMP: until we migrate descrips to summaries
            if (summary == null || summary.length() == 0) {
                summary = "<no summary>";
            }
            Widget toIssue = Link.create(summary, Pages.ISSUES, _linkPrefix + issue.issueId);
            setWidget(0, col, toIssue);
            getFlexCellFormatter().setStyleName(0, col++, "Description");

            if (_open) {
                setText(0, col, (issue.owner == null ? "" : issue.owner.toString()));
            } else {
                setText(0, col, IssueMsgs.stateMsg(issue));
            }
            getFlexCellFormatter().setStyleName(0, col++, "Owner");

            FlowPanel created = new FlowPanel();
            Widget actor;
            if (issue.state == Issue.STATE_OPEN) {
                created.add(new Label(MsoyUI.formatDate(issue.createdTime)));
                actor = Link.memberView(issue.creator);
            } else {
                if (issue.closedTime != null) {
                    created.add(new Label(MsoyUI.formatDate(issue.closedTime)));
                }
                actor = Link.memberView(issue.owner);
            }
            created.add(actor);
            setWidget(0, col, created);
            getFlexCellFormatter().setStyleName(0, col++, "Created");
        }
    }

    protected IssueModels _imodels;
    protected boolean _open, _owned;
    protected String _linkPrefix;

    protected Button _create, _refresh;

    protected static final IssuesMessages _msgs = (IssuesMessages)GWT.create(IssuesMessages.class);
    protected static final MsgsImages _mimgs = (MsgsImages)GWT.create(MsgsImages.class);

    protected static final int OPEN_ISSUES_PER_PAGE = 20;
    protected static final int CLOSED_ISSUES_PER_PAGE = 10;
}
