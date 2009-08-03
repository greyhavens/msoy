//
// $Id$

package com.threerings.msoy.web.gwt;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Used to parse the arguments supplied to the page.
 */
public class Args
    implements Iterable<String>
{
    /**
     * Composes multiple arguments into a single string argument that can be properly handled by
     * the Args class.
     */
    public static Args compose (Object... args)
    {
        Args aobj = new Args();
        for (Object arg : args) {
            if (arg instanceof Iterable<?>) {
                for (Object larg : (Iterable<?>)arg) {
                    aobj.add(larg);
                }
            } else {
                aobj.add(arg);
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
        int didx = historyToken.indexOf("-");
        return (didx >= 0) ? Args.fromToken(historyToken.substring(didx+1)) : new Args();
    }

    /**
     * Parses args from the supplied token. This token must not contain the page, just the args.
     */
    public static Args fromToken (String token)
    {
        Args args = new Args();
        do {
            int didx = token.indexOf(ARG_SEP);
            if (didx == -1) {
                args.add(unescape(token));
                token = null;
            } else {
                args.add(unescape(token.substring(0, didx)));
                token = token.substring(didx+1);
            }
        } while (token != null && token.length() > 0);
        return args;
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
     * Parses and returns the specified argument as a byte. Returns the default if the argument is
     * not a valid byte or is out of bounds.
     */
    public byte get (int index, byte defval)
    {
        if (index >= _args.size()) {
            return defval;
        }
        try {
            return Byte.parseByte(_args.get(index));
        } catch (Exception e) {
            return defval;
        }
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
     * Looks for an argument of the given name. If it exists and is not the last argument, returns
     * the value of the subsequent argument and removes both. Otherwise returns null and does not
     * change the arguments.
     */
    public String extractParameter (String name)
    {
        for (int ii = 0, ll = _args.size(); ii < ll; ++ii) {
            if (_args.get(ii).equals(name) && ii + 1 < ll) {
                _args.remove(ii);
                return _args.remove(ii);
            }
        }
        return null;
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
     * Turns this instance into a token that can be used in a URL.
     */
    public String toToken ()
    {
        StringBuilder builder = new StringBuilder();
        int idx = 0;
        for (String arg : _args) {
            if (idx++ > 0) { // can't use length() here as first arg may have been ""
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
        StringBuilder buf = new StringBuilder("/").append(page.getPath());
        for (String arg : _args) {
            buf.append("/").append(arg);
        }
        return buf.toString();
    }

    // from interface Iterable<String>
    public Iterator<String> iterator ()
    {
        return _args.iterator();
    }

    @Override // from Object
    public String toString ()
    {
        StringBuilder builder = new StringBuilder("[");
        int idx = 0;
        for (String arg : _args) {
            if (idx++ > 0) {
                builder.append(", ");
            }
            builder.append("'").append(arg).append("'");
        }
        return builder.append("]").toString();
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
