//
// $Id$

package com.threerings.msoy.money.server;

import com.threerings.msoy.money.data.all.MemberMoney;
import com.threerings.msoy.money.data.all.MoneyTransaction;

/**
 * Result of an operation on the money service that modified a member's, creator's, or
 * affiliate's accounts.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class MoneyResult
{
    public MoneyResult (
        MemberMoney newMemberMoney, MemberMoney newCreatorMoney,
        MemberMoney newAffiliateMoney, MoneyTransaction memberTransaction,
        MoneyTransaction creatorTransaction, MoneyTransaction affiliateTransaction)
    {
        _newMemberMoney = newMemberMoney;
        _newCreatorMoney = newCreatorMoney;
        _newAffiliateMoney = newAffiliateMoney;
        _memberTransaction = memberTransaction;
        _creatorTransaction = creatorTransaction;
        _affiliateTransaction = affiliateTransaction;
    }

    /**
     * The new amount of money in the member's account. 
     */
    public MemberMoney getNewMemberMoney ()
    {
        return _newMemberMoney;
    }
    
    /**
     * The new amount of money in the creator's account.  Null if the creator account was not 
     * modified.  Note that the creator account will still be modified if an operation involving
     * 0 coins was performed. 
     */
    public MemberMoney getNewCreatorMoney ()
    {
        return _newCreatorMoney;
    }

    /**
     * The new amount of money in the affiliate's account.  Null if the affiliate was not
     * modified.  Note that the affiliate account will still be modified if an operation involving
     * 0 coins was performed.
     */
    public MemberMoney getNewAffiliateMoney ()
    {
        return _newAffiliateMoney;
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
    
    protected MemberMoney _newMemberMoney;
    protected MemberMoney _newCreatorMoney;
    protected MemberMoney _newAffiliateMoney;
    protected MoneyTransaction _memberTransaction;
    protected MoneyTransaction _creatorTransaction;
    protected MoneyTransaction _affiliateTransaction;
}
