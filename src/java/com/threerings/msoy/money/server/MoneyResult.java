//
// $Id$

package com.threerings.msoy.money.server;

import com.threerings.msoy.money.data.all.MoneyHistory;

/**
 * Result of an operation on the money service that modified a member's, creator's, or
 * affiliate's accounts.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class MoneyResult
{
    public MoneyResult (final MemberMoney newMemberMoney, final MemberMoney newCreatorMoney,
        final MemberMoney newAffiliateMoney, final MoneyHistory memberTransaction,
        final MoneyHistory creatorTransaction, final MoneyHistory affiliateTransaction)
    {
        this.newMemberMoney = newMemberMoney;
        this.newCreatorMoney = newCreatorMoney;
        this.newAffiliateMoney = newAffiliateMoney;
        this.memberTransaction = memberTransaction;
        this.creatorTransaction = creatorTransaction;
        this.affiliateTransaction = affiliateTransaction;
    }

    /**
     * The new amount of money in the member's account. 
     */
    public MemberMoney getNewMemberMoney ()
    {
        return newMemberMoney;
    }
    
    /**
     * The new amount of money in the creator's account.  Null if the creator account was not 
     * modified.  Note that the creator account will still be modified if an operation involving
     * 0 coins was performed. 
     */
    public MemberMoney getNewCreatorMoney ()
    {
        return newCreatorMoney;
    }

    /**
     * The new amount of money in the affiliate's account.  Null if the affiliate was not
     * modified.  Note that the affiliate account will still be modified if an operation involving
     * 0 coins was performed.
     */
    public MemberMoney getNewAffiliateMoney ()
    {
        return newAffiliateMoney;
    }

    /**
     * The transaction that was performed on the member's account.
     */
    public MoneyHistory getMemberTransaction ()
    {
        return memberTransaction;
    }
    
    /**
     * The transaction that was performed on the creator's account.  Null if the creator was
     * not modified.  Note that the creator account will still be modified if an operation involving
     * 0 coins was performed. 
     */
    public MoneyHistory getCreatorTransaction ()
    {
        return creatorTransaction;
    }
    
    /**
     * The transaction that was performed on the affiliate's account.  Null if the affiliate
     * was not modified.  Note that the affiliate account will still be modified if an operation 
     * involving 0 coins was performed.
     */
    public MoneyHistory getAffiliateTransaction ()
    {
        return affiliateTransaction;
    }
    
    private final MemberMoney newMemberMoney;
    private final MemberMoney newCreatorMoney;
    private final MemberMoney newAffiliateMoney;
    private final MoneyHistory memberTransaction;
    private final MoneyHistory creatorTransaction;
    private final MoneyHistory affiliateTransaction;
}
