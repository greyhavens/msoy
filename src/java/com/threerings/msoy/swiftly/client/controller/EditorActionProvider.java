//
// $Id$

package com.threerings.msoy.swiftly.client.controller;

import javax.swing.Action;

/**
 * Interface to various actions needed by the Swiftly editor views.
 */
public interface EditorActionProvider
{
    public Action getBuildAction ();
    public Action getBuildExportAction ();
    public Action getShowConsoleAction ();
    public Action getCloseCurrentTabAction ();
    public Action getAddFileAction ();
    public Action getUploadFileAction ();
    public Action getDeleteFileAction ();
    public Action getRenameFileAction ();
}
