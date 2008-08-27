//
// $Id$

package com.threerings.msoy.money.server;

import java.math.BigDecimal;
import java.util.List;

import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.money.data.all.MemberMoney;
import com.threerings.msoy.money.data.all.MoneyHistory;
import com.threerings.msoy.money.data.all.MoneyType;
import com.threerings.presents.annotation.BlockingThread;

/**
 * Facade for all money (coins, bars, and bling) transactions. This is the starting place to
 * access these services.
 *
 * @author Kyle Sampson <kyle@threerings.net>
 */
@BlockingThread
public interface MoneyLogic
{
    /**
     * Secures a price for an item in bars. This ensures the user will be able to purchase an item
     * for a set price. This price will remain available for some amount of time (specified by
     * {@link MoneyConfiguration#getSecurePriceDuration()}. The secured price may also be removed
     * if the maximum number of secured prices system-wide has been reached (specified by
     * {@link MoneyConfiguration#getMaxSecuredPrices()}. In either case, an attempt to buy the
     * item will fail with a {@link NotSecuredException}.
     *
     * This will secure the bar price of the item as given, and it will also secure the equivalent
     * price in coins according to the current exchange rate.
     *
     * @param memberId ID of the member securing the price.
     * @param creatorId ID of the creator of the item being secured.
     * @param affiliateId ID of the affiliate associated with this purchase. Null if no affiliate.
     * @param item Item to secure the price for.
     * @param numBars Number of bars
     * @param description A description of the item that will appear on the member's transaction
     * history if purchased.
     * @return Price of the item secured in coins, according to the current exchange rate.
     */
    int secureBarPrice (
        int memberId, int creatorId, int affiliateId, ItemIdent item, int numBars,
        String description);

    /**
     * Secures a price for an item in coins. This ensures the user will be able to purchase an
     * item for a set price. This price will remain available for some amount of time (specified
     * by {@link MoneyConfiguration#getSecurePriceDuration()}. The secured price may also be
     * removed if the maximum number of secured prices system-wide has been reached (specified by
     * {@link MoneyConfiguration#getMaxSecuredPrices()}. In either case, an attempt to buy the
     * item will fail with a {@link NotSecuredException}.
     *
     * This will secure the coin price of the item as given, and it will also secure the
     * equivalent price in bars according to the current exchange rate.
     *
     * @param memberId ID of the member securing the price.
     * @param creatorId ID of the creator of the item being secured.
     * @param affiliateId ID of the affiliate associated with this purchase. Zero if no affiliate.
     * @param item Item to secure the price for.
     * @param numCoins Number of coins.
     * @param description A description of the item that will appear on the member's transaction
     * history if purchased.
     * @return Price of the item secured in bars, according to the current exchange rate.
     */
    int secureCoinPrice (
        int memberId, int creatorId, int affiliateId, ItemIdent item, int numCoins,
        String description);

    /**
     * Purchases an item using bars as the currency. This will only update the appropriate
     * accounts of an exchange of money -- item fulfillment must be handled separately. The item
     * must have been previously secured for some price through
     * {@link #secureBarPrice(int, int, Integer, int, int, int) secureBarPrice} or
     * {@link #secureCoinPrice(int, int, Integer, ItemIdent, int)}. The account balances will be
     * updated in the following manner:
     *
     * <ul> <li>Member: the bar price will be deducted from their account.</li> <li>Creator: an
     * amount of bling equivalent to some percentage of the purchase price will be added to their
     * account, according to the current exchange rate.</li> <li>Affiliate: an amount of bling
     * equivalent to some pre-determined percentage of the purchase price will be added to their
     * account, according to the current exchange rate.</li> </ul>
     *
     * @param memberId ID of the member making the purchase.
     * @param item Item to purchase.
     * @param support If true, the member is an admin or support person. They will always have
     * enough money to purchase the item, though it will drain the appropriate amount of money
     * until they have a balance of 0.
     * @return Results of the operation..
     * @throws NotEnoughMoneyException The member making the purchase does not have enough bars in
     * their account.
     * @throws NotSecuredException The member did not secure a price for the item.
     */
    MoneyResult buyItemWithBars (int memberId, ItemIdent item, boolean support)
        throws NotEnoughMoneyException, NotSecuredException;

    /**
     * Purchases an item using coins as the currency. This will only update the appropriate
     * accounts of an exchange of money -- item fulfillment must be handled separately. The item
     * must have been previously secured for some price through
     * {@link #secureBarPrice(int, int, Integer, int, int, int) secureBarPrice} or
     * {@link #secureCoinPrice(int, int, Integer, ItemIdent, int)}. The account balances will be
     * updated in the following manner:
     *
     * <ul> <li>Member: the coin price will be deducted from their account.</li> <li>Creator:
     * an amount of coins equivalent to some percentage of the purchase price will be added to
     * their account.</li> <li>Affiliate: an amount of coins equivalent to some pre-determined
     * percentage of the purchase price will be added to their account.</li> </ul>
     *
     * @param memberId ID of the member making the purchase.
     * @param item Item to purchase.
     * @param support If true, the member is an admin or support person. They will always have
     * enough money to purchase the item, though it will drain the appropriate amount of money
     * until they have a balance of 0.
     * @return Results of the operation.
     * @throws NotEnoughMoneyException The member making the purchase does not have enough coins
     * in their account.
     * @throws NotSecuredException The member did not secure a price for the item.
     */
    MoneyResult buyItemWithCoins (int memberId, ItemIdent item, boolean support)
        throws NotEnoughMoneyException, NotSecuredException;

    /**
     * The member has purchased some number of bars. This will add the number of bars to their
     * account, fulfilling the transaction.
     *
     * @param memberId ID of the member receiving bars.
     * @param numBars Number of bars to add to their account.
     * @return The money the member now has in their account.
     */
    MoneyResult buyBars (int memberId, int numBars);

    /**
     * Awards some number of coins to a member for some activity, such as playing a game. This
     * will also keep track of coins spent awarded for each creator, so that the creators can
     * receive bling when people play their games.
     *
     * @param memberId ID of the member to receive the coins.
     * @param creatorId ID of the creator of the item that caused the coins to be awarded.
     * @param affiliateId ID of the affiliate associated with the transaction. Zero if no
     * affiliate.
     * @param item Optional item that coins were awarded for (i.e. a game)
     * @param amount Number of coins to be awarded.
     * @param description Description that will appear in the user's transaction history.
     * @param userAction The user action that caused coins to be awarded.
     * @return Map containing member ID to the money that member has in the account. The recipient
     * of the coins, the creator, and the affiliate will all be included, if applicable.
     */
    MoneyResult awardCoins (
        int memberId, int creatorId, int affiliateId, ItemIdent item, int amount,
        String description, UserAction userAction);

    /**
     * Retrieves the current account balance (coins, bars, and bling) for the given member.
     *
     * @param memberId ID of the member to retrieve money for.
     * @return The money in their account.
     */
    MemberMoney getMoneyFor (int memberId);

    /**
     * Retrieves a money transaction history for a member, including one or more money types. The
     * returned list is sorted by transaction date ascending. A portion of the log can be returned
     * at a time for pagination.
     *
     * @param memberId ID of the member to retrieve money for.
     * @param type Money type to retrieve logs for. If null, then records for all money types are
     * returned.
     * @param start Zero-based index of the first log item to return.
     * @param count The number of log items to return. If Integer.MAX_VALUE, this will return all
     * records.
     * @param descending If true, the log will be sorted by transaction date descending.
     * @return List of requested past transactions.
     */
    List<MoneyHistory> getLog (
        int memberId, MoneyType type, int start, int count, boolean descending);

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
     * @throws NotEnoughMoneyException The account does not have the specified amount of bling
     * available, aight?
     */
    int exchangeBlingForBars (int memberId, double blingAmount)
        throws NotEnoughMoneyException;

    // Customer Representative actions

    /**
     * Deducts some amount of bling from the member's account. This will be used by CSR's for
     * corrective actions or when the member chooses to cash out their bling.
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

    /**
     * Initializes the money service by starting up required services, such as an expiration
     * monitor and queue listeners. This method is idempotent.
     */
    void init ();
}
