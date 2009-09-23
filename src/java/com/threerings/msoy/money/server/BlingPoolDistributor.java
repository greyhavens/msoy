//
// $Id$

package com.threerings.msoy.money.server;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.google.inject.Inject;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

import com.samskivert.util.Calendars;
import com.samskivert.util.Lifecycle;

import com.threerings.util.MessageBundle;

import com.threerings.msoy.admin.data.MoneyConfigObject;
import com.threerings.msoy.admin.server.RuntimeConfig;

import com.threerings.msoy.game.server.persist.GameInfoRecord;
import com.threerings.msoy.game.server.persist.GamePlayRecord;
import com.threerings.msoy.game.server.persist.MsoyGameRepository;

import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.TransactionType;
import com.threerings.msoy.money.server.persist.MoneyConfigRecord;
import com.threerings.msoy.money.server.persist.MoneyRepository;
import com.threerings.msoy.server.persist.CharityRecord;
import com.threerings.msoy.server.persist.MemberRepository;

import static com.threerings.msoy.Log.log;

/**
 * Responsible for distributing bling to creators of games that players have played on a daily
 * basis.  The amount to distribute is governed by the {@link MoneyConfigObject#blingPoolSize}
 * run-time configuration.  This is scheduled to run at 3:00a every day.  The amount of bling each
 * game creator receives is based on the amount of time players played their game, compared to the
 * total amount of time that players played games that day.
 *
 * <p> This class will guarantee that no other thread or process is running the distributor at the
 * same time, though all world servers will attempt to execute this at the same time.  This allows
 * for redundancy in case any one server is down.
 */
public class BlingPoolDistributor
    implements Lifecycle.Component
{
    @Inject public BlingPoolDistributor (Lifecycle cycle)
    {
        cycle.addComponent(this);
    }

    // from interface Lifecycle.Component
    public void init ()
    {
        try {
            // Create the quartz scheduler, with 1 thread.
            DirectSchedulerFactory.getInstance().createVolatileScheduler(1);
            _scheduler = DirectSchedulerFactory.getInstance().getScheduler();
            _scheduler.setJobFactory(new DistributorFactory());

            final JobDetail job = new JobDetail("Bling Distributor", GROUP, DistributorJob.class);
            final CronTrigger trigger = new CronTrigger("Bling Distributor", GROUP);
            trigger.setCronExpression(EXECUTE_SCHEDULE);
            _scheduler.scheduleJob(job, trigger);

        } catch (SchedulerException se) {
            throw new IllegalStateException(se);
        } catch (ParseException pe) {
            throw new IllegalStateException(pe);
        }

        // Get the money config record, automatically creating it if it doesn't currently exist.
        MoneyConfigRecord confRecord = _repo.getMoneyConfig(false);

        // Ensure no one has the lock.  This is necessary in case the lock wasn't successfully
        // released when it was last shutdown.  We need to ensure that we're not starting up one
        // node while the bling distribution is occurring (highly unlikely).
        _repo.completeBlingDistribution(confRecord.lastDistributedBling);

        try {
            _scheduler.start();
        } catch (SchedulerException se) {
            throw new IllegalStateException(se);
        }
    }

    // from interface Lifecycle.Component
    public void shutdown ()
    {
        try {
            _scheduler.shutdown();
        } catch (SchedulerException se) {
            throw new IllegalStateException(se);
        }
    }

    /**
     * Actually process the bling to be distributed.
     */
    protected void run ()
    {
        // Get the last time we ran this and calculate the number of days since that time
        // has passed.
        MoneyConfigRecord confRecord = _repo.getMoneyConfig(true);
        if (confRecord == null) {
            log.info("Another process is currently distributing bling, canceling.");
            return;
        }
        // from here down we need to do everything in a try so that we release the lock we just
        // created

        Calendar now = Calendar.getInstance();
        try {
            // figure out how much we'll be distributing
            int blingPool = _runtime.money.blingPoolSize * 100;

            Calendar lastRun = Calendars.at(confRecord.lastDistributedBling).asCalendar();
            Calendar midnight1 = Calendars.at(lastRun.getTime()).zeroTime().asCalendar();
            Calendar midnight2 = Calendars.at(now.getTime()).zeroTime().asCalendar();
            int days = Calendars.getDaysBetween(midnight1, midnight2);

            // now set midnight2 to be the day after midnight1,
            // in case we're doing more than one day
            midnight2 = Calendars.at(midnight1.getTime()).addDays(1).asCalendar();

            // We'll repeat this for the number of days since we last executed it (could be 0).
            for (int i = 0; i < days; i++) {
                distributeBling(blingPool, midnight1, midnight2);
                // increment one day each
                midnight1.add(Calendar.DATE, 1);
                midnight2.add(Calendar.DATE, 1);
            }

        } finally {
            // Complete bling distribution up to the date that we completed successfully in here.
            _repo.completeBlingDistribution(new java.sql.Date(now.getTimeInMillis()));
        }
    }

    /**
     * Distribute the specified amount of bling for games played between the two times.
     */
    protected void distributeBling (int blingPool, Calendar midnight1, Calendar midnight2)
    {
        log.info("Distributing bling.",
            "day", DateFormat.getDateInstance().format(midnight1.getTime()),
            "bling", _runtime.money.blingPoolSize); // don't log centibling..
        if (blingPool <= 0) {
            return; // but we did the logging...
        }

        // Get all the game play sessions for that day.
        Collection<GamePlayRecord> gamePlays = _mgameRepo.getGamePlaysBetween(
            midnight1.getTimeInMillis(), midnight2.getTimeInMillis());
        // get the info records for those that are eligible
        Map<Integer, GameInfoRecord> gameMap = loadEligibleGames(gamePlays);

        // Calculate a total and a map of game ID to the total minutes spent in the game.
        long totalMinutes = 0;
        Map<Integer, Long> minutesPerGame = Maps.newHashMap();
        for (GamePlayRecord gamePlay : gamePlays) {
            if (gameMap.containsKey(gamePlay.gameId)) {
                totalMinutes += gamePlay.playerMins;
                Long curMins = minutesPerGame.get(gamePlay.gameId);
                minutesPerGame.put(gamePlay.gameId,
                    ((curMins == null) ? 0L : curMins.longValue()) + gamePlay.playerMins);
            }
        }

        // Assuming we have a non-zero number of minutes games were played this day, grant a
        // portion of the bling pool to each game's creator.
        if (totalMinutes > 0) {
            for (Entry<Integer, Long> entry : minutesPerGame.entrySet()) {
                int awardedBling = (int)(blingPool * entry.getValue() / totalMinutes);
                awardBling(entry.getKey(), gameMap.get(entry.getKey()), awardedBling);
            }
        }
    }

    /**
     * Return a Map<gameId, GameInfoRecord> for all games specified in the GamePlayRecords
     * that are elibible for having bling awarded.
     */
    protected Map<Integer, GameInfoRecord> loadEligibleGames (Collection<GamePlayRecord> gamePlays)
    {
        // make a Set of all gameIds
        Set<Integer> gameIds = Sets.newHashSet();
        for (GamePlayRecord gamePlay : gamePlays) {
            gameIds.add(gamePlay.gameId);
        }

        // make a Set of all charity ids
        Set<Integer> charityIds = Sets.newHashSet();
        for (CharityRecord charity : _memberRepo.getCharities()) {
            charityIds.add(charity.memberId);
        }

        // load the records for the gameIds, omitting the charities
        Map<Integer, GameInfoRecord> gameMap = Maps.newHashMap();
        for (GameInfoRecord game : _mgameRepo.loadPublishedGames(gameIds)) {
            if (!charityIds.contains(game.creatorId)) {
                gameMap.put(game.gameId, game);
            }
        }
        return gameMap;
    }

    /**
     * Awards some amount of bling to the creator of the given game.
     *
     * @param game The game that bling will be awarded for.
     * @param amount The amount of centibling to award.
     */
    protected void awardBling (int gameId, GameInfoRecord game, int amount)
    {
        if (game == null) {
            log.info("Unable to award bling to no longer listed game",
                     "gameId", gameId, "amount", amount);
            return;
        }

        // Update account with the awarded bling.
        _repo.accumulateAndStoreTransaction(
            game.creatorId, Currency.BLING, amount, TransactionType.BLING_POOL,
            MessageBundle.tcompose("m.game_plays_bling_awarded", game.gameId, game.name),
            null, true); // TODO: create a "game" subject type?
        // Note: we do not need to post the transaction as a node action, because
        // bling is not part of a user's runtime money.
    }

    /** Necessary because DistributorJob is not a static class. */
    protected class DistributorFactory implements JobFactory
    {
        public Job newJob (TriggerFiredBundle bundle)
        {
            return new DistributorJob();
        }
    }

    /**
     * Job to execute when distribution should be performed.
     */
    protected class DistributorJob implements Job
    {
        public void execute (JobExecutionContext context)
            throws JobExecutionException
        {
            try {
                run();
            } catch (Exception e) {
                log.error("Exception occurried while running the bling distributor.", e);
                throw new JobExecutionException(e);
            }
        }
    }

    protected Scheduler _scheduler;

    // dependencies
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MoneyRepository _repo;
    @Inject protected MsoyGameRepository _mgameRepo;
    @Inject protected RuntimeConfig _runtime;

    /** Schedule to execute this distributor.  Daily at 3:00a. */
    protected static final String EXECUTE_SCHEDULE = "0 0 3 * * ?";

    /** Default group for scheduled jobs and triggers. */
    protected static final String GROUP = "group1";
}
