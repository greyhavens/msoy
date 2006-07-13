package com.threerings.msoy.client {

import mx.core.ScrollPolicy;

import mx.containers.Canvas;

import com.threerings.msoy.chat.client.ChatControl;

import com.threerings.msoy.data.MediaData;

/**
 * The control bar: the main menu and global UI element across all scenes.
 */
public class ControlBar extends Canvas
{
    public function ControlBar (ctx :MsoyContext)
    {
        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;

        width = 800;
        height = 59;

        var chatControl :ChatControl = new ChatControl(ctx);
        chatControl.x = 10;
        chatControl.y = 10;
        addChild(chatControl);

        // TODO: any fixed graphics should just be compiled in
        setStyle("backgroundImage", MediaData.BASE_URL + "uibar.png");
    }
}
}
