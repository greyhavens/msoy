//
// $Id$

package com.threerings.msoy.web.server;

import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import com.google.common.collect.Lists;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.IntSet;

import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.peer.server.PeerManager;

import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.PopularPlacesSnapshot;
import com.threerings.msoy.server.persist.MemberCardRecord;

import com.threerings.msoy.web.data.MemberCard;
import com.threerings.msoy.web.data.PlaceCard;
import com.threerings.msoy.web.data.ServiceException;

import static com.threerings.msoy.Log.log;

/**
 * Contains utility methods used by servlets.
 */
public class ServletUtil
{
    /**
     * Invokes the supplied operation on all peer nodes (on the distributed object manager thread)
     * and blocks the current thread until the execution has completed.
     */
    public static void invokePeerOperation (String name, final PeerManager.Operation op)
        throws ServiceException
    {
        final ServletWaiter<Void> waiter = new ServletWaiter<Void>(name);
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                try {
                    MsoyServer.peerMan.applyToNodes(op);
                    waiter.requestCompleted(null);
                } catch (Exception e) {
                    waiter.requestFailed(e);
                }
            }
        });
        waiter.waitForResult();
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
    public static List<MemberCard> resolveMemberCards (
        final IntSet memberIds, boolean onlineOnly, IntSet friendIds)
        throws ServiceException
    {
        List<MemberCard> cards = Lists.newArrayList();

        // hop over to the dobj thread and figure out which of these members is online
        final IntMap<MemberCard.Status> statuses = IntMaps.newHashIntMap();
        invokePeerOperation("resolveMemberCards(" + memberIds + ")", new PeerManager.Operation() {
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
        PopularPlacesSnapshot pps = MsoyServer.memberMan.getPPSnapshot();
        try {
            Set<Integer> keys = onlineOnly ? statuses.keySet() : memberIds;
            for (MemberCardRecord mcr : MsoyServer.memberRepo.loadMemberCards(keys)) {
                MemberCard card = mcr.toMemberCard();
                cards.add(card);

                // if this member is online, fill in their online status
                MemberCard.Status status = statuses.get(mcr.memberId);
                if (status != null) {
                    // game names are not filled in by MsoyNodeObject.getMemberCard so we have to
                    // get those from the popular places snapshot
                    if (status instanceof MemberCard.InGame) {
                        MemberCard.InGame gstatus = (MemberCard.InGame)status;
                        PlaceCard place = pps.getGame(gstatus.gameId);
                        if (place != null) {
                            gstatus.gameName = place.name;
                        }
                    }
                    card.status = status;
                }
            }

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to populate member cards.", pe);
        }

        if (friendIds != null) {
            for (MemberCard card : cards) {
                card.isFriend = friendIds.contains(card.name.getMemberId());
            }
        }

        return cards;
    }
}
