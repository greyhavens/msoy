package
{

    /**
     * This class is a wrapper around a simple TangleWord score storage object:
     * contains an associative list of players and their total scores,
     * and a simple array of words that have already been claimed this round.
     */
    public class Scoreboard
    {
        /** Constructor */
        public function Scoreboard ()
        {
            _data = new Object ();
            _data.scores = new Object ();  // maps player name => total score
            _data.claimed = new Object (); // maps word => player name
        }

        /** Defines a player with the given /name/, with zero score. */
        public function addPlayer (name : String) : void
        {
            getScore (name);  // this will auto-initialize the player's score
        }

        /** Retrieves the list of players, as an array of strings. */
        public function getPlayers () : Array
        {
            var data : Array = new Array ();
            for (var key : String in _data.scores)
            {
                data.push (key);
            }
            return data;                
        }
        
        /** Retrieves player's score, potentially zero. If the /players/
         *  object doesn't have the player's score, it's initialized on first access. */
        public function getScore (player : String) : Number
        {
            if (! _data.scores.hasOwnProperty (player)) {
                _data.scores[player] = 0;
            }
            return _data.scores[player];
        }
        
        /** Marks the /word/ as claimed, and adds the /score/ to the player's total. */
        public function addWord (player : String, word : String, score : Number) : void
        {
            _data.claimed[word] = player;
            _data.scores[player] = getScore (player) + score;
        }

        /** If this word was already claimed, returns true; otherwise false. */
        public function isWordClaimed (word : String) : Boolean
        {
            return _data.claimed.hasOwnProperty (word);
        }

        /** Resets all word claims (but not player scores). */
        public function resetWordClaims () : void
        {
            _data.claimed = new Object ();
        }

        /** For serialization use only: returns a copy to the data storage object */
        public function get internalScoreObject () : Object
        {
            return _data;
        }

        /** For serialization use only: sets a pointer to the data storage object */
        public function set internalScoreObject (data : Object) : void
        {
            _data = data;
        }
        

        // IMPLEMENTATION DETAILS

        /** Storage object that keeps a copy of player scores */
        private var _data : Object;
        
    }


}
