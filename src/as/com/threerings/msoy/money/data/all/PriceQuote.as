//
// $Id$

package com.threerings.msoy.money.data.all {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.SimpleStreamableObject;

/**
 * Contains secured prices when a member views an item. This can be cached and identified by a
 * PriceKey.
 */
public class PriceQuote extends SimpleStreamableObject
{
    /**
     * Get the currency with which this item was listed.
     */
    public function getListedCurrency () :Currency
    {
        return _listedCurrency;
    }

    /**
     * Get the listed amount, be it coins or bars.
     */
    public function getListedAmount () :int
    {
        return getAmount(_listedCurrency);
    }

    /**
     * Get the amount of this quote in the specified currency.
     */
    public function getAmount (currency :Currency) :int
    {
        return (currency == Currency.BARS) ? _bars : _coins;
    }

    public function getCoins () :int
    {
        return _coins;
    }

    public function getBars () :int
    {
        return _bars;
    }

    /**
     * Retrieve the amount of change, in coins, when bar-buying a coin-listed item.
     * If the listedType is BARS, this will always be 0.
     */
    public function getCoinChange () :int
    {
        return _coinChange;
    }

    /**
     * Get the raw exchange rate at the time the quote was generated.
     */
    public function getExchangeRate () :Number
    {
        return _rate;
    }

    /**
     * Return the current cost in cents for a single bar.
     */
    public function getCentsPerBar () :int
    {
        return _centsPerBar;
    }

    /* For debugging */
    override public function toString () :String
    {
        return "PriceQuote[listed=" + _listedCurrency + ", coins=" + _coins + ", bars=" + _bars +
            ", change=" + _coinChange + ", rate=" + _rate + ", centsPerBar=" + _centsPerBar + "]";
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        _listedCurrency = ins.readObject(Currency) as Currency;
        _coins = ins.readInt();
        _bars = ins.readInt();
        _coinChange = ins.readInt();
        _rate = ins.readFloat();
        _centsPerBar = ins.readInt();
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        // price quotes are read-only on the client
        throw new Error("Writing price quote: " + this);
    }

    protected var _listedCurrency :Currency;
    protected var _coins :int;
    protected var _bars :int;
    protected var _coinChange :int;
    protected var _rate :Number;
    protected var _centsPerBar :int;
}
}
