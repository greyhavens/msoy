//
// $Id$

package client.issues;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.ServiceUtil;

import com.threerings.msoy.fora.gwt.IssueService;
import com.threerings.msoy.fora.gwt.IssueServiceAsync;

/**
 * Displays issues.
 */
public class IssuePanel extends FlowPanel
{
    public IssuePanel (IssueModels imodels)
    {
        setStyleName("issuePanel");
        _imodels = imodels;
    }

    public void displayIssues ()
    {
        displayIssues(false);
    }

    public void displayOwnedIssues ()
    {
        displayIssues(true);
    }

    public void displayAssignIssues (int messageId, int page)
    {
//         _state = Issue.STATE_OPEN;
//         IssueListPanel issues = new IssueListPanel(this);
//         issues.displayAssignIssues(_state, _imodels, messageId, page);
//         setContents(createHeader(_msgs.assignIssueListHeader(), false), issues);
    }

    protected void displayIssues (boolean owned)
    {
        add(createHeader(owned ? _msgs.myOpenHeader() : _msgs.openHeader(), true));
        add(new IssueListPanel(true, owned, _imodels));
        add(WidgetUtil.makeShim(10, 10));
        add(createHeader(owned ? _msgs.myClosedHeader() : _msgs.closedHeader(), false));
        add(new IssueListPanel(false, owned, _imodels));
    }

    protected Widget createHeader (String title, boolean open)
    {
        SmartTable header = new SmartTable("Header", 0, 2);
        header.setWidth("100%");
        header.setText(0, 0, title);
        header.setText(0, 1, open ? _msgs.iOwner() : _msgs.iState(), 1, "Column");
        header.setText(0, 2, open ? _msgs.iCreated() : _msgs.iClosed(), 1, "Created");
        return header;
    }

    /** Our issue model cache. */
    protected IssueModels _imodels;

    protected static final IssuesMessages _msgs = (IssuesMessages)GWT.create(IssuesMessages.class);
    protected static final IssueServiceAsync _issuesvc = (IssueServiceAsync)
        ServiceUtil.bind(GWT.create(IssueService.class), IssueService.ENTRY_POINT);
}
