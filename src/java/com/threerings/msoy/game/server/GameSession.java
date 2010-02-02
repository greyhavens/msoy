//
// $Id$

package com.threerings.msoy.game.server;

import com.google.inject.Inject;
import com.samskivert.jdbc.WriteOnlyUnit;
import com.samskivert.util.Invoker;
import com.samskivert.util.StringUtil;
import com.threerings.presents.annotation.MainInvoker;
import com.threerings.crowd.server.CrowdSession;

import com.threerings.msoy.data.MsoyTokenRing;
import com.threerings.msoy.data.all.VisitorInfo;

import com.threerings.msoy.game.data.GameCredentials;
import com.threerings.msoy.game.data.PlayerObject;
import com.threerings.msoy.server.MemberLogic;
import com.threerings.msoy.server.MsoyObjectAccess;
import com.threerings.msoy.server.persist.MemberRepository;

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

        GameCredentials creds = (GameCredentials)getCredentials();
        final String vector = StringUtil.getOr(creds.vector, "game_session");
        final VisitorInfo info = new VisitorInfo(creds.visitorId, false);

        // If this is an embedded game session for a freshly created permaguest, we have not
        // yet associated their visitorId with a tracker and must do so here. But only once, so
        // for now we look in the database whether or not the tracker already exists. We might
        // want to create a field in {@link MsoyCredentials} that suggests a visitorId was
        // freshly created on the client.
        _invoker.postUnit(new WriteOnlyUnit("maybeNoteNewVisitor") {
            public void invokePersist () throws Exception {
                if (_memberRepo.entryVectorExists(info.id) != null) {
                    return;
                }
                _memberLogic.noteNewVisitor(info, false, vector, null);

                // DEBUG
                log.info("VisitorInfo created", "info", _plobj.visitorInfo,
                    "reason", "GameSession", "memberId", _plobj.memberName.getMemberId());
            }
        });

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

    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected MemberLogic _memberLogic;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected PlayerLocator _locator;
    @Inject protected PlayerNodeActions _playerActions;
}
