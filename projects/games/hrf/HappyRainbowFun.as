package {
	import flash.display.Sprite;
	import flash.events.Event;
	import flash.utils.*;
	import flash.text.*;
	
//	[SWF(width=200,height=150)]
	public class HappyRainbowFun extends Sprite
	{
		public static const DEBUG :Boolean = false;
		
                public function HappyRainbowFun ()
		{
            _dish = new Dish();
            _dish.x = 200;
//            this.addChild(_dish);

			_view = DEBUG ? new View(200, 150) : new View(200, 150);
			_view.addSurface(_dish);
			this.addChild(_view);

			_cyan = new Light();
			_cyan.position = new Vector(0.4, .7, .2);
			_cyan.color = new Color(0.2, 0.9, 0.4);
//			_cyan.color = new Color(0.5, 0.5, 0.5);
			_view.addLight(_cyan);
			trace("Light hue: " + _cyan.color.getHSV().hue);

			_red = new Light();
			_red.position = new Vector(-0.6, .6, -0.5);
			_red.color = new Color(0.2, 0.4, 1.0);
//			_red.color = new Color(0.5, 0.5, 0.5);
			_view.addLight(_red);
			trace("Light hue: " + _red.color.getHSV().hue);

            _label = new TextField();
            _label.autoSize = TextFieldAutoSize.LEFT;
            _label.background = false;
            _label.border = false;

            var format:TextFormat = new TextFormat();
            format.font = "Verdana";
            format.color = 0xFF0000;
            format.size = 16;

            _label.defaultTextFormat = format;
//            addChild(_label);            

            currentFrame = 0;
            
            renderFrame();
        }
        protected var _label :TextField;
        public function renderFrame () :void
        {
            _dish.tick();
            _dish.tick();
            _view.startRender();
            renderLine();
        }

        public function renderLine () :void
        {
            _label.text = "Rendering: " + _view.countProgress() + "%";
   			var time :Number = getTimer();
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
        public var currentFrame :int;
		protected var _red :Light;
		protected var _cyan :Light;

		protected var _view :View;
		protected var _dish :Dish;
	}
}
