//
// $Id$

package com.threerings.msoy.fora.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.web.data.ServiceException;

import com.threerings.msoy.data.all.MemberName;

/**
 * Defines issue related services available to the GWT client.
 */
public interface IssueService extends RemoteService
{
    /** Provides results for {@link #loadIssues}. */
    public static class IssueResult implements IsSerializable
    {
        /** The total count of issues. */
        public int issueCount;

        /** Returns true if we're able to manage issues. */
        public boolean isManager;

        /** The range of issues that were requested. */
        public List<Issue> issues;
    }

    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/issuesvc";

    /**
     * Loads issues of specific types, states.
     */
    IssueResult loadIssues (int type, int state, int offset, int count, boolean needTotalCount)
        throws ServiceException;

    /**
     * Loads issues of specific types, states owned by the user.
     */
    IssueResult loadOwnedIssues (int type, int state, int offset, int count, boolean needTotalCount)
        throws ServiceException;

    /**
     * Loads an issue from an issueId.
     */
    Issue loadIssue (int issueId)
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
