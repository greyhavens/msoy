//
// $Id$

package com.threerings.msoy.money.server;

import com.threerings.msoy.money.data.all.MoneyTransaction;

/**
 * Result of an operation on the money service that modified a member's, creator's, or
 * affiliate's accounts.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 * @author Ray Greenwell <ray@threerings.net>
 */
public class MoneyResult
{
    public MoneyResult (
        MoneyTransaction memberTx, MoneyTransaction creatorTx, MoneyTransaction affiliateTx)
    {
        _memberTransaction = memberTx;
        _creatorTransaction = creatorTx;
        _affiliateTransaction = affiliateTx;
    }

    /**
     * The transaction that was performed on the member's account.
     */
    public MoneyTransaction getMemberTransaction ()
    {
        return _memberTransaction;
    }
    
    /**
     * The transaction that was performed on the creator's account.  Null if the creator was
     * not modified.  Note that the creator account will still be modified if an operation involving
     * 0 coins was performed. 
     */
    public MoneyTransaction getCreatorTransaction ()
    {
        return _creatorTransaction;
    }
    
    /**
     * The transaction that was performed on the affiliate's account.  Null if the affiliate
     * was not modified.  Note that the affiliate account will still be modified if an operation 
     * involving 0 coins was performed.
     */
    public MoneyTransaction getAffiliateTransaction ()
    {
        return _affiliateTransaction;
    }
    
    protected MoneyTransaction _memberTransaction;
    protected MoneyTransaction _creatorTransaction;
    protected MoneyTransaction _affiliateTransaction;
}
