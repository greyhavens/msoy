//
// $Id$

package com.threerings.msoy.web.client;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.fora.gwt.Issue;
import com.threerings.msoy.fora.gwt.ForumMessage;

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
                            boolean needTotalCount,
                            AsyncCallback<IssueService.IssueResult> callback);

    /**
     * The asynchronous version of {@link IssueService#loadOwnedIssues}.
     */
    public void loadOwnedIssues (WebIdent ident, int type, int state, int offset, int count,
                                 boolean needTotalCount,
                                 AsyncCallback<IssueService.IssueResult> callback);

    /**
     * The asynchronous version of {@link IssueServie#loadIssue}.
     */
    void loadIssue (WebIdent ident, int issueId, AsyncCallback<Issue> callback);

    /**
     * The asynchronous version of {@link IssueServie#loadMessages}.
     */
    public void loadMessages (WebIdent ident, int issueId, int messageId,
                              AsyncCallback<List<ForumMessage>> callback);

    /**
     * The asynchronous version of {@link IssueService#createIssue}.
     */
    public void createIssue (WebIdent ident, Issue issue, int messageId,
                             AsyncCallback<Issue> callback);

    /**
     * The asynchronous version of {@link IssueService#updateIssue}.
     */
    void updateIssue (WebIdent ident, Issue issue, AsyncCallback<Issue> callback);

    /**
     * The asynchronous version of {@link IssueService#assignMessage}.
     */
    public void assignMessage (WebIdent ident, int issueId, int messageId,
                               AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link IssueService#loadOwners}.
     */
    void loadOwners (WebIdent ident, AsyncCallback<List<MemberName>> callback);
}
