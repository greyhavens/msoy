//
// $Id$

package com.threerings.msoy.game.server.persist;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.operator.Logic.And;
import com.samskivert.depot.operator.Conditionals.Equals;
import com.samskivert.depot.operator.Conditionals.GreaterThan;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.item.data.all.Game;

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
        return findAll(TrophyRecord.class, new Where(new And(
            new Equals(TrophyRecord.MEMBER_ID, memberId),
            new GreaterThan(TrophyRecord.GAME_ID, 0))));
    }

    /**
     * Loads the specified number of recently earned trophies for the specified member.
     */
    public List<TrophyRecord> loadRecentTrophies (int memberId, int count)
    {
        Where whereClause = new Where(new And(
            new Equals(TrophyRecord.MEMBER_ID, memberId),
            new GreaterThan(TrophyRecord.GAME_ID, 0)));
        return findAll(TrophyRecord.class, whereClause,
                       OrderBy.descending(TrophyRecord.WHEN_EARNED),
                       new Limit(0, count));
    }

    /**
     * Loads all of the specified member's trophies from the specified game.
     */
    public List<TrophyRecord> loadTrophies (int gameId, int memberId)
    {
        return findAll(TrophyRecord.class, new Where(TrophyRecord.GAME_ID, gameId,
                                                     TrophyRecord.MEMBER_ID, memberId));
    }

    /**
     * Returns a list of the idents of the trophies owned by the specified player for the specified
     * game.
     */
    public List<String> loadTrophyOwnership (int gameId, int memberId)
    {
        ArrayList<String> idents = new ArrayList<String>();
        Where where = new Where(TrophyRecord.GAME_ID, gameId, TrophyRecord.MEMBER_ID, memberId);
        for (TrophyOwnershipRecord orec : findAll(TrophyOwnershipRecord.class, where)) {
            idents.add(orec.ident);
        }
        return idents;
    }

    /**
     * Removes all of the in-development trophies for the given game and player.
     */
    public void removeDevelopmentTrophies (int gameId, int memberId)
    {
        if (!Game.isDevelopmentVersion(gameId)) {
            log.warning("Attempted to remove trophies for a non-development game", "gameId",
                gameId, "memberId", memberId);
            return;
        }

        deleteAll(TrophyRecord.class, new Where(new And(
            new Equals(TrophyRecord.MEMBER_ID, memberId),
            new Equals(TrophyRecord.GAME_ID, gameId))));
    }

    /**
     * Stores the supplied trophy record in the database.
     */
    public void storeTrophy (TrophyRecord trophy)
    {
        insert(trophy);
    }

    /**
     * Deletes all trophies held by the supplied member.
     */
    public void deleteTrophies (final int memberId)
    {
        deleteAll(TrophyRecord.class, new Where(TrophyRecord.MEMBER_ID, memberId));
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(TrophyRecord.class);
    }
}
