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

import com.google.inject.Inject;

import com.samskivert.util.CalendarUtil;
import com.samskivert.util.Logger;

import com.threerings.util.MessageBundle;

import com.threerings.presents.server.ShutdownManager;

import com.threerings.msoy.admin.data.MoneyConfigObject;
import com.threerings.msoy.admin.server.RuntimeConfig;

import com.threerings.msoy.game.server.persist.GamePlayRecord;
import com.threerings.msoy.game.server.persist.MsoyGameRepository;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.server.persist.GameRecord;

import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.TransactionType;
import com.threerings.msoy.money.server.persist.MoneyConfigRecord;
import com.threerings.msoy.money.server.persist.MoneyRepository;
import com.threerings.msoy.server.persist.CharityRecord;
import com.threerings.msoy.server.persist.MemberRepository;

/**
 * Responsible for distributing bling to creators of games that players have played on a daily
 * basis.  The amount to distribute is governed by the {@link MoneyConfigObject#blingPoolSize}
 * run-time configuration.  This is scheduled to run at 3:00a every day.  The amount of bling
 * each game creator receives is based on the amount of time players played their game, compared
 * to the total amount of time that players played games that day.
 * 
 * This class will guarantee that no other thread or process is running the distributor at the
 * same time, though all world servers will attempt to execute this at the same time.  This allows
 * for redundancy in case any one server is down.
 *  
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class BlingPoolDistributor
    implements ShutdownManager.Shutdowner
{
    @Inject
    public BlingPoolDistributor (RuntimeConfig runtime, MoneyRepository repo, 
        MsoyGameRepository mgameRepo, ShutdownManager sm, MemberRepository memberRepo)
    {
        _runtime = runtime;
        _repo = repo;
        _mgameRepo = mgameRepo;
        _memberRepo = memberRepo;
        
        sm.registerShutdowner(this);

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
    }

    /**
     * Starts the distributor, which will be invoked at a later time.
     */
    public void start ()
    {
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

            Calendar lastRun = Calendar.getInstance();
            lastRun.setTime(confRecord.lastDistributedBling); // make it lastRun

            Calendar midnight1 = (Calendar) lastRun.clone();
            CalendarUtil.zeroTime(midnight1); // make it midnight
            Calendar midnight2 = (Calendar) now.clone();
            CalendarUtil.zeroTime(midnight2); // make it midnight
            int days = CalendarUtil.getDaysBetween(midnight1, midnight2);

            // now set midnight2 to be the day after midnight1,
            // in case we're doing more than one day
            midnight2 = (Calendar) midnight1.clone();
            midnight2.add(Calendar.DATE, 1);

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
        Set<Integer> gameIds = new HashSet<Integer>();
        for (GamePlayRecord gamePlay : gamePlays) {
            gameIds.add(gamePlay.gameId);
        }

        // Load up all of the games for which we're going to award bling.
        Map<Integer, GameRecord> gameMap = new HashMap<Integer, GameRecord>();
        for (GameRecord game : _mgameRepo.loadListedGameRecords(gameIds)) {
            gameMap.put(game.gameId, game);
        }

        // No bling should be distributed from the pool to charities
        Set<Integer> charityIds = new HashSet<Integer>();
        for (CharityRecord charity : _memberRepo.getCharities()) {
            charityIds.add(charity.memberId);
        }
        
        // Calculate a total and a map of game ID to the total minutes spent in the game.
        long totalMinutes = 0;
        Map<Integer, Long> minutesPerGame = new HashMap<Integer, Long>();
        for (GamePlayRecord gamePlay : gamePlays) {
            // Ignore if creator is a charity
            if (gameMap.get(gamePlay.gameId) == null || 
                    !charityIds.contains(gameMap.get(gamePlay.gameId).creatorId)) {
                totalMinutes += gamePlay.playerMins;
                if (minutesPerGame.get(gamePlay.gameId) == null) {
                    minutesPerGame.put(gamePlay.gameId, (long)gamePlay.playerMins);
                } else {
                    minutesPerGame.put(gamePlay.gameId, 
                        minutesPerGame.get(gamePlay.gameId) + gamePlay.playerMins);
                }
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
     * Awards some amount of bling to the creator of the given game.
     * 
     * @param game The game that bling will be awarded for.
     * @param amount The amount of centibling to award.
     */
    protected void awardBling (int gameId, GameRecord game, int amount)
    {
        if (game == null) {
            log.info("Unable to award bling to no longer listed game",
                     "gameId", gameId, "amount", amount);
            return;
        }

        // Update account with the awarded bling.
        try {
            _repo.accumulateAndStoreTransaction(
                game.creatorId, Currency.BLING, amount, TransactionType.BLING_POOL,
                MessageBundle.tcompose("m.game_plays_bling_awarded", game.gameId, game.name),
                new ItemIdent(Item.GAME, game.itemId));
            // Note: we do not need to post the transaction as a node action, because
            // bling is not part of a user's runtime money.

        } catch (MoneyRepository.NoSuchMemberException nsme) {
            log.warning("Invalid game creator. Bling award cancelled.",
                "game", game.itemId, "creator", game.creatorId, "bling", amount);
        }
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

    /** Schedule to execute this distributor.  Daily at 3:00a. */
    protected static final String EXECUTE_SCHEDULE = "0 0 3 * * ?";

    /** Default group for scheduled jobs and triggers. */
    protected static final String GROUP = "group1"; 

    protected static final Logger log = Logger.getLogger(BlingPoolDistributor.class);

    protected final RuntimeConfig _runtime;
    protected final MoneyRepository _repo;
    protected final MsoyGameRepository _mgameRepo;
    protected final Scheduler _scheduler;
    protected final MemberRepository _memberRepo;
}
