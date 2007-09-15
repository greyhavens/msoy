//
// $Id$

package com.threerings.msoy.swiftly.client.view;

/**
 * A view which displays differently depending on the users access privileges.
 */
public interface AccessControlComponent
{
    /**
     * Show a view for the user having write access.
     */
    public void showWriteAccess ();

    /**
     * Show a view for the user having read only access.
     */
    public void showReadOnlyAccess ();
}
