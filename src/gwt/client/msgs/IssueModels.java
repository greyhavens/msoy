//
// $Id$

package client.msgs;

import java.util.Iterator;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.fora.data.Issue;
import com.threerings.msoy.web.client.IssueService;

import com.threerings.gwt.util.SimpleDataModel;

import client.util.HashIntMap;
import client.util.ServiceBackedDataModel;

/**
 * Various data models used by the issue services.
 */
public class IssueModels
{
    /** A data model that provides issues. */
    public static class Issues extends ServiceBackedDataModel
    {
        public Issues (int type, int state)
        {
            _type = type;
            _state = state;
        }

        /**
         * Looks up the specified issues in the ser of all issues ever fetched by this model.
         */
        public Issue getIssue (int issueId)
        {
            return (Issue)_issues.get(issueId);
        }

        // @Override // from ServieBackedDataModel
        public void prependItem (Object item) {
            super.prependItem(item);
            mapIssue((Issue)item);
        }

        // @Override // from ServiceBackedDataModel
        public void appendItem (Object item) {
            super.appendItem(item);
            mapIssue((Issue)item);
        }

        // @Override // from ServiceBackedDataModel
        public void onSuccess (Object result) {
            IssueService.IssueResult iresult = (IssueService.IssueResult)result;
            _isManager = iresult.isManager;
            for (int ii = 0; ii < iresult.issues.size(); ii++) {
                mapIssue((Issue)iresult.issues.get(ii));
            }
            super.onSuccess(result);
        }

        // @Override // from ServiceBackedDataModel
        protected void callFetchService (int start, int count, boolean needCount) {
            CMsgs.issuesvc.loadIssues(CMsgs.ident, _type, _state, start, count, needCount, this);
        }

        // @Override // from ServiceBackedDataModel
        protected int getCount (Object result) {
            return ((IssueService.IssueResult)result).issueCount;
        }

        // @Override // from ServiceBackedDataModel
        protected List getRows (Object result) {
            return ((IssueService.IssueResult)result).issues;
        }

        protected void mapIssue (Issue issue) {
            _issues.put(issue.issueId, issue);
        }

        protected int _type, _state;
        protected boolean _isManager;
        protected HashIntMap _issues = new HashIntMap();
    }

    /**
     * Notifies the cache that a new issue was posted.
     */
    public void newIssuePosted (Issue issue)
    {
        HashIntMap typeIssues = (HashIntMap)_issuesModel.get(issue.type);
        if (typeIssues == null) {
            return;
        }
        Issues imodel = (Issues)typeIssues.get(issue.state);
        if (imodel != null) {
            imodel.prependItem(issue);
        }
    }

    /**
     * Returns, creating if necessary, a data model that provides all of the issues for the
     * specified type and state.
     */
    public Issues getIssues (int type, int state, boolean refresh)
    {
        if (refresh) {
            return new Issues(type, state);
        }
        HashIntMap typeIssues = (HashIntMap)_issuesModel.get(type);
        if (typeIssues == null) {
            _issuesModel.put(type, typeIssues = new HashIntMap());
        }
        Issues issues = (Issues)typeIssues.get(state);
        if (issues == null) {
            typeIssues.put(state, issues = new Issues(type, state));
        }
        return issues;
    }

    /**
     * Locates the issue in question in the cache.  Returns null if the issue could not be found.
     */
    public Issue findIssue (int issueId)
    {
        for (Iterator iter = _issuesModel.values().iterator(); iter.hasNext(); ) {
            HashIntMap typeIssues = (HashIntMap)iter.next();
            for (Iterator iter2 = typeIssues.values().iterator(); iter2.hasNext(); ) {
                Issues model = (Issues)iter2.next();
                Issue issue = model.getIssue(issueId);
                if (issue != null) {
                    return issue;
                }
            }
        }
        return null;
    }

    /** A cached Issues data model. */
    protected HashIntMap _issuesModel = new HashIntMap();
}
