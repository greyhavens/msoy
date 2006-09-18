//
// $Id$

package com.threerings.msoy.item.web;

import java.util.Date;

/**
 * Represents a catalog listing of an item.
 * 
 * This class should really be <T extends Item>, but GWT won't let us.
 */
public class CatalogListing
{
    public Item item;
    public Date listedDate;
}
