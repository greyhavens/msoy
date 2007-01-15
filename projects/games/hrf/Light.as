package
{
	public class Light implements Surface
	{
		public var color :Color;
		public var position :Vector;
		public const R2 :Number = 0.05*0.05;
		public const NDIM :int = 16;

		public function test (eye :Vector, dir :Vector) :Object
		{
      		// do basic algebraic ray/sphere intersection
			_eyeToSphere.setVector(position);
			_eyeToSphere.add(eye, -1);
			var B :Number = _eyeToSphere.dot(dir);
			var C :Number = _eyeToSphere.dot(_eyeToSphere) - R2;
			var D :Number = B*B - C;
			if (D <= 0) {
				return null;
			}
			_hit.p = position;
            _hit.glow = 1/R2/20;
            _hit.c.setColor(color);
            return _hit;
		}

		protected var _eyeToSphere :Vector = new Vector();
        protected var _noise :ImprovedPerlin = new ImprovedPerlin();

		public function finalColor (hit :Object, c :Color) :void
		{
			// we don't care
		}

		protected var _hit :Object = { c: new Color() };
	}
}