//
// $Id$

package com.threerings.msoy.money.server.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.money.server.MemberMoney;
import com.threerings.msoy.money.server.MoneyConfiguration;
import com.threerings.msoy.money.server.MoneyHistory;
import com.threerings.msoy.money.server.MoneyLogic;
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
class MoneyLogicImpl
    implements MoneyLogic
{
    @Inject
    public MoneyLogicImpl(final MoneyRepository repo, final SecuredPricesCache securedPricesCache)
    {
        this.repo = repo;
        this.securedPricesCache = securedPricesCache;
    }
    
    @Retry(exception=StaleDataException.class)
    public void awardCoins (final int memberId, final int creatorId, final int affiliateId, final int amount)
    {
        Preconditions.checkArgument(!MemberName.isGuest(memberId), "Cannot award coins to guests.");
        Preconditions.checkArgument(!MemberName.isGuest(creatorId), "Cannot award coins to guests.");
        Preconditions.checkArgument(amount > 0, "amount is invalid: %d", amount);
        
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
        Preconditions.checkArgument(!MemberName.isGuest(memberId), "Guests cannot buy bars.");
        Preconditions.checkArgument(numBars > 0, "numBars is invalid: %d", numBars);
        
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
        Preconditions.checkArgument(!MemberName.isGuest(memberId), "Cannot retrieve money log for guests.");
        Preconditions.checkArgument(start >= 0, "start is invalid: %d", start);
        Preconditions.checkArgument(count > 0, "count is invalid: %d", count);
        
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
        Preconditions.checkArgument(!MemberName.isGuest(memberId), "Cannot retrieve money info for guests.");

        final MemberAccountRecord account = repo.getAccountById(memberId);
        return account != null ? account.getMemberMoney() : new MemberMoney(memberId); 
    }

    public int secureBarPrice (final int memberId, final int creatorId, final int affiliateId, 
        final ItemIdent item, final int numBars, final String description)
    {
        Preconditions.checkArgument(!MemberName.isGuest(memberId), "Guests cannot secure prices.");
        Preconditions.checkArgument(!MemberName.isGuest(memberId), "Creators cannot be guests.");
        Preconditions.checkArgument(item != null && (item.type != 0 || item.itemId != 0), "item is invalid: %s", 
            item.toString());
        Preconditions.checkArgument(numBars > 0, "bars is invalid: %d", numBars);
        
        // TODO: Use exchange rate to calculate coins.
        securedPricesCache.securePrice(memberId, item, new SecuredPrices(MoneyType.BARS, 0, numBars, 
            creatorId, affiliateId, description));
        return 0;
    }

    public int secureCoinPrice (final int memberId, final int creatorId, final int affiliateId, 
        final ItemIdent item, final int numCoins, final String description)
    {
        Preconditions.checkArgument(!MemberName.isGuest(memberId), "Guests cannot secure prices.");
        Preconditions.checkArgument(!MemberName.isGuest(memberId), "Creators cannot be guests.");
        Preconditions.checkArgument(item != null && (item.type != 0 || item.itemId != 0), "item is invalid: %s", 
            item.toString());
        Preconditions.checkArgument(numCoins > 0, "numCoins is invalid: %d", numCoins);
        
        // TODO: Use exchange rate to calculate bars.
        securedPricesCache.securePrice(memberId, item, new SecuredPrices(MoneyType.COINS, numCoins, 0, 
            creatorId, affiliateId, description));
        return 0;
    }

    public void updateMoneyConfiguration (final MoneyConfiguration config)
    {
    // TODO Auto-generated method stub

    }
    
    private void buyItem (final int memberId, final ItemIdent item, final MoneyType purchaseType)
        throws NotEnoughMoneyException, NotSecuredException
    {
        Preconditions.checkArgument(!MemberName.isGuest(memberId), "Guests cannot buy items.");
        Preconditions.checkArgument(item != null && (item.type != 0 || item.itemId != 0), "item is invalid: %s", 
            item.toString());
        Preconditions.checkArgument(purchaseType == MoneyType.BARS || purchaseType == MoneyType.COINS, 
            "purchaseType is invalid: %s", purchaseType.toString());
        
        // Get the secured prices for the item.
        final SecuredPrices prices = securedPricesCache.getSecuredPrice(memberId, item);
        if (prices == null) {
            throw new NotSecuredException(memberId, item);
        }
        final int amount = (purchaseType == MoneyType.BARS ? prices.getBars() : prices.getCoins());
        
        // Load up the accounts (member, creator, and affiliate).
        final MemberAccountRecord account = repo.getAccountById(memberId);
        if (account == null || !account.canAfford(amount, purchaseType)) {
            final int available = (account == null ? 0 : (purchaseType == MoneyType.BARS ? account.getBars() : 
                account.getCoins()));
            throw new NotEnoughMoneyException(available, amount, purchaseType, memberId);
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
        repo.addHistory(account.buyItem(amount, purchaseType, prices.getDescription(), item));
        repo.addHistory(creator.creatorPayout(amount, prices.getListedType(), "Item purchased: " + 
            prices.getDescription(), item));
        // TODO: update affiliate with some amount of bling.
        repo.saveAccount(account);
        repo.saveAccount(creator);
        if (affiliate != null) {
            repo.saveAccount(affiliate);
        }
    }
    
    private final MoneyRepository repo;
    private final SecuredPricesCache securedPricesCache;
}
