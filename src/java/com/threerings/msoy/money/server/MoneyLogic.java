//
// $Id$

package com.threerings.msoy.money.server;

import java.math.BigDecimal;

import java.util.EnumSet;
import java.util.List;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.item.data.all.CatalogIdent;
import com.threerings.msoy.item.data.all.ItemIdent;

import com.threerings.msoy.money.data.all.MemberMoney;
import com.threerings.msoy.money.data.all.MoneyHistory;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PriceQuote;
import com.threerings.msoy.money.data.all.TransactionType;

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
     * Secures a price for an item. This ensures the user will be able to purchase an item
     * for a set price. This price will remain available for some amount of time (specified by
     * {@link MoneyConfiguration#getSecurePriceDuration()}. The secured price may also be removed
     * if the maximum number of secured prices system-wide has been reached (specified by
     * {@link MoneyConfiguration#getMaxSecuredPrices()}. In either case, an attempt to buy the
     * item will fail with a {@link NotSecuredException}.
     *
     * @param buyerId the memberId of the buying user.
     * @param item the identity of the catalog listing.
     * @param listedCurrency the currency at which the item is listed.
     * @param listedAmount the amount at which the item is listed.
     * @param creatorId the memberId of the seller.
     * @param affiliateId the id of the affiliate associated with the purchase. Uhm..
     * @param description A description of the item that will appear on the member's transaction
     * history if purchased.
     * @return A full PriceQuote for the item.
     */
    PriceQuote securePrice (
        int buyerId, CatalogIdent item, Currency listedCurrency, int listedAmount,
        int sellerId, int affiliateId, String description);

    /**
     * Yeah, some shite.
     * Purchases an item. This will only update the appropriate
     * accounts of an exchange of money -- item fulfillment must be handled separately.
     *
     * @param buyer the member record of the buying user.
     * @param item the identity of the catalog listing.
     * @param listedCurrency the currency at which the item is listed.
     * @param listedAmount the amount at which the item is listed.
     * @param buyType the currency the buyer is using
     * @param buyAmount the amount the buyer has validated to purchase the item.
     * @param isSupport is the buyer a member of the support staff?
     * @throws NotSecuredException iff there is no secured price for the item and the authorized
     * buy amount is not enough money.
     */
    MoneyResult buyItem (
        MemberRecord buyer, CatalogIdent item, Currency listedCurrency, int listedAmount,
        Currency buyType, int buyAmount)
        throws NotEnoughMoneyException, NotSecuredException;

    /**
     * The member has purchased some number of bars. This will add the number of bars to their
     * account, fulfilling the transaction.
     *
     * @param memberId ID of the member receiving bars.
     * @param numBars Number of bars to add to their account.
     * @return The money the member now has in their account.
     */
    MoneyResult buyBars (int memberId, int numBars, String description);

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
     * @param currency Money type to retrieve logs for. If null, then records for all money types are
     * returned.
     * @param transactionTypes Set of transaction types to retrieve logs for.  If null, all
     *      transactionTypes will be retrieved.
     * @param start Zero-based index of the first log item to return.
     * @param count The number of log items to return. If Integer.MAX_VALUE, this will return all
     * records.
     * @param descending If true, the log will be sorted by transaction date descending.
     * @return List of requested past transactions.
     */
    List<MoneyHistory> getLog (
        int memberId, Currency currency, EnumSet<TransactionType> transactionTypes,
        int start, int count, boolean descending);

    /**
     * Retrieves the total number of transaction history entries we have stored for this query.
     *
     * @param memberId ID of the member to count transactions for.
     * @param currency Currency to retrieve logs for. If null, then records for all money types are
     * counted.
     * @param transactionTypes Set of transaction types to retrieve logs for.  If null, all
     *      transactionTypes will be counted.
     */
    int getHistoryCount (
        int memberId, Currency currency, EnumSet<TransactionType> transactionTypes);

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
