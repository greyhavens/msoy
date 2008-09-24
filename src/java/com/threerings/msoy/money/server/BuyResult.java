//
// $Id$

package com.threerings.msoy.money.server;

import com.threerings.msoy.money.data.all.MoneyTransaction;

/**
 * The result of a purchase, this returns the member, creator, and affiliate transactions.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 * @author Ray Greenwell <ray@threerings.net>
 */
public class BuyResult
{
    /**
     * @param magicFreeBuy should be true if the item was magic'd up for support
     * personnel and we shouldn't increment the stat of the purchase.
     */
    public BuyResult (
        boolean magicFreeBuy, MoneyTransaction memberTx,
        MoneyTransaction creatorTx, MoneyTransaction affiliateTx)
    {
        _magicFree = magicFreeBuy;
        _memberTransaction = memberTx;
        _creatorTransaction = creatorTx;
        _affiliateTransaction = affiliateTx;
    }

    public boolean wasMagicFreeBuy ()
    {
        return _magicFree;
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
    
    protected boolean _magicFree;
    protected MoneyTransaction _memberTransaction;
    protected MoneyTransaction _creatorTransaction;
    protected MoneyTransaction _affiliateTransaction;
}
