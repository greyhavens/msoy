//
// $Id$

package com.threerings.msoy.swiftly.client.view;

/**
 * An object interested in notification when a component has been removed from the UI.
 */
public interface RemovalNotifier<T>
{
    public void componentRemoved (T component);
}
