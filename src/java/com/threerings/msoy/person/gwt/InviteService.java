//
// $Id$

package com.threerings.msoy.person.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.web.gwt.EmailContact;
import com.threerings.msoy.web.gwt.ServiceException;

/**
 * Handles invitation-related functionality.
 */
public interface InviteService extends RemoteService
{
    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/invitesvc";

    /**
     * Loads up e-mail addresses from a user's webmail account.
     */
    List<EmailContact> getWebMailAddresses (String email, String password, boolean skipFriends)
        throws ServiceException;

    /**
     * Return the invitation details for the given ident.
     */
    MemberInvites getInvitationsStatus ()
        throws ServiceException;

    /**
     * Send out some of this person's available invites.
     *
     * @param anonymous if true, the invitations will not be from the caller but will be
     * anonymous. This is only allowed for admin callers.
     */
    InvitationResults sendInvites (List<EmailContact> addresses, String fromName,
                                   String customMessage, boolean anonymous)
        throws ServiceException;

    /**
     * Send out invitations to a game. This is a little bit different from the normal invite
     * sending. We don't count the sender's invites and we automatically use the display name
     * as the from field on the email.
     */
    InvitationResults sendGameInvites (
        List<EmailContact> addresses, int gameId, String from, String url, String customMessage)
        throws ServiceException;

    /**
     * Removes a pending invitation.
     */
    void removeInvitation (String inviteId)
        throws ServiceException;

    /**
     * Gets the ID of the current user's home room.  Necessary for the new share page (this may
     * in fact become more specialized to retrieve any data necessary for that page).
     *
     * @return ID of the current user's home room.
     */
    int getHomeSceneId ()
        throws ServiceException;
}
