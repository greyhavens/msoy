//
// $Id$

package com.threerings.msoy.money.server;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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
import com.threerings.msoy.admin.data.ServerConfigObject;
import com.threerings.msoy.admin.server.RuntimeConfig;
import com.threerings.msoy.item.server.persist.GamePlayRecord;
import com.threerings.msoy.item.server.persist.GameRecord;
import com.threerings.msoy.item.server.persist.GameRepository;
import com.threerings.msoy.money.server.persist.MemberAccountRecord;
import com.threerings.msoy.money.server.persist.MoneyConfigRecord;
import com.threerings.msoy.money.server.persist.MoneyRepository;
import com.threerings.msoy.money.server.persist.MoneyTransactionRecord;
import com.threerings.presents.server.ShutdownManager;
import com.threerings.presents.server.ShutdownManager.Shutdowner;
import com.threerings.util.MessageBundle;

/**
 * Responsible for distributing bling to creators of games that players have played on a daily
 * basis.  The amount to distribute is governed by the {@link ServerConfigObject#blingPoolSize}
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
    implements Shutdowner
{
    @Inject
    public BlingPoolDistributor (MoneyRepository repo, GameRepository gameRepo, ShutdownManager sm)
    {
        _repo = repo;
        _gameRepo = gameRepo;
        
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
        // If no bling should be awarded, exit outta here
        int blingPool = RuntimeConfig.server.blingPoolSize * 100;
        if (blingPool == 0) {
            logger.info("No bling to distribute, canceling.");
            return;
        }
        
        // Get the last time we ran this and calculate the number of days since that time
        // has passed.
        MoneyConfigRecord confRecord = _repo.getMoneyConfig(true);
        if (confRecord == null) {
            logger.info("Another process is currently distributing bling, canceling.");
            return;
        }

        Calendar calLastRun = toCal(confRecord.lastDistributedBling);
        try {
            int days = CalendarUtil.getDaysBetween(calLastRun, Calendar.getInstance());
            
            // We'll repeat this for the number of days since we last executed it.
            Calendar cal2 = toCal(confRecord.lastDistributedBling);
            cal2.add(Calendar.DATE, 1);
            for (int i = 0; i < days; i++) {
                logger.info("Distributing bling for " + 
                    DateFormat.getDateInstance().format(calLastRun.getTime()));
                
                // Get all the game play sessions for this day.
                Collection<GamePlayRecord> gamePlays = _gameRepo.getGamePlaysBetween(
                    calLastRun.getTimeInMillis(), cal2.getTimeInMillis());
                
                // Calculate a total and a map of game ID to the total minutes spent in the game
                long totalMinutes = 0;
                Map<Integer, Long> minutesPerGame = new HashMap<Integer, Long>();
                for (GamePlayRecord gamePlay : gamePlays) {
                    totalMinutes += gamePlay.playerMins;
                    if (minutesPerGame.get(gamePlay.gameId) == null) {
                        minutesPerGame.put(gamePlay.gameId, (long)gamePlay.playerMins);
                    } else {
                        minutesPerGame.put(gamePlay.gameId, 
                            minutesPerGame.get(gamePlay.gameId) + gamePlay.playerMins);
                    }
                }
                
                // Get all the games, since we need the creatorId from them.
                Map<Integer, GameRecord> gameMap = new HashMap<Integer, GameRecord>();
                for (GameRecord game : _gameRepo.loadItems(minutesPerGame.keySet())) {
                    gameMap.put(game.itemId, game);
                }
                
                // Assuming we have a non-zero number of minutes games were played this day, grant a
                // portion of the bling pool to each game's creator.
                if (totalMinutes > 0) {
                    for (Entry<Integer, Long> entry : minutesPerGame.entrySet()) {
                        int awardedBling = (int)(blingPool * entry.getValue() / totalMinutes);
                        awardBling(gameMap.get(entry.getKey()), awardedBling);
                    }
                }
                
                // Increment the day to use when retrieving game play records
                calLastRun.add(Calendar.DATE, 1);
                cal2.add(Calendar.DATE, 1);
            }
            
            logger.info("Finished distributing bling.");
        } finally {
            // Complete bling distribution up to the date that we completed successfully in here.
            _repo.completeBlingDistribution(new java.sql.Date(calLastRun.getTimeInMillis()));
        }
    }
    
    /**
     * Awards some amount of bling to the creator of the given game.
     * 
     * @param game The game that bling will be awarded for.
     * @param amount The amount of centibling to award.
     */
    protected void awardBling (GameRecord game, int amount)
    {
        // Update account with the awarded bling.
        MemberAccountRecord account = _repo.getAccountById(game.creatorId);
        String description = MessageBundle.tcompose("m.game_plays_bling_awarded", amount,
            game.itemId, game.description);
        MoneyTransactionRecord tx = account.gamePlaysPayout(description, game.itemId, amount);
        
        // Save the account and new transaction records.
        _repo.saveAccount(account);
        _repo.addTransaction(tx);
    }
    
    protected Calendar toCal (Date date)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        CalendarUtil.zeroTime(cal);
        return cal;
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
                logger.error("Exception occurried while running the bling distributor.", e);
                throw new JobExecutionException(e);
            }
        }
    }
    
    /** Schedule to execute this distributor.  Daily at 3:00a. */
    protected static final String EXECUTE_SCHEDULE = "0 0 3 * * ?";
    
    /** Default group for scheduled jobs and triggers. */
    protected static final String GROUP = "group1"; 
    
    protected static final Logger logger = Logger.getLogger(BlingPoolDistributor.class);
    
    protected final MoneyRepository _repo;
    protected final GameRepository _gameRepo;
    protected final Scheduler _scheduler;
}
