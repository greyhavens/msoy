package {
        import flash.display.Sprite;
        import flash.events.Event;
        import flash.utils.*;
        import flash.text.*;

        [SWF(width=640,height=480)]
        public class HappyRainbowFun extends Sprite
        {
            public static const DEBUG :Boolean = false;

            public var currentFrame :int;

            public function HappyRainbowFun ()
            {
                _view = DEBUG ? new View(40, 30) : new View(200, 150);
                this.scaleX = this.scaleY = 3.2;
                this.addChild(_view);

                var hue :Number = 0;
                for (var i :int = 0; i < 8; i ++) {
                    var light :Light = new Light();
                    do {
                        light.position = new Vector(2*Math.random()-1, 0.2, 2*Math.random()-1);
                    } while (light.position.dot(light.position) > 1);
                    light.color = new Color();
                    hue = (hue + Math.random() * 100) % 360;
                    light.color.setHSV(hue, .4 + .4*Math.random(), .6 + .3*Math.random());
                    light.color.multiply(0.1);
                    _view.addLight(light);
                    _view.addSurface(light);
                }

                _dish = new Dish();
                if (DEBUG) {
                    _dish.x = 200;
                    this.addChild(_dish);
                }
                _view.addSurface(_dish);

                currentFrame = 0;
            
                renderFrame();
            }

            public function renderFrame () :void
            {
                _dish.tick();
                _dish.tick();
                _view.startRender();
                renderLine();
            }

            public function renderLine () :void
            {
				var time :uint = getTimer();
				var done :Boolean = _view.renderNextLine();
                if (done) {
                    if (DEBUG && currentFrame == 2) {
                        return;
                    }
                    currentFrame ++;
                }
                time = getTimer() - time;
                setTimeout(done ? renderFrame : renderLine, time);
            }

            protected var _red :Light;
            protected var _cyan :Light;

            protected var _view :View;
            protected var _dish :Dish;
        }
}
