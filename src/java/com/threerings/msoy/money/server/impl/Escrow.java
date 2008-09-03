//
// $Id$

package com.threerings.msoy.money.server.impl;

import java.io.Serializable;

import com.threerings.msoy.money.data.all.PriceQuote;

/**
 * Contains all the information for a quoted transaction.
 *
 * @author Ray Greenwell <ray@threerings.net>
 */
public class Escrow
    implements Serializable
{
    public Escrow (final int creatorId, final int affiliateId, final String description, 
        final PriceQuote quote)
    {
        _creatorId = creatorId;
        _affiliateId = affiliateId;
        _description = description;
        _quote = quote;
    }

    public int getCreatorId ()
    {
        return _creatorId;
    }

    public int getAffiliateId ()
    {
        return _affiliateId;
    }

    public String getDescription ()
    {
        return _description;
    }

    public PriceQuote getQuote ()
    {
        return _quote;
    }

    private final int _creatorId;
    private final int _affiliateId;
    private final String _description;
    private final PriceQuote _quote;
}
