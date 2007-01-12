package
{
	public interface Surface
	{
		function test (eye :Vector, dir :Vector) :Object;
		function finalColor (hit :Object, c :Color) :void;
	}
}