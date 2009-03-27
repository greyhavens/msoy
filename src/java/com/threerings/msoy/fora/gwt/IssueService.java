//
// $Id$

package com.threerings.msoy.fora.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.gwt.util.PagedResult;

import com.threerings.msoy.web.gwt.ServiceException;

import com.threerings.msoy.data.all.MemberName;

/**
 * Defines issue related services available to the GWT client.
 */
public interface IssueService extends RemoteService
{
    /** Provides results for {@link #loadIssues}. */
    public static class IssueResult extends PagedResult<Issue>
    {
        /** Returns true if we're able to manage issues. */
        public boolean isManager;
    }

    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/issuesvc";

    /**
     * Loads the specified issues.
     */
    IssueResult loadIssues (boolean open, int offset, int count, boolean needTotalCount)
        throws ServiceException;

    /**
     * Loads the specified issues, owned by the caller.
     */
    IssueResult loadOwnedIssues (boolean open, int offset, int count, boolean needTotalCount)
        throws ServiceException;

    /**
     * Loads an issue from an issueId.
     */
    Issue loadIssue (int issueId)
        throws ServiceException;

    /**
     * Loads a single forum message.
     */
    ForumMessage loadMessage (int messageId)
        throws ServiceException;

    /**
     * Loads a list of ForumMessage for an issueId.
     */
    List<ForumMessage> loadMessages (int issueId, int messageId)
        throws ServiceException;

    /**
     * Creates an issue.
     */
    Issue createIssue (Issue issue, int messageId)
        throws ServiceException;

    /**
     * Updates an issue.
     */
    Issue updateIssue (Issue issue)
        throws ServiceException;

    /**
     * Assigns a message to an issue.
     */
    void assignMessage (int issueId, int messageId)
        throws ServiceException;

    /**
     * Loads a list of possible issue owners.
     */
    List<MemberName> loadOwners ()
        throws ServiceException;
}
