//
// $Id$

package com.threerings.msoy.web.client;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.web.data.Invitation;
import com.threerings.msoy.web.data.InvitationResults;
import com.threerings.msoy.web.data.MemberCard;
import com.threerings.msoy.web.data.MemberInvites;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

/**
 * Defines member-specific services available to the GWT/AJAX web client.
 */
public interface MemberService extends RemoteService
{
    /**
     * Looks up a member's card.
     */
    public MemberCard getMemberCard (int memberId)
        throws ServiceException;

    /**
     * Figure out whether or not a given member is your friend.
     */
    public boolean getFriendStatus (WebIdent ident, int memberId)
        throws ServiceException;

    /**
     * Accept a friend invitation.
     */
    public void addFriend (WebIdent ident, int friendId)
        throws ServiceException;

    /**
     * Remove a friend.
     */
    public void removeFriend (WebIdent ident, int friendId)
        throws ServiceException;

    /**
     * Loads all items in a player's inventory of the specified type and optionally restricted to
     * the specified suite.
     *
     * @gwt.typeArgs <com.threerings.msoy.item.data.all.Item>
     */
    public List loadInventory (WebIdent ident, byte type, int suiteId)
        throws ServiceException;

    /**
     * Return the invitation details for the given ident.
     */
    public MemberInvites getInvitationsStatus (WebIdent ident)
        throws ServiceException;

    /**
     * Send out some of this person's available invites.
     *
     * @param anonymous if true, the invitations will not be from the caller but will be
     * anonymous. This is only allowed for admin callers.
     *
     * @gwt.typeArgs addresses <com.threerings.msoy.web.data.EmailContact>
     */
    public InvitationResults sendInvites (WebIdent ident, List addresses, String fromName,
                                          String customMessage, boolean anonymous)
        throws ServiceException;

    /**
     * Grabs the details for an Invitation for the use of the InvitationDialog.
     *
     * @param viewing If true, this will ensure that the viewed date in the database has been set.
     * If false, the viewdate will be left alone.
     */
    public Invitation getInvitation (String inviteId, boolean viewing)
        throws ServiceException;

    /**
     * Removes a pending invitation.
     */
    public void removeInvitation (WebIdent ident, String inviteId)
        throws ServiceException;

    /**
     * Adds the email address from the given invite to the opt-out list.
     */
    public void optOut (String inviteId)
        throws ServiceException;

    /**
     * Fetch the highest ranked Whirled memebers for display on a leader board. 
     * Returns a list of {@link MemberCard} records.
     *
     * @gwt.typeArgs <com.threerings.msoy.web.data.MemberCard>
     */
    public List getLeaderList ()
        throws ServiceException;
}
