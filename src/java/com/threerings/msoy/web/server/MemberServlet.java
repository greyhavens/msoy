//
// $Id$

package com.threerings.msoy.web.server;

import java.util.Collections;
import java.util.List;

import com.google.inject.Inject;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntSet;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.ReferralInfo;
import com.threerings.msoy.server.FriendManager;
import com.threerings.msoy.server.MemberLogic;
import com.threerings.msoy.server.persist.InvitationRecord;
import com.threerings.msoy.server.persist.MemberCardRecord;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.person.server.persist.ProfileRepository;

import com.threerings.msoy.web.client.MemberService;
import com.threerings.msoy.web.data.Invitation;
import com.threerings.msoy.web.data.MemberCard;
import com.threerings.msoy.web.data.ServiceCodes;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link MemberService}.
 */
public class MemberServlet extends MsoyServiceServlet
    implements MemberService
{
    // from interface MemberService
    public MemberCard getMemberCard (int memberId)
        throws ServiceException
    {
        try {
            for (MemberCardRecord mcr : _memberRepo.loadMemberCards(
                     Collections.singleton(memberId))) {
                return mcr.toMemberCard();
            }
            return null;

        } catch (PersistenceException pe) {
            log.warning("getMemberCard failed [id=" + memberId + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from MemberService
    public boolean getFriendStatus (WebIdent ident, final int memberId)
        throws ServiceException
    {
        final MemberRecord memrec = _mhelper.requireAuthedUser(ident);
        try {
            return _memberRepo.getFriendStatus(memrec.memberId, memberId);
        } catch (PersistenceException pe) {
            log.warning("getFriendStatus failed [for=" + memberId + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from MemberService
    public void addFriend (WebIdent ident, final int friendId)
        throws ServiceException
    {
        MemberRecord memrec = _mhelper.requireAuthedUser(ident);
        _memberLogic.establishFriendship(memrec, friendId);
    }

    // from MemberService
    public void removeFriend (WebIdent ident, final int friendId)
        throws ServiceException
    {
        MemberRecord memrec = _mhelper.requireAuthedUser(ident);
        _memberLogic.clearFriendship(memrec.memberId, friendId);
    }

    // from MemberService
    public Invitation getInvitation (String inviteId, boolean viewing)
        throws ServiceException
    {
        try {
            InvitationRecord invRec = _memberRepo.loadInvite(inviteId, viewing);
            if (invRec == null) {
                return null;
            }

            // if we're viewing this invite, log that it was viewed
            if (viewing) {
                _eventLog.inviteViewed(inviteId);
            }

            MemberName inviter = null;
            if (invRec.inviterId > 0) {
                inviter = _memberRepo.loadMemberName(invRec.inviterId);
            }
            return invRec.toInvitation(inviter);

        } catch (PersistenceException pe) {
            log.warning("getInvitation failed [inviteId=" + inviteId + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from MemberService
    public void optOut (String inviteId)
        throws ServiceException
    {
        try {
            if (_memberRepo.inviteAvailable(inviteId) != null) {
                _memberRepo.optOutInvite(inviteId);
            }
        } catch (PersistenceException pe) {
            log.warning("optOut failed [inviteId=" + inviteId + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from MemberService
    public List<MemberCard> getLeaderList ()
        throws ServiceException
    {
        try {
            // locate the members that match the supplied search
            IntSet mids = new ArrayIntSet();
            mids.addAll(_memberRepo.getLeadingMembers(MAX_LEADER_MATCHES));

            // resolve cards for these members
            List<MemberCard> results = _mhelper.resolveMemberCards(mids, false, null);
            Collections.sort(results, MemberHelper.SORT_BY_LEVEL);
            return results;

        } catch (PersistenceException pe) {
            log.warning("Failure fetching leader list", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from MemberService
    public int getABTestGroup (ReferralInfo info, String testName, boolean logEvent)
    {
        return _memberLogic.getABTestGroup(testName, info, logEvent);
    }

    // from MemberService
    public void trackClientAction (ReferralInfo info, String actionName, String details)
    {
        _eventLog.clientAction(info.tracker, actionName, details);
    }

    // from MemberService
    public void trackTestAction (ReferralInfo info, String actionName, String testName)
    {
        int abTestGroup = -1;
        if (testName != null) {
            // grab the group without logging a tracking event about it
            abTestGroup = _memberLogic.getABTestGroup(testName, info, false);
        } else {
            testName = "";
        }
        _eventLog.testAction(info.tracker, actionName, testName, abTestGroup);
    }

    // our dependencies
    @Inject protected ProfileRepository _profileRepo;
    @Inject protected FriendManager _friendMan;
    @Inject protected MemberLogic _memberLogic;

    /** Maximum number of members to return for the leader board */
    protected static final int MAX_LEADER_MATCHES = 100;
    
}
