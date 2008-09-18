//
// $Id$

package com.threerings.msoy.money.server;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.inject.Inject;
import com.samskivert.util.CalendarUtil;
import com.threerings.msoy.admin.server.RuntimeConfig;
import com.threerings.msoy.item.server.persist.GamePlayRecord;
import com.threerings.msoy.item.server.persist.GameRecord;
import com.threerings.msoy.item.server.persist.GameRepository;
import com.threerings.msoy.money.server.persist.MemberAccountRecord;
import com.threerings.msoy.money.server.persist.MoneyConfigRecord;
import com.threerings.msoy.money.server.persist.MoneyRepository;
import com.threerings.msoy.money.server.persist.MoneyTransactionRecord;
import com.threerings.util.MessageBundle;

public class BlingPoolDistributor
{
    @Inject
    public BlingPoolDistributor (MoneyRepository repo, GameRepository gameRepo)
    {
        _repo = repo;
        _gameRepo = gameRepo;
    }
    
    public void start ()
    {
        // Get the money config record, automatically creating it if it doesn't currently exist.
        _repo.getMoneyConfig(false);
    }
    
    protected void run ()
    {
        // If no bling should be awarded, exit outta here
        int blingPool = RuntimeConfig.server.blingPoolSize * 100;
        if (blingPool == 0) {
            return;
        }
        
        // Get the last time we ran this and calculate the number of days since that time
        // has passed.
        MoneyConfigRecord confRecord = _repo.getMoneyConfig(true);
        Calendar calLastRun = toCal(confRecord.lastDistributedBling);
        int days = CalendarUtil.getDaysBetween(calLastRun, Calendar.getInstance());
        
        // We'll repeat this for the number of days since we last executed it.
        Calendar cal2 = toCal(confRecord.lastDistributedBling);
        cal2.add(1, Calendar.DATE);
        for (int i = 0; i < days; i++) {
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
            calLastRun.add(1, Calendar.DATE);
            cal2.add(1, Calendar.DATE);
        }
        
        // Update the money config with the new last executed
        _repo.completeBlingDistribution(new java.sql.Date(calLastRun.getTimeInMillis()));
    }
    
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
    
    protected final MoneyRepository _repo;
    protected final GameRepository _gameRepo;
}
