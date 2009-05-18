//
// $Id$

package com.threerings.msoy.money.server;

import java.util.List;

import com.threerings.msoy.money.data.all.BalanceInfo;
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
        boolean magicFreeBuy, MoneyTransaction memberTx, MoneyTransaction changeTx,
        List<MoneyTransaction> creatorTxs, MoneyTransaction affiliateTx,
        MoneyTransaction charityTx)
    {
        _magicFree = magicFreeBuy;
        _memberTransaction = memberTx;
        _changeTransaction = changeTx;
        _creatorTransactions = creatorTxs;
        _affiliateTransaction = affiliateTx;
        _charityTransaction = charityTx;
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
     * The transaction for the change in coins when purchasing an item listed in coins with bars.
     * Null if there was no change.
     */
    public MoneyTransaction getChangeTransaction ()
    {
        return _changeTransaction;
    }

    /**
     * The transactions that were performed on the primary creator's and contributors' accounts.
     * Null if no creators were modified.  Note that the creators' accounts will still be modified
     * if an operation involving 0 coins was performed.
     */
    public List<MoneyTransaction> getCreatorTransactions ()
    {
        return _creatorTransactions;
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

    /**
     * The transaction that was performed on the charity's account.  Null if no charity was
     * involved.
     */
    public MoneyTransaction getCharityTransaction ()
    {
        return _charityTransaction;
    }

    public BalanceInfo getBuyerBalances ()
    {
        int buyerId = _memberTransaction.memberId;
        BalanceInfo balances = new BalanceInfo();
        // examine each transaction in turn, freely blowing away balance info from an earlier
        // transaction if applicable, since payouts are done in buyer, creator, affiliate order.
        updateBalance(balances, buyerId, _memberTransaction);
        updateBalance(balances, buyerId, _changeTransaction);
        for (MoneyTransaction tx : _creatorTransactions) {
            updateBalance(balances, buyerId, tx);
        }
        updateBalance(balances, buyerId, _affiliateTransaction);
        return balances;
    }

    protected void updateBalance (BalanceInfo balances, int memberId, MoneyTransaction tx)
    {
        if (tx == null || tx.memberId != memberId) {
            return;
        }

        switch (tx.currency) {
        case COINS:
            balances.coins = tx.balance;
            break;

        case BARS:
            balances.bars = tx.balance;
            break;

        case BLING:
            balances.bling = tx.balance;
            break;
        }
    }

    protected boolean _magicFree;
    protected MoneyTransaction _memberTransaction;
    protected MoneyTransaction _changeTransaction;
    protected List<MoneyTransaction> _creatorTransactions;
    protected MoneyTransaction _affiliateTransaction;
    protected MoneyTransaction _charityTransaction;
}
