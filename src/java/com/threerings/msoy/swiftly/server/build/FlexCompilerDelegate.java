//
// $Id$

package com.threerings.msoy.swiftly.server.build;

import java.lang.reflect.Method;

import java.util.Timer;
import java.util.TimerTask;

/**
 * A wrapper around the Flex compiler used to ensure the process terminates.
 */
public class FlexCompilerDelegate
{
    public static class CompilerKiller extends TimerTask
    {
        public CompilerKiller (long delay)
        {
            _delay = delay;
        }

        @Override
        public void run ()
        {
            System.err.println(
                "CompilerKiller killed the JVM after " + _delay + " milliseconds.");
            System.exit(1);
        }

        protected final long _delay;
    }

    /**
     * Runs the Flex compiler with a timer that kills the process after a delay.
     * NOTE: Argument parsing implemented without an external library in order to not pollute
     * the compiler classpath.
     */
    public static void main (String[] args)
    {
        // attempt to see if the first argument is the kill delay parameter
        if (args.length == 0 || !args[0].matches("--kill_delay=[0-9]+")) {
            System.err.println("--kill_delay=VALUE must be the first argument");
            System.exit(1);
        }

        long delay = 0;
        try {
            delay = Long.parseLong(args[0].split("=")[1]);
        } catch (NumberFormatException nfe) {
            System.err.println("Badly formatted --kill_delay argument. [" + args[0] + "].");
            System.exit(1);
        }
        // shift off the first argument
        String[] newArgs = new String[args.length - 1];
        for (int ii = 1; ii < args.length; ii++) {
            newArgs[ii - 1] = args[ii];
        }
        args = newArgs;

        Timer killer = new Timer();
        killer.schedule(new CompilerKiller(delay), delay);

        try {
            Class<?> compiler = Class.forName("flex2.tools.Compiler");
            Method mainMethod = compiler.getMethod(
                "main", new Class[] { STRING_ARRAY_PROTOTYPE.getClass() });
            mainMethod.invoke(null, (Object)args);
        } catch (Exception e) {
            System.err.println("Failed to load and invoke compiler " + e);
            System.exit(1);
        }
    }

    protected static final String[] STRING_ARRAY_PROTOTYPE = new String[0];
}
