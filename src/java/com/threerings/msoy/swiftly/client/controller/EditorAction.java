//
// $Id$

package com.threerings.msoy.swiftly.client.controller;

import javax.swing.AbstractAction;

import com.threerings.msoy.swiftly.client.Translator;

/**
 * Provide a wrapper around AbstractAction that knows about ActionResource.
 */
public abstract class EditorAction extends AbstractAction
{
    public EditorAction (ActionResource resource, Translator translator)
    {
        super(translator.xlate(resource.name), resource.icon);
        putValue(AbstractAction.SHORT_DESCRIPTION, translator.xlate(resource.description));
    }
}
