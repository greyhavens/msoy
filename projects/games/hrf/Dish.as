package
{
    public class Dish extends CellMatrix implements Surface
    {
        public static const nDim :uint = 64;
        public static const cDim :uint = 256;
        public static const r :Number = 0.01;
        public static const COPPER :Color = new Color(1.0, 0.4, 0.2);

        public function Dish ()
        {
            super(cDim, cDim);
        }

        protected var _noise :ImprovedPerlin = new ImprovedPerlin();

		public function test (eye :Vector, dir :Vector) :Object
		{
			// find the length of the ray that intersects y=0
			if (dir.y == 0) {
				// infinitely long, screw that
				return null;
			}
			// otherwise...
			var l :Number = eye.y / -dir.y;
			if (l <= 0) {
				return null;
			}

			// now find which (x, z) the intersection is at
			var x :Number = eye.x + l*dir.x;
			var z :Number = eye.z + l*dir.z;
			_hit.p.setXYZ(x, 0, z);

			// if we missed, display a white surrounding
            if (x*x + z*z >= 1) {
                _hit.n.setXYZ(0, 1, 0);
                _hit.c.setRGB(1, 1, 1);
                _hit.fD = 0.8;
                _hit.fS = 0.2;
                _hit.glow = 0;
                return _hit;
            }

            // note: this is -not- real antialiasing; that'd require separate raycasts;
            // the only reason we get away with this is that we need to do the finite
            // difference anyway.

            getSample(sample, x + 0, z + 0);
            getSample(sample_l, x - r, z + 0);
            getSample(sample_r, x + r, z + 0);
            getSample(sample_u, x - 0, z - r);
            getSample(sample_d, x - 0, z + r);

            var strength :Number = 0;
            var hue :Number = 0;
            for (var i :int = 0; i < myBits.length; i ++) {
                strength += myBits[i].strength;
                hue += myBits[i].hue * myBits[i].strength;
            }
            hue = strength == 0 ? 0 : hue/strength;
            strength /= myBits.length;

            _hit.n.setXYZ((sample_r.height - sample_l.height)/(2*r),
                          256,
                          (sample_d.height - sample_u.height)/(2*r));
            _hit.n.normalize();
            _hit.c.setColor(COPPER, 1-strength);
            _hit.c.addHSV(hue, (.25 + .5*strength), strength);

            _hit.fD = 0.4 + strength/2;
            _hit.fS = 0.8 - strength/2;
            _hit.glow = Math.max(0, strength-0.8);

			return _hit;
		}

        protected var sample :Object = { color: new Color() };
        protected var sample_l :Object = { color: new Color() };
        protected var sample_r :Object = { color: new Color() };
        protected var sample_u :Object = { color: new Color() };
        protected var sample_d :Object = { color: new Color() };

        protected var myBits :Array = [ sample, sample_l, sample_r, sample_u, sample_d ];

        protected var lastStrength :Number = 0;

        protected function getSample (bit :Object, x :Number, z :Number) :void
        {
            if (x*x + z*z >= 1) {
                bit.strength = 0;
                bit.height = 0;
                bit.hue = 0;
                return;
            }
            var cell :Cell = getCell(toCell(x), toCell(z));
            var ground :Number = _noise.noise(toNoise(x), toNoise(z), 0);
            ground = Math.min(ground + cell.strength, 1);
            bit.strength = cell.strength;
            bit.height = ground;
            bit.hue = cell.hue;
        }

		public function finalColor (hit :Object, c :Color) :void
		{
		    if (hit.p.dot(hit.p) < 1) {
                setLight(toCell(hit.p.x), toCell(hit.p.z), c);
		    }
		}

        public function toCell (x :Number) :Number { return (x+1)*cDim/2; }
        public function toNoise (x :Number) :Number { return (x+1)*nDim/2; }

        public function tick () :void
        {
            transformMatrix();
        }

		protected var _hit :Object =
			{ x:0, y:0, z:0, p: new Vector(), n: new Vector(), c: new Color() };
    }
}