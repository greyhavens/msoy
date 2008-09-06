//
// $Id$

package com.threerings.msoy.money.data.all;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.threerings.msoy.item.data.all.ItemIdent;

/**
 * Contains the history of a single transaction involving coins, bars, or bling.
 * 
 * Immutable, except for serialization needs.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class MoneyHistory
    implements IsSerializable
{
    // Required by for serializing
    public MoneyHistory () { }

    public MoneyHistory (final int memberId, final Date timestamp, final Currency currency,
        final double amount, final TransactionType transactionType, final boolean spent, 
        final String description, final ItemIdent item, final MoneyHistory referenceTx)
    {
        _memberId = memberId;
        _timestamp = timestamp;
        _currency = currency;
        _amount = amount;
        _transactionType = transactionType;
        _spent = spent;
        _description = description;
        _item = item;
        _referenceTx = referenceTx;
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
    public Currency getCurrency ()
    {
        return _currency;
    }

    /**
     * Item involved in the transaction, or null if no item.
     */
    public ItemIdent getItem ()
    {
        return _item;
    }
    
    /**
     * The type of this transaction.
     */
    public TransactionType getTransactionType ()
    {
        return _transactionType;
    }
    
    /**
     * For some transaction types, the transaction that corresponds to this transaction for the
     * other member involved.  For example, if A sold an item to B, two transactions would be
     * created.  Their reference transactions will be each other.  If there is no reference
     * transaction, this should be null.
     */
    public MoneyHistory getReferenceTx ()
    {
        return _referenceTx;
    }

    protected int _memberId;
    protected Date _timestamp;
    protected Currency _currency;
    protected TransactionType _transactionType;
    protected double _amount; // TODO int
    protected boolean _spent;
    protected String _description;
    protected ItemIdent _item;
    protected MoneyHistory _referenceTx;
}
