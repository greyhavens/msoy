package com.threerings.msoy.swiftly.client.model;

import com.threerings.msoy.swiftly.client.TranslationMessage;

/**
 * An implementation of TranslatorMessage that works with MessageBundles.
 */
public class BundleTranslationMessage
    implements TranslationMessage
{
    public BundleTranslationMessage (String key)
    {
            _key = key;
            _args = new Object[0];
    }

    public BundleTranslationMessage (String key, Object... args)
    {
        _key = key;
        _args = args;
    }

    // from TranslatorMessage
    public String getMessageKey ()
    {
        return _key;
    }

    // from TranslatorMessage
    public Object[] getMessageArgs ()
    {
        return _args;
    }

    private final String _key;
    private final Object[] _args;
}
