package
{
	public class Color
	{
		public var r: Number;
		public var g: Number;
		public var b :Number;
		
		public function Color (r :Number = 0, g :Number = 0, b :Number = 0)
		{
			this.r = r;
			this.g = g;
			this.b = b;
		}
		
		public function clone () :Color
		{
			return new Color(r, g, b);
		}
		
		public function toString () :String
		{
			return "[r=" + r + ", g=" + g + ", b=" + b + "]";		
		}
		
		public function getHSV () :Object
		{
		    return RGBtoHSV(r, g, b);
		}

		public function setColor (other :Color, factor :Number = 1) :void
		{
		    this.r = factor * other.r;
		    this.g = factor * other.g;
		    this.b = factor * other.b;
		}

		public function setRGB (r :Number, g :Number, b :Number, factor :Number = 1) :void
		{
            this.r = factor * r;
            this.g = factor * g;
            this.b = factor * b;
		}

		public function setHSV (h :Number, s :Number, v :Number) :void
		{
            var rgb :Object = HSVtoRGB(h, s, v);
			this.r = rgb.r;
			this.g = rgb.g;
			this.b = rgb.b;
		}

		public function addColor (other :Color, factor :Number = 1) :void
		{
			this.r += factor * other.r;
			this.g += factor * other.g;
			this.b += factor * other.b;
		}

		public function addRGB (r :Number, g :Number, b :Number) :void
		{
			this.r += r;
			this.g += g;
			this.b += b;
		}

		public function addHSV (h :Number, s :Number, v :Number) :void
		{
            var rgb :Object = HSVtoRGB(h, s, v);
			this.r += rgb.r;
			this.g += rgb.g;
			this.b += rgb.b;
		}

		public function multiplyColor (other :Color) :void
		{
		    this.r *= other.r;
		    this.g *= other.g;
		    this.b *= other.b;
		}

		public function multiply (factor :Number) :void
		{
		    this.r *= factor;
		    this.g *= factor;
		    this.b *= factor;
		}

		public function clamp () :uint
		{
//		    var rByte :uint = (uint) (Math.log(r) * 0xFF);
//		    var gByte :uint = (uint) (Math.log(g) * 0xFF);
//		    var bByte :uint = (uint) (Math.log(b) * 0xFF);
			var rByte :uint = (uint) (Math.min(Math.max(0, r), 1) * 0xFF);
			var gByte :uint = (uint) (Math.min(Math.max(0, g), 1) * 0xFF);
			var bByte :uint = (uint) (Math.min(Math.max(0, b), 1) * 0xFF);
			return rByte * 0x10000 + gByte * 0x100 + bByte;
		}
		
		
		protected function HSVtoRGB (h :Number, s :Number, v :Number) :Object
		{
            if (s == 0) {
        		// achromatic (grey)
                _rgb.r = _rgb.g = _rgb.b = v;
                return _rgb;
            }

        	h /= 60;			// sector 0 to 5
            var i :int = Math.floor(h);
	        var f :Number = h - i;			// factorial part of h
        	var p :Number = v * ( 1 - s );
        	var q :Number = v * ( 1 - s * f );
            var t :Number = v * ( 1 - s * ( 1 - f ) );

        	switch (i) {
        		case 0:
        			_rgb.r = v;
        			_rgb.g = t;
        			_rgb.b = p;
        			break;
        		case 1:
        			_rgb.r = q;
        			_rgb.g = v;
        			_rgb.b = p;
        			break;
        		case 2:
        			_rgb.r = p;
        			_rgb.g = v;
        			_rgb.b = t;
        			break;
        		case 3:
        			_rgb.r = p;
        			_rgb.g = q;
        			_rgb.b = v;
        			break;
        		case 4:
        			_rgb.r = t;
        			_rgb.g = p;
        			_rgb.b = v;
        			break;
        		case 5:
        			_rgb.r = v;
        			_rgb.g = p;
        			_rgb.b = q;
        			break;
	        }
	        return _rgb;
		}
		
		protected function RGBtoHSV (r :Number, g :Number, b :Number) :Object
		{
        	var min :Number = Math.min(r, g, b);
            var max :Number = Math.max(r, g, b);
            _hsv.val = max;
            var delta :Number = max - min;

            if (max == 0) {
        		// r = g = b = 0		// s = 0, v is undefined
        		_hsv.sat = 0;
                _hsv.hue = -1;
        		return _hsv;
            }
            _hsv.sat = delta / max;

        	if( r == max ) {
                _hsv.hue = (g-b)/delta;        // between yellow & magenta
        	} else if(g == max) {
        	    _hsv.hue = 2 + (b-r)/delta;    // between cyan & yellow
             } else {
                _hsv.hue = 4 + (r-g)/delta;    // between magenta & cyan
             }
            _hsv.hue *= 60;
    		return _hsv;
		}
        protected var _hsv :Object = { h: 0, s: 0, v: 0 };		
        protected var _rgb :Object = { r: 0, g: 0, b: 0 };
	}
}