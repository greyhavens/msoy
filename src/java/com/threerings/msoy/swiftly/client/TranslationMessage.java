//
// $Id$

package com.threerings.msoy.swiftly.client;

/**
 * Wrap surrogate keys in an interface for Translators.
 */
public interface TranslationMessage
{
    /**
     * Return the i18n surrogate key for this TranslatorMessage.
     */
    String getMessageKey ();

    /**
     * Return the optional translation arguments this TranslatorMessage.
     */
    Object[] getMessageArgs ();
}
