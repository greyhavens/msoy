//
// $Id$

package com.threerings.msoy.money.server.impl;

/**
 * Contains all the information for a quoted transaction.
 *
 * @author Ray Greenwell <ray@threerings.net>
 */
public class Escrow
{
    public Escrow (int creatorId, int affiliateId, String description, PriceQuote quote)
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
