//
// $Id$

package com.threerings.msoy.web.server;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.IntSet;

import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MemberManager;
import com.threerings.msoy.server.persist.MemberCardRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.peer.data.HostedGame;
import com.threerings.msoy.peer.data.HostedRoom;
import com.threerings.msoy.peer.data.MemberGame;
import com.threerings.msoy.peer.data.MemberScene;
import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.msoy.group.gwt.GroupMemberCard;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.ServiceException;

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
            int rankDiff = c2.rank.compareTo(c1.rank);
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
    {
        if (authToken == null) {
            return null;
        }

        // if we don't have a session token -> member id mapping, then...
        Integer memberId = getMemberId(authToken);
        if (memberId == null) {
            // ...try looking up this session token, they may have originally authenticated with
            // another server and then started talking to us
            MemberRecord mrec = _memberRepo.loadMemberForSession(authToken);
            if (mrec == null) {
                return null;
            }
            mapMemberId(authToken, mrec.memberId);
            return mrec;
        }

        // otherwise we already have a valid session token -> member id mapping, so use it
        return _memberRepo.loadMember(memberId);
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
        final Collection<Integer> memberIds, boolean onlineOnly, IntSet friendIds)
        throws ServiceException
    {
        List<MemberCard> cards = Lists.newArrayList();

        // hop over to the dobj thread and figure out which of these members is online
        final IntMap<MemberCard.Status> statuses = IntMaps.newHashIntMap();

        _servletLogic.invokePeerOperation(
            "resolveMemberCards(" + memberIds + ")", new MsoyPeerManager.NodeOp() {
            public void apply (MsoyNodeObject mnobj) {
                for (Integer id : memberIds) {
                    MemberCard.Status status = getStatus(mnobj, id);

                    // due to fiddly business we have to merge an AVR game player's scene status
                    // into its game status, but we don't know which one is going to show up first
                    // so we have to do some extra awesome checking
                    MemberCard.Status estatus = statuses.get(id);
                    if (estatus instanceof MemberCard.InAVRGame &&
                        status instanceof MemberCard.InScene) {
                        ((MemberCard.InAVRGame)estatus).sceneId = ((MemberCard.InScene)status).sceneId;
                    } else if (estatus instanceof MemberCard.InScene &&
                               status instanceof MemberCard.InAVRGame) {
                        ((MemberCard.InAVRGame)status).sceneId = ((MemberCard.InScene)estatus).sceneId;
                        statuses.put(id, status); // overwrite scene status with AVRG status

                    } else if (estatus instanceof MemberCard.InGame) {
                        // hrm, don't overwrite game status

                    } else {
                        statuses.put(id, status);
                    }
                }
            }
        });

        // now load up the rest of their member card information
        try {
            Collection<Integer> keys = onlineOnly ? statuses.keySet() : memberIds;
            for (MemberCardRecord mcr : _memberRepo.loadMemberCards(keys)) {
                MemberCard card = mcr.toMemberCard();
                cards.add(card);

                // if this member is online, fill in their online status
                MemberCard.Status status = statuses.get(mcr.memberId);
                if (status != null) {
                    card.status = status;
                }
            }

        } catch (Exception e) {
            log.warning("Failed to populate member cards.", e);
        }

        if (friendIds != null) {
            for (MemberCard card : cards) {
                card.isFriend = friendIds.contains(card.name.getMemberId());
            }
        }

        return cards;
    }

    protected MemberCard.Status getStatus (MsoyNodeObject mnobj, int memberId)
    {
        MemberGame game = mnobj.memberGames.get(memberId);

        MemberCard.InGame inGame = null;
        MemberCard.InAVRGame inAVRGame = null;

        if (game != null) {
            // don't show developer versions of games
            if (game.gameId != 0 && !Game.isDevelopmentVersion(game.gameId)) {
                if (game.avrGame) {
                    inAVRGame = new MemberCard.InAVRGame();
                    inAVRGame.gameId = game.gameId;
                } else {
                    inGame = new MemberCard.InGame();
                    inGame.gameId = game.gameId;
                }
                HostedGame hgame = mnobj.hostedGames.get(game.gameId);
                if (hgame != null) {
                    inGame.gameName = hgame.name;
                }
            }
        }

        if (inGame == null) {
            MemberScene scene = mnobj.memberScenes.get(memberId);
            if (scene != null) {
                if (inAVRGame != null) {
                    inAVRGame.sceneId = scene.sceneId;
                    return inAVRGame;
                }
                MemberCard.InScene inScene = new MemberCard.InScene();
                inScene.sceneId = scene.sceneId;
                HostedRoom room = mnobj.hostedScenes.get(scene.sceneId);
                if (room != null) {
                    inScene.sceneName = room.name;
                }
                return inScene;
            }
        } else {
            return inGame;
        }

        return null;
    }

    /** Contains a mapping of authenticated members. */
    protected Map<String,Integer> _members =
        Collections.synchronizedMap(new HashMap<String,Integer>());

    // our dependencies
    @Inject protected MemberManager _memberMan;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected ServletLogic _servletLogic;
}
