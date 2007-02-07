package
{

    /**
       This class is a wrapper around a simple TangleWord score storage object:
       the object contains nested associative lists (mapping players to score lists,
       which map words to score values), and this class provides accessors for
       navigating these lists.
    */
    public class Scoreboard
    {
        /** Constructor */
        public function Scoreboard ()
        {
            _scores = new Object ();
        }

        /** Defines a player with the given /name/, with zero score. */
        public function addPlayer (name : String) : void
        {
            getScoreboard (name);  // this will auto-initialize the player's score
        }

        /** Retrieves the player's current total score */
        public function getTotalScore (name : String) : Number
        {
            var total : Number = 0;
            var score : Object = getScoreboard (name);
            for (var word : String in score)
            {
                if (score[word] is Number)
                {
                    total += (score[word] as Number);
                }
            }

            return total;            
        }

        /** Retrieves the list of players, as an array of strings. */
        public function getPlayers () : Array
        {
            var data : Array = new Array ();
            for (var key : String in _scores)
            {
                data.push (key);
            }
            return data;                
        }

        /** Tries to retrieve the player who scored the specified word. If the word
            hasn't been scored yet, returns null. */
        public function getWordOwner (word : String) : String
        {
            var players : Array = getPlayers ();
            for each (var player : String in players)
            {
                var owner : String = null;
                var words : Array = new Array ();
                var scores : Array = new Array ();
                getWordsAndScores (player, words, scores);

                for each (var w : String in words)
                {
                    if (w == word) return player;
                }
            }

            return null;
        }                

        /** Adds a /word/ with the specified /score/ to the player's scoreboard. */
        public function addWord (player : String, word : String, score : Number) : void
        {
            var scoreboard : Object = getScoreboard (player);
            scoreboard[word] = score;
            Assert.Fail ("Added score: " + player + ", " + word + ", " + score);
        }

        /** Populates the specified /words/ and /scores/ arrays with all of the
            words and word scores achieved by the player.

            NOTE: This function modifes the arrays /words/ and /scores/ passed in
            as function parameters.
        */
        public function getWordsAndScores (player : String, words : Array, scores : Array) : void
        {
            var scoreboard : Object = getScoreboard (player);
            for (var word : String in scoreboard)
            {
                words.push (word);
                scores.push (scoreboard[word]);
            }
        }


        /** For serialization use only: returns a copy to the data storage object */
        public function get internalScoreObject () : Object
        {
            return _scores;
        }

        /** For serialization use only: sets a pointer to the data storage object */
        public function set internalScoreObject (scores : Object) : void
        {
            _scores = scores;
        }
        


        // PRIVATE METHODS

        /**
           Retrieves player's scoreboard object, potentially empty.
           The scoreboard object is an associative array mapping from
           words to numeric scores.
        */
        private function getScoreboard (player : String) : Object
        {
            if (! _scores.hasOwnProperty (player))
            {
                _scores[player] = new Object ();
            }

            return _scores[player];
        }        


        // PRIVATE VARIABLES

        /** Storage object that keeps a copy of player scores */
        private var _scores : Object;
        
    }


}
