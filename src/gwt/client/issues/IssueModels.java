//
// $Id$

package client.issues;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.fora.gwt.Issue;
import com.threerings.msoy.fora.gwt.IssueService;
import com.threerings.msoy.fora.gwt.IssueServiceAsync;

import client.shell.CShell;
import client.util.ServiceBackedDataModel;
import client.util.ServiceUtil;

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
        protected void onSuccess (
            IssueService.IssueResult result, AsyncCallback<List<Issue>> callback)
        {
            _isManager = result.isManager;
            for (Issue issue : result.issues) {
                mapIssue(issue);
            }
            super.onSuccess(result, callback);
        }

        @Override // from ServiceBackedDataModel
        protected void callFetchService (int start, int count, boolean needCount,
            AsyncCallback<IssueService.IssueResult> callback)
        {
            _issuesvc.loadIssues(_type, _state, start, count, needCount, callback);
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
        protected Map<Integer, Issue> _issues = new HashMap<Integer, Issue>();
    }

    /** A data model that provides owned issues. */
    public static class OwnedIssues extends Issues
    {
        public OwnedIssues (int type, int state)
        {
            super(type, state);
        }

        @Override // from Issues
        protected void callFetchService (int start, int count, boolean needCount,
            AsyncCallback<IssueService.IssueResult> callback)
        {
            _issuesvc.loadOwnedIssues(_type, _state, start, count, needCount, callback);
        }
    }

    /**
     * Clears out our cached issues.
     */
    public void flush ()
    {
        _issuesModel.clear();
        _ownedModel.clear();
    }

    /**
     * Returns, creating if necessary, a data model that provides all of the issues for the
     * specified type and state.
     */
    public Issues getIssues (int type, int state, boolean refresh)
    {
        Map<Integer, Issues> typeIssues = getTypeMap(type, _issuesModel);
        Issues issues;
        if (refresh || (issues = typeIssues.get(state)) == null) {
            CShell.log("Creating new model for " + type + " " + state);
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
        Map<Integer, OwnedIssues> typeIssues = getTypeMap(type, _ownedModel);
        OwnedIssues issues;
        if (refresh || (issues = typeIssues.get(state)) == null) {
            CShell.log("Creating owned new model for " + type + " " + state);
            typeIssues.put(state, issues = new OwnedIssues(type, state));
        }
        return issues;
    }

    /**
     * Returns a mapping of states to Issues/OwnedIssues for a certain type.
     */
    public <V extends Issues> Map<Integer, V> getTypeMap (
        int type, Map<Integer, Map<Integer, V>> model)
    {
        Map<Integer, V> typeIssues = model.get(type);
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
    protected <V extends Issues> Issue findIssue (int issueId, Map<Integer, Map<Integer, V>> map)
    {
        for (Map<Integer, V> typeIssues : map.values()) {
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
    protected Map<Integer, Map<Integer, Issues>> _issuesModel =
        new HashMap<Integer, Map<Integer, Issues>>();

    /** A cached OwnedIssues data model. */
    protected Map<Integer, Map<Integer, OwnedIssues>> _ownedModel =
        new HashMap<Integer, Map<Integer, OwnedIssues>>();

    protected static final IssueServiceAsync _issuesvc = (IssueServiceAsync)
        ServiceUtil.bind(GWT.create(IssueService.class), IssueService.ENTRY_POINT);
}
