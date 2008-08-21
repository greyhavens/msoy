//
// $Id$

package com.threerings.msoy.badge.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Defines the various badge categories, which can be used to suggest to the user
 * which badges to pursue next, based on their past activity.
 */
public enum StampCategory
    implements IsSerializable
{
    SOCIAL, GAME, CREATION, SHOPPING
}
