//
// $Id$

package com.threerings.msoy.game.server;

import com.google.inject.Inject;

import com.threerings.crowd.server.CrowdClient;

import com.threerings.msoy.Log;
import com.threerings.msoy.data.MsoyTokenRing;

import com.threerings.msoy.game.data.MsoyGameCredentials;
import com.threerings.msoy.game.data.PlayerObject;

/**
 * Manages the server side of a client connection for the MSOY Game server.
 */
public class MsoyGameClient extends CrowdClient
{
    @Override // from PresentsClient
    protected void sessionWillStart ()
    {
        super.sessionWillStart();

        _plobj = (PlayerObject) _clobj;

        // configure their access control tokens
        MsoyTokenRing tokens = (MsoyTokenRing) _authdata;
        MsoyGameCredentials credentials = (MsoyGameCredentials) getCredentials();

        _plobj.setTokens(tokens == null ? new MsoyTokenRing() : tokens);

        Log.log.info("Player session starting", "memberId", _plobj.memberName.getMemberId(),
            "memberName", _plobj.memberName, "playerId", _plobj.getOid());

        // let our various server entities know that this member logged on
        _locator.playerLoggedOn(_plobj);
    }

    @Override // from PresentsClient
    protected void sessionConnectionClosed ()
    {
        super.sessionConnectionClosed();

        // if we're a guest, end our session now, there's no way to reconnect
        if (_plobj != null && _plobj.isGuest()) {
            safeEndSession();
        }
    }

    @Override // from PresentsClient
    protected void sessionDidEnd ()
    {
        super.sessionDidEnd();

        if (_plobj == null) {
            return;
        }

        // let our various server entities know that this member logged off
        _locator.playerLoggedOff(_plobj);

        // nothing more needs doing for guests
        if (_plobj.isGuest()) {
            _plobj = null;
            return;
        }

//         final MemberName name = _plobj.memberName;
//         final StatSet stats = _plobj.stats;
        _plobj = null;

//         // update the member record in the database
//         _invoker.postUnit(new Invoker.Unit("sessionDidEnd:" + name) {
//             public boolean invoke () {
//                 try {
//                     // write out any modified stats
//                     Stat[] statArr = new Stat[stats.size()];
//                     stats.toArray(statArr);
//                     _statRepo.writeModified(name.getMemberId(), statArr);

//                 } catch (Exception e) {
//                     log.warning("Failed to note ended session [member=" + name + "].", e);
//                 }
//                 return false;
//             }
//         });
    }

    /** A casted reference to the userobject. */
    protected PlayerObject _plobj;

    @Inject protected PlayerLocator _locator;
}
