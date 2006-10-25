//
// $Id$

package com.threerings.msoy.web.client;

import com.threerings.msoy.web.data.WebCreds;

/**
 * Contains a reference to the various bits that we're likely to need in the
 * web client interface.
 */
public class WebContext
{
    /** Our credentials or null if we are not logged in. */
    public WebCreds creds;

    /** Provides user-related services. */
    public WebUserServiceAsync usersvc;

    /** Provides item-related services. */
    public ItemServiceAsync itemsvc;

    /** Provides profile-related services. */
    public ProfileServiceAsync profilesvc;

    /** Provides person-related services. */
    public PersonServiceAsync personsvc;

    /** Provides member-related service. */
    public MemberServiceAsync membersvc;

    /** Provides group-related services. */
    public GroupServiceAsync groupsvc;

    /** Provides mail-related services. */
    public MailServiceAsync mailsvc;

    /** Provides catalog-related services. */
    public CatalogServiceAsync catalogsvc;

    /** Provides game-related services. */
    public GameServiceAsync gamesvc;

}
