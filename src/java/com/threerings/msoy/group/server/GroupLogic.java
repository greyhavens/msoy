//
// $Id$

package com.threerings.msoy.group.server;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DuplicateKeyException;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.data.all.GroupName;

import com.threerings.msoy.server.MemberNodeActions;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.StatLogic;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.admin.data.CostsConfigObject;
import com.threerings.msoy.admin.server.RuntimeConfig;

import com.threerings.msoy.room.server.persist.MsoySceneRepository;
import com.threerings.msoy.room.server.persist.SceneRecord;

import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PriceQuote;
import com.threerings.msoy.money.data.all.PurchaseResult;
import com.threerings.msoy.money.server.MoneyLogic;

import com.threerings.msoy.group.data.all.Group;
import com.threerings.msoy.group.data.all.GroupMembership;
import com.threerings.msoy.group.data.all.Group.Policy;
import com.threerings.msoy.group.data.all.GroupMembership.Rank;
import com.threerings.msoy.group.gwt.GroupCard;
import com.threerings.msoy.group.gwt.GroupCodes;
import com.threerings.msoy.group.gwt.GroupExtras;
import com.threerings.msoy.group.server.persist.GroupMembershipRecord;
import com.threerings.msoy.group.server.persist.GroupRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;

import com.threerings.msoy.web.gwt.ServiceCodes;
import com.threerings.msoy.web.gwt.ServiceException;

import static com.threerings.msoy.Log.log;

/**
 * Contains group related services used by servlets and other blocking thread code.
 */
@BlockingThread @Singleton
public class GroupLogic
{
    /**
     * Return a price quote for creating a new group.
     */
    public PriceQuote quoteCreateGroup (MemberRecord mrec)
        throws ServiceException
    {
        return _moneyLogic.securePrice(mrec.memberId, GROUP_PURCHASE_KEY,
            Currency.COINS, getGroupCoinCost());
    }

    /**
     * Create a new group
     */
    public PurchaseResult<Group> createGroup (
        final MemberRecord mrec, Group group, GroupExtras extras,
        Currency currency, int authedAmount)
        throws ServiceException
    {
        // make sure the name is valid; this is checked on the client as well
        if (!GroupName.isValidName(group.name)) {
            log.warning("Asked to create group with invalid name",
                "member", mrec.who(), "name", group.name);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        if (group.official && !mrec.isAdmin()) {
            log.warning("Non-admin creating official group", "member", mrec.who());
            throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
        }

        final GroupRecord grec = new GroupRecord();
        grec.name = group.name;
        grec.blurb = group.blurb;
        grec.policy = group.policy;
        grec.forumPerms = group.getForumPerms();
        grec.partyPerms = group.partyPerm;
        grec.official = group.official;
        if (group.logo != null) {
            grec.logoMimeType = group.logo.mimeType;
            grec.logoMediaHash = group.logo.hash;
            grec.logoMediaConstraint = group.logo.constraint;
        }
        grec.homepageUrl = extras.homepageUrl;
        grec.charter = extras.charter;
        grec.catalogItemType = extras.catalogItemType;
        grec.catalogTag = extras.catalogTag;

        // we fill this in ourselves
        grec.creatorId = mrec.memberId;

        // execute the purchase
        PurchaseResult<Group> result = _moneyLogic.buyGroup(
            mrec, GROUP_PURCHASE_KEY, currency, authedAmount, Currency.COINS, getGroupCoinCost(),
            grec.name, new MoneyLogic.BuyOperation<Group>() {
            public Group create (boolean magicFree, Currency currency, int amountPaid)
                throws ServiceException
            {
                try {
                    // create the group and then add the creator to it
                    _groupRepo.createGroup(grec);
                    _groupRepo.joinGroup(grec.groupId, grec.creatorId, Rank.MANAGER);
                } catch (DuplicateKeyException dke) {
                    // inform the user that the name is already in use
                    throw new ServiceException(GroupCodes.E_GROUP_NAME_IN_USE);
                }
                return grec.toGroupObject();
            }
        }).toPurchaseResult();

        // if the creator is online, update their runtime data
        try {
            GroupMembership gm = new GroupMembership();
            gm.group = grec.toGroupName();
            gm.rank = Rank.MANAGER;
            MemberNodeActions.joinedGroup(grec.creatorId, gm);
        } catch (Exception e) { // don't let this booch the purchase
            log.warning("Error notifying of new group", "memberId", mrec.memberId, e);
        }

        return result;
    }

    /**
     * Updates information for a particular group.
     */
    public void updateGroup (MemberRecord mrec, Group group, GroupExtras extras)
        throws ServiceException
    {
        // make sure the name is valid; this is checked on the client as well
        if (!GroupName.isValidName(group.name)) {
            log.warning("Asked to update group with invalid name",
                "member", mrec.who(), "name", group.name);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        try {
            if (!mrec.isSupport() &&
                _groupRepo.getRank(group.groupId, mrec.memberId) != Rank.MANAGER) {
                log.warning("in updateGroup, invalid permissions");
                throw new ServiceException("m.invalid_permissions");
            }

            if (group.official && !mrec.isAdmin()) {
                log.warning("Non-admin updating group to be official", "member", mrec.who());
                throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
            }
            // make sure they're not making a tagged group exclusive
            if ((group.policy == Group.Policy.EXCLUSIVE) &&
                    !_groupRepo.getTagRepository().getTags(group.groupId).isEmpty()) {
                throw new ServiceException(GroupCodes.E_GROUP_TAGS_ON_EXCLUSIVE);
            }

            GroupRecord grec = _groupRepo.loadGroup(group.groupId);
            if (grec == null) {
                log.warning("Cannot update non-existent group", "id", group.groupId);
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
            }
            // TEMP: block editing group name (except for support+)
            if (!mrec.isSupport() && !group.name.equals(grec.name)) {
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
            }

            Map<ColumnExp, Object> updates = grec.findUpdates(group, extras);
            if (updates.size() > 0) {
                _groupRepo.updateGroup(group.groupId, updates);
            }

        } catch (DuplicateKeyException dke) {
            throw new ServiceException(GroupCodes.E_GROUP_NAME_IN_USE);
        }
    }

    /**
     * Resolves the {@link GroupCard#homeSnapshot} data for the supplied set of cards.
     */
    public void resolveSnapshots (Collection<GroupCard> cards)
    {
        Map<Integer, GroupCard> cmap = Maps.newHashMap();
        for (GroupCard card : cards) {
            cmap.put(card.homeSceneId, card);
        }
        for (SceneRecord srec : _sceneRepo.loadScenes(cmap.keySet())) {
            cmap.get(srec.sceneId).homeSnapshot = srec.getSnapshotFull();
        }
    }

    /**
     * Gets the groups that the given member belongs to.
     */
    public Set<Integer> getMemberGroupIds (int memberId)
    {
        Set<Integer> groupIds = Sets.newHashSet();
        for (GroupMembershipRecord gmrec : _groupRepo.getMemberships(memberId)) {
            groupIds.add(gmrec.groupId);
        }
        groupIds.add(ServerConfig.getAnnounceGroupId());
        return groupIds;
    }

    /**
     * Gets the groups with an EXLUSIVE policy with respect to the given member id.
     * @param groupIds the ids of the groups that the user is a member of. If null, the member's
     *     groups will be loaded too
     */
    public Set<Integer> getHiddenGroupIds (int memberId, Set<Integer> groupIds)
    {
        Set<Integer> exclusive = Sets.newHashSet();
        exclusive.addAll(_groupRepo.getGroupIdsWithPolicy(Policy.EXCLUSIVE));
        exclusive.removeAll(groupIds == null ? getMemberGroupIds(memberId) : groupIds);
        return exclusive;
    }

    /**
     * Return the current cost of forming a new group, in coins.
     */
    protected int getGroupCoinCost ()
    {
        return _runtime.getCoinCost(CostsConfigObject.NEW_GROUP);
    }

    /** An arbitrary key for quoting group creation (purchase). */
    protected static final Object GROUP_PURCHASE_KEY = new Object();

    // our dependencies
    @Inject protected GroupRepository _groupRepo;
    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected MsoySceneRepository _sceneRepo;
    @Inject protected RuntimeConfig _runtime;
    @Inject protected StatLogic _statLogic;
}
