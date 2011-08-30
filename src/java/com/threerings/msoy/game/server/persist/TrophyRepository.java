//
// $Id$

package com.threerings.msoy.game.server.persist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.game.gwt.GameInfo;
import com.threerings.msoy.game.server.GameUtil;

import static com.threerings.msoy.Log.log;

/**
 * Manages the trophy persistent storage.
 */
@Singleton @BlockingThread
public class TrophyRepository extends DepotRepository
{
    @Inject public TrophyRepository (PersistenceContext perCtx)
    {
        super(perCtx);
    }

    /**
     * Loads all of the specified member's trophies for non-development games.
     */
    public List<TrophyRecord> loadTrophies (int memberId)
    {
        return from(TrophyRecord.class).where(
            TrophyRecord.MEMBER_ID.eq(memberId), TrophyRecord.GAME_ID.greaterThan(0)).select();
    }

    /**
     * Loads the specified number of recently earned trophies for the specified member.
     */
    public List<TrophyRecord> loadRecentTrophies (int memberId, int count)
    {
        return from(TrophyRecord.class).
            where(TrophyRecord.MEMBER_ID.eq(memberId), TrophyRecord.GAME_ID.greaterThan(0)).
            descending(TrophyRecord.WHEN_EARNED).limit(count).select();
    }

    /**
     * Loads all of the specified member's trophies from the specified game.
     */
    public List<TrophyRecord> loadTrophies (int gameId, int memberId)
    {
        return from(TrophyRecord.class).where(
            TrophyRecord.GAME_ID, gameId, TrophyRecord.MEMBER_ID, memberId).select();
    }

    /**
     * Counts the number of trophies a user has for a given game.
     */
    public int countTrophies (int gameId, int memberId)
    {
        return from(TrophyRecord.class).where(
            TrophyRecord.GAME_ID.eq(gameId), TrophyRecord.MEMBER_ID.eq(memberId)).selectCount();
    }

    /**
     * Counts the total number of trophies a user has for all games. This ends up getting shown
     * in the Facebook status panel.
     * TODO: measure the performance and consider caching in MemberRecord (if FB userbase grows)
     */
    public int countTrophies (int memberId)
    {
        return from(TrophyRecord.class).where(TrophyRecord.MEMBER_ID.eq(memberId)).selectCount();
    }

    /**
     * Returns a list of the idents of the trophies owned by the specified player for the specified
     * game.
     */
    public List<String> loadTrophyOwnership (int gameId, int memberId)
    {
        ArrayList<String> idents = Lists.newArrayList();
        for (TrophyOwnershipRecord orec : from(TrophyOwnershipRecord.class).where(
                 TrophyRecord.GAME_ID, gameId, TrophyRecord.MEMBER_ID, memberId).select()) {
            idents.add(orec.ident);
        }
        return idents;
    }

    /**
     * Removes all of the in-development trophies for the given game and player.
     */
    public void removeDevelopmentTrophies (int gameId, int memberId)
    {
        if (!GameUtil.isDevelopmentVersion(gameId)) {
            log.warning("Attempted to remove trophies for a non-development game", "gameId",
                gameId, "memberId", memberId);
            return;
        }
        from(TrophyRecord.class).where(
            TrophyRecord.MEMBER_ID.eq(memberId), TrophyRecord.GAME_ID.eq(gameId)).delete();
    }

    /**
     * Stores the supplied trophy record in the database.
     */
    public void storeTrophy (TrophyRecord trophy)
    {
        insert(trophy);
    }

    /**
     * Deletes all data associated with the supplied members. This is done as a part of purging
     * member accounts.
     */
    public void purgeMembers (Collection<Integer> memberIds)
    {
        from(TrophyRecord.class).where(TrophyRecord.MEMBER_ID.in(memberIds)).delete();
    }

    /**
     * Deletes all awarded trophies for the specified game.
     */
    public void purgeGame (int gameId)
    {
        from(TrophyRecord.class).where(TrophyRecord.GAME_ID, gameId).delete(null);
        int devGameId = GameInfo.toDevId(gameId);
        from(TrophyRecord.class).where(TrophyRecord.GAME_ID, devGameId).delete(null);
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(TrophyRecord.class);
    }
}
