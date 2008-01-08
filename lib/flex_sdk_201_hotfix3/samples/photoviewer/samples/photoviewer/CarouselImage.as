////////////////////////////////////////////////////////////////////////////////
//
// Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
// All Rights Reserved.
// The following is Sample Code and is subject to all restrictions on such code
// as contained in the End User License Agreement accompanying this product.
// If you have received this file from a source other than Adobe,
// then your use, modification, or distribution of it requires
// the prior written permission of Adobe.
//
////////////////////////////////////////////////////////////////////////////////
package samples.photoviewer
{

    import flash.display.Graphics;

    import mx.controls.Image;
    import mx.core.UIComponent;
    import mx.utils.GraphicsUtil;

    /**
     *  The color of the frame.  The default is black.
     */
    [Style(name="frameColor", type="uint", format="color", inherit="no")]

    /**
     *  The thickness of the surrounding frame.  The default is 1.
     */
    [Style(name="frameThickness", type="Number", format="Length", inherit="no")]

    /**
     * The size in pixels of a frame line drawn from the corner.
     * The default is 5.
     */
    [Style(name="frameSize", type="Number", format="Length", inherit="no")]


    public class CarouselImage extends UIComponent
    {
        private var image:Image;

        public function CarouselImage()
        {
            super();
            image = new Image();
        }

        override protected function createChildren():void
        {
            super.createChildren();
            image.setStyle("verticalAlign", "middle");
            image.setStyle("horizontalAlign", "center");
            addChild(image);
        }

        public function get source():String
        {
            return image.source as String;
        }

        public function set source(value:String):void
        {
            image.source = value;
        }

        override public function set width(value:Number):void
        {
            super.width = value;
            image.width = value;
        }

        override public function get width():Number
        {
            return super.width;
        }

        override public function set height(value:Number):void
        {
            super.height = value;
            image.height = value;
        }

        override public function get height():Number
        {
            return super.height;
        }

        override protected function updateDisplayList(unscaledWidth:Number, unscaledHeight:Number):void
        {
            super.updateDisplayList(unscaledWidth, unscaledHeight);
            var frameColor:uint = getStyle("frameColor");
            var frameThickness:Number = getStyle("frameThickness");
            var frameSize:Number = getStyle("frameSize");

            //draw the four corners
            if (frameThickness > 0)
            {
                var g:Graphics = graphics;
                g.clear();
                g.lineStyle(frameThickness, frameColor, 1);

                //upper left corner
                g.moveTo(0, frameSize);
                g.lineTo(0, 0);
                g.lineTo(frameSize, 0);

                //lower left corner
                g.moveTo(0, unscaledHeight - frameSize);
                g.lineTo(0, unscaledHeight);
                g.lineTo(frameSize, unscaledHeight);

                //upper right corner
                g.moveTo(unscaledWidth - frameSize, 0);
                g.lineTo(unscaledWidth, 0);
                g.lineTo(unscaledWidth, frameSize);

                //lower right corner
                g.moveTo(unscaledWidth, unscaledHeight - frameSize);
                g.lineTo(unscaledWidth, unscaledHeight);
                g.lineTo(unscaledWidth - frameSize, unscaledHeight);
            }
        }

    }
}