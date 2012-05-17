//
// $Id$

package com.threerings.msoy.group.server;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.samskivert.util.Tuple;

import com.samskivert.depot.DuplicateKeyException;

import com.threerings.gwt.util.PagedResult;
import com.threerings.web.gwt.ServiceException;

import com.threerings.msoy.chat.data.MsoyChatChannel;
import com.threerings.msoy.chat.server.MsoyChatChannelManager;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.HashMediaDesc;
import com.threerings.msoy.data.all.Theme;
import com.threerings.msoy.data.all.VizMemberName;
import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.fora.server.ForumLogic;
import com.threerings.msoy.fora.server.persist.ForumRepository;
import com.threerings.msoy.fora.server.persist.ForumThreadRecord;
import com.threerings.msoy.group.data.all.Group;
import com.threerings.msoy.group.data.all.GroupMembership;
import com.threerings.msoy.group.data.all.GroupMembership.Rank;
import com.threerings.msoy.group.data.all.Medal;
import com.threerings.msoy.group.gwt.BrandDetail.BrandShare;
import com.threerings.msoy.group.gwt.GalaxyData;
import com.threerings.msoy.group.gwt.GroupCard;
import com.threerings.msoy.group.gwt.GroupCodes;
import com.threerings.msoy.group.gwt.GroupDetail;
import com.threerings.msoy.group.gwt.GroupExtras;
import com.threerings.msoy.group.gwt.GroupMemberCard;
import com.threerings.msoy.group.gwt.GroupService;
import com.threerings.msoy.group.server.persist.EarnedMedalRecord;
import com.threerings.msoy.group.server.persist.GroupMembershipRecord;
import com.threerings.msoy.group.server.persist.GroupRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.group.server.persist.MedalRecord;
import com.threerings.msoy.group.server.persist.MedalRepository;
import com.threerings.msoy.group.server.persist.ThemeRepository;
import com.threerings.msoy.item.data.ItemCodes;
import com.threerings.msoy.mail.gwt.*;
import com.threerings.msoy.mail.server.persist.*;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PriceQuote;
import com.threerings.msoy.money.data.all.PurchaseResult;
import com.threerings.msoy.person.gwt.FeedMessageType;
import com.threerings.msoy.person.server.FeedLogic;
import com.threerings.msoy.room.data.MsoySceneModel;
import com.threerings.msoy.room.server.persist.MsoySceneRepository;
import com.threerings.msoy.room.server.RoomLogic;
import com.threerings.msoy.server.MemberManager;
import com.threerings.msoy.server.persist.MemberCardRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.persist.MemberRepository.MemberSearchRecord;
import com.threerings.msoy.server.persist.TagHistoryRecord;
import com.threerings.msoy.server.persist.TagNameRecord;
import com.threerings.msoy.server.persist.TagRepository;
import com.threerings.msoy.server.PopularPlacesSnapshot;
import com.threerings.msoy.server.TagLogic;
import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.ServiceCodes;
import com.threerings.msoy.web.gwt.TagHistory;
import com.threerings.msoy.web.server.MsoyServiceServlet;

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
        MemberRecord mrec = getAuthedUser();
        GalaxyData data = new GalaxyData();

        // load up featured groups
        List<GroupRecord> featured = _groupRepo.getGroups(GalaxyData.FEATURED_GROUPS_COUNT);
        data.featuredGroups = populateGroups(
            Lists.newArrayList(Iterables.transform(featured, GroupRecord.TO_CARD)));

        // load up my groups
        if (mrec != null) {
            List<GroupRecord> groups = _groupRepo.getFullMemberships(
                mrec.memberId, GalaxyData.MY_GROUPS_COUNT);
            data.myGroups = Lists.newArrayList(Iterables.transform(groups, GroupRecord.TO_CARD));
        } else {
            data.myGroups = Lists.newArrayList();
        }

        // load up the official groups
        data.officialGroups = Lists.newArrayList(
            Iterables.transform(_groupRepo.getOfficialGroups(), GroupRecord.TO_CARD));

        return data;
    }

    // from GroupService
    public PagedResult<GroupCard> getGroups (
        int offset, int count, GroupQuery query, boolean needCount)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser();

        List<GroupRecord> records = _groupRepo.getGroups(
            offset, count, query, mrec != null && mrec.isSupport());
        PagedResult<GroupCard> result = new PagedResult<GroupCard>();
        result.page = populateGroups(
            Lists.newArrayList(Iterables.transform(records, GroupRecord.TO_CARD)));

        if (needCount) {
            result.total = _groupRepo.getGroupCount(query, mrec != null && mrec.isSupport());
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
        detail.brand = _groupLogic.loadBrandDetail(groupId);
        detail.extras = grec.toExtrasObject();
        detail.homeSnapshot = _sceneRepo.loadSceneSnapshot(grec.homeSceneId);
        detail.creator = _memberRepo.loadMemberName(grec.creatorId);
        detail.theme = _themeLogic.loadTheme(grec.groupId);
        detail.memberCount = _groupRepo.countMembers(grec.groupId);
        detail.myRank = Rank.NON_MEMBER;

        // determine our rank info if we're a member
        if (mrec != null) {
            Tuple<Rank, Long> minfo = _groupRepo.getMembership(grec.groupId, mrec.memberId);
            detail.myRank = minfo.left;
            detail.myRankAssigned = minfo.right;
        }

        // check visibility
        if (!isVisible(mrec, grec.policy, detail.myRank)) {
            return null;
        }

        // load up recent threads for this group (ordered by thread id)
        List<ForumThreadRecord> thrrecs = _forumRepo.loadRecentThreads(groupId, 3);
        Map<Integer,GroupName> gmap =
            Collections.singletonMap(detail.group.groupId, detail.group.getName());
        detail.threads = _forumLogic.resolveThreads(mrec, thrrecs, gmap, false, true);

        // fill in the current population of the group
        PopularPlacesSnapshot pps = _memberMan.getPPSnapshot();
        PopularPlacesSnapshot.Place card = pps.getGroup(groupId);
        if (card != null) {
            detail.population = card.population;
        }

        // we need to order by rank then by last online. this can't be done efficiently without
        // a join on ProfileRecord and GroupMembershipRecord, so do two passes...

        // first managers
        Set<Integer> managers = Sets.newHashSet(
            _groupRepo.getMemberIdsWithRank(groupId, Rank.MANAGER));
        detail.topMembers = resolveGroupMemberCards(
            groupId, managers, 0, GroupDetail.NUM_TOP_MEMBERS);

        // then shareholders (if we are below the needed number)
        Set<Integer> holders = Sets.newHashSet();
        if (detail.topMembers.size() < GroupDetail.NUM_TOP_MEMBERS) {
            for (BrandShare share : detail.brand.shareHolders) {
                // but don't add the managers again
                int memberId = share.member.getId();
                if (!managers.contains(memberId)) {
                    holders.add(memberId);
                }
            }

            detail.topMembers.addAll(resolveGroupMemberCards(
                groupId, holders, 0, GroupDetail.NUM_TOP_MEMBERS - detail.topMembers.size()));
        }

        // then everyone else (if we are below the needed number)
        if (detail.topMembers.size() < GroupDetail.NUM_TOP_MEMBERS) {
            List<Integer> everyone = _groupRepo.getMemberIdsWithRank(groupId, Rank.MEMBER);
            // except the shareholders, which we already added
            everyone.removeAll(holders);
            detail.topMembers.addAll(resolveGroupMemberCards(
                groupId, everyone, 0, GroupDetail.NUM_TOP_MEMBERS - detail.topMembers.size()));
        }

        return detail;
    }

    // from interface GroupService
    public PagedResult<GroupMemberCard> getGroupMembers (int groupId, int offset, int count)
        throws ServiceException
    {
        // resolve cards for one page of all members in the group
        List<Integer> memberIds = _groupRepo.getMemberIds(groupId);

        PagedResult<GroupMemberCard> result = new PagedResult<GroupMemberCard>();
        result.total = memberIds.size();
        result.page = resolveGroupMemberCards(groupId, memberIds, offset, count);
        return result;
    }

    // from interface GroupService
    public void transferRoom (int groupId, int sceneId)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser();
        _roomLogic.checkCanGiftRoom(mrec, sceneId);

        // ensure the caller is a manager of this group
        if (_groupRepo.getRank(groupId, mrec.memberId) != Rank.MANAGER) {
            throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
        }
        GroupRecord grec = _groupRepo.loadGroup(groupId);
        if (grec == null) {
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
        _roomLogic.enactRoomTransfer(sceneId, MsoySceneModel.OWNER_TYPE_GROUP, groupId,
            grec.toGroupName(), false);
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

    // from interface GroupService
    public Theme getTheme (int groupId)
        throws ServiceException
    {
        return _themeLogic.loadTheme(groupId);
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
                    if (memberId != requesterId && info.left.policy == Group.Policy.EXCLUSIVE) {
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
    public PriceQuote quoteCreateGroup ()
        throws ServiceException
    {
        return _groupLogic.quoteCreateGroup(requireAuthedUser());
    }

    @Override
    public PriceQuote quoteCreateTheme ()
        throws ServiceException
    {
        return _themeLogic.quoteCreateTheme(requireAuthedUser());
    }

    // from interface GroupService
    public PurchaseResult<Group> createGroup (
        Group group, GroupExtras extras, Currency currency, int authedAmount)
        throws ServiceException
    {
        return _groupLogic.createGroup(
            requireValidatedUser(), group, extras, currency, authedAmount);
    }

    @Override
    public PurchaseResult<Theme> createTheme (int groupId, Currency currency, int authedAmount)
        throws ServiceException
    {
        return _themeLogic.createTheme(requireValidatedUser(), groupId, currency, authedAmount);
    }

    // from interface GroupService
    public void updateGroup (Group group, GroupExtras extras)
        throws ServiceException
    {
        _groupLogic.updateGroup(requireAuthedUser(), group, extras);
    }

    @Override
    public void updateTheme (Theme theme)
        throws ServiceException
    {
        _themeLogic.updateTheme(requireAuthedUser(), theme);
    }

    // from interface GroupService
    public void leaveGroup (int groupId, int memberId)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();

        Tuple<Rank, Long> tgtinfo = _groupRepo.getMembership(groupId, memberId);
        if (tgtinfo.left == Rank.NON_MEMBER) {
            log.info("Requested to remove non-member from group", "who", mrec.who(), "gid", groupId,
                "mid", memberId);
            return; // no harm no foul
        }

        // if we're not removing ourselves, make sure we're support or outrank the target
        if (mrec.memberId != memberId) {
            Tuple<Rank, Long> gminfo = _groupRepo.getMembership(groupId, mrec.memberId);
            if (!mrec.isSupport() && !mayChangeOtherMember(gminfo, tgtinfo)) {
                log.warning("Rejecting remove from group request", "who", mrec.who(),
                            "gid", groupId, "mid", memberId, "reqinfo", gminfo, "tgtinfo", tgtinfo);
                throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
            }
        }

        // TODO: if this was the group's last manager, auto-promote e.g. the oldest member?
        _groupLogic.leaveGroup(groupId, memberId);

        // also let the chat channel manager know that this group lost member
        _channelMan.bodyRemovedFromChannel(
            MsoyChatChannel.makeGroupChannel(GroupName.makeKey(groupId)), memberId);
    }

    // from interface GroupService
    public void joinGroup (int groupId)
        throws ServiceException
    {
        MemberRecord mrec = requireValidatedUser();

        GroupRecord grec = _groupRepo.loadGroup(groupId);
        if (grec == null) {
            log.warning("Requested to join non-existent group", "who", mrec.who(), "gid", groupId);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        if (grec.policy != Group.Policy.PUBLIC) {
            log.warning("Requested to join a private group", "who", mrec.who(), "gid", groupId);
            throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
        }

        addToGroup(mrec, grec);
    }

    // from interface GroupService
    public void joinGroupFromInvite (int groupId, int conversationId, long sent)
        throws ServiceException
    {
        MemberRecord mrec = requireValidatedUser();

        GroupRecord grec = _groupRepo.loadGroup(groupId);
        if (grec == null) {
            log.warning("Requested to join non-existent group", "who", mrec.who(), "gid", groupId);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        if (!hasInvitation(mrec, groupId, conversationId, sent)) {
            log.warning("Missing group invitation", "who", mrec.who(), "gid", groupId);
            throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
        }

        log.info("Did have invitation");

        addToGroup(mrec, grec);
    }

    protected boolean hasInvitation (MemberRecord mrec, int groupId, int conversationId, long sent)
    {
        ConversationRecord convrec = _mailRepo.loadConversation(conversationId);
        if (convrec == null || convrec.targetId != mrec.memberId) {
            return false; // Missing conversation, or not addressed to this member
        }
        ConvMessageRecord msgrec = _mailRepo.loadMessage(conversationId, sent);
        if (msgrec == null || _groupRepo.getRank(groupId, msgrec.authorId) == Rank.NON_MEMBER) {
            return false; // The inviter isn't a member of the group
        }
        ConvMessage msg = msgrec.toConvMessage();
        if (msg.payload == null || !(msg.payload instanceof GroupInvitePayload)) {
            return false; // No group invite
        }
        GroupInvitePayload invite = (GroupInvitePayload)msg.payload;
        return invite.groupId == groupId;
    }

    /** Add a member to a group, with no validation at all. */
    protected void addToGroup (MemberRecord mrec, GroupRecord grec)
    {
        // create a record indicating that we've joined this group
        _groupLogic.addMember(grec, mrec.memberId, Rank.MEMBER);

        // also let the chat channel manager know that this group has a new member
        _channelMan.bodyAddedToChannel(
            MsoyChatChannel.makeGroupChannel(grec.toGroupName()), mrec.memberId);

        // if the group is non-private, publish that they joined in their feed
        if (grec.policy != Group.Policy.EXCLUSIVE) {
            _feedLogic.publishMemberMessage(mrec.memberId, FeedMessageType.FRIEND_JOINED_GROUP,
                grec.groupId, grec.name, grec.toLogo());
        }
    }

    // from interface GroupService
    public void updateMemberRank (int groupId, int memberId, Rank newRank)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();

        Tuple<Rank, Long> gminfo = _groupRepo.getMembership(groupId, mrec.memberId);
        Tuple<Rank, Long> tgtinfo = _groupRepo.getMembership(groupId, memberId);

        if (!mrec.isSupport() && !mayChangeOtherMember(gminfo, tgtinfo)) {
            log.warning("in updateMemberRank, invalid permissions");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        _groupLogic.setRank(groupId, memberId, newRank);
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
        if (!canManage(mrec, groupId)) {
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
    public List<TagHistory> getTagHistory (int groupId)
        throws ServiceException
    {
        return _tagLogic.getTagHistory(groupId, _groupRepo.getTagRepository(), 0, 0);
    }

    // from interface GroupService
    public List<String> getTags (int groupId) throws ServiceException
    {
        List<TagNameRecord> trecs = _groupRepo.getTagRepository().getTags(groupId);
        return Lists.newArrayList(Iterables.transform(trecs, TagNameRecord.TO_TAG));
    }

    // from interface GroupService
    public List<GroupCard> getMyGroups ()
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        List<GroupRecord> groups = _groupRepo.getFullMemberships(mrec.memberId, -1);
        return populateGroups(Lists.newArrayList(Iterables.transform(groups, GroupRecord.TO_CARD)));
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
                    if (info.right.rank.compareTo(Rank.MANAGER) < 0) {
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

        if (!canManage(mrec, medal.groupId)) {
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
        medalRec.iconHash = HashMediaDesc.unmakeHash(medal.icon);
        medalRec.iconMimeType = medal.icon.getMimeType();
        _medalRepo.storeMedal(medalRec);
    }

    // from interface GroupService
    public GroupService.MedalsResult getAwardedMedals (int groupId)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser();
        GroupService.MedalsResult result = new GroupService.MedalsResult();
        GroupRecord grec = _groupRepo.loadGroup(groupId);
        if (grec == null) {
            throw new ServiceException(GroupCodes.E_INVALID_GROUP);
        }
        result.groupName = grec.toGroupName();
        result.medals = Lists.newArrayList();
        result.rank = mrec == null ? Rank.NON_MEMBER :
            _groupRepo.getMembership(groupId, mrec.memberId).left;

        // check visibility
        if (!isVisible(mrec, grec.policy, result.rank)) {
            throw new ServiceException(GroupCodes.E_INVALID_GROUP);
        }

        // we could do a Join to accomplish a similar result, but we need to make sure we return
        // Medals that have not been earned by anybody yet, so we need to first grab the full set
        // of available medals
        List<MedalRecord> groupMedals = _medalRepo.loadGroupMedals(groupId);
        Map<Integer, List<VizMemberName>> memberNameLists = Maps.newHashMap();
        for (MedalRecord medalRec : groupMedals) {
            GroupService.MedalOwners owners = new GroupService.MedalOwners();
            owners.medal = medalRec.toMedal();
            owners.owners = Lists.newArrayList();
            result.medals.add(owners);
            memberNameLists.put(medalRec.medalId, owners.owners);
        }
        List<EarnedMedalRecord> earnedMedals = _medalRepo.loadEarnedMedals(
            Lists.transform(groupMedals, MedalRecord.TO_MEDAL_ID));
        // go through a couple of transformations and a db query to get VizMemberNames for each
        // member.
        Set<Integer> memberIds = Sets.newHashSet(
            Iterables.transform(earnedMedals, EarnedMedalRecord.TO_MEMBER_ID));
        Map<Integer, VizMemberName> memberNames = Maps.newHashMap();
        for (VizMemberName vizMemberName :
                 toVizMemberNames(_memberRepo.loadMemberCards(memberIds))) {
            memberNames.put(vizMemberName.getId(), vizMemberName);
        }
        // now that we have each member's VizMemberName, add them to the appropriate lists and ship
        // the whole package off to the client.
        for (EarnedMedalRecord earnedMedalRec : earnedMedals) {
            List<VizMemberName> earnees = memberNameLists.get(earnedMedalRec.medalId);
            earnees.add(memberNames.get(earnedMedalRec.memberId));
        }
        return result;
    }

    // from interface GroupService
    public List<Medal> getMedals (int groupId)
        throws ServiceException
    {
        // Note that the user must already have loaded the group detail in order to be calling this
        // If the client is hacked, let 'em see the medals, no great shakes
        return Lists.newArrayList(
            Lists.transform(_medalRepo.loadGroupMedals(groupId), MedalRecord.TO_MEDAL));
    }

    // from interface GroupService
    public List<VizMemberName> searchGroupMembers (int groupId, String search)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();

        // Group member searching is currently only used for finding people to award medals to,
        // which is a manager-only ability.
        Rank rank = _groupRepo.getMembership(groupId, mrec.memberId).left;
        if (!mrec.isSupport() && rank != Rank.MANAGER) {
            log.warning("Non-manager attempted to search through group members", "memberId",
                mrec.memberId, "groupId", groupId);
            throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
        }

        // If this person is support+, we need to check if this is an official group.  If so, we
        // search across all Whirled members.
        if (mrec.isSupport()) {
            GroupRecord grec = _groupRepo.loadGroup(groupId);
            if (grec.official) {
                List<Integer> memberIds = Lists.transform(
                    _memberRepo.findMembersByDisplayName(search, MAX_MEMBER_MATCHES),
                        new Function<MemberSearchRecord, Integer>() {
                            public Integer apply (MemberSearchRecord record) {
                                return record.memberId;
                            }
                        });

                return toVizMemberNames(_memberRepo.loadMemberCards(memberIds));
            }
        }

        Set<Integer> memberIds = Sets.newHashSet(_groupRepo.getMemberIds(groupId));
        List<Integer> gmemberIds = _memberRepo.findMembersInCollection(search, memberIds);
        return toVizMemberNames(_memberRepo.loadMemberCards(gmemberIds));
    }

    // from interface GroupService
    public void awardMedal (int memberId, int medalId)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();

        // make sure the medal exists
        MedalRecord medalRec = _medalRepo.loadMedal(medalId);
        if (medalRec == null) {
            log.warning("Attempted to award an unknown medal", "medalId", medalId);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        // make sure the person calling this method has the correct permission.
        if (!canManage(mrec, medalRec.groupId)) {
            log.warning("Non-manager attempted to award a group medal", "memberId", mrec.memberId,
                "groupId", medalRec.groupId);
            throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
        }

        // Make sure the group is either official or contains this member.
        GroupRecord groupRec = _groupRepo.loadGroup(medalRec.groupId);
        if (!groupRec.official &&
            _groupRepo.getRank(medalRec.groupId, memberId) == Rank.NON_MEMBER) {
            log.warning("Attempted to grant a medal to a group non-member", "recipientId", memberId,
                "granterId", mrec.memberId, "medalId", medalId);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        // finally, award the medal.
        try {
            _medalRepo.awardMedal(memberId, medalId);

        } catch (DuplicateKeyException dke) {
            // Not really log noteworthy.  This just means the person attempting to grant the
            // medal made a mistake that our UI allows him to make.
            throw new ServiceException(GroupCodes.E_GROUP_MEMBER_HAS_MEDAL);
        }

        // publish a member message with {medal name, medal image URL, group name, group id} as the
        // data
        _feedLogic.publishMemberMessage(
            memberId, FeedMessageType.FRIEND_WON_MEDAL, medalRec.name,
            medalRec.createIconMedia(), groupRec.name, groupRec.groupId);
    }

    // from GroupService
    public Medal getMedal (int medalId)
        throws ServiceException
    {
        // Note: no access checking here, if someone manages to figure out the medal id of a medal
        // for a group they don't belong to, let 'em see it
        MedalRecord medalRec = _medalRepo.loadMedal(medalId);
        return medalRec == null ? null : medalRec.toMedal();
    }

    // from GroupService
    public void setBrandShares (int brandId, int targetId, int shares)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();

        // make sure we're allow to administer this brand
        if (_groupRepo.getMembership(brandId, mrec.memberId).left != Rank.MANAGER) {
            throw new ServiceException(MsoyAuthCodes.ACCESS_DENIED);
        }

        // make sure the target's a member of the brand's group
        if (_groupRepo.getMembership(brandId, targetId).left == Rank.NON_MEMBER) {
            // anything else is a bug or a hacked client
            throw new ServiceException(ItemCodes.E_INTERNAL_ERROR);
        }

        _groupRepo.setBrandShare(brandId, targetId, shares);
    }

    /**
     * Fill in the current number of people in rooms (population) and the number of total threads
     * for a list of group cards.
     */
    protected List<GroupCard> populateGroups (List<GroupCard> groups)
    {
        PopularPlacesSnapshot pps = _memberMan.getPPSnapshot();
        for (GroupCard card : groups) {
            PopularPlacesSnapshot.Place pcard = pps.getGroup(card.name.getGroupId());
            if (pcard != null) {
                card.population = pcard.population;
            }
        }
        return groups;
    }

    /**
     * Using a given list of member ids, sort by last time online and return one page of fully
     * resolved group member cards.
     */
    protected List<GroupMemberCard> resolveGroupMemberCards (
        int groupId, Collection<Integer> memberIds, int offset, int count)
    {
        // load a page of member cards, sorted by last online
        Map<Integer, MemberCardRecord> cards = Maps.newLinkedHashMap();
        for (MemberCardRecord mcr : _memberRepo.loadMemberCards(memberIds, offset, count, true)) {
            cards.put(mcr.memberId, mcr);
        }

        // load group membership information
        Map<Integer, GroupMembershipRecord> gmrecs = Maps.newHashMap();
        for (GroupMembershipRecord gmrec : _groupRepo.getMembers(groupId, cards.keySet())) {
            gmrecs.put(gmrec.memberId, gmrec);
        }

        // finally convert everything to runtime records in last online order
        List<GroupMemberCard> result = Lists.newArrayList();
        for (MemberCardRecord mcr : cards.values()) {
            GroupMembershipRecord gmrec = gmrecs.get(mcr.memberId);
            if (gmrec != null) {
                result.add(mcr.toMemberCard(gmrec.toGroupMemberCard()));
            }
        }

        return result;
    }

    /**
     * Checks if the membership info of an actor is sufficient to perform rank changes or remove
     * another member.
     */
    protected boolean mayChangeOtherMember (Tuple<Rank, Long> actor, Tuple<Rank, Long> target)
    {
        // managers can change non managers and other managers who were promoted later
        Rank mgr = Rank.MANAGER;
        return actor.left == mgr && (target.left != mgr || actor.right < target.right);
    }

    protected boolean canManage (MemberRecord mrec, int groupId)
    {
        return mrec.isSupport() || _groupRepo.getRank(groupId, mrec.memberId) == Rank.MANAGER;
    }

    protected static List<VizMemberName> toVizMemberNames (Iterable<MemberCardRecord> records)
    {
        return Lists.newArrayList(
            Iterables.transform(records, new Function<MemberCardRecord, VizMemberName>() {
            public VizMemberName apply (MemberCardRecord record) {
                MemberCard card = record.toMemberCard();
                return VizMemberName.create(card.name, card.photo);
            }
        }));
    }

    protected static boolean isVisible (
        MemberRecord mrec, Group.Policy policy, GroupMembership.Rank rank)
    {
        if (mrec != null && mrec.isSupport()) {
            return true;
        }
        rank = (rank == null ? GroupMembership.Rank.NON_MEMBER : rank);
        return policy != Group.Policy.EXCLUSIVE || rank != GroupMembership.Rank.NON_MEMBER;
    }

    // our dependencies
    @Inject protected FeedLogic _feedLogic;
    @Inject protected ForumLogic _forumLogic;
    @Inject protected ForumRepository _forumRepo;
    @Inject protected GroupLogic _groupLogic;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected MailRepository _mailRepo;
    @Inject protected MedalRepository _medalRepo;
    @Inject protected MemberManager _memberMan;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MsoyChatChannelManager _channelMan;
    @Inject protected MsoySceneRepository _sceneRepo;
    @Inject protected RoomLogic _roomLogic;
    @Inject protected TagLogic _tagLogic;
    @Inject protected ThemeLogic _themeLogic;
    @Inject protected ThemeRepository _themeRepo;

    /** The number of matches to return when searching against all display names in the database. */
    protected static int MAX_MEMBER_MATCHES = 100;
}
