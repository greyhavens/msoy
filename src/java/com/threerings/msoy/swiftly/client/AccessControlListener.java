//
// $Id$

package com.threerings.msoy.swiftly.client;

public interface AccessControlListener
{
    /**
     * Called to inform this component to display a writeable interface.
     */
    public void setWriteAccess();

    /**
     * Called to inform this component to display a read-only interface.
     */
    public void setReadOnlyAccess();
}
