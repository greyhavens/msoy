package
{
	public class Vector
	{
		public var x :Number;
		public var y :Number;
		public var z :Number;
		
		public function Vector (x :Number = 0, y :Number = 0, z :Number = 0)
		{
			this.x = x;
			this.y = y;
			this.z = z;
		}
		
		public function clone () :Vector
		{
			return new Vector(x, y, z);
		}

		public function toString () :String
		{
			return "[x=" + x + ", y=" + y + ", z=" + z + "]";
		}
		
		public function setXYZ (x :Number, y :Number, z :Number) :void
		{
			this.x = x;
			this.y = y;
			this.z = z;
		}

		public function setVector (v :Vector) :void
		{
			this.x = v.x;
			this.y = v.y;
			this.z = v.z;
		}

		public function add (other :Vector, factor :Number = 1) :void
		{
			this.x += factor * other.x;
			this.y += factor * other.y;
			this.z += factor * other.z;
		}

		public function addXYZ (x :Number, y :Number, z :Number) :void
		{
			this.x += x;
			this.y += y;
			this.z += z;
		}
		
		public function multiply (factor :Number) :void
		{
			this.x *= factor;
			this.y *= factor;
			this.z *= factor;
		}
		
		public function normalize () :void
		{
			var length :Number = length();
			this.x /= length;
			this.y /= length;
			this.z /= length;
		}
		
		public function length () :Number
		{
			return Math.sqrt(x*x + y*y + z*z);
		}

		public function dot (other: Vector) :Number
		{
			return x*other.x + y*other.y + z*other.z;
		}
	}
}