package {
    import flash.display.Graphics;
    import mx.graphics.RectangularDropShadow;
    import mx.skins.Border;
    import flash.display.GradientType;
    public class haloButtonTrans extends Border {

        private var dropShadow:RectangularDropShadow;

        public function haloButtonTrans() {
        }

        override protected function updateDisplayList(unscaledWidth:Number, unscaledHeight:Number):void {
            super.updateDisplayList(unscaledWidth, unscaledHeight);
            var cornerRadius:Number = getStyle("cornerRadius");

            drawRoundRect(0, 0, unscaledWidth, unscaledHeight, {tl: 5, tr: 5, bl: 5, br: 5}, 0, 0);
            drawRoundRect(
						0, 0, unscaledWidth, unscaledHeight, 5,
						[ "0xb7babc", "0xb7babc" ], 1,
						verticalGradientMatrix(0, 0, unscaledWidth, unscaledHeight),
						GradientType.LINEAR, null, 
						{ x: 1, y: 1, w: unscaledWidth - 2, h: unscaledHeight - 2, r: cornerRadius - 1 }) 
        }
    }
}

