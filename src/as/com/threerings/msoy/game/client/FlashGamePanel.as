package com.threerings.msoy.game.client {

import com.threerings.crowd.util.CrowdContext;

import com.threerings.ezgame.client.EZGamePanel;

import com.threerings.msoy.chat.client.ChatTextArea;

import com.threerings.msoy.world.client.MsoySprite; // TODO: make new base class

import com.threerings.msoy.game.data.FlashGameConfig;

public class FlashGamePanel extends EZGamePanel
{
    public function FlashGamePanel (ctx :CrowdContext, ctrl :FlashGameController)
    {
        super(ctx, ctrl);

        // TODO: all this may change
        var cfg :FlashGameConfig = (ctrl.getPlaceConfig() as FlashGameConfig);
        _gameView = new MsoySprite(cfg.game);
        addChild(_gameView);

        addChild(new ChatTextArea(ctx));
    }

    /** The actual flash game. */
    protected var _gameView :MsoySprite;
}
