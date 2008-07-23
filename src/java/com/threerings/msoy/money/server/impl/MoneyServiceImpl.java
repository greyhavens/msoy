//
// $Id$

package com.threerings.msoy.money.server.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.money.server.MemberMoney;
import com.threerings.msoy.money.server.MoneyConfiguration;
import com.threerings.msoy.money.server.MoneyHistory;
import com.threerings.msoy.money.server.MoneyService;
import com.threerings.msoy.money.server.MoneyType;
import com.threerings.msoy.money.server.NotEnoughMoneyException;
import com.threerings.msoy.money.server.NotSecuredException;

/**
 * Default implementation of the money service.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
final class MoneyServiceImpl
    implements MoneyService
{
    @Inject
    public MoneyServiceImpl(final MoneyRepository repo)
    {
        this.repo = repo;
    }
    
    public void awardCoins (final int memberId, final int creatorId, final Integer affiliateId, final int amount)
    {
    // TODO Auto-generated method stub

    }

    public void buyBars (final int memberId, final int numBars)
    {
        int retries = 0;
        do {
            try {
                MemberAccountRecord account = repo.getAccountById(memberId);
                if (account == null) {
                    account = new MemberAccountRecord(memberId);
                }
                final MemberAccountHistoryRecord history = account.buyBars(numBars);
                repo.saveAccount(account);
                repo.addHistory(history);
                return;
            } catch (final StaleDataException sde) {
                // Try again!
                retries++;
            }
        } while (retries < 3);
        
        throw new IllegalStateException("Cannot update account after 3 retries due to stale data.");
    }

    public void buyItemWithBars (final int memberId, final ItemIdent item)
        throws NotEnoughMoneyException, NotSecuredException
    {
    // TODO Auto-generated method stub

    }

    public void buyItemWithCoins (final int memberId, final ItemIdent item)
        throws NotEnoughMoneyException, NotSecuredException
    {
    // TODO Auto-generated method stub

    }

    public void deductBling (final int memberId, final double amount)
    {
    // TODO Auto-generated method stub

    }

    public int exchangeBlingForBars (final int memberId, final double blingAmount)
        throws NotEnoughMoneyException
    {
        // TODO Auto-generated method stub
        return 0;
    }

    public BigDecimal getBlingWorth (final int memberId)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public List<MoneyHistory> getLog (final int memberId, final Set<MoneyType> types)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public MoneyConfiguration getMoneyConfiguration ()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public MemberMoney getMoneyFor (final int memberId)
    {
        final MemberAccountRecord account = repo.getAccountById(memberId);
        return account != null ? account.getMemberMoney() : new MemberMoney(memberId, 0, 0, 0.0); 
    }

    public void giveCoins (final int srcMemberId, final int destMemberId, final int amount)
        throws NotEnoughMoneyException
    {
    // TODO Auto-generated method stub

    }

    public int secureBarPrice (final int memberId, final int creatorId, final Integer affiliateId, 
        final ItemIdent item, final int numBars)
    {
        // TODO Auto-generated method stub
        return 0;
    }

    public void secureCoinPrice (final int memberId, final int creatorId, final Integer affiliateId, 
        final ItemIdent item, final int numCoins)
    {
    // TODO Auto-generated method stub

    }

    public void updateMoneyConfiguration (final MoneyConfiguration config)
    {
    // TODO Auto-generated method stub

    }
    
    private final MoneyRepository repo;
}
