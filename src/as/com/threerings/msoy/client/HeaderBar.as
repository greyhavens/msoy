package com.threerings.msoy.client {

import mx.core.ScrollPolicy;

import mx.containers.HBox;

public class HeaderBar extends HBox
{
    public static const HEIGHT :int = 20;

    public function HeaderBar (ctx :WorldContext) 
    {
        _ctx = ctx;
        styleName = "headerBar";

        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;

        percentWidth = 100;
        height = HEIGHT;
    }

    protected var _ctx :WorldContext;
}
}
