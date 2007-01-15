package
{
	import flash.display.Bitmap;
	import flash.display.BitmapData;
	
	public class View extends Bitmap
	{
	    protected var _nextBitmap :BitmapData;
	    
		public static const AMBIENT_LIGHT :Color = new Color(0.2, 0.2, 0.2);
//		public static const AMBIENT_LIGHT :Color = new Color(0, 0, 0);

		public function View (width: int, height: int)
		{
			super(new BitmapData(width, height));
		}

        protected var _line :int;
        
        public function countProgress () :int
        {
            return _line * 100 / bitmapData.height;
        }
        
		protected var viewAngle :Number;
		protected var viewDistance :Number;
		protected var cs :Number;
		protected var si :Number;

		protected var eye :Vector;
		protected var viewPlaneScale :Number;
		protected var pointDir :Vector;

        public function startRender () :void
        {
            _nextBitmap = new BitmapData(width, height);
            _nextBitmap.lock();
            _line = 0;
            
   			viewAngle = -50*Math.PI/180;
			viewDistance = 1.1;
			cs = Math.cos(viewAngle);
			si = Math.sin(viewAngle);

			eye = new Vector(0, 1.7, -1.7);
			viewPlaneScale = 1/_nextBitmap.width;
			pointDir = new Vector();

        }

		public function renderNextLine () :Boolean
		{
			for (var x :int = 0; x < _nextBitmap.width; x ++) {
                var y :int = _line;
				var pX :Number = (x - _nextBitmap.width/2) * viewPlaneScale;
				var pY :Number = -(y - _nextBitmap.height/2) * viewPlaneScale;
				pointDir.x = pX;
				pointDir.y = pY*cs + viewDistance*si;
				pointDir.z = -pY*si + viewDistance*cs;
				pointDir.normalize();
				_nextBitmap.setPixel(x, y, renderPoint(eye, pointDir));
			}
			bitmapData.setPixel(0, _line, 0x5599bb);
            _line ++;

            if (_line >= _nextBitmap.height) {
                _nextBitmap.unlock();
                this.bitmapData = _nextBitmap;
                _nextBitmap = null;
    			return true;
            }
            return false;
		}
		
		public function addSurface (surface :Surface) :void
		{
			_surfaces.push(surface);
		}

		public function addLight (light :Light) :void
		{
			_lights.push(light);
		}

		protected function renderPoint (eye :Vector, dir :Vector) :uint
		{
			for (var i :int = 0; i < _surfaces.length; i ++) {
				var hit :Object = _surfaces[i].test(eye, dir);
				if (hit != null) {
					return renderHit(_surfaces[i] as Surface, eye, hit);
				}
			}
			return 0;
		}
		
		protected function renderHit (surface :Surface, eye :Vector, hit :Object) :uint
		{
            color.setColor(AMBIENT_LIGHT);
            color.multiplyColor(hit.c);

   			// calculate the eye vector
   			pointToEye.setVector(eye);
   			pointToEye.add(hit.p, -1);
            var pDist :Number = pointToEye.dot(pointToEye);
   			pointToEye.normalize();

            // the surface can return a null normal to imply fD=fS=0, e.g. a pure light source
            if (hit.n != null) {
    			var normal :Vector = hit.n;

    			for (var i :int = 0; i < _lights.length; i ++) {
    				var light :Light = _lights[i];
    
    				// calculate the light vector
    				pointToLight.setVector(light.position);
    				pointToLight.add(hit.p, -1);
    				var d2 :Number = pointToLight.dot(pointToLight);
    
    				pointToLight.normalize();
    
    				// that's enough to calculate the diffuse term
    				var diffuse :Number = Math.max(0, pointToLight.dot(normal));
    
    				// now the half-angle for Blinn specular
    				halfAngle.setVector(pointToLight);
    				halfAngle.add(pointToEye);
    				halfAngle.multiply(0.5);
    				halfAngle.normalize();
    				var blinnDot :Number = Math.max(0, halfAngle.dot(normal));
    				var specular :Number = Math.pow(blinnDot, 40);
    
    				// multiple colours term by term, RGB-wise
                    work.setColor(light.color);
                    work.multiplyColor(hit.c);
                    // and add this light's contribution
    				color.addColor(work, (hit.fD*diffuse + hit.fS*specular)/d2);
    			}
            }
			// finally add the intrinsic glow
			color.addColor(hit.c, hit.glow);

			// use unclamped colour here; this is for simulation, not display
			surface.finalColor(hit, color);
			return color.clamp();
		}

        protected var color :Color = new Color();
        protected var work :Color = new Color();

		protected var pointToEye :Vector = new Vector();
		protected var pointToLight :Vector = new Vector();
		protected var halfAngle :Vector = new Vector();

		protected var _lights :Array = new Array();
		protected var _surfaces :Array = new Array();
	}
}