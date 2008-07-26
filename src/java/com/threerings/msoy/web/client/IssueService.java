//
// $Id$

package com.threerings.msoy.web.client;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.fora.gwt.Issue;
import com.threerings.msoy.fora.gwt.ForumMessage;

import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

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
    public IssueResult loadIssues (WebIdent ident, int type, int state, int offset, int count,
                                   boolean needTotalCount)
        throws ServiceException;

    /**
     * Loads issues of specific types, states owned by the user.
     */
    public IssueResult loadOwnedIssues (WebIdent ident, int type, int state, int offset, int count,
                                        boolean needTotalCount)
        throws ServiceException;

    /**
     * Loads an issue from an issueId.
     */
    public Issue loadIssue (WebIdent ident, int issueId)
        throws ServiceException;

    /**
     * Loads a list of ForumMessage for an issueId.
     */
    public List<ForumMessage> loadMessages (WebIdent ident, int issueId, int messageId)
        throws ServiceException;

    /**
     * Creates an issue.
     */
    public Issue createIssue (WebIdent ident, Issue issue, int messageId)
        throws ServiceException;

    /**
     * Updates an issue.
     */
    public Issue updateIssue (WebIdent ident, Issue issue)
        throws ServiceException;

    /**
     * Assigns a message to an issue.
     */
    public void assignMessage (WebIdent ident, int issueId, int messageId)
        throws ServiceException;

    /**
     * Loads a list of possible issue owners.
     */
    public List<MemberName> loadOwners (WebIdent ident)
        throws ServiceException;
}
