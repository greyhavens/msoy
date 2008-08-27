//
// $Id$

package com.threerings.msoy.money.data.all;

import java.util.Date;

import com.threerings.msoy.item.data.all.ItemIdent;

/**
 * Contains the history of a single transaction involving coins, bars, or bling.
 * 
 * @Immutable
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class MoneyHistory
{
    public MoneyHistory (
        final int memberId, final Date timestamp, final MoneyType type,
        final double amount, final boolean spent, final String description,
        final ItemIdent item)
    {
        _memberId = memberId;
        _timestamp = timestamp;
        _type = type;
        _amount = amount;
        _spent = spent;
        _description = description;
        _item = item;
    }

    /**
     * ID of the member account this history is for.
     */
    public int getMemberId ()
    {
        return _memberId;
    }

    /**
     * Time the transaction was performed.
     */
    public Date getTimestamp ()
    {
        return _timestamp;
    }

    /**
     * The amount that was exchanged in the transaction.
     */
    public double getAmount ()
    {
        return _amount;
    }

    /**
     * If true, this amount was deducted from the account. Otherwise it was credited to the
     * account.
     */
    public boolean isSpent ()
    {
        return _spent;
    }

    /**
     * Description of the transaction.
     */
    public String getDescription ()
    {
        return _description;
    }

    /**
     * Type of money that was transferred.
     */
    public MoneyType getType ()
    {
        return _type;
    }

    /**
     * Item involved in the transaction, or null if no item.
     */
    public ItemIdent getItem ()
    {
        return _item;
    }

    private final int _memberId;
    private final Date _timestamp;
    private final MoneyType _type;
    private final double _amount;
    private final boolean _spent;
    private final String _description;
    private final ItemIdent _item;
}
