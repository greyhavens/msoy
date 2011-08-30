//
// $Id$

package com.threerings.msoy.tutorial.client {

import flash.geom.Rectangle;

import mx.collections.ICollectionView;
import mx.collections.IViewCursor;
import mx.controls.Menu;
import mx.core.IFlexDisplayObject;

import com.threerings.msoy.client.MsoyContext;

/**
 * Popup helper that highlights a menu item using the standard tutorial effect.
 */
public class MenuHighlightHelper extends UIHighlightHelper
{
    /**
     * Creats a new menu highlight helper.
     * @param ctx the context of the msoy application
     * @param popper either a UI component or a function that returns the UI component that will
     *        popup the menu. In the case of the former, the highlight always surrounds the fixed
     *        component. In the case of the latter, the highlight will adapt to surround whatever
     *        component is returned at the beginning of the frame and disappearing when null is
     *        returned.
     *          <listing verion="3.0">
     *              function comp () :UIComponent;
     *          </listing>
     * @param command the command within the menu to highlight. The menu items of the current menu
     *        (in <code>MsoyControllger.getCurrentMenu()</code> are scanned for a "command"
     *        property that matches.
     */
    public function MenuHighlightHelper (ctx :MsoyContext, popper :Object, command :String)
    {
        super(ctx.getTopPanel(), popper);
        _ctx = ctx;
        _command = command;
    }

    override protected function adjustHighlightArea (
        comp :IFlexDisplayObject, area :Rectangle) :void
    {
        super.adjustHighlightArea(comp, area);
        if (_mine) {
            area.inflate(-10, 5);
        }
    }

    override protected function getComp () :IFlexDisplayObject
    {
        var menu :Menu = _ctx.getMsoyController().getCurrentMenu();
        if (menu != null) {
            var cursor :IViewCursor = (menu.dataProvider as ICollectionView).createCursor();
            for (; !cursor.afterLast; cursor.moveNext()) {
                if ("command" in cursor.current && cursor.current.command == _command) {
                    break;
                }
            }
            if (!cursor.afterLast && menu.isItemVisible(cursor.current)) {
                _mine = true;
                return menu.itemToItemRenderer(cursor.current);
            }
        }
        _mine = false;
        return super.getComp();
    }

    protected var _ctx :MsoyContext;
    protected var _command :String;
    protected var _mine :Boolean;
}
}
