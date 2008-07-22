//
// $Id$

package com.threerings.msoy.money.server;

import java.util.Date;

/**
 * Contains the history of a single transaction involving coins, bars, or bling.
 *
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class MoneyHistory
{
    public MoneyHistory (final int memberId, final Date timestamp, final MoneyType type, final double amount,
        final boolean spent, final String description, final int itemId, final int itemType)
    {
        this.memberId = memberId;
        this.timestamp = timestamp;
        this.type = type;
        this.amount = amount;
        this.spent = spent;
        this.description = description;
        this.itemId = itemId;
        this.itemType = itemType;
    }

    /**
     * ID of the member account this history is for.
     */
    public int getMemberId ()
    {
        return memberId;
    }

    /**
     * Time the transaction was performed.
     */
    public Date getTimestamp ()
    {
        return timestamp;
    }

    /**
     * The amount that was exchanged in the transaction.
     */
    public double getAmount ()
    {
        return amount;
    }

    /**
     * If true, this amount was deducted from the account.  Otherwise it was credited
     * to the account.
     */
    public boolean isSpent ()
    {
        return spent;
    }

    /**
     * Description of the transaction.
     */
    public String getDescription ()
    {
        return description;
    }

    /**
     * Type of money that was transferred.
     */
    public MoneyType getType ()
    {
        return type;
    }

    /**
     * ID of the item that was involved in this transaction.
     * @return
     */
    public int getItemId ()
    {
        return itemId;
    }

    /**
     * Type of the item that was involved in this transaction.
     */
    public int getItemType ()
    {
        return itemType;
    }

    private final int memberId;
    private final Date timestamp;
    private final MoneyType type;
    private final double amount;
    private final boolean spent;
    private final String description;
    private final int itemId;
    private final int itemType;
}
