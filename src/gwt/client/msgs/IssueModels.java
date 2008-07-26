//
// $Id$

package client.msgs;

import java.util.List;
import java.util.HashMap;

import com.threerings.msoy.fora.gwt.Issue;
import com.threerings.msoy.web.client.IssueService;

import client.util.ServiceBackedDataModel;

/**
 * Various data models used by the issue services.
 */
public class IssueModels
{
    /** A data model that provides issues. */
    public static class Issues extends ServiceBackedDataModel<Issue, IssueService.IssueResult>
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
            return _issues.get(issueId);
        }

        @Override // from ServieBackedDataModel
        public void prependItem (Issue item) {
            super.prependItem(item);
            mapIssue(item);
        }

        @Override // from ServiceBackedDataModel
        public void appendItem (Issue item) {
            super.appendItem(item);
            mapIssue(item);
        }

        @Override // from ServiceBackedDataModel
        public void onSuccess (IssueService.IssueResult result) {
            _isManager = result.isManager;
            for (Issue issue : result.issues) {
                mapIssue(issue);
            }
            super.onSuccess(result);
        }

        @Override // from ServiceBackedDataModel
        protected void callFetchService (int start, int count, boolean needCount) {
            CMsgs.issuesvc.loadIssues(CMsgs.ident, _type, _state, start, count, needCount, this);
        }

        @Override // from ServiceBackedDataModel
        protected int getCount (IssueService.IssueResult result) {
            return result.issueCount;
        }

        @Override // from ServiceBackedDataModel
        protected List<Issue> getRows (IssueService.IssueResult result) {
            return result.issues;
        }

        protected void mapIssue (Issue issue) {
            _issues.put(issue.issueId, issue);
        }

        protected int _type, _state;
        protected boolean _isManager;
        protected HashMap<Integer, Issue> _issues = new HashMap<Integer, Issue>();
    }

    /** A data model that provides owned issues. */
    public static class OwnedIssues extends Issues
    {
        public OwnedIssues (int type, int state)
        {
            super(type, state);
        }

        @Override // from ServiceBackedDataModel
        protected void callFetchService (int start, int count, boolean needCount) {
            CMsgs.issuesvc.loadOwnedIssues(
                    CMsgs.ident, _type, _state, start, count, needCount, this);
        }
    }

    /**
     * Notifies the cache that a new issue was posted.
     */
    public void newIssuePosted (Issue issue)
    {
        HashMap<Integer, Issues> typeIssues = _issuesModel.get(issue.type);
        if (typeIssues == null) {
            return;
        }
        Issues imodel = typeIssues.get(issue.state);
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
        HashMap<Integer, Issues> typeIssues = getTypeMap(type, _issuesModel);
        Issues issues;
        if (refresh || (issues = typeIssues.get(state)) == null) {
            typeIssues.put(state, issues = new Issues(type, state));
        }
        return issues;
    }

    /**
     * Returns, creating if necessary, a data model that provides all of the issues for the
     * specified type and state.
     */
    public OwnedIssues getOwnedIssues (int type, int state, boolean refresh)
    {
        HashMap<Integer, OwnedIssues> typeIssues = getTypeMap(type, _ownedModel);
        OwnedIssues issues;
        if (refresh || (issues = (OwnedIssues)typeIssues.get(state)) == null) {
            typeIssues.put(state, issues = new OwnedIssues(type, state));
        }
        return issues;
    }

    /**
     * Returns a mapping of states to Issues/OwnedIssues for a certain type.
     */
    public <V extends Issues> HashMap<Integer, V> getTypeMap (
        int type, HashMap<Integer, HashMap<Integer, V>> model)
    {
        HashMap<Integer, V> typeIssues = model.get(type);
        if (typeIssues == null) {
            model.put(type, typeIssues = new HashMap<Integer, V>());
        }
        return typeIssues;
    }

    /**
     * Locates the issue in question in the cache.  Returns null if the issue could not be found.
     */
    public Issue findIssue (int issueId)
    {
        Issue issue = findIssue(issueId, _ownedModel);
        return issue != null ? issue : findIssue(issueId, _issuesModel);
    }

    /**
     * Finds an issue in a specified model.
     */
    protected <V extends Issues> Issue findIssue (
        int issueId, HashMap<Integer, HashMap<Integer, V>> map)
    {
        for (HashMap<Integer, V> typeIssues : map.values()) {
            for (V model : typeIssues.values()) {
                Issue issue = model.getIssue(issueId);
                if (issue != null) {
                    return issue;
                }
            }
        }
        return null;
    }

    /** A cached Issues data model. */
    protected HashMap<Integer, HashMap<Integer, Issues>> _issuesModel =
        new HashMap<Integer, HashMap<Integer, Issues>>();

    /** A cached OwnedIssues data model. */
    protected HashMap<Integer, HashMap<Integer, OwnedIssues>> _ownedModel =
        new HashMap<Integer, HashMap<Integer, OwnedIssues>>();
}
