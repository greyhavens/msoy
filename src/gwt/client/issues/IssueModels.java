//
// $Id$

package client.issues;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;


import com.threerings.msoy.fora.gwt.Issue;
import com.threerings.msoy.fora.gwt.IssueService;
import com.threerings.msoy.fora.gwt.IssueServiceAsync;

import client.util.MsoyPagedServiceDataModel;

/**
 * Various data models used by the issue services.
 */
public class IssueModels
{
    /** A data model that provides issues. */
    public static class Issues extends MsoyPagedServiceDataModel<Issue, IssueService.IssueResult>
    {
        public Issues (boolean open)
        {
            _open = open;
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
            for (Issue issue : result.page) {
                mapIssue(issue);
            }
            super.onSuccess(result, callback);
        }

        @Override // from ServiceBackedDataModel
        protected void callFetchService (int start, int count, boolean needCount,
            AsyncCallback<IssueService.IssueResult> callback)
        {
            _issuesvc.loadIssues(_open, start, count, needCount, callback);
        }

        protected void mapIssue (Issue issue) {
            _issues.put(issue.issueId, issue);
        }

        protected boolean _open;
        protected boolean _isManager;
        protected Map<Integer, Issue> _issues = Maps.newHashMap();
    }

    /** A data model that provides owned issues. */
    public static class OwnedIssues extends Issues
    {
        public OwnedIssues (boolean open)
        {
            super(open);
        }

        @Override // from Issues
        protected void callFetchService (int start, int count, boolean needCount,
            AsyncCallback<IssueService.IssueResult> callback)
        {
            _issuesvc.loadOwnedIssues(_open, start, count, needCount, callback);
        }
    }

    /**
     * Clears out our cached issues.
     */
    public void flush ()
    {
        _imodels.clear();
        _omodels.clear();
    }

    /**
     * Returns, creating if necessary, a data model that provides all of the issues for the
     * specified state.
     */
    public Issues getIssues (boolean open, boolean refresh)
    {
        Issues issues;
        if (refresh || (issues = _imodels.get(open)) == null) {
            _imodels.put(open, issues = new Issues(open));
        }
        return issues;
    }

    /**
     * Returns, creating if necessary, a data model that provides all of the issues for the
     * specified state.
     */
    public OwnedIssues getOwnedIssues (boolean open, boolean refresh)
    {
        OwnedIssues issues;
        if (refresh || (issues = _omodels.get(open)) == null) {
            _omodels.put(open, issues = new OwnedIssues(open));
        }
        return issues;
    }

    /**
     * Locates the issue in question in the cache.  Returns null if the issue could not be found.
     */
    public Issue findIssue (int issueId)
    {
        Issue issue = findIssue(issueId, _omodels);
        return issue != null ? issue : findIssue(issueId, _imodels);
    }

    /**
     * Finds an issue in a specified model.
     */
    protected <V extends Issues> Issue findIssue (int issueId, Map<Boolean, V> map)
    {
        for (V model : map.values()) {
            Issue issue = model.getIssue(issueId);
            if (issue != null) {
                return issue;
            }
        }
        return null;
    }

    /**
     * A cached Issues data model.
     */
    protected Map<Boolean, Issues> _imodels = Maps.newHashMap();

    /**
     * A cached OwnedIssues data model.
     */
    protected Map<Boolean, OwnedIssues> _omodels = Maps.newHashMap();

    protected static final IssueServiceAsync _issuesvc = GWT.create(IssueService.class);
}
