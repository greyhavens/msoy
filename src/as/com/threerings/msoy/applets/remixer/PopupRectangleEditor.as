//
// $Id$

package com.threerings.msoy.applets.remixer {

import flash.geom.Rectangle;

import mx.controls.TextInput;

import mx.containers.Grid;

import mx.events.ValidationResultEvent;

import com.threerings.flex.GridUtil;

public class PopupRectangleEditor extends PopupPointEditor
{
    override protected function configureUI (ctx :RemixContext, entry :Object, grid :Grid) :void
    {
        super.configureUI(ctx, entry, grid);

        _w = new TextInput();
        _h = new TextInput();

        GridUtil.addRow(grid, ctx.REMIX.get("l.width"), _w);
        GridUtil.addRow(grid, ctx.REMIX.get("l.height"), _h);

        configureValidator(_w, checkWidthValid, 0);
        configureValidator(_h, checkHeightValid, 0);

        var r :Rectangle = entry.value as Rectangle;
        if (r != null) {
            _x.text = String(r.x);
            _y.text = String(r.y);
            _w.text = String(r.width);
            _h.text = String(r.height);
        }

        kick(_x, _y, _w, _h);
    }

    override protected function getNewValue () :Object
    {
        return new Rectangle(Number(_x.text), Number(_y.text), Number(_w.text), Number(_h.text));
    }

    protected function checkWidthValid (event :ValidationResultEvent) :void
    {
        _widthValid = (event.type == ValidationResultEvent.VALID);
        checkValid();
    }

    protected function checkHeightValid (event :ValidationResultEvent) :void
    {
        _heightValid = (event.type == ValidationResultEvent.VALID);
        checkValid();
    }

    override protected function checkValid () :void
    {
        _okBtn.enabled = _xValid && _yValid && _widthValid && _heightValid;
    }

    protected var _w :TextInput;
    protected var _h :TextInput;

    protected var _widthValid :Boolean;
    protected var _heightValid :Boolean;
}
}

