//
// $Id$

package com.threerings.msoy.money.server;

import com.google.inject.Inject;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.money.data.all.MemberMoney;
import com.threerings.msoy.peer.server.MemberNodeAction;
import com.threerings.msoy.peer.server.MsoyPeerManager;

public class MoneyNodeActions
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
    public void moneyUpdated (final MemberMoney money)
    {
        _peerMan.invokeNodeAction(new MoneyUpdated(money.memberId, money.coins, money.accCoins));
    }

    protected static class MoneyUpdated extends MemberNodeAction
    {
        public MoneyUpdated (int memberId, int coins, long accCoins)
        {
            super(memberId);
            _coins = coins;
            _accCoins = accCoins;
        }
        
        public MoneyUpdated () { }

        @Override
        protected void execute (final MemberObject memobj)
        {
            memobj.startTransaction();
            try {
                memobj.setFlow(_coins);
                if (_accCoins != memobj.accFlow) {
                    memobj.setAccFlow((int)_accCoins);
                }
            } finally {
                memobj.commitTransaction();
            }
        }

        protected int _coins;
        protected long _accCoins;
    }

    protected final MsoyPeerManager _peerMan;
}
