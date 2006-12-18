package {

import flash.display.*;
import flash.text.*;
import flash.events.*;
import flash.ui.*;
import flash.utils.*;
import flash.external.ExternalInterface;

import mx.core.SoundAsset;

import PopularPlace;

[SWF(width="640", height="480")]
public class HotSpots extends Sprite
{
    public function HotSpots ()
    {
        _places = PopularPlace.fromLoaderInfo(this.root.loaderInfo);

        var maxPop :int = 0;
        for (var i :int = 0; i < _places.length; i ++) {
            maxPop = Math.max(maxPop, _places[i].population);
        }
        for (i = 0; i < _places.length; i ++) {
            var labelText :TextField = new TextField();
            labelText.text = _places[i].name + " (" + _places[i].population + ")";
            labelText.antiAliasType = AntiAliasType.ADVANCED;
            labelText.autoSize = TextFieldAutoSize.CENTER;
            labelText.wordWrap = false;

            var label :Sprite = new Sprite();
            label.addChild(labelText);
            with (label.graphics) {
                clear();
                beginFill(0xFFFFFF);
                drawRoundRect(0, 0, labelText.width + 20, labelText.height, 10, 10);
                endFill();
                lineStyle(2, 0x000000);
                drawRoundRect(0, 0, labelText.width + 20, labelText.height, 10, 10);
            }
            label.x = 10 + Math.random()*(630 - label.width);
            label.y = 10 + Math.random()*(470 - label.height);

            var scale :Number = 0.7 + 0.8*(_places[i].population / maxPop);
            label.scaleX = scale;
            label.scaleY = scale;
            this.addChild(label);
        }
    }

        var ureq :URLRequest = new URLRequest(url);
        if (preferSameWindowOrTab) {
            try {
                flash.net.navigateToURL(ureq, "_self");
                return true;
            } catch (err :SecurityError) {
                // ignore; fall back to using a blank window, below...
            }
        }

    protected var _places :Array;
}
}
