//
// $Id$

package com.threerings.msoy.group.server;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.Tuple;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MemberLogic;
import com.threerings.msoy.server.MemberManager;
import com.threerings.msoy.server.MemberNodeActions;
import com.threerings.msoy.server.PopularPlacesSnapshot;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.MemberCardRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.TagHistoryRecord;
import com.threerings.msoy.server.persist.TagNameRecord;
import com.threerings.msoy.server.persist.TagRepository;

import com.threerings.msoy.web.gwt.ServiceCodes;
import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.gwt.TagHistory;
import com.threerings.msoy.web.server.MemberHelper;
import com.threerings.msoy.web.server.MsoyServiceServlet;

import com.threerings.msoy.chat.data.MsoyChatChannel;
import com.threerings.msoy.chat.server.MsoyChatChannelManager;

import com.threerings.msoy.fora.server.ForumLogic;
import com.threerings.msoy.fora.server.persist.ForumRepository;
import com.threerings.msoy.fora.server.persist.ForumThreadRecord;
import com.threerings.msoy.room.data.MsoySceneModel;
import com.threerings.msoy.room.server.persist.MsoySceneRepository;
import com.threerings.msoy.room.server.persist.SceneRecord;

import com.threerings.msoy.group.data.all.Group;
import com.threerings.msoy.group.data.all.GroupMembership;
import com.threerings.msoy.group.data.all.Medal;
import com.threerings.msoy.group.gwt.GalaxyData;
import com.threerings.msoy.group.gwt.GroupCard;
import com.threerings.msoy.group.gwt.GroupCodes;
import com.threerings.msoy.group.gwt.GroupDetail;
import com.threerings.msoy.group.gwt.GroupExtras;
import com.threerings.msoy.group.gwt.GroupMemberCard;
import com.threerings.msoy.group.gwt.GroupService;
import com.threerings.msoy.group.gwt.MyGroupCard;
import com.threerings.msoy.group.server.persist.GroupMembershipRecord;
import com.threerings.msoy.group.server.persist.GroupRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.group.server.persist.MedalRecord;
import com.threerings.msoy.group.server.persist.MedalRepository;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link GroupService}.
 */
public class GroupServlet extends MsoyServiceServlet
    implements GroupService
{
    // from GroupService
    public GalaxyData getGalaxyData ()
        throws ServiceException
    {
        GalaxyData data = new GalaxyData();

        // load up my groups
        data.myGroups = Lists.newArrayList();
        if (getAuthedUser() != null) {
            List<MyGroupCard> myGroups = getMyGroups(MyGroupCard.SORT_BY_NEWEST_POST);
            for (MyGroupCard group : myGroups) {
                data.myGroups.add(group);
                if (data.myGroups.size() == GalaxyData.MY_GROUPS_COUNT) {
                    break;
                }
            }
        }

        // TODO: make sure these are in a sensible order?
        data.officialGroups = Lists.newArrayList();
        for (GroupRecord grec : _groupRepo.getOfficialGroups()) {
            data.officialGroups.add(grec.toGroupCard());
        }

        return data;
    }

    // from GroupService
    public GroupsResult getGroups (int offset, int count, GroupQuery query, boolean needCount)
        throws ServiceException
    {
        List<GroupRecord> records = _groupRepo.getGroups(offset, count, query);
        GroupsResult result = new GroupsResult();
        result.groups = populateGroupCard(Lists.newArrayList(Iterables.transform(records,
            GroupRecord.TO_CARD)));

        if (needCount) {
            result.totalCount = _groupRepo.getGroupCount(query);
        }

        return result;
    }

    /**
     * Fetches the members of a given group, as {@link GroupMembership} records. This method
     * does not distinguish between a nonexistent group and a group without members;
     * both situations yield empty collections.
     */
    public GroupDetail getGroupDetail (int groupId)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser();

        // load the group record
        GroupRecord grec = _groupRepo.loadGroup(groupId);
        if (grec == null) {
            return null;
        }

        // set up the detail
        GroupDetail detail = new GroupDetail();
        detail.group = grec.toGroupObject();
        detail.extras = grec.toExtrasObject();
        detail.homeSnapshot = _sceneRepo.loadSceneSnapshot(grec.homeSceneId);
        detail.creator = _memberRepo.loadMemberName(grec.creatorId);
        detail.memberCount = _groupRepo.countMembers(grec.groupId);

        // determine our rank info if we're a member
        if (mrec != null) {
            Tuple<Byte, Long> minfo = _groupRepo.getMembership(grec.groupId, mrec.memberId);
            detail.myRank = minfo.left;
            detail.myRankAssigned = minfo.right;
        }

        // load up recent threads for this group (ordered by thread id)
        List<ForumThreadRecord> thrrecs = _forumRepo.loadRecentThreads(groupId, 3);
        Map<Integer,GroupName> gmap =
            Collections.singletonMap(detail.group.groupId, detail.group.getName());
        detail.threads = _forumLogic.resolveThreads(mrec, thrrecs, gmap, false, true);

        // fill in the current population of the group
        PopularPlacesSnapshot pps = _memberMan.getPPSnapshot();
        PopularPlacesSnapshot.Place card = pps.getWhirled(groupId);
        if (card != null) {
            detail.population = card.population;
        }

        // collect the top members ordered by rank, then last online
        detail.topMembers = loadGroupMembers(
            grec.groupId, GroupMembership.RANK_MEMBER, GroupDetail.NUM_TOP_MEMBERS, true);

        return detail;
    }

    // from interface GroupService
    public MembersResult getGroupMembers (int groupId)
        throws ServiceException
    {
        GroupRecord grec = _groupRepo.loadGroup(groupId);
        if (grec == null) {
            return null;
        }
        MembersResult result = new MembersResult();
        result.name = grec.toGroupName();
        result.members = loadGroupMembers(grec.groupId, GroupMembership.RANK_MEMBER, 0, false);
        return result;
    }

    // from interface GroupService
    public void transferRoom (int groupId, int sceneId)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser();

        // ensure the caller is a manager of this group
        if (_groupRepo.getRank(groupId, mrec.memberId) != GroupMembership.RANK_MANAGER) {
            throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
        }

        // ensure the caller is the owner of this scene
        SceneRecord scene = _sceneRepo.loadScene(sceneId);
        if (scene.ownerType != MsoySceneModel.OWNER_TYPE_MEMBER ||
            scene.ownerId != mrec.memberId) {
            throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
        }

        // sign the deed over
        _sceneRepo.transferSceneOwnership(sceneId, MsoySceneModel.OWNER_TYPE_GROUP, groupId);
    }

    // from interface GroupService
    public GroupInfo getGroupInfo (int groupId)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser();
        GroupRecord grec = _groupRepo.loadGroup(groupId);
        if (grec == null) {
            return null;
        }

        GroupInfo info = new GroupInfo();
        info.name = grec.toGroupName();
        if (mrec != null) {
            info.rank = _groupRepo.getRank(groupId, mrec.memberId);
        }
        return info;
    }

    // from GroupService
    public Integer getGroupHomeId (final int groupId)
        throws ServiceException
    {
        return _memberLogic.getHomeId(MsoySceneModel.OWNER_TYPE_GROUP, groupId);
    }

    // from interface GroupService
    public List<GroupMembership> getMembershipGroups (final int memberId, final boolean canInvite)
        throws ServiceException
    {
        MemberRecord reqrec = getAuthedUser();
        final int requesterId = (reqrec == null) ? 0 : reqrec.memberId;

        MemberRecord mRec = _memberRepo.loadMember(memberId);
        if (mRec == null) {
            log.warning("Requested group membership for unknown member", "memberId", memberId);
            return Collections.emptyList();
        }

        return _groupRepo.resolveGroupMemberships(
            memberId, new Predicate<Tuple<GroupRecord,GroupMembershipRecord>>() {
                public boolean apply (Tuple<GroupRecord,GroupMembershipRecord> info) {
                    // if we're not the person in question, don't show exclusive groups
                    if (memberId != requesterId && info.left.policy == Group.POLICY_EXCLUSIVE) {
                        return false;
                    }
                    // if we're only including groups into which we can invite, enforce that
                    if (canInvite && !Group.canInvite(info.left.policy, info.right.rank)) {
                        return false;
                    }
                    return true;
                }
            });
    }

    // from interface GroupService
    public Group createGroup (Group group, GroupExtras extras)
        throws ServiceException
    {
        return _groupLogic.createGroup(requireAuthedUser(), group, extras);
    }

    // from interface GroupService
    public void updateGroup (Group group, GroupExtras extras)
        throws ServiceException
    {
        _groupLogic.updateGroup(requireAuthedUser(), group, extras);
    }

    // from interface GroupService
    public void leaveGroup (int groupId, int memberId)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();

        Tuple<Byte, Long> tgtinfo = _groupRepo.getMembership(groupId, memberId);
        if (tgtinfo.left == GroupMembership.RANK_NON_MEMBER) {
            log.info("Requested to remove non-member from group", "who", mrec.who(), "gid", groupId,
                "mid", memberId);
            return; // no harm no foul
        }

        // if we're not removing ourselves, make sure we're a manager and outrank the target
        if (mrec.memberId != memberId) {
            Tuple<Byte, Long> gminfo = _groupRepo.getMembership(groupId, mrec.memberId);
            if (gminfo.left != GroupMembership.RANK_MANAGER ||
                (tgtinfo.left == GroupMembership.RANK_MANAGER && tgtinfo.right < gminfo.right)) {
                log.warning("Rejecting remove from group request", "who", mrec.who(),
                            "gid", groupId, "mid", memberId, "reqinfo", gminfo, "tgtinfo", tgtinfo);
                throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
            }
        }

        // if we made it this far, go ahead and remove the member from the group
        _groupRepo.leaveGroup(groupId, memberId);

        // if the group has no members left, remove the group as well
        if (_groupRepo.countMembers(groupId) == 0) {
            // TODO: delete this group's scenes
            log.warning("Group deleted, but we haven't implemented group scene deletion!",
                "groupId", groupId);
            _groupRepo.deleteGroup(groupId);
        }

        // let the dobj world know that this member has been removed
        MemberNodeActions.leftGroup(memberId, groupId);

        // also let the chat channel manager know that this group lost member
        _channelMan.bodyRemovedFromChannel(
            MsoyChatChannel.makeGroupChannel(GroupName.makeKey(groupId)), memberId);
    }

    // from interface GroupService
    public void joinGroup (int groupId)
        throws ServiceException
    {
        final MemberRecord mrec = requireAuthedUser();

        // make sure the group in question exists
        final GroupRecord grec = _groupRepo.loadGroup(groupId);
        if (grec == null) {
            log.warning("Requested to join non-existent group", "who", mrec.who(), "gid", groupId);
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

        // also let the chat channel manager know that this group has a new member
        _channelMan.bodyAddedToChannel(
            MsoyChatChannel.makeGroupChannel(grec.toGroupName()), mrec.memberId);
    }

    // from interface GroupService
    public void updateMemberRank (int groupId, int memberId, byte newRank)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();

        if (!mrec.isSupport() &&
            _groupRepo.getRank(groupId, mrec.memberId) != GroupMembership.RANK_MANAGER) {
            log.warning("in updateMemberRank, invalid permissions");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        _groupRepo.setRank(groupId, memberId, newRank);

        // TODO: MemberNodeActions.groupRankUpdated(memberId, groupId, newRank)
    }

    // from interface GroupService
    public TagHistory tagGroup (int groupId, String tag, boolean set)
        throws ServiceException
    {
        String tagName = tag.trim().toLowerCase();
        if (!TagNameRecord.VALID_TAG.matcher(tagName).matches()) {
            log.warning("in tagGroup, invalid tag", "tag", tagName);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        MemberRecord mrec = requireAuthedUser();
        if (!mrec.isSupport() &&
            _groupRepo.getRank(groupId, mrec.memberId) != GroupMembership.RANK_MANAGER) {
            log.warning("in tagGroup, invalid permissions");
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
    }

    // from interface GroupService
    public List<TagHistory> getRecentTags () throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
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
    }

    // from interface GroupService
    public List<String> getTags (int groupId) throws ServiceException
    {
        List<TagNameRecord> trecs = _groupRepo.getTagRepository().getTags(groupId);
        return Lists.newArrayList(Iterables.transform(trecs, TagNameRecord.TO_TAG));
    }

    // from interface GroupService
    public List<MyGroupCard> getMyGroups (byte sortMethod)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        final int memberId = mrec.memberId;

        // Include Whirled Announcements in "my groups"
        List<GroupRecord> groupRecords = _groupRepo.getFullMemberships(memberId);
        GroupRecord announceGroup = _groupRepo.loadGroup(ServerConfig.getAnnounceGroupId());
        if (announceGroup != null) {
            groupRecords.add(announceGroup);
        }

        List<MyGroupCard> myGroupCards = Lists.newArrayList();
        PopularPlacesSnapshot pps = _memberMan.getPPSnapshot();

        for (GroupRecord record : groupRecords) {
            final MyGroupCard card = new MyGroupCard();

            // collect basic info from the GroupRecord
            card.blurb = record.blurb;
            card.homeSceneId = record.homeSceneId;
            if (record.toLogo() != null) {
                card.logo = record.toLogo();
            }
            card.name = record.toGroupName();
            card.official = record.official;

            // fetch thread information
            card.numThreads = _forumRepo.loadThreadCount(record.groupId);
            card.numPosts = _forumRepo.loadMessageCount(record.groupId);

            List<ForumThreadRecord> threads = _forumRepo.loadRecentThreads(record.groupId, 1);
            if (threads.size() > 0) {
                Map<Integer,GroupName> gmap =
                    Collections.singletonMap(record.groupId, card.name);
                card.latestThread = _forumLogic.resolveThreads(
                    mrec, threads, gmap, false, true).get(0);
            }

            // fetch current population from PopularPlacesSnapshot
            PopularPlacesSnapshot.Place pcard = pps.getWhirled(card.name.getGroupId());
            if (pcard != null) {
                card.population = pcard.population;
            }

            // determine our rank info
            card.rank = _groupRepo.getRank(record.groupId, mrec.memberId);

            myGroupCards.add(card);
        }

        // sort by the preferred sort method
        if (sortMethod == MyGroupCard.SORT_BY_PEOPLE_ONLINE) {
            Collections.sort(myGroupCards, SORT_BY_PEOPLE_ONLINE);
        }
        else if (sortMethod == MyGroupCard.SORT_BY_NAME) {
            Collections.sort(myGroupCards, SORT_BY_NAME);
        }
        else if (sortMethod == MyGroupCard.SORT_BY_MANAGER) {
            Collections.sort(myGroupCards, SORT_BY_MANAGER);
        }
        else if (sortMethod == MyGroupCard.SORT_BY_NEWEST_POST) {
            Collections.sort(myGroupCards, SORT_BY_NEWEST_POST);
        }

        return myGroupCards;
    }

    // from interface GroupService
    public List<GroupMembership> getGameGroups (final int gameId)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        final int memberId = mrec.memberId;

        MemberRecord mRec = _memberRepo.loadMember(memberId);
        if (mRec == null) {
            log.warning("Requested group membership for unknown member", "memberId", memberId);
            return Collections.emptyList();
        }

        return _groupRepo.resolveGroupMemberships(
            memberId, new Predicate<Tuple<GroupRecord,GroupMembershipRecord>>() {
                public boolean apply (Tuple<GroupRecord,GroupMembershipRecord> info) {
                    // only groups the member manages
                    if (info.right.rank < GroupMembership.RANK_MANAGER) {
                        return false;
                    }
                    // exclude groups connected to other games
                    if (info.left.gameId != 0
                        && Math.abs(info.left.gameId) != Math.abs(gameId)) {
                        return false;
                    }
                    return true;
                }
            });
    }

    // from interface GroupService
    public void updateMedal (Medal medal)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();

        if (medal.groupId < 1) {
            log.warning("Medal provided with an invalid group id", "groupId", medal.groupId);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        if (medal.name == null || medal.name.equals("") ||
            medal.description == null || medal.description.equals("") ||
            medal.icon == null) {
            log.warning("Incomplete medal provided, but it should have been checked on the client");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        if (!mrec.isSupport() &&
            _groupRepo.getRank(medal.groupId, mrec.memberId) != GroupMembership.RANK_MANAGER) {
            log.warning("Non-manager attempted to update a group medal", "memberId", mrec.memberId,
                "groupId", medal.groupId);
            throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
        }

        if (medal.medalId == 0 && _medalRepo.groupContainsMedalName(medal.groupId, medal.name)) {
            log.warning("Attempted to create a medal with a duplicate name");
            throw new ServiceException(GroupCodes.E_GROUP_MEDAL_NAME_IN_USE);
        }

        MedalRecord medalRec = new MedalRecord();
        medalRec.medalId = medal.medalId;
        medalRec.groupId = medal.groupId;
        medalRec.name = medal.name;
        medalRec.description = medal.description;
        medalRec.iconHash = medal.icon.hash;
        medalRec.iconMimeType = medal.icon.mimeType;
        _medalRepo.storeMedal(medalRec);
    }

    /**
     * Fill in the current number of people in rooms (population) and the number of total threads
     * for a list of group cards.
     */
    protected List<GroupCard> populateGroupCard (List<GroupCard> groups)
    {
        PopularPlacesSnapshot pps = _memberMan.getPPSnapshot();
        for (GroupCard card : groups) {
            PopularPlacesSnapshot.Place pcard = pps.getWhirled(card.name.getGroupId());
            if (pcard != null) {
                card.population = pcard.population;
            }
            card.threadCount = _forumRepo.loadThreadCount(card.name.getGroupId());
        }
        return groups;
    }

    /**
     * Load GroupMemberCard for members of this group
     * @param minRank Only include members with at least this rank
     * @param sortByRank If true, sort by rank then last online, if false by last online
     * @param limit The maximum number of members to return, or <= 0 for no limit
     */
    protected List<GroupMemberCard> loadGroupMembers (
        int groupId, byte minRank, int limit, boolean sortByRank)
    {
        IntMap<GroupMemberCard> members = IntMaps.newHashIntMap();
        for (GroupMembershipRecord gmrec : _groupRepo.getMembers(groupId, minRank, true)) {
            members.put(gmrec.memberId, gmrec.toGroupMemberCard());
        }

        List<GroupMemberCard> mlist = Lists.newArrayList();
        for (MemberCardRecord mcr : _memberRepo.loadMemberCards(members.keySet())) {
            mlist.add(mcr.toMemberCard(members.get(mcr.memberId)));
        }
        if (mlist.size() < members.size()) {
            Set<Integer> stale = Sets.newHashSet(members.keySet());
            for (GroupMemberCard gmc : mlist) {
                stale.remove(gmc.name.getMemberId());
            }
            log.warning("Gropu has stale members", "groupId", groupId, "ids", stale);
        }
        if (sortByRank) {
            Collections.sort(mlist, MemberHelper.SORT_BY_RANK);
        }
        else {
            Collections.sort(mlist, MemberHelper.SORT_BY_LAST_ONLINE);
        }
        if (limit > 0 && mlist.size() > limit) {
            List<GroupMemberCard> truncatedMemberList = Lists.newArrayList();
            for (int i = 0; i < limit; i++) {
                truncatedMemberList.add(mlist.get(i));
            }
            return truncatedMemberList;
        }
        else {
            return mlist;
        }
    }

    // our dependencies
    @Inject protected MemberManager _memberMan;
    @Inject protected MemberLogic _memberLogic;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected MedalRepository _medalRepo;
    @Inject protected ForumLogic _forumLogic;
    @Inject protected ForumRepository _forumRepo;
    @Inject protected MsoySceneRepository _sceneRepo;
    @Inject protected GroupLogic _groupLogic;
    @Inject protected MsoyChatChannelManager _channelMan;

    /** Compartor for sorting by population then by last post date. */
    protected static Comparator<MyGroupCard> SORT_BY_PEOPLE_ONLINE =
        new Comparator<MyGroupCard>() {
        public int compare (MyGroupCard c1, MyGroupCard c2) {
            int rv = c2.population - c1.population;
            if (rv != 0) {
                return rv;
            }
            if (c1.latestThread != null && c2.latestThread == null) {
                return -1;
            } else if (c1.latestThread == null && c2.latestThread != null) {
                return 1;
            } else if (c1.latestThread != null && c2.latestThread != null) {
                return c2.latestThread.mostRecentPostId - c1.latestThread.mostRecentPostId;
            }
            // if neither has a single post or active user, sort by name
            return c1.name.toString().toLowerCase().compareTo(c2.name.toString().toLowerCase());
        }
    };

    /** Compartor for sorting by population then by last post date. */
    protected static Comparator<MyGroupCard> SORT_BY_NAME = new Comparator<MyGroupCard>() {
        public int compare (MyGroupCard c1, MyGroupCard c2) {
            return c1.name.toString().toLowerCase().compareTo(c2.name.toString().toLowerCase());
        }
    };

    /** Compartor for sorting by manager status then population then by last post date. */
    protected static Comparator<MyGroupCard> SORT_BY_MANAGER = new Comparator<MyGroupCard>() {
        public int compare (MyGroupCard c1, MyGroupCard c2) {
            if (c1.rank == GroupMembership.RANK_MANAGER && c2.rank < GroupMembership.RANK_MANAGER) {
                return -1;
            } else if (c2.rank == GroupMembership.RANK_MANAGER &&
                       c1.rank < GroupMembership.RANK_MANAGER) {
                return 1;
            }

            // from here down is the same as SORT_BY_PEOPLE_ONLINE
            int rv = c2.population - c1.population;
            if (rv != 0) {
                return rv;
            }
            if (c1.latestThread != null && c2.latestThread == null) {
                return -1;
            } else if (c1.latestThread == null && c2.latestThread != null) {
                return 1;
            } else if (c1.latestThread != null && c2.latestThread != null) {
                return c2.latestThread.mostRecentPostId - c1.latestThread.mostRecentPostId;
            }
            // if neither has a single post or active user, sort by name
            return c1.name.toString().toLowerCase().compareTo(c2.name.toString().toLowerCase());
        }
    };

    /** Compartor for sorting by last post date then by population. */
    protected static Comparator<MyGroupCard> SORT_BY_NEWEST_POST = new Comparator<MyGroupCard>() {
        public int compare (MyGroupCard c1, MyGroupCard c2) {
            if (c1.latestThread != null && c2.latestThread == null) {
                return -1;
            } else if (c1.latestThread == null && c2.latestThread != null) {
                return 1;
            } else if (c1.latestThread != null && c2.latestThread != null) {
                return c2.latestThread.mostRecentPostId - c1.latestThread.mostRecentPostId;
            }
            int rv = c2.population - c1.population;
            if (rv != 0) {
                return rv;
            }
            // if neither has a single post or active user, sort by name
            return c1.name.toString().toLowerCase().compareTo(c2.name.toString().toLowerCase());
        }
    };
}
