//
// $Id$

package com.threerings.msoy.web.gwt;

import java.util.ArrayList;

/**
 * Quick and dirty Markup language builder to avoid eyestrain from reading angle brackets and \"
 * and reduce the chance of missing or misspelled closing tags.
 */
public class MarkupBuilder
{
    /**
     * Creates a new markup builder using double quotes for attribute values.
     */
    public MarkupBuilder ()
    {
        this('"');
    }

    /**
     * Creates a new markup builder using the given quote character for attribute values.
     */
    public MarkupBuilder (char quote)
    {
        _quote = quote;
    }

    /**
     * Open up a tag with the given name and attributes.
     */
    public MarkupBuilder open (String name, String ... kvPairs)
    {
        startContent();
        _tagcontents.add(0);
        _tags.add(name);
        _buff.append("<").append(name);
        for (int ii = 0; ii < kvPairs.length; ii += 2) {
            _buff.append(" ").append(kvPairs[ii]);
            _buff.append("=").append(_quote).append(kvPairs[ii+1]).append(_quote);
        }
        return this;
    }

    /**
     * Append the given text inside the currently open tag.
     */
    public MarkupBuilder append (String text)
    {
        startContent();
        _buff.append(text);
        return this;
    }

    /**
     * Append an empty string to the currently open tag. This is useful to force a script tag to
     * use the explicit close form instead of &lt;script/&gt;.
     */
    public MarkupBuilder append ()
    {
        return append("");
    }

    /**
     * Close the most recent tag.
     */
    public MarkupBuilder close ()
    {
        int size = _tagcontents.size();
        if (_tagcontents.get(size - 1) == 0) {
            _buff.append("/>");
        } else {
            _buff.append("</").append(_tags.get(size - 1)).append(">");
        }
        _tags.remove(size - 1);
        _tagcontents.remove(size - 1);
        return this;
    }

    /**
     * Close all tags and return the document as a string.
     */
    public String finish ()
    {
        while (_tagcontents.size() > 0) {
            close();
        }
        return toString();
    }

    /**
     * Resets the builder so we are ready to start afresh.
     */
    public MarkupBuilder reset ()
    {
        _buff.setLength(0);
        _tagcontents.clear();
        _tags.clear();
        return this;
    }

    // from Object
    public String toString ()
    {
        return _buff.toString();
    }

    protected void startContent ()
    {
        int size = _tagcontents.size();
        if (size > 0) {
            int count = _tagcontents.get(size - 1);
            if (count == 0) {
                _buff.append(">");
            }
            _tagcontents.set(size - 1, count + 1);
        }
    }

    protected ArrayList<Integer> _tagcontents = new ArrayList<Integer>();
    protected ArrayList<String> _tags = new ArrayList<String>();
    protected StringBuilder _buff = new StringBuilder();
    protected char _quote;
}
