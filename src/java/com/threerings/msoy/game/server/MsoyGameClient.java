//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.crowd.server.CrowdClient;

import com.threerings.msoy.data.MsoyTokenRing;

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
        _plobj.setTokens(tokens == null ? new MsoyTokenRing() : tokens);

        // let our various server entities know that this member logged on
        MsoyGameServer.playerLoggedOn(_plobj);
    }

    @Override // from PresentsClient
    protected void sessionConnectionClosed ()
    {
        super.sessionConnectionClosed();

//         // if we're a guest, end our session now, there's no way to reconnect
//         if (_plobj != null && _plobj.isGuest()) {
//             safeEndSession();
//         }

        // TEMP: for now end everyone's game session if they disconnect; we don't currently support
        // reentering a game automatically so we might as well give the player the boot
        if (_plobj != null) {
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
        MsoyGameServer.playerLoggedOff(_plobj);

        // nothing more needs doing for guests
        if (_plobj.isGuest()) {
            _plobj = null;
            return;
        }

//         final MemberName name = _plobj.memberName;
//         final StatSet stats = _plobj.stats;
        _plobj = null;

//         // update the member record in the database
//         MsoyServer.invoker.postUnit(new Invoker.Unit("sessionDidEnd:" + name) {
//             public boolean invoke () {
//                 try {
//                     // write out any modified stats
//                     Stat[] statArr = new Stat[stats.size()];
//                     stats.toArray(statArr);
//                     MsoyServer.statRepo.writeModified(name.getMemberId(), statArr);

//                 } catch (Exception e) {
//                     log.log(Level.WARNING,
//                             "Failed to note ended session [member=" + name + "].", e);
//                 }
//                 return false;
//             }
//         });
    }

    /** A casted reference to the userobject. */
    protected PlayerObject _plobj;
}
