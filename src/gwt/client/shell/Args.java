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
            builder.append(escape(args[ii]));
        }
        return builder.toString();
    }

    /**
     * Composes multiple arguments into a single string argument that can be properly handled by
     * the Args class.
     */
    public static String compose (List<String> args)
    {
        StringBuffer builder = new StringBuffer();
        for (int ii = 0; ii < args.size(); ii++) {
            if (ii > 0) {
                builder.append(ARG_SEP);
            }
            builder.append(escape(args.get(ii).toString()));
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
            return Integer.parseInt(_args.get(index));
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
        return _args.get(index);
    }

    /**
     * If this args list contains the specified string, returns its index.
     * Otherwise returns the value -1.
     */
    public int indexOf (String value)
    {
        for (int ii = 0, count = _args.size(); ii < count; ii++) {
            String element = _args.get(ii);
            if ((element == null && value == null) || element.equals(value)) {
                return ii;
            }
        }
        return -1;
    }

    /**
     * Splices off the arguments from the specified index onward and returns them as an array for
     * recomposition with {@link #compose}.
     */
    public String[] splice (int fromIndex)
    {
        String[] args = new String[_args.size()-fromIndex];
        for (int ii = 0; ii < args.length; ii++) {
            args[ii] = _args.get(ii + fromIndex);
        }
        return args;
    }

    /**
     * Removes arguments between the start index, up to but not including the end index,
     * and returns the result as a new array for recomposition with {@link #compose}.
     *
     * <p>For example, suppose your Args holds elements { "a", "b", "c", "d", "e" }.
     * Calling <code>remove(1, 3)</code> will return an array of strings
     * containing elements { "a", "d", "e" }.
     */
    public String[] remove (int start, int end)
    {
        int total = _args.size();
        String[] args = new String[total - (end - start)];

        int from = 0;
        int to = 0;
        while (from < total) {
            if (from < start || from >= end) {
                args[to] = _args.get(from);
                to++;
            }
            from++;
        }
        return args;
    }

    /**
     * Called by the application when configuring our arguments.
     */
    public void setToken (String token)
    {
        do {
            int didx = token.indexOf(ARG_SEP);
            if (didx == -1) {
                _args.add(unescape(token));
                token = null;
            } else {
                _args.add(unescape(token.substring(0, didx)));
                token = token.substring(didx+1);
            }
        } while (token != null && token.length() > 0);
    }

    /**
     * Returns our args in path-like format: a1/a2/a3/...
     */
    public String toPath (Pages page)
    {
        StringBuffer buf = new StringBuffer("/").append(page.getPath());
        for (String arg : _args) {
            buf.append("/").append(arg);
        }
        return buf.toString();
    }

    @Override // from Object
    public String toString ()
    {
        return _args.toString();
    }

    // Since we're using _ for our own purposes, encode it as %- and thus also % as %%
    protected static String escape (String str)
    {
        return str.replaceAll(ARG_ESC, ARG_ESC_ESC).replaceAll(ARG_SEP, ARG_ESC_SEP);
    }

    protected static String unescape (String str)
    {
        return str.replaceAll(ARG_ESC_SEP, ARG_SEP).replaceAll(ARG_ESC_ESC, ARG_ESC);
    }

    protected List<String> _args = new ArrayList<String>();

    protected static final String ARG_SEP = "_";
    protected static final String ARG_ESC = "%";
    protected static final String ARG_ESC_ESC = "%%";
    protected static final String ARG_ESC_SEP = "%-"; // Note: dash, not underscore!
}
