package
{
    public class Cell
    {
        public var hue :Number;
        public var strength :Number;

        public function Cell (hue :Number = 0, strength :Number = 0)
        {
            setBits(hue, strength);
        }
        
        public function setBits (hue :Number, strength :Number) :void
        {
            this.hue = hue;
            this.strength = strength;
        }
        
        public function setCell (other :Cell) :void
        {
            hue = other.hue;
            strength = other.strength;
        }
    }
}