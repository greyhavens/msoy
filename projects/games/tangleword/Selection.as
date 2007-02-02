package
{
    import flash.geom.Point;

    /**
       Represents the set of letters the player chose for their current word.
       It's really just typed wrapper around a list of Point objects.
    */
    public class Selection
    {
        /** Simple constructor */
        public function Selection (board : Board)
        {
            _board = board;
        }

        /** Add a single point */
        public function push (p : Point) : uint
        {
            return elements.push (p);
        }

        /** Get rid of what's on the top */
        public function pop () : Point
        {
            return elements.pop ();
        }

        /** What's on top? */
        public function peek () : Point
        {
            if (elements.length > 0)
                return elements[elements.length - 1];
            else
                return null;
        }
        

        // PUBLIC DATA MEMBERS

        public var elements : Array = new Array ();


        // PRIVATE DATA MEMBERS

        private var _board : Board;
    }
    
}
