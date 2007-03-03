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
            _data.totalScores = new Object (); // maps player name => total score
            _data.roundScores = new Object (); // mapes player name => round score
            _data.claimed = new Object ();     // maps word => player name
        }

        /** Defines a player with the given /name/, with zero score. */
        public function addPlayer (name : String) : void
        {
            getTotalScore (name);  // this will auto-initialize the player's score
            getRoundScore (name);  // ... and the round score
        }

        /** Retrieves the list of players, as an array of strings. */
        public function getPlayers () : Array
        {
            var data : Array = new Array ();
            for (var key : String in _data.totalScores)
            {
                data.push (key);
            }
            return data;                
        }
        
        /** Retrieves player's total score, potentially zero. If the scoring
         *  object doesn't have the player's score, it's initialized on first access. */
        public function getTotalScore (player : String) : Number
        {
            if (! _data.totalScores.hasOwnProperty (player)) {
                _data.totalScores[player] = 0;
            }
            return _data.totalScores[player];
        }

        /** Retrieves player's round score, potentially zero. If the scoring
         *  object doesn't have the player's score, it's initialized on first access. */
        public function getRoundScore (player : String) : Number
        {
            if (! _data.roundScores.hasOwnProperty (player)) {
                _data.roundScores[player] = 0;
            }
            return _data.roundScores[player];
        }
        
        /** Marks the /word/ as claimed, and adds the /score/ to the player's total. */
        public function addWord (player : String, word : String, score : Number) : void
        {
            _data.claimed[word] = player;
            _data.roundScores[player] = getRoundScore (player) + score;
            _data.totalScores[player] = getTotalScore (player) + score;
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
            _data.roundScores = new Object ();
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
