//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.fora.data.Issue;

import com.threerings.msoy.web.data.WebIdent;

/**
 * The asynchronous (client-side) version of {@link IssueService}.
 */
public interface IssueServiceAsync
{
    /**
     * The asynchronous version of {@link IssueService#loadIssues}.
     */
    public void loadIssues (WebIdent ident, int type, int state, int offset, int count,
                            boolean needTotalCount, AsyncCallback callback);

    /**
     * The asynchronous version of {@link IssueService#loadOwnedIssues}.
     */
    public void loadOwnedIssues (WebIdent ident, int type, int state, int offset, int count,
                                 boolean needTotalCount, AsyncCallback callback);

    /**
     * The asynchronous version of {@link IssueServie#loadIssue}.
     */
    public void loadIssue (WebIdent ident, int issueId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link IssueServie#loadMessages}.
     */
    public void loadMessages (WebIdent ident, int issueId, int messageId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link IssueService#createIssue}.
     */
    public void createIssue (
        WebIdent ident, Issue issue, int messageId, AsyncCallback<Issue> callback);

    /**
     * The asynchronous version of {@link IssueService#updateIssue}.
     */
    public void updateIssue (WebIdent ident, Issue issue, AsyncCallback<Issue> callback);

    /**
     * The asynchronous version of {@link IssueService#assignMessage}.
     */
    public void assignMessage (
        WebIdent ident, int issueId, int messageId, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link IssueService#loadOwners}.
     */
    public void loadOwners (WebIdent ident, AsyncCallback callback);
}
