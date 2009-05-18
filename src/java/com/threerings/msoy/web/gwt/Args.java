//
// $Id$

package com.threerings.msoy.web.gwt;

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
    public static Args compose (Object... args)
    {
        Args aobj = new Args();
        for (Object arg : args) {
            if (arg instanceof Args) {
                aobj._args.addAll(((Args)arg)._args);
            } else {
                aobj._args.add(String.valueOf(arg));
            }
        }
        return aobj;
    }

    /**
     * Extracts, parses and returns the args from the supplied history token. The history token
     * contains the page and the args.
     */
    public static Args fromHistory (String historyToken)
    {
        Args args = new Args();
        int didx = historyToken.indexOf("-");
        if (didx >= 0) {
            args.setToken(historyToken.substring(didx+1));
        }
        return args;
    }

    /**
     * Parses args from the supplied token. This token must not contain the page, just the args.
     */
    public static Args fromToken (String token)
    {
        Args args = new Args();
        args.setToken(token);
        return args;
    }

    /**
     * Creates a new blank args instance.
     */
    public Args ()
    {
        _args = new ArrayList<String>();
    }

    /**
     * Returns the number of arguments available.
     */
    public int getArgCount ()
    {
        return _args.size();
    }

    /**
     * Adds the supplied argument to the end of this args list.
     */
    public void add (Object arg)
    {
        _args.add(String.valueOf(arg));
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
     * Returns true if the specified argument is a prefixed numeric id, like <code>s2343</code>,
     * false otherwise.
     */
    public boolean isPrefixedId (int index, String prefix)
    {
        String rawarg = get(index, "");
        if (rawarg.startsWith(prefix)) {
            try {
                Integer.parseInt(rawarg.substring(prefix.length()));
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    /**
     * Returns the id prefixed by the specified string, i.e. <code>s2343</code>. If the argument is
     * invalid, non-existent or otherwise bogus, the supplied default value is returned.
     */
    public int getPrefixedId (int index, String prefix, int defId)
    {
        String rawarg = get(index, "");
        if (rawarg.startsWith(prefix)) {
            try {
                return Integer.parseInt(rawarg.substring(prefix.length()));
            } catch (Exception e) {
                return defId;
            }
        }
        return defId;
    }

    /**
     * Recomposes the arguments from the specified index onward with {@link #compose}.
     */
    public Args recompose (int fromIndex)
    {
        return recomposeWithout(0, fromIndex);
    }

    /**
     * Removes arguments between the start index, up to but not including the end index, and
     * recomposes the remaining arguments using {@link #compose}.
     *
     * <p>For example, suppose your Args holds elements { "a", "b", "c", "d", "e" }. Calling
     * <code>remove(1, 2)</code> will return an array of strings containing elements { "a", "d",
     * "e" }.
     */
    public Args recomposeWithout (int start, int span)
    {
        List<String> args = new ArrayList<String>();
        for (int ii = 0, ll = Math.min(_args.size(), start); ii < ll; ii++) {
            args.add(_args.get(ii));
        }
        for (int ii = start+span, ll = _args.size(); ii < ll; ii++) {
            args.add(_args.get(ii));
        }
        return compose(args);
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
     * Turns this instance into a token that can be used in a URL.
     */
    public String toToken ()
    {
        StringBuilder builder = new StringBuilder();
        for (String arg : _args) {
            if (builder.length() > 0) {
                builder.append(ARG_SEP);
            }
            builder.append(escape(arg));
        }
        return builder.toString();
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
        return "" + _args.size() + ": " + _args.toString();
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
