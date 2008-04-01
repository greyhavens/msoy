//
// $Id$

package com.threerings.msoy.web.server;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.logging.Level;

import com.google.common.collect.Sets;
import com.google.common.collect.Lists;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.DuplicateKeyException;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.Predicate;
import com.samskivert.util.RandomUtil;
import com.samskivert.util.Tuple;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.SceneBookmarkEntry;
import com.threerings.msoy.server.MemberNodeActions;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.PopularPlacesSnapshot;
import com.threerings.msoy.server.persist.MemberCardRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.TagHistoryRecord;
import com.threerings.msoy.server.persist.TagNameRecord;
import com.threerings.msoy.server.persist.TagPopularityRecord;
import com.threerings.msoy.server.persist.TagRepository;

import com.threerings.msoy.world.data.MsoySceneModel;
import com.threerings.msoy.world.server.persist.SceneRecord;

import com.threerings.msoy.group.data.Group;
import com.threerings.msoy.group.data.GroupCodes;
import com.threerings.msoy.group.data.GroupDetail;
import com.threerings.msoy.group.data.GroupExtras;
import com.threerings.msoy.group.data.GroupMemberCard;
import com.threerings.msoy.group.data.GroupMembership;
import com.threerings.msoy.group.server.persist.GroupMembershipRecord;
import com.threerings.msoy.group.server.persist.GroupRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;

import com.threerings.msoy.web.client.GroupService;
import com.threerings.msoy.web.data.GalaxyData;
import com.threerings.msoy.web.data.GroupCard;
import com.threerings.msoy.web.data.PlaceCard;
import com.threerings.msoy.web.data.ServiceCodes;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.TagHistory;
import com.threerings.msoy.web.data.WebIdent;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link GroupService}.
 */
public class GroupServlet extends MsoyServiceServlet
    implements GroupService
{
    // from GroupService
    public GalaxyData getGalaxyData (WebIdent ident)
        throws ServiceException
    {
        try {
            GalaxyData data = new GalaxyData();

            // determine our featured whirled based on who's online now
            PopularPlacesSnapshot pps = MsoyServer.memberMan.getPPSnapshot();
            List<GroupCard> popWhirleds = Lists.newArrayList();
            for (PlaceCard card : pps.getTopWhirleds()) {
                GroupRecord group = _groupRepo.loadGroup(card.placeId);
                if (group != null) {
                    GroupCard gcard = group.toGroupCard();
                    gcard.population = card.population;
                    popWhirleds.add(gcard);
                    if (popWhirleds.size() == GalaxyData.FEATURED_WHIRLED_COUNT) {
                        break;
                    }
                }
            }
            // if we don't have enough people online, supplement with other groups
            if (popWhirleds.size() < GalaxyData.FEATURED_WHIRLED_COUNT) {
                int count = GalaxyData.FEATURED_WHIRLED_COUNT - popWhirleds.size();
                for (GroupRecord group : _groupRepo.getGroupsList(0, count)) {
                    popWhirleds.add(group.toGroupCard());
                }
            }
            data.featuredWhirleds = popWhirleds.toArray(new GroupCard[popWhirleds.size()]);

            // load up our popular tags
            List<String> popularTags = Lists.newArrayList();
            for (TagPopularityRecord popRec : _groupRepo.getTagRepository().getPopularTags(
                     GalaxyData.POPULAR_TAG_COUNT)) {
                popularTags.add(popRec.tag);
            }
            data.popularTags = popularTags;

            return data;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "getGalaxyData failed [for=" + ident + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from GroupService
    public List<GroupCard> getGroupsList (WebIdent ident)
        throws ServiceException
    {
        try {
            List<GroupCard> groups = Lists.newArrayList();
            for (GroupRecord gRec : _groupRepo.getGroupsList(0, Integer.MAX_VALUE)) {
                groups.add(gRec.toGroupCard());
            }

            // fill in the current population of these groups
            PopularPlacesSnapshot pps = MsoyServer.memberMan.getPPSnapshot();
            for (GroupCard group : groups) {
                PlaceCard card = pps.getWhirled(group.name.getGroupId());
                if (card != null) {
                    group.population = card.population;
                }
            }

            return groups;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "getGroupsList failed", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    /**
     * Fetches the members of a given group, as {@link GroupMembership} records. This method
     * does not distinguish between a nonexistent group and a group without members;
     * both situations yield empty collections.
     */
    public GroupDetail getGroupDetail (WebIdent ident, int groupId)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser(ident);

        try {
            // load the group record
            GroupRecord grec = _groupRepo.loadGroup(groupId);
            if (grec == null) {
                return null;
            }

            // set up the detail
            GroupDetail detail = new GroupDetail();
            detail.group = grec.toGroupObject();
            detail.extras = grec.toExtrasObject();
            detail.creator = MsoyServer.memberRepo.loadMemberName(grec.creatorId);

            // determine our rank info if we're a member
            if (mrec != null) {
                GroupMembershipRecord gmrec = _groupRepo.getMembership(grec.groupId, mrec.memberId);
                if (gmrec != null) {
                    detail.myRank = gmrec.rank;
                    detail.myRankAssigned = gmrec.rankAssigned.getTime();
                }
            }

            return detail;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "getGroupDetail failed [groupId=" + groupId + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface GroupService
    public MembersResult getGroupMembers (WebIdent ident, int groupId)
        throws ServiceException
    {
        try {
            GroupRecord grec = _groupRepo.loadGroup(groupId);
            if (grec == null) {
                return null;
            }
            MembersResult result = new MembersResult();
            result.name = grec.toGroupName();
            result.members = loadGroupMembers(grec.groupId, GroupMembership.RANK_MEMBER);
            return result;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "getGroupMembers failed [groupId=" + groupId + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface GroupService
    public RoomsResult getGroupRooms (WebIdent ident, int groupId)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser(ident);

        try {
            RoomsResult result = new RoomsResult();
            List<GroupService.Room> rooms = Lists.newArrayList();
            for (SceneBookmarkEntry scene : MsoyServer.sceneRepo.getOwnedScenes(
                    MsoySceneModel.OWNER_TYPE_GROUP, groupId)) {
                GroupService.Room room = new GroupService.Room();
                room.sceneId = scene.sceneId;
                room.name = scene.sceneName;
                // TODO: load decor thumbnail
                rooms.add(room);
            }
            result.groupRooms = rooms;

            rooms = Lists.newArrayList();
            for (SceneBookmarkEntry scene : MsoyServer.sceneRepo.getOwnedScenes(mrec.memberId)) {
                if (scene.sceneId == mrec.homeSceneId) {
                    continue;
                }
                GroupService.Room room = new GroupService.Room();
                room.sceneId = scene.sceneId;
                room.name = scene.sceneName;
                // TODO: load decor thumbnail for when a room is transfered to the group
                rooms.add(room);
            }
            result.callerRooms = rooms;

            return result;
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "getGroupRooms failed [groupId=" + groupId + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface GroupService
    public void transferRoom (WebIdent ident, int groupId, int sceneId)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser(ident);

        try {
            // ensure the caller is a manager of this group
            GroupMembershipRecord membership = _groupRepo.getMembership(groupId, ident.memberId);
            if (membership.rank != GroupMembership.RANK_MANAGER) {
                throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
            }

            // ensure the caller is the owner of this scene
            SceneRecord scene = MsoyServer.sceneRepo.loadScene(sceneId);
            if (scene.ownerType != MsoySceneModel.OWNER_TYPE_MEMBER ||
                scene.ownerId != mrec.memberId) {
                throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
            }

            // sign the deed over
            MsoyServer.sceneRepo.transferSceneOwnership(
                sceneId, MsoySceneModel.OWNER_TYPE_GROUP, groupId);

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "transferRoom failed [groupId=" + groupId + ", sceneId=" +
                sceneId + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface GroupService
    public GroupInfo getGroupInfo (WebIdent ident, int groupId)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser(ident);
        try {
            GroupRecord grec = _groupRepo.loadGroup(groupId);
            if (grec == null) {
                return null;
            }

            GroupInfo info = new GroupInfo();
            info.name = grec.toGroupName();
            if (mrec != null) {
                GroupMembershipRecord gmrec = _groupRepo.getMembership(groupId, mrec.memberId);
                if (gmrec != null) {
                    info.rank = gmrec.rank;
                }
            }
            return info;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "getGroupInfo failed [id=" + groupId + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from GroupService
    public Integer getGroupHomeId (WebIdent ident, final int groupId)
        throws ServiceException
    {
        final ServletWaiter<Integer> waiter =new ServletWaiter<Integer>(
            "getHomeId[" + groupId + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.memberMan.getHomeId(MsoySceneModel.OWNER_TYPE_GROUP, groupId, waiter);
            }
        });
        return waiter.waitForResult();
    }

    // from interface GroupService
    public List<GroupCard> searchGroups (WebIdent ident, String searchString)
        throws ServiceException
    {
        try {
            List<GroupCard> groups = Lists.newArrayList();
            for (GroupRecord grec : _groupRepo.searchGroups(searchString)) {
                groups.add(grec.toGroupCard());
            }
            return fillInPopulation(groups);

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "searchGroups failed [searchString=" + searchString + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface GroupService
    public List<GroupCard> searchForTag (WebIdent ident, String tag)
        throws ServiceException
    {
        try {
            List<GroupCard> groups = Lists.newArrayList();
            for (GroupRecord grec : _groupRepo.searchForTag(tag)) {
                groups.add(grec.toGroupCard());
            }
            return fillInPopulation(groups);

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "searchForTag failed [tag=" + tag + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface GroupService
    public List<GroupMembership> getMembershipGroups (
        WebIdent ident, final int memberId, final boolean canInvite)
        throws ServiceException
    {
        MemberRecord reqrec = getAuthedUser(ident);
        final int requesterId = (reqrec == null) ? 0 : reqrec.memberId;

        try {
            MemberRecord mRec = MsoyServer.memberRepo.loadMember(memberId);
            if (mRec == null) {
                log.warning("Requested group membership for unknown member [id=" + memberId + "].");
                return Collections.emptyList();
            }

            return _groupRepo.resolveGroupMemberships(
                memberId, new Predicate<Tuple<GroupRecord,GroupMembershipRecord>>() {
                    public boolean isMatch (Tuple<GroupRecord,GroupMembershipRecord> info) {
                        // if we're not the person in question, don't show exclusive groups
                        if (memberId != requesterId && info.left.policy == Group.POLICY_EXCLUSIVE) {
                            return false;
                        }
                        // if we're only including groups we can invite to, strip out non-public
                        // groups of which we're not managers
                        if (canInvite && info.left.policy != Group.POLICY_PUBLIC &&
                            info.right.rank != GroupMembership.RANK_MANAGER) {
                            return false;
                        }
                        return true;
                    }
                });

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "getMembershipGroups failed [id=" + memberId + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface GroupService
    public Group createGroup (WebIdent ident, Group group, GroupExtras extras)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);

        // make sure the name is valid; this is checked on the client as well
        if (!isValidName(group.name)) {
            log.log(Level.WARNING, "Asked to create group with invalid name [for=" + mrec.who() +
                    ", name=" + group.name + "].");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        try {
            final GroupRecord grec = new GroupRecord();
            grec.name = group.name;
            grec.blurb = group.blurb;
            grec.policy = group.policy;
            if (group.logo != null) {
                grec.logoMimeType = group.logo.mimeType;
                grec.logoMediaHash = group.logo.hash;
                grec.logoMediaConstraint = group.logo.constraint;
            }
            grec.homepageUrl = extras.homepageUrl;
            grec.charter = extras.charter;
            if (extras.background != null) {
                grec.backgroundMimeType = extras.background.mimeType;
                grec.backgroundHash = extras.background.hash;
            }
            grec.catalogItemType = extras.catalogItemType;
            grec.catalogTag = extras.catalogTag;

            // we fill this in ourselves
            grec.creatorId = mrec.memberId;

            // create the group and then add the creator to it
            _groupRepo.createGroup(grec);
            _groupRepo.joinGroup(grec.groupId, grec.creatorId, GroupMembership.RANK_MANAGER);

            // if the creator is online, update their runtime data
            GroupMembership gm = new GroupMembership();
            gm.group = grec.toGroupName();
            gm.rank = GroupMembership.RANK_MANAGER;
            MemberNodeActions.joinedGroup(grec.creatorId, gm);

            return grec.toGroupObject();

        } catch (DuplicateKeyException dke) {
            throw new ServiceException(GroupCodes.E_GROUP_NAME_IN_USE);

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to create group [for=" + mrec.who() +
                    ", group=" + group + ", extras=" + extras + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface GroupService
    public void updateGroup (WebIdent ident, Group group, GroupExtras extras)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);

        // make sure the name is valid; this is checked on the client as well
        if (!isValidName(group.name)) {
            log.log(Level.WARNING, "Asked to update group with invalid name [for=" + mrec.who() +
                    ", name=" + group.name + "].");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        try {
            GroupMembershipRecord gmrec = _groupRepo.getMembership(group.groupId, mrec.memberId);
            if (gmrec == null || gmrec.rank != GroupMembership.RANK_MANAGER) {
                log.log(Level.WARNING, "in updateGroup, invalid permissions");
                throw new ServiceException("m.invalid_permissions");
            }

            GroupRecord grec = _groupRepo.loadGroup(group.groupId);
            if (grec == null) {
                throw new PersistenceException("Group not found [id=" + group.groupId + "]");
            }
            Map<String, Object> updates = grec.findUpdates(group, extras);
            if (updates.size() > 0) {
                _groupRepo.updateGroup(group.groupId, updates);
            }

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "updateGroup failed [group=" + group +
                    ", extras=" + extras + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface GroupService
    public void leaveGroup (WebIdent ident, int groupId, int memberId)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);

        try {
            GroupMembershipRecord tgtrec = _groupRepo.getMembership(groupId, memberId);
            if (tgtrec == null) {
                log.info("Requested to remove non-member from group [who=" + mrec.who() +
                         ", gid=" + groupId + ", mid=" + memberId + "].");
                return; // no harm no foul
            }

            // if we're not removing ourselves, make sure we're a manager and outrank the target
            if (mrec.memberId != memberId) {
                GroupMembershipRecord gmrec = _groupRepo.getMembership(groupId, mrec.memberId);
                if (gmrec == null || gmrec.rank != GroupMembership.RANK_MANAGER ||
                    (tgtrec.rank == GroupMembership.RANK_MANAGER &&
                     tgtrec.rankAssigned.getTime() < gmrec.rankAssigned.getTime())) {
                    log.warning("Rejecting remove from group request [who=" + mrec.who() +
                                ", gid=" + groupId + ", mid=" + memberId +
                                ", reqrec=" + gmrec + ", tgtrec=" + tgtrec + "].");
                    throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
                }
            }

            // if we made it this far, go ahead and remove the member from the group
            _groupRepo.leaveGroup(groupId, memberId);

            // if the group has no members left, remove the group as well
            if (_groupRepo.countMembers(groupId) == 0) {
                // TODO: delete this group's scenes
                log.warning("Group deleted, but we haven't implemented group scene deletion! " +
                            "[id=" + groupId + "].");
                _groupRepo.deleteGroup(groupId);
            }

            // let the dobj world know that this member has been removed
            MemberNodeActions.leftGroup(memberId, groupId);

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "leaveGroup failed [who=" + mrec.who() + ", gid=" + groupId +
                    ", mid=" + memberId + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface GroupService
    public void joinGroup (WebIdent ident, int groupId)
        throws ServiceException
    {
        final MemberRecord mrec = requireAuthedUser(ident);

        try {
            // make sure the group in question exists
            final GroupRecord grec = _groupRepo.loadGroup(groupId);
            if (grec == null) {
                log.warning("Requested to join non-existent group [who=" + mrec.who() +
                            ", gid=" + groupId + "].");
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
            }

            // TODO: we currently only prevent members from joining private groups by not showing
            // them the UI for joining; this will eventually be a problem

            // create a record indicating that we've joined this group
            _groupRepo.joinGroup(groupId, mrec.memberId, GroupMembership.RANK_MEMBER);

            // update this member's distributed object if they're online anywhere
            GroupMembership gm = new GroupMembership();
            gm.group = grec.toGroupName();
            gm.rank = GroupMembership.RANK_MEMBER;
            MemberNodeActions.joinedGroup(mrec.memberId, gm);

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "joinGroup failed [who=" + mrec.who() +
                    ", gid=" + groupId + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface GroupService
    public void updateMemberRank (WebIdent ident, int groupId, int memberId, byte newRank)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);

        try {
            GroupMembershipRecord gmrec = _groupRepo.getMembership(groupId, mrec.memberId);
            if (!mrec.isSupport() && 
                (gmrec == null || gmrec.rank != GroupMembership.RANK_MANAGER)) {
                log.log(Level.WARNING, "in updateMemberRank, invalid permissions");
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
            }

            _groupRepo.setRank(groupId, memberId, newRank);

            // TODO: MemberNodeActions.groupRankUpdated(memberId, groupId, newRank)

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "updateMemberRank failed [groupId=" + groupId + ", memberId=" +
                memberId + ", newRank=" + newRank + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface GroupService
    public TagHistory tagGroup (WebIdent ident, int groupId, String tag, boolean set)
        throws ServiceException
    {
        String tagName = tag.trim().toLowerCase();
        if (!TagNameRecord.VALID_TAG.matcher(tagName).matches()) {
            log.log(Level.WARNING, "in tagGroup, invalid tag: " + tagName);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        MemberRecord mrec = requireAuthedUser(ident);

        try {
            GroupMembershipRecord gmrec = _groupRepo.getMembership(groupId, mrec.memberId);
            if (gmrec == null || gmrec.rank != GroupMembership.RANK_MANAGER) {
                log.log(Level.WARNING, "in tagGroup, invalid permissions");
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
            }

            long now = System.currentTimeMillis();

            TagRepository tagRepo = _groupRepo.getTagRepository();
            TagNameRecord tagRec = tagRepo.getOrCreateTag(tagName);

            TagHistoryRecord historyRecord = set ?
                tagRepo.tag(groupId, tagRec.tagId, mrec.memberId, now) :
                tagRepo.untag(groupId, tagRec.tagId, mrec.memberId, now);
            if (historyRecord != null) {
                TagHistory history = new TagHistory();
                history.member = mrec.getName();
                history.tag = tagRec.tag;
                history.action = historyRecord.action;
                history.time = new Date(historyRecord.time.getTime());
                return history;
            }
            return null;
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "tagGroup failed [groupId=" + groupId + ", tag=" + tag +
                ", set=" + set + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface GroupService
    public Collection<TagHistory> getRecentTags (WebIdent ident) throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);

        try {
            MemberName memName = mrec.getName();
            TagRepository tagRepo = _groupRepo.getTagRepository();
            List<TagHistory> list = Lists.newArrayList();
            for (TagHistoryRecord record : tagRepo.getTagHistoryByMember(mrec.memberId)) {
                TagNameRecord tag = record.tagId == -1 ? null :
                    tagRepo.getTag(record.tagId);
                TagHistory history = new TagHistory();
                history.member = memName;
                history.tag = tag == null ? null : tag.tag;
                history.action = record.action;
                history.time = new Date(record.time.getTime());
                list.add(history);
            }
            return list;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "getRecentTags failed [for=" + mrec.who() + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface GroupService
    public Collection<String> getTags (WebIdent ident, int groupId) throws ServiceException
    {
        try {
            List<String> result = Lists.newArrayList();
            for (TagNameRecord tagName : _groupRepo.getTagRepository().getTags(groupId)) {
                result.add(tagName.tag);
            }
            return result;
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "getTags failed [groupId=" + groupId + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    protected List<GroupCard> fillInPopulation (List<GroupCard> groups)
    {
        PopularPlacesSnapshot pps = MsoyServer.memberMan.getPPSnapshot();
        for (GroupCard card : groups) {
            PlaceCard pcard = pps.getWhirled(card.name.getGroupId());
            if (pcard != null) {
                card.population = pcard.population;
            }
        }
        return groups;
    }

    protected List<GroupMemberCard> loadGroupMembers (int groupId, byte minRank)
        throws PersistenceException
    {
        IntMap<GroupMemberCard> members = IntMaps.newHashIntMap();
        for (GroupMembershipRecord gmrec : _groupRepo.getMembers(groupId)) {
            if (gmrec.rank >= minRank) { // TODO: filter in the database query
                members.put(gmrec.memberId, gmrec.toGroupMemberCard());
            }
        }
        List<GroupMemberCard> mlist = Lists.newArrayList();
        for (MemberCardRecord mcr : MsoyServer.memberRepo.loadMemberCards(members.keySet())) {
            mlist.add(mcr.toMemberCard(members.get(mcr.memberId)));
        }
        if (mlist.size() < members.size()) {
            Set<Integer> stale = Sets.newHashSet(members.keySet());
            for (GroupMemberCard gmc : mlist) {
                stale.remove(gmc.name.getMemberId());
            }
            log.warning("Group has stale members [groupId=" + groupId + ", ids=" + stale + "].");
        }
        Collections.sort(mlist, ServletUtil.SORT_BY_LAST_ONLINE);
        return mlist;
    }

    protected static boolean isValidName (String name)
    {
        return Character.isLetter(name.charAt(0)) || Character.isDigit(name.charAt(0));
    }

    protected GroupRepository _groupRepo = MsoyServer.groupRepo;
}
