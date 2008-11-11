//
// $Id$

package com.threerings.msoy.underwire.server;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.google.common.base.Function;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.jdbc.ConnectionProvider;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.persist.MemberWarningRecord;
import com.threerings.msoy.underwire.gwt.MsoyAccount;

import com.threerings.underwire.server.GameInfoProvider;
import com.threerings.underwire.web.data.Account;

/**
 * Provides game-specific info for Whirled.
 */
@Singleton
public class MsoyGameInfoProvider extends GameInfoProvider
{
    @Override // from GameInfoProvider
    public void init (ConnectionProvider conprov)
    {
        // nothing to init
    }

    @Override // from GameInfoProvider
    public HashMap<String,String> resolveGameNames (HashSet<String> names)
    {
        HashMap<String,String> map = new HashMap<String,String>();
        for (MemberName name : _memberRepo.loadMemberNames(names, TO_INT).values()) {
            map.put(Integer.toString(name.getMemberId()), name.toString());
        }
        return map;
    }

    @Override // from GameInfoProvider
    public String[] lookupAccountNames (String gameName)
    {
        List<Integer> memberIds =
            _memberRepo.findMembersByDisplayName(gameName, true, LOOKUP_LIMIT);
        ArrayList<String> names = new ArrayList<String>(memberIds.size());
        for (Integer memberId : memberIds) {
            names.add(memberId.toString());
        }
        return names.toArray(new String[names.size()]);
    }

    @Override // from GameInfoProvider
    public void populateAccount (Account account)
    {
        MemberRecord member = _memberRepo.loadMember(account.email);
        if (member != null) {
            account.firstSession = new Date(member.created.getTime());
            account.lastSession = new Date(member.lastSession.getTime());
            account.altName = member.permaName;
            ((MsoyAccount)account).greeter = member.isGreeter();
            ((MsoyAccount)account).troublemaker = member.isTroublemaker();
            MemberWarningRecord warning = _memberRepo.loadMemberWarningRecord(member.memberId);
            if (warning != null) {
                account.tempBan = warning.banExpires == null ?
                    null : new Date(warning.banExpires.getTime());
                account.warning = warning.warning;
            }
        }
    }

    // our dependencies
    @Inject protected MemberRepository _memberRepo;

    // maximum number of display names to return
    protected static final int LOOKUP_LIMIT = 50;

    protected static final Function<String,Integer> TO_INT = new Function<String,Integer>() {
        public Integer apply (String value) {
            try {
                return Integer.valueOf(value);
            } catch (NumberFormatException nfe) {
                return 0;
            }
        }
    };
}
