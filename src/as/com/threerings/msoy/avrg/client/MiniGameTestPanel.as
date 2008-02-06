//
// $Id$

package com.threerings.msoy.avrg.client {

import mx.containers.Grid;

import mx.controls.HSlider;

import com.threerings.flex.CommandButton;
import com.threerings.flex.GridUtil;

/**
 * A panel containing controls to fake minigame performance, for use in testing.
 */
public class MiniGameTestPanel extends Grid
{
    public function MiniGameTestPanel (container :MiniGameContainer)
    {
        _container = container;
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        _score = new HSlider();
        _score.minimum = 0;
        _score.maximum = 1;
        _score.value = .5;

        _style = new HSlider();
        _style.minimum = 0;
        _style.maximum = 1;
        _style.value = 0;

        var btn :CommandButton = new CommandButton("Perform", perform);

        GridUtil.addRow(this, "score", _score, [2, 1], btn, [1, 2]);
        GridUtil.addRow(this, "style", _style, [2, 1]);
    }

    protected function perform () :void
    {
        _container.recordPerformance(_score.value, _style.value);
    }

    protected var _container :MiniGameContainer;

    protected var _score :HSlider;
    protected var _style :HSlider;
}
}
