//
// $Id$

package com.threerings.msoy.swiftly.client;

import com.threerings.msoy.swiftly.client.model.TranslatableError;
import com.threerings.msoy.swiftly.data.SwiftlyCodes;
import com.threerings.util.MessageBundle;
import com.threerings.util.MessageManager;

/**
 * An implementation of Translator which uses MessageBundles for translation.
 */
public class MessageBundleTranslator
    implements Translator
{
    public MessageBundleTranslator (MessageManager msgmgr)
    {
        _msgmgr = msgmgr;
    }

    // from Translator
    public String xlate (String key)
    {
        MessageBundle mb = _msgmgr.getBundle(SwiftlyCodes.SWIFTLY_MSGS);
        return (mb == null) ? key : mb.xlate(key);
    }

    // from Translator
    public String xlate (String key, Object... args)
    {
        MessageBundle mb = _msgmgr.getBundle(SwiftlyCodes.SWIFTLY_MSGS);
        return (mb == null) ? key : mb.get(key, args);
    }

    // from Translator
    public String xlate (String key, String... strings)
    {
        MessageBundle mb = _msgmgr.getBundle(SwiftlyCodes.SWIFTLY_MSGS);
        return (mb == null) ? key : mb.get(key, strings);
    }

    // from Translator
    public String xlate (TranslationMessage msg)
    {
        return xlate(msg.getMessageKey(), msg.getMessageArgs());
    }

    // from Translator
    public String xlate (TranslatableError error)
    {
        return this.xlate(error.getMessage());
    }

    private final MessageManager _msgmgr;

}
