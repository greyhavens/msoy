//
// $Id$

package com.threerings.msoy.money.server;

import com.google.inject.Inject;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.server.persist.MoneyTransactionRecord;
import com.threerings.msoy.peer.server.MemberNodeAction;
import com.threerings.msoy.peer.server.MsoyPeerManager;

class MoneyNodeActions
{
    @Inject
    public MoneyNodeActions (final MsoyPeerManager peerMan)
    {
        _peerMan = peerMan;
    }

    /**
     * Dispatches a notification that a member's money count has changed to whichever server they
     * are logged into.
     */
    public void moneyUpdated (final MoneyTransactionRecord tx)
    {
        moneyUpdated(tx.memberId, tx.currency, tx.amount);
    }
    
    public void moneyUpdated (int memberId, Currency currency, int amount) 
    {
        if (currency != Currency.BLING) { // avoid spamming the other nodes
            _peerMan.invokeNodeAction(new MoneyUpdated(memberId, currency, amount));
        }
    }

    protected static class MoneyUpdated extends MemberNodeAction
    {
        public MoneyUpdated (int memberId, Currency currency, int amount)
        {
            super(memberId);
            _currency = currency;
            _amount = amount;
        }
        
        public MoneyUpdated () { }

        @Override
        protected void execute (final MemberObject memobj)
        {
            memobj.startTransaction();
            try {
                switch (_currency) {
                case COINS:
                    memobj.setCoins(memobj.coins + _amount);
                    if (_amount > 0) {
                        memobj.setAccCoins(memobj.accCoins + _amount);
                    }
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

        protected Currency _currency;
        protected int _amount;
    }

    protected final MsoyPeerManager _peerMan;
}
