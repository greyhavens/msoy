//
// $Id$

package com.threerings.msoy.group.server;

import java.util.Collection;
import java.util.Map;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DuplicateKeyException;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.room.server.persist.MsoySceneRepository;
import com.threerings.msoy.room.server.persist.SceneRecord;
import com.threerings.msoy.server.MemberNodeActions;
import com.threerings.msoy.server.StatLogic;

import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.group.data.all.Group;
import com.threerings.msoy.group.data.all.GroupMembership;
import com.threerings.msoy.group.gwt.GroupCard;
import com.threerings.msoy.group.gwt.GroupCodes;
import com.threerings.msoy.group.gwt.GroupExtras;
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
     * Create a new group
     */
    public Group createGroup (MemberRecord mrec, Group group, GroupExtras extras)
        throws ServiceException
    {
        // make sure the name is valid; this is checked on the client as well
        if (!isValidName(group.name)) {
            log.warning("Asked to create group with invalid name [for=" + mrec.who() +
                    ", name=" + group.name + "].");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        if (group.official && !mrec.isAdmin()) {
            log.warning("Non-admin creating official group", "member", mrec.who());
            throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
        }

        try {
            final GroupRecord grec = new GroupRecord();
            grec.name = group.name;
            grec.blurb = group.blurb;
            grec.policy = group.policy;
            grec.forumPerms = group.forumPerms;
            grec.partyPerms = group.partyPerms;
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
        }
    }

    /**
     * Updates information for a particular group.
     */
    public void updateGroup (MemberRecord mrec, Group group, GroupExtras extras)
        throws ServiceException
    {
        // make sure the name is valid; this is checked on the client as well
        if (!isValidName(group.name)) {
            log.warning("Asked to update group with invalid name [for=" + mrec.who() +
                    ", name=" + group.name + "].");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        try {
            if (!mrec.isSupport() &&
                _groupRepo.getRank(group.groupId, mrec.memberId) != GroupMembership.RANK_MANAGER) {
                log.warning("in updateGroup, invalid permissions");
                throw new ServiceException("m.invalid_permissions");
            }

            if (group.official && !mrec.isAdmin()) {
                log.warning("Non-admin updating group to be official", "member", mrec.who());
                throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
            }

            GroupRecord grec = _groupRepo.loadGroup(group.groupId);
            if (grec == null) {
                log.warning("Cannot update non-existent group", "id", group.groupId);
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
            cmap.get(srec.sceneId).homeSnapshot = srec.getSnapshot();
        }
    }

    protected static boolean isValidName (String name)
    {
        return Character.isLetterOrDigit(name.charAt(0));
    }

    // our dependencies
    @Inject protected StatLogic _statLogic;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected MsoySceneRepository _sceneRepo;
}
