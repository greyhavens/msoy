//
// $Id$

package com.threerings.msoy.server;

import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.IntSet;

import com.threerings.msoy.web.gwt.ExternalAuther;

import com.threerings.msoy.facebook.server.FacebookAuthHandler;

import com.threerings.msoy.server.persist.MemberRepository;

/**
 * Provides access to our {@link ExternalAuthHandler}.
 */
@Singleton
public class ExternalAuthLogic
{
    /**
     * Returns the handler for the supplied external authentication source or null if no handler
     * exists for the supplied source.
     */
    public ExternalAuthHandler getHandler (ExternalAuther auther)
    {
        switch (auther) {
        case FACEBOOK:
            return _faceAuther;
        default:
            return null;
        }
    }

    /**
     * Creates friend connections between the specified member and the supplied list of external
     * site friend identifiers.
     */
    public void wireUpExternalFriends (int memberId, ExternalAuther auther, List<String> friendIds)
    {
        if (friendIds == null) {
            return; // nothing doing!
        }

        IntSet haveIds = _memberRepo.loadFriendIds(memberId);
        for (int friendId : _memberRepo.lookupExternalAccounts(auther, friendIds)) {
            if (!haveIds.contains(friendId)) {
                _memberRepo.noteFriendship(memberId, friendId);
            }
        }
    }

    // dependencies
    @Inject protected MemberRepository _memberRepo;
    @Inject protected FacebookAuthHandler _faceAuther;
}
