//
// $Id$

package com.threerings.msoy.web.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

// A marker interface to tag anything that can show up in someone's feed
public interface Activity
    extends IsSerializable
{
    /** When this activity happened. */
    long startedAt ();
}
