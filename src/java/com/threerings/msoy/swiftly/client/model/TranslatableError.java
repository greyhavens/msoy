package com.threerings.msoy.swiftly.client.model;

import com.threerings.msoy.swiftly.client.TranslationMessage;

/**
 * Holds TranslatorMessages. Useful for enums.
 */
public interface TranslatableError
{
    /**
     * Returns the TranslatorMessage wrapped by the implementor.
     */
    public TranslationMessage getMessage ();
}