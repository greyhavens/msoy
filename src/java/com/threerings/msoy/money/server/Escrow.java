//
// $Id$

package com.threerings.msoy.money.server;

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

    protected final int _creatorId;
    protected final int _affiliateId;
    protected final String _description;
    protected final PriceQuote _quote;
}
