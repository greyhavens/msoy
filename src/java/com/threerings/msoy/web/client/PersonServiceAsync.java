//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The asynchronous (client-side) version of {@link PersonService}.
 */
public interface PersonServiceAsync
{
    /**
     * The asynchronous version of {@link PersonService#loadBlurbs}.
     */
    public void loadBlurbs (int memberId, AsyncCallback callback);
}
