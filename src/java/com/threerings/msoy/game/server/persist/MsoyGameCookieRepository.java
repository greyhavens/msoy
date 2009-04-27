package com.threerings.msoy.game.server.persist;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Computed;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.QueryClause;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.operator.Conditionals;
import com.samskivert.depot.operator.Logic;
import com.whirled.game.server.persist.GameCookieRecord;
import com.whirled.game.server.persist.GameCookieRepository;

/**
 * Repo providing msoy-specific game cookie operations.
 */
@Singleton
public class MsoyGameCookieRepository extends GameCookieRepository
{
    /**
     * Computed record for checking if a player has a cookie without actually loading it. It would
     * be protected but depot needs to create it by reflection.
     */
    @Entity @Computed
    public static class HasCookieRecord extends PersistentRecord
    {
        // hmm, this is annoying. I have to provide a name mathcing the ColExp here
        @Computed(shadowOf=GameCookieRecord.class)
        public int USER_ID;
    }

    @Inject public MsoyGameCookieRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    /**
     * Gets the subset of players who have a cookie for the given game id.
     */
    public Set<Integer> getCookiedPlayers (int gameId, Set<Integer> playerIds)
    {
        if (playerIds.isEmpty()) {
            return playerIds;
        }

        List<QueryClause> clauses = Lists.newArrayList(
            new Where(new Logic.And(
                new Conditionals.Equals(GameCookieRecord.GAME_ID, gameId),
                new Conditionals.In(GameCookieRecord.USER_ID, playerIds))),
            new FromOverride(GameCookieRecord.class));

        playerIds = Sets.newHashSet();
        for (HasCookieRecord count : findAll(HasCookieRecord.class, clauses)) {
            playerIds.add(count.USER_ID);
        }
        return playerIds;
    }
}
