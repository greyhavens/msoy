//
// $Id$

package com.threerings.msoy.server;

import com.threerings.msoy.server.persist.MailRepository;
import com.threerings.msoy.server.persist.MemberRepository;

/**
 * Manage msoy mail.
 */
public class MailManager
{
    /**
     * Prepares our mail manager for operation.
     */
    public void init (MailRepository mailRepo, MemberRepository memberRepo)
    {
        _mailRepo = mailRepo;
        _memberRepo = memberRepo;
    }

    /** Provides access to persistent mail data. */
    protected MailRepository _mailRepo;
    
    /** Provides access to persistent member data. */
    protected MemberRepository _memberRepo;
}
