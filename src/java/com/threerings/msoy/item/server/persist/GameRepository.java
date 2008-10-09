//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.IntIntMap;

import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Computed;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.clause.GroupBy;
import com.samskivert.jdbc.depot.clause.Join;
import com.samskivert.jdbc.depot.clause.Limit;
import com.samskivert.jdbc.depot.clause.OrderBy;
import com.samskivert.jdbc.depot.clause.QueryClause;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.expression.SQLExpression;
import com.samskivert.jdbc.depot.operator.Conditionals;
import com.samskivert.jdbc.depot.operator.Logic.And;
import com.samskivert.jdbc.depot.operator.SQLOperator;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.server.persist.TagHistoryRecord;
import com.threerings.msoy.server.persist.TagRecord;

/**
 * Manages the persistent store of {@link Game} items.
 */
@Singleton
public class GameRepository extends ItemRepository<GameRecord>
{
    @Entity(name="GameTagRecord")
    public static class GameTagRecord extends TagRecord
    {
    }

    @Entity(name="GameTagHistoryRecord")
    public static class GameTagHistoryRecord extends TagHistoryRecord
    {
    }

    /** Used by {@link #loadGenreCounts}. */
    @Entity @Computed(shadowOf=GameRecord.class)
    public static class GenreCountRecord extends PersistentRecord
    {
        /** The genre in question. */
        public byte genre;

        /** The number of games in that genre .*/
        @Computed(fieldDefinition="count(*)")
        public int count;
    }

    @Inject public GameRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    /**
     * Loads the count of how many listed games we have in each genre.
     */
    public IntIntMap loadGenreCounts ()
    {
        IntIntMap counts = new IntIntMap();
        for (GenreCountRecord gcr : findAll(
                 GenreCountRecord.class, new Join(getItemClass(), ItemRecord.ITEM_ID,
                                                  getCatalogClass(), CatalogRecord.LISTED_ITEM_ID),
                 new GroupBy(getItemColumn(GameRecord.GENRE)))) {
            counts.put(gcr.genre, gcr.count);
        }
        return counts;
    }

    /**
     * Loads all listed game records in the specified genre, sorted from highest to lowest rating.
     *
     * @param genre the genre of game record to load or -1 to load all (listed) games.
     * @param limit a limit to the number of records loaded or <= 0 to load all records.
     */
    public List<GameRecord> loadGenre (byte genre, int limit)
    {
        return loadGenre(genre, limit, null);
    }

    /**
     * Loads all listed game records in the specified genre, sorted from highest to lowest rating.
     *
     * @param genre the genre of game record to load or -1 to load all (listed) games.
     * @param limit a limit to the number of records loaded or <= 0 to load all records.
     * @param search string to search for in the title, tags and description
     */
    public List<GameRecord> loadGenre (byte genre, int limit, String search)
    {
        List<QueryClause> clauses = Lists.newArrayList();
        clauses.add(new Join(getItemClass(), ItemRecord.ITEM_ID,
                             getCatalogClass(), CatalogRecord.LISTED_ITEM_ID));
        if (limit > 0) {
            clauses.add(new Limit(0, limit));
        }

        // sort out the primary and secondary order by clauses
        List<SQLExpression> obExprs = Lists.newArrayList();
        List<OrderBy.Order> obOrders = Lists.newArrayList();
        addOrderByRating(obExprs, obOrders);
        clauses.add(new OrderBy(obExprs.toArray(new SQLExpression[obExprs.size()]),
                                obOrders.toArray(new OrderBy.Order[obOrders.size()])));

        // build the where clause with genre and/or search string
        List<SQLOperator> whereBits = Lists.newArrayList();
        if (genre >= 0) {
            whereBits.add(new Conditionals.Equals(GameRecord.GENRE_C, genre));
        }
        if (search != null && search.length() > 0) {
            whereBits.add(buildSearchClause(search));
        }
        if (whereBits.size() > 0) {
            clauses.add(new Where(new And(whereBits)));
        }

        // finally fetch all the game records of interest
        return findAll(getItemClass(), clauses);
    }

    /**
     * Updates the game id of the specified (original) game item record.
     */
    public void updateGameId (int itemId, int gameId)
    {
        updatePartial(getItemClass(), itemId, GameRecord.GAME_ID, gameId);
    }

    @Override // from ItemRepository
    protected Class<GameRecord> getItemClass ()
    {
        return GameRecord.class;
    }

    @Override // from ItemRepository
    protected Class<CatalogRecord> getCatalogClass ()
    {
        return coerceCatalog(GameCatalogRecord.class);
    }

    @Override // from ItemRepository
    protected Class<CloneRecord> getCloneClass ()
    {
        return coerceClone(GameCloneRecord.class);
    }

    @Override // from ItemRepository
    protected Class<RatingRecord> getRatingClass ()
    {
        return coerceRating(GameRatingRecord.class);
    }

    @Override // from ItemRepository
    protected TagRecord createTagRecord ()
    {
        return new GameTagRecord();
    }

    @Override // from ItemRepository
    protected TagHistoryRecord createTagHistoryRecord ()
    {
        return new GameTagHistoryRecord();
    }
}
