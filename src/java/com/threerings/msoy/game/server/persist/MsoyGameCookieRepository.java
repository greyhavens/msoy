package com.threerings.msoy.game.server.persist;

import java.util.Collections;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.samskivert.depot.PersistenceContext;
import com.whirled.game.server.persist.GameCookieRepository;

/**
 * Repo providing msoy-specific game cookie operations.
 */
@Singleton
public class MsoyGameCookieRepository extends GameCookieRepository
{
    @Inject public MsoyGameCookieRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    /**
     * Gets the subset of players who have a cookie for the given game id.
     */
    public Set<Integer> getCookiedPlayers (int gameId, Set<Integer> playerIds)
    {
        // TODO
        // select "memberId", count(*) from "GameCookieRecord" where "gameId" = {gameId} and
        //     "memberId" in {memberIds} group by "memberId";
        return Collections.emptySet();
    }
}
