//
// $Id$

package client.util;

import client.shell.CShell;

import client.util.events.StatusChangeEvent;

import com.google.gwt.i18n.client.NumberFormat;
import com.threerings.msoy.money.data.all.BalanceInfo;

/**
 * I can't think of a better name.
 */
public class MoneyUtil
{
    /**
     * Update the balances displayed in the header.
     */
    public static void updateBalances (BalanceInfo balances)
    {
        // if the flash client is around, we don't want to update any balances, because the
        // values from the flash client could include earned money from games that have not
        // yet been persisted to the database.
        if (FlashClients.clientConnected()) {
            return;
        }

        if (balances.coins != null) {
            CShell.frame.dispatchEvent(new StatusChangeEvent(StatusChangeEvent.COINS,
                balances.coins, balances.coins));
        }
        if (balances.bars != null) {
            CShell.frame.dispatchEvent(new StatusChangeEvent(StatusChangeEvent.BARS,
                balances.bars, balances.bars));
        }
    }

    /**
     * Converts the amount of pennies into a string to display to the user as a valid currency.
     * Note: there are some other utilities around to do this, but they're either in a different
     * project (and there's some concern about exposing them directly), or they don't properly
     * take into account floating-point round off errors.  This may get replaced or expanded
     * later on.
     */
    public static String formatUSD (int pennies)
    {
        int dollars = pennies / 100;
        int cents = pennies % 100;
        return "USD $" + NumberFormat.getDecimalFormat().format(dollars) + '.' +
            (cents < 10 ? '0' : "") + cents;
    }
}
