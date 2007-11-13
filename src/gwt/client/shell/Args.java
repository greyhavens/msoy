//
// $Id$

package client.shell;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to parse the arguments supplied to the page.
 */
public class Args
{
    /**
     * Composes multiple arguments into a single string argument that can be properly handled by
     * the Args class.
     */
    public static String compose (String[] args)
    {
        StringBuffer builder = new StringBuffer();
        for (int ii = 0; ii < args.length; ii++) {
            if (ii > 0) {
                builder.append(ARG_SEP);
            }
            builder.append(args[ii]);
        }
        return builder.toString();
    }

    /**
     * Composes multiple arguments into a single string argument that can be properly handled by
     * the Args class.
     */
    public static String compose (List args)
    {
        StringBuffer builder = new StringBuffer();
        for (int ii = 0; ii < args.size(); ii++) {
            if (ii > 0) {
                builder.append(ARG_SEP);
            }
            builder.append(args.get(ii));
        }
        return builder.toString();
    }

    /**
     * Convenience function.
     */
    public static String compose (String action, int arg)
    {
        return compose(new String[] { action, ""+arg });
    }

    /**
     * Convenience function.
     */
    public static String compose (String action, String arg)
    {
        return compose(new String[] { action, arg });
    }

    /**
     * Convenience function.
     */
    public static String compose (String action, String arg1, String arg2)
    {
        return compose(new String[] { action, arg1, arg2 });
    }

    /**
     * Returns the number of arguments available.
     */
    public int getArgCount ()
    {
        return _args.size();
    }

    /**
     * Parses and returns the specified argument as an integer. Returns the default if the argument
     * is not a valid integer or is out of bounds.
     */
    public int get (int index, int defval)
    {
        if (index >= _args.size()) {
            return defval;
        }
        try {
            return Integer.parseInt((String)_args.get(index));
        } catch (Exception e) {
            return defval;
        }
    }

    /**
     * Returns the specified argument as a string. Returns the default if the argument is out of
     * bounds.
     */
    public String get (int index, String defval)
    {
        if (index >= _args.size()) {
            return defval;
        }
        return (String)_args.get(index);
    }

    /**
     * Splices off the arguments from the specified index onward and returns them as an array for
     * recomposition with {@link #compose}.
     */
    public String[] splice (int fromIndex)
    {
        String[] args = new String[_args.size()-fromIndex];
        for (int ii = 0; ii < args.length; ii++) {
            args[ii] = (String)_args.get(ii + fromIndex);
        }
        return args;
    }

    /**
     * Called by {@link Application} when configuring our arguments.
     */
    public void setToken (String token)
    {
        do {
            int didx = token.indexOf(ARG_SEP);
            if (didx == -1) {
                _args.add(token);
                token = null;
            } else {
                _args.add(token.substring(0, didx));
                token = token.substring(didx+1);
            }
        } while (token != null && token.length() > 0);
    }

    protected List _args = new ArrayList();

    protected static final String ARG_SEP = "_";
}
