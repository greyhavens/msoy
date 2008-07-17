//
// $Id$

package com.threerings.msoy.money.server;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Facade for all money (coins, bars, and bling) transactions.  This is the starting place to access
 * these services.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
public interface MoneyService
{
    /**
     * Secures a price for an item in bars.  This ensures the user will be able to purchase
     * an item for a set price.  This price will remain available for some amount of time
     * (specified by {@link MoneyConfiguration#getSecurePriceDuration()}.  The secured price
     * may also be removed if the maximum number of secured prices system-wide has been
     * reached (specified by {@link MoneyConfiguration#getMaxSecuredPrices()}.  In either case,
     * an attempt to buy the item will fail with a {@link NotSecuredException}.
     * 
     * This will secure the bar price of the item as given, and it will also secure the equivalent
     * price in coins according to the current exchange rate.
     * 
     * @param memberId ID of the member securing the price.
     * @param creatorId ID of the creator of the item being secured.
     * @param affiliateId ID of the affiliate associated with this purchase.  Null if no affiliate.
     * @param itemId ID of the item whose price is being secured.
     * @param itemType Type of the item whose price is being secured.
     * @param numBars Number of bars
     * @return Price of the item secured in coins, according to the current exchange rate.
     */
    int secureBarPrice (int memberId, int creatorId, Integer affiliateId, int itemId, int itemType, int numBars);
    
    /**
     * Secures a price for an item in coins.  This ensures the user will be able to purchase
     * an item for a set price.  This price will remain available for some amount of time
     * (specified by {@link MoneyConfiguration#getSecurePriceDuration()}.  The secured price
     * may also be removed if the maximum number of secured prices system-wide has been
     * reached (specified by {@link MoneyConfiguration#getMaxSecuredPrices()}.  In either case,
     * an attempt to buy the item will fail with a {@link NotSecuredException}.
     * 
     * This will only secure the price of an item in coins -- no bar price will be secured, and thus
     * an attempt to purchase this item with bars will result in a {@link CoinsOnlyException}.
     * 
     * @param memberId ID of the member securing the price.
     * @param creatorId ID of the creator of the item being secured.
     * @param affiliateId ID of the affiliate associated with this purchase.  Null if no affiliate.
     * @param itemId ID of the item whose price is being secured.
     * @param itemType Type of the item whose price is being secured.
     * @param numCoins Number of coins.
     */
    void secureCoinPrice (int memberId, int creatorId, Integer affiliateId, int itemId, int itemType, int numCoins);
    
    /**
     * Purchases an item using bars as the currency.  This will only update the appropriate accounts
     * of an exchange of money -- item fulfillment must be handled separately.  The item must have
     * been previously secured for some price through {@link #secureBarPrice(int, int, Integer, int, int, int)
     * secureBarPrice}.  The account balances will be updated in the following manner:
     * 
     * <ul>
     *     <li>Member: the bar price will be deducted from their account.</li>
     *     <li>Creator: an amount of bling equivalent to some percentage of the purchase price
     *         will be added to their account, according to the current exchange rate.</li>
     *     <li>Affiliate: an amount of bling equivalent to some pre-determined percentage of
     *         the purchase price will be added to their account, according to the current
     *         exchange rate.</li>
     * </ul>
     *     
     * @param memberId ID of the member making the purchase.
     * @param itemId ID of the item being purchased.
     * @param itemType Type of the item being purchased.
     * @throws NotEnoughMoneyException The member making the purchase does not have enough bars in
     *      their account.
     * @throws NotSecuredException The member did not secure a price for the item.
     * @throws CoinsOnlyException The price secured for the item was only in coins -- it cannot
     *      be purchased for bars.
     */
    void buyItemWithBars (int memberId, int itemId, int itemType)
        throws NotEnoughMoneyException, NotSecuredException, CoinsOnlyException;
    
    /**
     * Purchases an item using coins as the currency.  This will only update the appropriate accounts
     * of an exchange of money -- item fulfillment must be handled separately.  The item must have
     * been previously secured for some price through {@link #secureCoinPrice(int, int, Integer, int, int, int)
     * secureCoinPrice}.  The account balances will be updated in the following manner:
     * 
     * <ul>
     *     <li>Member: the coin price will be deducted from their account.</li>
     *     <li>Creator: an amount of coins equivalent to some percentage of the purchase price
     *         will be added to their account.</li>
     *     <li>Affiliate: an amount of coins equivalent to some pre-determined percentage of
     *         the purchase price will be added to their account.</li>
     * </ul>
     *     
     * @param memberId ID of the member making the purchase.
     * @param itemId ID of the item being purchased.
     * @param itemType Type of the item being purchased.
     * @throws NotEnoughMoneyException The member making the purchase does not have enough coins in
     *      their account.
     * @throws NotSecuredException The member did not secure a price for the item.
     */
    void buyItemWithCoins (int memberId, int itemId, int itemType)
        throws NotEnoughMoneyException, NotSecuredException;
    
    /**
     * The member has purchased some number of bars.  This will add the number of bars to their
     * account, fulfilling the transaction.
     * 
     * @param memberId ID of the member receiving bars.
     * @param numBars Number of bars to add to their account.
     */
    void buyBars (int memberId, int numBars);
    
    /**
     * Awards some number of coins to a member for some activity, such as playing a game.  This
     * may also result in some bling being awarded to the creator of the game or the affiliate.
     * 
     * @param memberId ID of the member to receive the coins.
     * @param creatorId ID of the creator of the item that caused the coins to be awarded.
     * @param affiliateId ID of the affiliate associated with the transaction.  Null if no
     *      affiliate.
     * @param amount Number of coins to be awarded.
     */
    void awardCoins (int memberId, int creatorId, Integer affiliateId, int amount);
    
    /**
     * Transfers some number of coins from one account to another one.
     * 
     * @param srcMemberId ID of the member who will give the coins.
     * @param destMemberId ID of the member who will receive the coins.
     * @param amount Number of coins to transfer.
     * @throws NotEnoughMoneyException The source account does not have enough coins.
     */
    void giveCoins (int srcMemberId, int destMemberId, int amount)
        throws NotEnoughMoneyException;
    
    /**
     * Retrieves the current account balance (coins, bars, and bling) for the given member.
     * 
     * @param memberId ID of the member to retrieve money for.
     * @return The money in their account.
     */
    MemberMoney getMoneyFor (int memberId);
    
    /**
     * Retrieves a money transaction history for a member, including one or more money types.
     * 
     * @param memberId ID of the member to retrieve money for.
     * @param types Set of money types to retrieve logs for.  Must contain at least 1 type.
     * @return List of past transactions, in no particular order.
     */
    List<MoneyHistory> getLog (int memberId, Set<MoneyType> types);
    
    /**
     * Retrieves the amount that a member's current bling is worth in American dollars.
     * 
     * @param memberId ID of the member to retrieve bling for.
     * @return The amount the bling is worth in American dollars.
     */
    BigDecimal getBlingWorth (int memberId);
    
    /**
     * Converts some amount of bling in a member's account into bars.
     * 
     * @param memberId ID of the member.
     * @param blingAmount Amount of bling to convert to bars.
     * @return Number of bars added to the account.
     * @throws NotEnoughMoneyException The account does not have the specified amount of
     *      bling available.
     */
    int exchangeBlingForBars (int memberId, double blingAmount)
        throws NotEnoughMoneyException;

    
    // Customer Representative actions
    
    /**
     * Deducts some amount of bling from the member's account.  This will be used by
     * CSR's for corrective actions or when the member chooses to cash out their bling.
     */
    void deductBling (int memberId, double amount);
    
    
    // Administrator actions
    
    /**
     * Updates the current configuration for the Money service.
     */
    void updateMoneyConfiguration (MoneyConfiguration config);
    
    /**
     * Retrieves the current configuration of the Money service.
     */
    MoneyConfiguration getMoneyConfiguration ();
}
