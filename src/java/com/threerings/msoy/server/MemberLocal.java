//
// $Id$

package com.threerings.msoy.server;

import java.util.List;

import com.google.common.collect.Lists;

import com.threerings.util.StreamableArrayIntSet;
import com.threerings.util.StreamableHashIntMap;

import com.threerings.crowd.server.BodyLocal;
import com.threerings.stats.data.StatSet;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.PlayerMetrics;

import com.threerings.msoy.badge.data.BadgeType;
import com.threerings.msoy.badge.data.EarnedBadgeSet;
import com.threerings.msoy.badge.data.InProgressBadgeSet;
import com.threerings.msoy.badge.data.all.EarnedBadge;
import com.threerings.msoy.badge.data.all.InProgressBadge;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.notify.data.Notification;
import com.threerings.msoy.party.data.PartySummary;
import com.threerings.msoy.room.data.EntityMemoryEntry;
import com.threerings.msoy.room.data.RoomObject;
import com.threerings.msoy.room.server.RoomManager;

import static com.threerings.msoy.Log.log;

/**
 * Contains server-side only information for a member.
 */
public class MemberLocal extends BodyLocal
{
    /** The number of non-idle seconds that have elapsed in this member's session. When the member
     * is forwarded between servers, this value is incremented by the time they spent on the server
     * from which they are departing. */
    public int sessionSeconds;

    /** Statistics tracked for this player. */
    public StatSet stats;

    /** Metrics tracked for this player. */
    public PlayerMetrics metrics = new PlayerMetrics();

    /** The set of badges that this player owns. */
    public EarnedBadgeSet badges;

    /** The version of the "Badges set" that this user has seen. If this is behind
     * BadgeType.VERSION, then the member's InProgressBadges will be recomputed. */
    public short badgesVersion;

    /** The set of badges that this player is working towards. */
    public InProgressBadgeSet inProgressBadges;

    /** A list of notifications that will be dispatched when the client's NotificationDirector asks
     * for them. Will be null once the deferred notifications have been dispatched. */
    public List<Notification> deferredNotifications;

    /** Rooms we've visited during our current Whirled Tour. */
    public StreamableArrayIntSet touredRooms;

    /** The memories of the member's avatar. */
    public List<EntityMemoryEntry> memories;

    /** Info on the party this member is currently rocking (or null if they're dull). */
    public PartySummary party;

    /**
     * Adds an EarnedBadge to the member's BadgeSet (or updates the existing badge if the badge
     * level has increased) and dispatches an event indicating that a new badge was awarded.
     *
     * @return true if the badge was updated in the member's BadgeSet, and false otherwise.
     */
    public boolean badgeAwarded (EarnedBadge badge)
    {
        boolean added = badges.addOrUpdateBadge(badge);

        // remove this badge's associated InProgressBadge if the badge's highest level has
        // been reached
        BadgeType badgeType = BadgeType.getType(badge.badgeCode);
        if (badgeType != null && badge.level >= badgeType.getNumLevels() - 1) {
            inProgressBadges.removeBadge(badge.badgeCode);
        }

        return added;
    }

    /**
     * Adds an InProgressBadge to the member's in-progress badge set (or updates the existing badge
     * if the badge level has increased).
     *
     * @return true if the badge was updated in the member's in-progress badge set, and false
     * otherwise.
     */
    public boolean inProgressBadgeUpdated (InProgressBadge badge)
    {
        return inProgressBadges.addOrUpdateBadge(badge);
    }

    /**
     * Called when a player has just switched from one avatar to a new one or by {@link #willEnter}
     * below. In either case, {@link #memories} is expected to contain the memories for the avatar;
     * either because it was put there (and possibly serialized in the case of a peer move) when
     * the player left a previous room, or because we put them there manually as part of avatar
     * resolution (see {@link MemberManager#finishSetAvatar}).
     */
    public void putAvatarMemoriesIntoRoom (RoomObject roomObj)
    {
        if (memories == null) {
            return;
        }
        roomObj.startTransaction();
        try {
            for (EntityMemoryEntry entry : memories) {
                if (roomObj.memories.contains(entry)) {
                    log.warning("WTF? Room already contains memory entry",
                                "room", roomObj.getOid(), "entry", entry);
                } else {
                    roomObj.addToMemories(entry);
                }
            }
        } finally {
            roomObj.commitTransaction();
        }
        memories = null;
    }

    /**
     * Called when we depart a room to remove our avatar memories from the room and store them in
     * this local storage.
     */
    public void takeAvatarMemoriesFromRoom (MemberObject memobj, RoomObject roomObj)
    {
        List<EntityMemoryEntry> mems = null;
        if (memobj.avatar != null) {
            // any memories we deposited in the room for safe-keeping must be enumerated
            mems = Lists.newArrayList();
            ItemIdent avatar = memobj.avatar.getIdent();
            for (EntityMemoryEntry entry : roomObj.memories) {
                if (avatar.equals(entry.item)) {
                    mems.add(entry);
                }
            }
            if (mems.size() > 0) {
                // and removed from the room
                roomObj.startTransaction();
                try {
                    for (EntityMemoryEntry entry : mems) {
                        roomObj.removeFromMemories(entry.getKey());
                    }
                } finally {
                    roomObj.commitTransaction();
                }
            }
        }
        memobj.getLocal(MemberLocal.class).memories = mems;
    }

    /**
     * Called when we enter or leave a party.
     */
    public void updateParty (MemberObject memobj, PartySummary party)
    {
        if (party == null) {
            memobj.setPartyId(0);
            this.party = null;
        } else {
            memobj.setPartyId(party.id);
            this.party = party;
        }
    }

    /**
     * Called by the {@link RoomManager} when we're about to enter a room.
     */
    public void willEnter (MemberObject memobj, RoomObject roomObj)
    {
        roomObj.startTransaction();
        try {
            // add our avatar memories to this room
            putAvatarMemoriesIntoRoom(roomObj);

            // if we're in a party, maybe put our party summary in the room as well
            if (party != null && !roomObj.parties.containsKey(party.id)) {
                roomObj.addToParties(party);
            }

        } finally {
            roomObj.commitTransaction();
        }
    }

    /**
     * Called by the {@link RoomManager} when we're about to leave a room.
     */
    public void willLeave (MemberObject memobj, RoomObject roomObj)
    {
        roomObj.startTransaction();
        try {
            // remove our avatar memories from this room
            takeAvatarMemoriesFromRoom(memobj, roomObj);

            // if we're in a party and the last member to leave this room, clean up our bits
            if (party != null && !roomObj.parties.containsKey(party.id)) {
                roomObj.removeFromParties(party.id);
                party = null;
            }

        } finally {
            roomObj.commitTransaction();
        }
    }
}
