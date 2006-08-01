//
// $Id$

package com.threerings.msoy.web.client;

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
}
