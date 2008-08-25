//
// $Id$

package com.threerings.msoy.money.server.persist;

import java.util.List;

import com.threerings.msoy.money.data.all.MoneyType;

/**
 * Interface for retrieving and persisting entities in the money service.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
public interface MoneyRepository
{
    /**
     * Retrieves a member's account info by member ID.
     */
    MemberAccountRecord getAccountById (int memberId);
    
    /**
     * Adds or updates the given account.
     * 
     * @param account Account to update.
     */
    void saveAccount (MemberAccountRecord account);
    
    /**
     * Adds a history record for an account.
     * 
     * @param history History record to update.
     */
    void addHistory (MemberAccountHistoryRecord history);
    
    List<MemberAccountHistoryRecord> getHistory (int memberId, MoneyType type, 
        int start, int count, boolean descending);
    
    int deleteOldHistoryRecords (MoneyType type, long maxAge);
}
