package com.threerings.msoy.world.chat.client {

import flash.display.BitmapData;

/**
 * Extends the basic chat bubble with a cheesy bleeding-text effect.
 */
public class BloodChatBubble extends ChatBubble
{
    override protected function bitmapCreated (src :BitmapData) :void
    {
        // For every vertical run of N black pixels, fill nonblack pixels
        // below it with a run of red pixels up to length N.
        var blackCount :int;
        var redCount :int;
        for (var xx :int = 0; xx < src.width; xx++) {
            blackCount = 0;
            redCount = 0;
            for (var yy :int = 0; yy < src.height; yy++) {
                if (src.getPixel(xx, yy) == 0) {
                    blackCount++;

                } else {
                    if (blackCount > 0) {
                        redCount = int(blackCount * Math.random());
                        blackCount = 0;
                    }
                    if (redCount-- > 0) {
                        src.setPixel(xx, yy, 0xFF0000);
                    }
                }
            }
        }
    }
}
}
