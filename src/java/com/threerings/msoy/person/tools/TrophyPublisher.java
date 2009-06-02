//
// $Id$

package com.threerings.msoy.person.tools;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import com.samskivert.depot.PersistenceContext;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.util.Lifecycle;

import com.threerings.msoy.server.ServerConfig;

import com.threerings.msoy.game.server.persist.GameInfoRecord;
import com.threerings.msoy.game.server.persist.MsoyGameRepository;
import com.threerings.msoy.item.server.persist.TrophySourceRecord;
import com.threerings.msoy.item.server.persist.TrophySourceRepository;

import com.threerings.msoy.person.server.FeedLogic;

/**
 * Tests the publishing of a trophy.
 */
public class TrophyPublisher
{
    /** Configures dependencies needed by the Msoy servers. */
    public static class Module extends AbstractModule
    {
        @Override protected void configure () {
            bind(PersistenceContext.class).toInstance(new PersistenceContext());
        }
    }

    public static void main (String[] args)
        throws Exception
    {
        if (args.length < 3) {
            throw new Exception("Usage: TrophyPublisher memberId gameId trophy_ident");
        }

        int memberId = Integer.parseInt(args[0]);
        int gameId = Integer.parseInt(args[1]);
        String trophyIdent = args[2];

        final Injector injector = Guice.createInjector(new Module());
        final Lifecycle cycle = injector.getInstance(Lifecycle.class);
        final FeedLogic feedLogic = injector.getInstance(FeedLogic.class);
        final MsoyGameRepository gameRepo = injector.getInstance(MsoyGameRepository.class);
        final TrophySourceRepository trophyRepo =
            injector.getInstance(TrophySourceRepository.class);
        // initialize our persistence context
        final PersistenceContext perCtx = injector.getInstance(PersistenceContext.class);
        ConnectionProvider conprov = ServerConfig.createConnectionProvider();
        perCtx.init("msoy", conprov, null);
        perCtx.initializeRepositories(true);

        try {
            GameInfoRecord game = gameRepo.loadGame(gameId);
            if (game == null) {
                throw new Exception("No such game with id " + gameId);
            }

            TrophySourceRecord trophy = null;
            for (TrophySourceRecord trec : trophyRepo.loadGameOriginals(gameId)) {
                if (trec.ident.equals(trophyIdent)) {
                    trophy = trec;
                    break;
                }
            }
            if (trophy == null) {
                throw new Exception("No trophy '" + trophyIdent + "' in " + game.name + ".");
            }

            System.out.println("Awarding " + trophy.name + " from " + game.name + "...");
            feedLogic.publishTrophyEarned(memberId, trophy.name, trophy.getThumbMediaDesc(),
                                          gameId, game.name, game.description);

        } finally {
            cycle.shutdown();
        }
    }
}
