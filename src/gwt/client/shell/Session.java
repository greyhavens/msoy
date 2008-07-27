//
// $Id$

package client.shell;

import java.util.ArrayList;
import java.util.List;

import com.threerings.msoy.web.data.SessionData;

/**
 * A central place where we keep track of whether or not we've logged on or logged off.
 */
public class Session
{
    /** An interface used for observing logon and logoff. */
    public static interface Observer {
        /** Called when we have just logged on. */
        void didLogon (SessionData data);

        /** Called when we have just logged off. */
        void didLogoff ();
    }

    /**
     * Registers to be notified when we logon or logoff.
     */
    public static void addObserver (Observer observer)
    {
        _observers.add(observer);
    }

    /**
     * Removes an observer registration.
     */
    public static void removeObserver (Observer observer)
    {
        _observers.remove(observer);
    }

    /**
     * Call this method if you know we've just logged on and want to let everyone who cares know
     * about it.
     */
    public static void didLogon (SessionData data)
    {
        for (Observer observer : _observers) {
            try {
                observer.didLogon(data);
            } catch (Exception e) {
                CShell.log("Observer choked in didLogon [observer=" + observer + "]", e);
            }
        }
    }

    /**
     * Call this method if you know we've just logged off and want to let everyone who cares know
     * about it.
     */
    public static void didLogoff ()
    {
        for (Observer observer : _observers) {
            try {
                observer.didLogoff();
            } catch (Exception e) {
                CShell.log("Observer choked in didLogon [observer=" + observer + "]", e);
            }
        }
    }

    protected static List<Observer> _observers = new ArrayList<Observer>();
}
