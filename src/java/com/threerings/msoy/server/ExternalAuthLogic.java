//
// $Id$

package com.threerings.msoy.server;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.IntSet;

import com.threerings.msoy.web.gwt.ExternalAuther;

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
        return _exhandlers.get(auther);
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

    /** Our external authentication handlers. */
    protected Map<ExternalAuther, ExternalAuthHandler> _exhandlers = ImmutableMap.of(
        ExternalAuther.FACEBOOK, FacebookAuthHandler.getInstance()
    );

    // dependencies
    @Inject protected MemberRepository _memberRepo;
}
