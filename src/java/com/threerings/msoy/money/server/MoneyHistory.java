//
// $Id$

package com.threerings.msoy.money.server;

import java.util.Date;

import net.jcip.annotations.Immutable;

import com.threerings.msoy.item.data.all.ItemIdent;

/**
 * Contains the history of a single transaction involving coins, bars, or bling.
 *
 * @author Kyle Sampson <kyle@threerings.net>
 */
@Immutable
public class MoneyHistory
{
    public MoneyHistory (final int memberId, final Date timestamp, final MoneyType type, final double amount,
        final boolean spent, final String description, final ItemIdent item)
    {
        this.memberId = memberId;
        this.timestamp = timestamp;
        this.type = type;
        this.amount = amount;
        this.spent = spent;
        this.description = description;
        this.item = item;
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
     * Item involved in the transaction, or null if no item.
     */
    public ItemIdent getItem ()
    {
        return item;
    }

    private final int memberId;
    private final Date timestamp;
    private final MoneyType type;
    private final double amount;
    private final boolean spent;
    private final String description;
    private final ItemIdent item;
}
