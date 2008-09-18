//
// $Id$

package com.threerings.msoy.badge.ui {

import mx.collections.ArrayCollection;

import mx.controls.List;

import mx.core.ClassFactory;
import mx.core.ScrollPolicy;

import com.threerings.msoy.ui.FlyingPanel;

import com.threerings.msoy.client.MsoyContext;

/**
 * A flying panel that display an array of EarnedBadges in a giant vertical List.  This is only
 * used on dev deployments for testing.
 */
public class BadgeListPanel extends FlyingPanel
{
    public function BadgeListPanel (ctx :MsoyContext, badges :Array) :void
    {
        super(ctx);
        showCloseButton = true;
        _badges = badges;
        _mctx = ctx;

        width = 300;
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var list :List = new List();
        list.verticalScrollPolicy = ScrollPolicy.ON;
        list.selectable = false;
        list.percentWidth = 100;
        list.percentHeight = 100;
        list.variableRowHeight = true;
        var factory :ClassFactory = new ClassFactory(BadgeRenderer);
        factory.properties =  { mctx: _mctx };
        list.itemRenderer = factory;
        list.dataProvider = new ArrayCollection(_badges);
        addChild(list);
    }

    protected var _mctx :MsoyContext;
    protected var _badges :Array;
}
}

import mx.containers.HBox;
import mx.containers.VBox;

import mx.controls.Label;

import mx.core.ScrollPolicy;

import com.threerings.flash.MediaContainer;

import com.threerings.flex.FlexWrapper;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.badge.data.all.EarnedBadge;

class BadgeRenderer extends HBox
{
    // Initialized by the ClassFactory
    public var mctx :MsoyContext;

    public function BadgeRenderer ()
    {
        super();

        height = 60;
        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;
        setStyle("backgroundAlpha", 0);
        // the horizontalGap should be 8...
    }

    override public function set data (value :Object) :void
    {
        super.data = value;

        if (processedDescriptors) {
            configureUI();
        }
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        configureUI();
    }

    /**
     * Update the UI elements with the data we're displaying.
     */
    protected function configureUI () :void
    {
        removeAllChildren();

        if (this.data != null && this.data is EarnedBadge) {
            var textContainer :VBox = new VBox();
            var badge :EarnedBadge = this.data as EarnedBadge;
            var name :Label = new Label();
            name.text = mctx.xlate(MsoyCodes.PASSPORT_MSGS, badge.nameProp);
            textContainer.addChild(name);
            var level :Label = new Label();
            level.text = "Level: " + badge.level;
            textContainer.addChild(level);
            addChild(textContainer);
            // The flex control Image chews through the processor like crazy here, so we use
            // our own MediaContainer, and everything is nice and speedy.
            addChild(new FlexWrapper(new MediaContainer(badge.imageUrl)));
        }
    }
}
