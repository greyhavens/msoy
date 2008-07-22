//
// $Id$

package com.threerings.msoy.swiftly.client.controller;

import javax.swing.Action;

/**
 * Interface to various actions needed by the Swiftly editor views.
 */
public interface EditorActionProvider
{
    Action getBuildAction ();
    Action getBuildExportAction ();
    Action getShowConsoleAction ();
    Action getCloseCurrentTabAction ();
    Action getAddFileAction ();
    Action getUploadFileAction ();
    Action getDeleteFileAction ();
    Action getRenameFileAction ();
}
