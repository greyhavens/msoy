//
// $Id$

package com.threerings.msoy.game.server;

import com.google.inject.Inject;

import com.threerings.crowd.server.CrowdSession;

import com.threerings.msoy.data.MsoyTokenRing;
import com.threerings.msoy.data.all.VisitorInfo;

import com.threerings.msoy.game.data.GameCredentials;
import com.threerings.msoy.game.data.PlayerObject;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.MsoyObjectAccess;

import static com.threerings.msoy.Log.log;

/**
 * Manages the server side of a client connection for the MSOY Game server.
 */
public class GameSession extends CrowdSession
{
    @Override // from PresentsSession
    protected void sessionWillStart ()
    {
        super.sessionWillStart();

        _plobj = (PlayerObject) _clobj;
        _plobj.setAccessController(MsoyObjectAccess.PLAYER);

        // configure their access control tokens
        MsoyTokenRing tokens = (MsoyTokenRing) _authdata;
        _plobj.setTokens(tokens == null ? new MsoyTokenRing() : tokens);

        // if this is a guest account, they didn't get a VisitorInfo through the resolver.
        // so let's pull one from their flash credentials, or manufacture a brand new one.
        if (_plobj.visitorInfo == null) {
            GameCredentials credentials = (GameCredentials) getCredentials();
            if (credentials.visitorId != null) {
                _plobj.visitorInfo = new VisitorInfo(credentials.visitorId, false);
            } else {
                _plobj.visitorInfo = new VisitorInfo();
                _eventLog.visitorInfoCreated(_plobj.visitorInfo, false);
            }
        }

        log.debug("Player session starting", "memberId", _plobj.memberName.getMemberId(),
                  "memberName", _plobj.memberName, "oid", _plobj.getOid());

        // let our various server entities know that this member logged on
        _locator.playerLoggedOn(_plobj);
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

        // clear out this player's association with this game
        _playerActions.clearPlayerGame(_plobj.getMemberId());

        // clear out our player object reference
        _plobj = null;
    }

    @Override // from PresentsSession
    protected long getFlushTime ()
    {
        return 10 * 1000L; // give them just long enough to replace their session
    }

    /** A casted reference to the userobject. */
    protected PlayerObject _plobj;

    @Inject protected MsoyEventLogger _eventLog;
    @Inject protected PlayerLocator _locator;
    @Inject protected PlayerNodeActions _playerActions;
}
