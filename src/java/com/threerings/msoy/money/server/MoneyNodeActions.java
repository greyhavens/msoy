//
// $Id$

package com.threerings.msoy.money.server;

import com.google.inject.Inject;

import com.threerings.presents.peer.data.NodeObject;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyAuthName;
import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.game.data.GameAuthName;
import com.threerings.msoy.server.MemberLocator;

import com.threerings.msoy.game.data.PlayerObject;
import com.threerings.msoy.game.server.PlayerLocator;
import com.threerings.msoy.peer.server.MemberNodeAction;
import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.server.persist.MoneyTransactionRecord;

public class MoneyNodeActions
{
    @Inject
    public MoneyNodeActions (final MsoyPeerManager peerMan)
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
        moneyUpdated(memberId, Currency.COINS, amount, true);
    }

    /**
     * Dispatches a notification that a member's money count has changed to whichever server they
     * are logged into.
     */
    public void moneyUpdated (final MoneyTransactionRecord tx, boolean updateAcc)
    {
        moneyUpdated(tx.memberId, tx.currency, tx.amount, updateAcc);
    }

    /**
     * Dispatches a notification that a member's money count has changed to whichever server they
     * are logged into.
     */
    public void moneyUpdated (int memberId, Currency currency, int amount, boolean updateAcc)
    {
        if (currency != Currency.BLING) { // avoid spamming the other nodes
            _peerMan.invokeNodeAction(new MoneyUpdated(memberId, currency, amount, updateAcc));
        }
    }

    protected static class MoneyUpdated extends MsoyPeerManager.NodeAction
    {
        public MoneyUpdated (int memberId, Currency currency, int amount, boolean updateAcc)
        {
            _memberId = memberId;
            _currency = currency;
            _amount = amount;
            _updateAcc = updateAcc;

            _memberKey = MsoyAuthName.makeKey(memberId);
            _playerKey = GameAuthName.makeKey(memberId);
        }

        public MoneyUpdated () { }

        @Override // from MsoyPeerManager.NodeAction
        public boolean isApplicable (NodeObject nodeobj)
        {
            return (nodeobj.clients.containsKey(_memberKey) ||
                    nodeobj.clients.containsKey(_playerKey));
        }

        @Override // from MsoyPeerManager.NodeAction
        protected void execute ()
        {
            MemberObject memobj = _mlocator.lookupMember(_memberId);
            if (memobj != null) {
                memobj.startTransaction();
                try {
                    switch (_currency) {
                    case COINS:
                        memobj.setCoins(memobj.coins + _amount);
                        if (_amount > 0 && _updateAcc) {
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

            PlayerObject plobj = _plocator.lookupPlayer(_memberId);
            if (plobj != null) {
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
        }

        protected int _memberId;
        protected Currency _currency;
        protected int _amount;
        protected boolean _updateAcc;

        protected transient MsoyAuthName _memberKey;
        protected transient GameAuthName _playerKey;

        @Inject protected transient MemberLocator _mlocator;
        @Inject protected transient PlayerLocator _plocator;
    }

    protected final MsoyPeerManager _peerMan;
}
