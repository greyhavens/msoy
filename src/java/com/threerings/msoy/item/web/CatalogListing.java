//
// $Id$

package com.threerings.msoy.item.web;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.threerings.io.Streamable;

/**
 * Represents a catalog listing of an item.
 * 
 */
public class CatalogListing
    implements Streamable, IsSerializable
{
    public Item item;
    public Date listedDate;
}
