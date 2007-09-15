//
// $Id$

package com.threerings.msoy.swiftly.client;

import com.threerings.msoy.swiftly.client.model.TranslatableError;

/**
 * Provides mechanisms to translate i18n surrogate keys.
 */
public interface Translator
{
    /**
     * Translate the given i18n surrogate key.
     */
    public String xlate (String key);

    /**
     * Translate the given i18n surrogate key substituting in the supplied list of Objects.
     */
    public String xlate (String key, Object... args);

    /**
     * Translate the given i18n surrogate key substituting in the supplied list of Strings.
     */
    public String xlate (String key, String... strings);

    /**
     * Translate the given TranslatorMessage.
     */
    public String xlate (TranslationMessage msg);

    /**
     * Translate the given TranslatableError.
     */
    public String xlate (TranslatableError error);
}
