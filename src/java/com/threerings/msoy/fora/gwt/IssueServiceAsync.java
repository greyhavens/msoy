//
// $Id$

package com.threerings.msoy.fora.gwt;

import java.util.List;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.threerings.msoy.data.all.MemberName;

/**
 * Provides the asynchronous version of {@link IssueService}.
 */
public interface IssueServiceAsync
{
    /**
     * The async version of {@link IssueService#loadMessages}.
     */
    void loadMessages (int issueId, int messageId, AsyncCallback<List<ForumMessage>> callback);

    /**
     * The async version of {@link IssueService#loadIssues}.
     */
    void loadIssues (boolean open, int offset, int count, boolean needTotalCount, AsyncCallback<IssueService.IssueResult> callback);

    /**
     * The async version of {@link IssueService#loadOwnedIssues}.
     */
    void loadOwnedIssues (boolean open, int offset, int count, boolean needTotalCount, AsyncCallback<IssueService.IssueResult> callback);

    /**
     * The async version of {@link IssueService#loadIssue}.
     */
    void loadIssue (int issueId, AsyncCallback<Issue> callback);

    /**
     * The async version of {@link IssueService#loadMessage}.
     */
    void loadMessage (int messageId, AsyncCallback<ForumMessage> callback);

    /**
     * The async version of {@link IssueService#createIssue}.
     */
    void createIssue (Issue issue, int messageId, AsyncCallback<Issue> callback);

    /**
     * The async version of {@link IssueService#reopenIssue}.
     */
    void reopenIssue (int issueId, String newDescription, AsyncCallback<Void> callback);

    /**
     * The async version of {@link IssueService#updateIssue}.
     */
    void updateIssue (Issue issue, AsyncCallback<Issue> callback);

    /**
     * The async version of {@link IssueService#assignMessage}.
     */
    void assignMessage (int issueId, int messageId, AsyncCallback<Void> callback);

    /**
     * The async version of {@link IssueService#loadOwners}.
     */
    void loadOwners (AsyncCallback<List<MemberName>> callback);
}
