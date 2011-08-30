//
// $Id$

package com.threerings.msoy.money.server;

import com.google.inject.Inject;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.game.data.PlayerObject;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.server.persist.MoneyTransactionRecord;
import com.threerings.msoy.peer.server.MemberPlayerNodeAction;
import com.threerings.msoy.peer.server.MsoyPeerManager;

public class MoneyNodeActions
{
    @Inject
    public MoneyNodeActions (MsoyPeerManager peerMan)
    {
        _peerMan = peerMan;
    }

    /**
     * Reports to a member that they have earned some number of coins. This will notify interested
     * clients that coins were earned, without actually awarding the coins.  Future calls to {@link
     * MoneyLogic#awardCoins(int, int, boolean, UserAction)} to award the coins must use "false"
     * for notify to indicate the user was already notified of the earnings.
     *
     * @param memberId the member who earned coins.
     * @param amount number of coins earned.
     */
    public void coinsEarned (int memberId, int amount)
    {
        moneyUpdated(memberId, Currency.COINS, amount);
    }

    /**
     * Dispatches a notification that a member's money count has changed to whichever server they
     * are logged into.
     */
    public void moneyUpdated (MoneyTransactionRecord tx)
    {
        moneyUpdated(tx.memberId, tx.getCurrency(), tx.amount);
    }

    protected void moneyUpdated (int memberId, Currency currency, int amount)
    {
        if (currency != Currency.BLING) { // avoid spamming the other nodes
            _peerMan.invokeNodeAction(new MoneyUpdated(memberId, currency, amount));
        }
    }

    protected static class MoneyUpdated extends MemberPlayerNodeAction
    {
        public MoneyUpdated (int memberId, Currency currency, int amount)
        {
            super(memberId);
            _currency = currency;
            _amount = amount;
        }

        public MoneyUpdated () { }

        @Override // from MemberNodeAction
        protected void execute (MemberObject memobj)
        {
            memobj.startTransaction();
            try {
                switch (_currency) {
                case COINS:
                    memobj.setCoins(memobj.coins + _amount);
                    break;
                case BARS:
                    memobj.setBars(memobj.bars + _amount);
                    break;
                case BLING:
                    // Changes to bling are ignored.
                    break;
                }
            } finally {
                memobj.commitTransaction();
            }
        }

        @Override // from MemberPlayerNodeAction
        protected void execute (PlayerObject plobj)
        {
            switch (_currency) {
            case COINS:
                plobj.setCoins(plobj.coins + _amount);
                break;
            case BARS:
                plobj.setBars(plobj.bars + _amount);
                break;
            case BLING:
                // Changes to bling are ignored.
                break;
            }
        }

        protected Currency _currency;
        protected int _amount;
    }

    protected final MsoyPeerManager _peerMan;
}
