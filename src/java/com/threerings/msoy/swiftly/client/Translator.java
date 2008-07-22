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
    String xlate (String key);

    /**
     * Translate the given i18n surrogate key substituting in the supplied list of Objects.
     */
    String xlate (String key, Object... args);

    /**
     * Translate the given i18n surrogate key substituting in the supplied list of Strings.
     */
    String xlate (String key, String... strings);

    /**
     * Translate the given TranslatorMessage.
     */
    String xlate (TranslationMessage msg);

    /**
     * Translate the given TranslatableError.
     */
    String xlate (TranslatableError error);
}
