//
// $Id$

package client.edgames;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.edgame.gwt.EditGameService;
import com.threerings.msoy.edgame.gwt.EditGameServiceAsync;

import client.edutil.EditorTable;
import client.shell.DynamicLookup;

/**
 * A base class for game editor panels. Just adds some handy statics.
 */
public class GameEditorTable extends EditorTable
{
    protected static final EditGamesMessages _msgs = GWT.create(EditGamesMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final EditGameServiceAsync _gamesvc = GWT.create(EditGameService.class);
}
