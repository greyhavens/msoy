//
// $Id$

package client.issues;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.fora.gwt.Issue;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.CShell;
import client.shell.Page;

public class IssuesPage extends Page
{
    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        String action = args.get(0, "");

        if (action.equals("mine")) {
            int type = args.get(1, Issue.TYPE_BUG);
            IssuePanel issues = new IssuePanel(_imodels);
            issues.displayOwnedIssues(type, Issue.STATE_OPEN, false);
            setContent(_msgs.myIssuesTitle(), issues);

        } else if (action.equals("create")) {
            setContent(_msgs.newIssue(), new EditIssuePanel(_imodels));

        } else if (action.equals("i")) {
            setContent(_msgs.viewIssue(), new EditIssuePanel(_imodels, args.get(1, 0)));

        } else { // action.equals("list") or no action
            int type = args.get(1, Issue.TYPE_BUG), state = args.get(2, Issue.STATE_OPEN);
            boolean refresh = args.get(3, "").equals("r");
            IssuePanel issues = new IssuePanel(_imodels);
            issues.displayIssues(type, state, refresh);
            setContent(_msgs.issuesTitle(), issues);
        }

//         } else if (page == Nav.ASSIGN && CShell.isSupport()) {
//             int messageId = args.get(1, 0), pageNum = args.get(2, 0);
//             IssuePanel issues = new IssuePanel(_imodels);
//             issues.displayAssignIssues(Issue.TYPE_BUG, messageId, pageNum);
//             setContent(_msgs.issuesTitle(), issues);

//         } else if (page == Nav.BUG || page == Nav.ASSIGN || page == Nav.OWNED) {

//         } else if (page == Nav.ASSIGNED) {
//             int messageId = args.get(1, 0), pageNum = args.get(2, 0), issueId = args.get(3, 0);
//             IssuePanel issues = new IssuePanel(_imodels);
//             if (CShell.isSupport()) {
//                 issues.displayIssue(issueId, 0, messageId, pageNum);
//             } else {
//                 issues.displayIssue(issueId, 0);
//             }
//             setContent(_msgs.issuesTitle(), issues);
    }

    @Override
    public Pages getPageId ()
    {
        return Pages.ISSUES;
    }

    protected IssueModels _imodels = new IssueModels();

    protected static final IssuesMessages _msgs = GWT.create(IssuesMessages.class);
}
