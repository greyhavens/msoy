//
// $Id$

package com.threerings.msoy.money.server;

/**
 * Occurs when a member attempts to use some amount of money they do not have.
 *
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class NotEnoughMoneyException extends MoneyException
{
    public NotEnoughMoneyException (final double moneyAvailable, final double moneyRequested,
        final MoneyType type, final int memberId)
    {
        super("An attempt to secure " + moneyRequested + " currency of type " + type + " from member ID " +
            memberId + " failed because only " + moneyAvailable + " is available.");
        this.moneyAvailable = moneyAvailable;
        this.moneyRequested = moneyRequested;
        this.type = type;
        this.memberId = memberId;
    }

    public NotEnoughMoneyException (final double moneyAvailable, final double moneyRequested,
        final MoneyType type, final int memberId, final Throwable cause)
    {
        super("An attempt to secure " + moneyRequested + " currency of type " + type + " from member ID " +
            memberId + " failed because only " + moneyAvailable + " is available.", cause);
        this.moneyAvailable = moneyAvailable;
        this.moneyRequested = moneyRequested;
        this.type = type;
        this.memberId = memberId;
    }

    public int getMemberId ()
    {
        return memberId;
    }

    public double getMoneyAvailable ()
    {
        return moneyAvailable;
    }

    public double getMoneyRequested ()
    {
        return moneyRequested;
    }

    public MoneyType getType ()
    {
        return type;
    }

    private final double moneyAvailable;
    private final double moneyRequested;
    private final MoneyType type;
    private final int memberId;
}
