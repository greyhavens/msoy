//
// $Id$

package com.threerings.msoy.money.server.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;
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
 * TODO: Transactional support
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
@Singleton
class MoneyServiceImpl
    implements MoneyService
{
    @Inject
    public MoneyServiceImpl(final MoneyRepository repo, final SecuredPricesCache securedPricesCache)
    {
        this.repo = repo;
        this.securedPricesCache = securedPricesCache;
    }
    
    @Retry(exception=StaleDataException.class)
    public void awardCoins (final int memberId, final int creatorId, final int affiliateId, final int amount)
    {
        MemberAccountRecord account = repo.getAccountById(memberId);
        if (account == null) {
            account = new MemberAccountRecord(memberId);
        }
        final MemberAccountHistoryRecord history = account.awardCoins(amount);
        repo.saveAccount(account);
        repo.addHistory(history);
        // TODO: creator and affiliate
    }

    @Retry(exception=StaleDataException.class)
    public void buyBars (final int memberId, final int numBars)
    {
        MemberAccountRecord account = repo.getAccountById(memberId);
        if (account == null) {
            account = new MemberAccountRecord(memberId);
        }
        final MemberAccountHistoryRecord history = account.buyBars(numBars);
        repo.saveAccount(account);
        repo.addHistory(history);
    }

    @Retry(exception=StaleDataException.class)
    public void buyItemWithBars (final int memberId, final ItemIdent item)
    throws NotEnoughMoneyException, NotSecuredException
    {
        buyItem(memberId, item, MoneyType.BARS);
    }

    @Retry(exception=StaleDataException.class)
    public void buyItemWithCoins (final int memberId, final ItemIdent item)
        throws NotEnoughMoneyException, NotSecuredException
    {
        buyItem(memberId, item, MoneyType.COINS);
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

    public List<MoneyHistory> getLog (final int memberId, final MoneyType type, final int start, 
        final int count, final boolean descending)
    {
        final List<MoneyHistory> log = new ArrayList<MoneyHistory>();
        for (final MemberAccountHistoryRecord record : repo.getHistory(memberId, type, start, count, descending)) {
            log.add(record.createMoneyHistory());
        }
        return log;
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

    public int secureBarPrice (final int memberId, final int creatorId, final int affiliateId, 
        final ItemIdent item, final int numBars, final String description)
    {
        // TODO: Use exchange rate to calculate coins.
        securedPricesCache.securePrice(memberId, item, new SecuredPrices(0, numBars, creatorId, affiliateId, description));
        return 0;
    }

    public int secureCoinPrice (final int memberId, final int creatorId, final int affiliateId, 
        final ItemIdent item, final int numCoins, final String description)
    {
        // TODO: Use exchange rate to calculate bars.
        securedPricesCache.securePrice(memberId, item, new SecuredPrices(numCoins, 0, creatorId, affiliateId, description));
        return 0;
    }

    public void updateMoneyConfiguration (final MoneyConfiguration config)
    {
    // TODO Auto-generated method stub

    }
    
    private void buyItem (final int memberId, final ItemIdent item, final MoneyType type)
        throws NotEnoughMoneyException, NotSecuredException
    {
        // Get the secured prices for the item.
        final SecuredPrices prices = securedPricesCache.getSecuredPrice(memberId, item);
        if (prices == null) {
            throw new NotSecuredException(memberId, item);
        }
        final int amount = (type == MoneyType.BARS ? prices.getBars() : prices.getCoins());
        
        // Load up the accounts (member, creator, and affiliate).
        final MemberAccountRecord account = repo.getAccountById(memberId);
        if (account == null || !account.canAfford(amount, type)) {
            final int available = (account == null ? 0 : (type == MoneyType.BARS ? account.getBars() : account.getCoins()));
            throw new NotEnoughMoneyException(available, amount, type, memberId);
        }
        MemberAccountRecord creator = repo.getAccountById(prices.getCreatorId());
        if (creator == null) {
            creator = new MemberAccountRecord(prices.getCreatorId());
        }
        MemberAccountRecord affiliate = null;
        if (prices.getAffiliateId() != 0) {
            affiliate = repo.getAccountById(prices.getAffiliateId());
            if (affiliate == null) {
                affiliate = new MemberAccountRecord(prices.getAffiliateId());
            }
        }
        
        // The item no longer needs to be in the cache.
        securedPricesCache.removeSecuredPrice(memberId, item);
        
        // Update and save the accounts, and add history records for the changes.
        repo.addHistory(account.buyItem(amount, type, prices.getDescription(), item));
        // TODO: update creator and affiliate with some amount of bling.
        repo.saveAccount(account);
        repo.saveAccount(creator);
        if (affiliate != null) {
            repo.saveAccount(affiliate);
        }
    }
    
    private final MoneyRepository repo;
    private final SecuredPricesCache securedPricesCache;
}
