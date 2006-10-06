package {

import flash.display.Shape;
import flash.display.Sprite;

import flash.events.Event;
import flash.events.MouseEvent;

import flash.utils.getTimer;

import com.threerings.ezgame.EZGame;
import com.threerings.ezgame.PropertyChangedEvent;

public class Ball extends Shape
{
    /** The radius of the ball. */
    public static const RADIUS :int = 5;
    
    public function Ball (
        gameObj :EZGame, board :Board, own :Boolean, color :uint)
    {
        _gameObj = gameObj;
        _board = board;
        _own = own;
        
        // draw the ball
        graphics.beginFill(color);
        graphics.drawCircle(RADIUS, RADIUS, RADIUS);
        
        // make sure the states are initialized
        if (gameObj.get(BALL_STATES) == null) {
            gameObj.set(BALL_STATES,
                new Array(createState(0), createState(1)));
        }
        _paddle = (_own ? _board.ownPaddle : _board.oppPaddle);
        
        // listen for events
        board.addEventListener(MouseEvent.MOUSE_DOWN, mouseDownHandler);
        board.addEventListener(Event.ENTER_FRAME, enterFrameHandler);
        gameObj.addEventListener(PropertyChangedEvent.TYPE, propChanged);
    }
    
    protected function mouseDownHandler (event :MouseEvent) :void
    {
        // release the ball from the paddle
        if (_paddle == _board.ownPaddle) {
            release();
        }
    }
    
    protected function enterFrameHandler (event :Event) :void
    {
        var time :int = getTimer(),
            dt :Number = (time - _lastFrame);
        _lastFrame = time;
        
        // follow the attached paddle, if any
        if (_paddle != null) {
            setLocation(getAttachX(), getStartY(_paddle == _board.ownPaddle));
            return;
        }
       
        // follow the path, if any
        _pdist += dt * _speed;
        _speed += dt * _accel;
        while (_path != null) {
            var p1 :Object = _path[_pidx],
                p2 :Object = _path[_pidx + 1],
                dx :Number = p2.x - p1.x,
                dy :Number = p2.y - p1.y,
                dist :Number = Math.sqrt(dx*dx + dy*dy),
                t :Number = _pdist / dist,
                bcoll :Object = _path[_pidx].bcoll;
            if (bcoll != null) {
                _board.bricks.wasHit(bcoll.bx, bcoll.by);
            }
            if (t >= 1 && _pidx < _path.length - 2) {
                // proceed to next segment
                _pdist -= dist;
                _pidx++;
                
            } else {
                if (t >= 1) {
                    // reached the end of the path
                    t = 1;
                    _path = null;
                }
                setLocation(p1.x + dx * t, p1.y + dy * t);
                var wy :Number = _board.height - Paddle.HEIGHT - RADIUS;
                if (dy > 0 && _y >= wy && !_passedPaddle) { // check for hit
                    _passedPaddle = true;
                    if (_x >= (_board.ownPaddle.x - RADIUS) &&
                        _x <= (_board.ownPaddle.x + Paddle.WIDTH + RADIUS)) {
                        var s :Number = (wy - p1.y) / dy,
                            wx :Number = p1.x + s * dx;
                        hit(wx, Math.atan2(dy, -dx), (t - s) * dist);
                        return;
                    }
                }
                if (dy > 0 && _path == null) { // reattach
                    _paddle = _board.ownPaddle;
                    _gameObj.set(BALL_STATES,
                        createState(_gameObj.getMyIndex()), getIndex());
                }
                return;
            }
        }
    }

    protected function propChanged (event :PropertyChangedEvent) :void
    {
        if (event.name != BALL_STATES ||
            event.index != getIndex()) {
            return;
        }
        var state :Object = event.newValue;
        if (state.pidx == _gameObj.getMyIndex()) {
            return; // it came from us
        }
        // if the path is null, it's attached to the opponent's paddle
        if (state.path == null) {
            _paddle = _board.oppPaddle;
        
        // otherwise, start following the contained path
        } else {
            followPath(state.path, false);
        }
    }
    
    protected function release () :void
    {
        // the two balls launch in mirrored directions
        var angle :Number = _own ? LAUNCH_ANGLE : (Math.PI - LAUNCH_ANGLE);
        
        // fire the ball and announce the update
        _gameObj.set(BALL_STATES,
            createState(_gameObj.getMyIndex(), followNewPath(_x, angle)),
            getIndex());
    }
    
    protected function hit (
        hx :Number, incidence :Number, penetration: Number) :void
    {
        // compute the reflection angle
        var px :Number = hx - (_board.ownPaddle.x + Paddle.WIDTH/2),
            normal :Number = Math.PI/2 - Math.atan(px / Paddle.CURVATURE_DIST),
            reflection :Number = 2*normal - incidence;
        
        // clamp to limits so that the ball doesn't bounce off the walls
        // for too long
        reflection = Math.min(Math.max(reflection, MIN_REFLECTION_ANGLE),
            MAX_REFLECTION_ANGLE);
        
        // fire the ball and announce the update
        _gameObj.set(BALL_STATES,
            createState(_gameObj.getMyIndex(),
                followNewPath(hx, reflection, penetration)),
            getIndex());
    }
    
    protected function followNewPath (
        position :Number, angle :Number, progress: Number = 0) :Object
    {
        // compute the entire path up to the point where the ball
        // leaves the field
        var dx :Number = Math.cos(angle),
            dy :Number = -Math.sin(angle),
            px :Number = position + dx * progress,
            py :Number = (_board.height - (Paddle.HEIGHT + RADIUS)) +
                dy * progress,
            time :int = getTimer(),
            oob :Boolean = false,
            nodes :Array = new Array({x: px, y: py}),
            path :Object = {btime: time, nodes: nodes},
            plen :Number = 0;
        do {
            var wx :Number = 0,
                wy :Number = Number.MAX_VALUE,
                t :Number;
            // check against bricks
            var bcoll :Object = _board.bricks.intersect(
                px, py, dx, dy, BASE_SPEED, time);
            if (bcoll != null) {
                wx = bcoll.x;
                wy = bcoll.y;
                dx = bcoll.dx;
                dy = bcoll.dy;
                t = bcoll.dist;
                
            } else {
                if (dx != 0) { // check against the walls
                    wx = (dx < 0) ? RADIUS : (_board.width - RADIUS);
                    t = (wx - px) / dx;
                    wy = py + t * dy;
                }
                if (wy > -RADIUS && wy < _board.height + RADIUS) { 
                    dx = -dx; // bounce off wall
                    
                } else { // compute point of exit
                    wy = (dy < 0 ? -RADIUS : (_board.height + RADIUS));
                    t = (wy - py) / dy;
                    wx = px + t * dx;
                    oob = true;
                }
            }
            plen += t;
            time += (t * BASE_SPEED);
            nodes.push({x: (px = wx), y: (py = wy), bcoll: bcoll});
              
        } while (!oob);
        
        followPath(path, true);
        return path;
    }
    
    protected function followPath (path :Object, own :Boolean) :void
    {
        _paddle = null;
        _passedPaddle = false;
        
        // prepare the path received from the opponent by rotating it
        // around and adjusting timestamps
        if (!own) {
            var time :int = getTimer();
            for each (var node :Object in path.nodes) {
                node.x = _board.width - node.x;
                node.y = _board.height - node.y;
                var bcoll :Object = node.bcoll;
                if (bcoll != null) {
                    bcoll.bx = _board.bricks.columns - (bcoll.bx + 1);
                    bcoll.by =  Bricks.LAYERS - (bcoll.by + 1);
                    _board.bricks.willBeHit(bcoll.bx, bcoll.by,
                        time + (bcoll.ctime - path.btime));
                }
            }
        }
        
        // adjust the acceleration to compensate for latency
        //dy = Math.abs(dy);
        //var duration :Number = (_plen / BASE_SPEED) +
        //        _board.latency * (bottom ? +2 : -2);
        //_accel = 2*(_plen - _speed*duration) / (duration*duration);
        
        _path = path.nodes;
        setLocation(_path[0].x, _path[0].y);
        _pidx = 0;
        _pdist = 0;
    }
    
    protected function getAttachX () :Number
    {
        return _paddle.x + Paddle.WIDTH *
            (((_paddle == _board.ownPaddle) == _own) ? 0.75 : 0.25);
    }
    
    protected function getStartY (bottom :Boolean) :Number
    {
        var theight :Number = Paddle.HEIGHT + RADIUS;
        return bottom ? (_board.height - theight) : theight;
    }
    
    protected function getIndex () :int
    {
        var pidx :int = _gameObj.getMyIndex();
        return _own ? pidx : (1 - pidx);
    }
    
    protected function setLocation (x :int, y :int) :void
    {
        this.x = (_x = x) - RADIUS;
        this.y = (_y = y) - RADIUS;
    }
    
    protected function createState (pidx :int, path :Object = null) :Object
    {
        return {pidx: pidx, path: path};
    }
    
    protected var _gameObj :EZGame;
    protected var _board :Board;
    protected var _own :Boolean;
    
    /** If non-null, the paddle to which this ball is attached. */
    protected var _paddle :Paddle;
    
    /** The position of the ball's center. */
    protected var _x :Number, _y :Number;
    
    /** The speed at which the ball travels. */
    protected var _speed :Number = BASE_SPEED, _accel :Number = 0;
    
    /** The path that the ball is following. */
    protected var _path :Array, _plen :Number;
    protected var _pidx :int, _pdist :Number;
    
    /** Set when the ball passes the bottom paddle and thus misses
     * its chance to be hit. */
    protected var _passedPaddle :Boolean;
    
    /** The time of the last frame. */
    protected var _lastFrame :int;
    
    /** The property used to track ball states. */
    protected static const BALL_STATES :String = "ballStates";
    
    /** The angle used for the initial ball launch. */
    protected static const LAUNCH_ANGLE :Number = Math.PI / 4;
    
    /** Upper and lower bounds on the angles at which bounced balls travel. */
    protected static const MIN_REFLECTION_ANGLE :Number = Math.PI / 8,
        MAX_REFLECTION_ANGLE :Number = Math.PI * 7 / 8;
        
    /** The base speed of the ball. */
    protected static const BASE_SPEED :Number = 0.2;
}
}
