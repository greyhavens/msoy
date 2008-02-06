//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

/** 
 * Contains the data for the WhatIsWhirled page.
 */
public class WhatIsWhirledData
    implements IsSerializable
{
    /** The total number of players in Whirled. */
    public int players;

    /** The total number of places in Whirled. */
    public int places;

    /** The total number of games in Whirled. */
    public int games;
}
