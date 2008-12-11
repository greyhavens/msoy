//
// $Id$

package com.threerings.msoy.game.server;

import com.google.inject.Inject;

import com.threerings.crowd.server.CrowdSession;

import com.threerings.msoy.data.MsoyTokenRing;
import com.threerings.msoy.data.all.VisitorInfo;

import com.threerings.msoy.game.data.MsoyGameCredentials;
import com.threerings.msoy.game.data.PlayerObject;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.MsoyObjectAccess;

import static com.threerings.msoy.Log.log;

/**
 * Manages the server side of a client connection for the MSOY Game server.
 */
public class MsoyGameSession extends CrowdSession
{
    @Override // from PresentsSession
    protected void sessionWillStart ()
    {
        super.sessionWillStart();

        _plobj = (PlayerObject) _clobj;
        _plobj.setAccessController(MsoyObjectAccess.USER);

        // configure their access control tokens
        MsoyTokenRing tokens = (MsoyTokenRing) _authdata;
        _plobj.setTokens(tokens == null ? new MsoyTokenRing() : tokens);
        MsoyGameCredentials credentials = (MsoyGameCredentials) getCredentials();

        // if this is a guest account, they didn't get a VisitorInfo through the resolver.
        // so let's pull one from their flash credentials, or manufacture a brand new one.
        if (_plobj.visitorInfo == null) {
            if (credentials.visitorId != null) {
                _plobj.visitorInfo = new VisitorInfo(credentials.visitorId, false);
            } else {
                _plobj.visitorInfo = new VisitorInfo();
                _eventLog.visitorInfoCreated(_plobj.visitorInfo, false);
            }
        }

        log.info("Player session starting", "memberId", _plobj.memberName.getMemberId(),
                 "memberName", _plobj.memberName, "oid", _plobj.getOid());

        // let our various server entities know that this member logged on
        _locator.playerLoggedOn(_plobj);
    }

    @Override // from PresentsSession
    protected void sessionConnectionClosed ()
    {
        super.sessionConnectionClosed();

        if (_plobj != null) {
            // there's no sensible way to resume sessions to a game at the moment
            safeEndSession();
        }
    }

    @Override // from PresentsSession
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
    @Inject protected MsoyEventLogger _eventLog;
}
