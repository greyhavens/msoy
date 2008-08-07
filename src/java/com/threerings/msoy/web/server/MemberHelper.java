//
// $Id$

package com.threerings.msoy.web.server;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.IntSet;

import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.peer.server.PeerManager;

import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.server.MemberManager;
import com.threerings.msoy.server.PopularPlacesSnapshot;
import com.threerings.msoy.server.persist.MemberCardRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.group.gwt.GroupMemberCard;
import com.threerings.msoy.web.data.MemberCard;
import com.threerings.msoy.web.data.ServiceCodes;
import com.threerings.msoy.web.data.ServiceException;

import static com.threerings.msoy.Log.log;

/**
 * Handles some member-related things for the servlets.
 */
@Singleton
public class MemberHelper
{
    /** Sorts lists of GroupMemberCard by rank then most recently online to least. */
    public static Comparator<GroupMemberCard> SORT_BY_RANK = new Comparator<GroupMemberCard>() {
        public int compare (GroupMemberCard c1, GroupMemberCard c2) {
            int rankDiff = c2.rank - c1.rank;
            if (rankDiff != 0) {
                return rankDiff;
            }
            int statusDiff = MemberCard.compare(c1.status, c2.status);
            if (statusDiff != 0) {
                return statusDiff;
            }
            return MemberName.compareNames(c1.name, c2.name);
        }
    };

    /** Sorts lists of MemberCard most recently online to least. */
    public static Comparator<MemberCard> SORT_BY_LAST_ONLINE = new Comparator<MemberCard>() {
        public int compare (MemberCard c1, MemberCard c2) {
            int rv = MemberCard.compare(c1.status, c2.status);
            if (rv != 0) {
                return rv;
            }
            return MemberName.compareNames(c1.name, c2.name);
        }
    };

    /** Sorts lists of MemberCard highest level to lowest, with last online then name as
     * tiebreaker. */
    public static Comparator<MemberCard> SORT_BY_LEVEL = new Comparator<MemberCard>() {
        public int compare (MemberCard c1, MemberCard c2) {
            if (c1.level > c2.level) {
                return -1;
            }
            else if (c2.level > c1.level) {
                return 1;
            }
            int rv = MemberCard.compare(c1.status, c2.status);
            if (rv != 0) {
                return rv;
            }
            return MemberName.compareNames(c1.name, c2.name);
        }
    };

    /**
     * Looks up a member id based on their session token. May return null if this member does not
     * have a session mapped into memory on this server.
     */
    public Integer getMemberId (String sessionToken)
    {
        return _members.get(sessionToken);
    }

    /**
     * Maps a session token to a member id for later retrieval via {@link #getMemberId}.
     */
    public void mapMemberId (String sessionToken, int memberId)
    {
        _members.put(sessionToken, memberId);
    }

    /**
     * Clears a session token to member id mapping.
     */
    public void clearMemberId (String sessionToken)
    {
        _members.remove(sessionToken);
    }

    /**
     * Clears all session tokens for a member id.
     */
    public void clearSessionToken (int memberId)
    {
        for (Iterator<Map.Entry<String,Integer>> iter = _members.entrySet().iterator();
                iter.hasNext(); ) {
            Map.Entry<String,Integer> entry = iter.next();
            if (entry.getValue() == memberId) {
                iter.remove();
            }
        }
    }

    /**
     * Returns the member record for the supplied auth token, or null if the ident represents an
     * expired session or is null.
     */
    public MemberRecord getAuthedUser (String authToken)
        throws ServiceException
    {
        if (authToken == null) {
            return null;
        }

        // if we don't have a session token -> member id mapping, then...
        Integer memberId = getMemberId(authToken);
        if (memberId == null) {
            // ...try looking up this session token, they may have originally authenticated with
            // another server and then started talking to us
            try {
                MemberRecord mrec = _memberRepo.loadMemberForSession(authToken);
                if (mrec == null) {
                    return null;
                }
                mapMemberId(authToken, mrec.memberId);
                return mrec;
            } catch (PersistenceException pe) {
                log.warning("Failed to load session [tok=" + authToken + "].", pe);
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
            }
        }

        // otherwise we already have a valid session token -> member id mapping, so use it
        try {
            return _memberRepo.loadMember(memberId);
        } catch (PersistenceException pe) {
            log.warning("Failed to load member [id=" + memberId + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    /**
     * Looks up the member information associated with the supplied session authentication
     * information.
     *
     * @exception ServiceException thrown if the session has expired or is otherwise invalid.
     */
    public MemberRecord requireAuthedUser (String authTok)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser(authTok);
        if (mrec == null) {
            throw new ServiceException(MsoyAuthCodes.SESSION_EXPIRED);
        }
        return mrec;
    }

    /**
     * Resolves a set of member ids into populated {@link MemberCard} instances with additional
     * online status information.
     *
     * @param memberIds the ids of the members for whom to resolve cards.
     * @param onlineOnly if true, all non-online members will be filtered from the results.
     * @param friendIds if non-null, indicates the ids of the friends of the caller and will be
     * used to fill in {@link MemberCard#isFriend}, otherwise isFriend will be left false.
     */
    public List<MemberCard> resolveMemberCards (
        final IntSet memberIds, boolean onlineOnly, IntSet friendIds)
        throws ServiceException
    {
        List<MemberCard> cards = Lists.newArrayList();

        // hop over to the dobj thread and figure out which of these members is online
        final IntMap<MemberCard.Status> statuses = IntMaps.newHashIntMap();
        _servletLogic.invokePeerOperation(
            "resolveMemberCards(" + memberIds + ")", new PeerManager.Operation() {
            public void apply (NodeObject nodeobj) {
                MsoyNodeObject mnobj = (MsoyNodeObject)nodeobj;
                for (int memberId : memberIds) {
                    MemberCard.Status status = mnobj.getMemberStatus(memberId);
                    if (status != null) {
                        statuses.put(memberId, status);
                    }
                }
            }
        });

        // now load up the rest of their member card information
        PopularPlacesSnapshot pps = _memberMan.getPPSnapshot();
        try {
            Set<Integer> keys = onlineOnly ? statuses.keySet() : memberIds;
            for (MemberCardRecord mcr : _memberRepo.loadMemberCards(keys)) {
                MemberCard card = mcr.toMemberCard();
                cards.add(card);

                // if this member is online, fill in their online status
                MemberCard.Status status = statuses.get(mcr.memberId);
                if (status != null) {
                    // game names are not filled in by MsoyNodeObject.getMemberCard so we have to
                    // get those from the popular places snapshot
                    if (status instanceof MemberCard.InGame) {
                        MemberCard.InGame gstatus = (MemberCard.InGame)status;
                        PopularPlacesSnapshot.Place place = pps.getGame(gstatus.gameId);
                        if (place != null) {
                            gstatus.gameName = place.name;
                        }
                    }
                    card.status = status;
                }
            }

        } catch (PersistenceException pe) {
            log.warning("Failed to populate member cards.", pe);
        }

        if (friendIds != null) {
            for (MemberCard card : cards) {
                card.isFriend = friendIds.contains(card.name.getMemberId());
            }
        }

        return cards;
    }

    /** Contains a mapping of authenticated members. */
    protected Map<String,Integer> _members =
        Collections.synchronizedMap(new HashMap<String,Integer>());

    // our dependencies
    @Inject protected ServletLogic _servletLogic;
    @Inject protected MemberManager _memberMan;
    @Inject protected MemberRepository _memberRepo;
}
