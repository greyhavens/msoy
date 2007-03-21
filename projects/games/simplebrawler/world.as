package{
	import flash.display.*;
	import flash.events.*;
	import flash.geom.*;
	
	[SWF(width="800", height="505")]
	public class world extends MovieClip{
		public var local_player:MovieClip;
		public var goal:MovieClip;
		public var floor:MovieClip = new MovieClip();
		public var hud:MovieClip;
		public var camera:MovieClip;
		
		public var time_speed:Number = 1.0;
		public var fps:int = 40;
		
		public var world_width: Number = 800;
		
		public function world(){
			stage.frameRate = fps*time_speed
        	root.loaderInfo.addEventListener(Event.UNLOAD, handleUnload);
			load ();
		}
		
		//-------------------------------------LOAD----------------------------------------------
    	protected function load () :void
		{
			
			this.camera._zoom.bg.ground.addEventListener(MouseEvent.CLICK, floorCLICK_handler);
			
			goal = new destination();
			this.camera._zoom.bg.addChild(goal);
			
			local_player = create_player("Local");
			
			
			this.hud.addEventListener("enterFrame", hud_enterFrame);
    	}
		
		//-------------------------------------UNLOAD--------------------------------------------
    	protected function handleUnload (event :Event) :void
    	{
    	}
		
		//-------------------------------------UPDATE HUD----------------------------------------
		private function hud_enterFrame(e:Event){
			hud.fps_output.text = "FPS: "+stage.frameRate;
			
			var pos:Point = new Point(local_player.x, local_player.y);
			pos = localToGlobal(pos);
			var cam_x:Number = root.camera.x;
			var cam_goal: Number = (pos.x-stage.stageWidth/2)*-1;
			
			cam_x = (cam_x-cam_goal)*0.1;

			root.camera.x -= cam_x;
			
			//Edge of World Checks
			var bg1_w:Number = root.camera._zoom.bg.bg_1.width-stage.stageWidth;
			if (root.camera.x < bg1_w*(-1)){root.camera.x = bg1_w*(-1);}
			if (root.camera.x > 0){root.camera.x = 0;}
			
			update_bg();
			
		}
		
		//-------------------------------------UPDATE BG-----------------------------------------
		private function update_bg() :void{
			var cam: MovieClip = root.camera;
			var bg1: MovieClip = root.camera._zoom.bg.bg_1;
			var bg2: MovieClip = root.camera._zoom.bg.bg_2;
			var bg3: MovieClip = root.camera._zoom.bg.bg_3;
			var bg4: MovieClip = root.camera._zoom.bg.bg_4;
			var bg5: MovieClip = root.camera._zoom.bg.bg_5;
			
			var bg1_w:Number = bg1.width-stage.stageWidth;
			var bg2_w:Number = bg2.width-stage.stageWidth;
			var bg3_w:Number = bg3.width-stage.stageWidth;
			var bg4_w:Number = bg4.width-stage.stageWidth;
			var bg5_w:Number = bg5.width-stage.stageWidth;
			
			var bg2_r:Number = bg2_w/bg1_w;
			var bg3_r:Number = bg3_w/bg1_w;
			var bg4_r:Number = bg4_w/bg1_w;
			var bg5_r:Number = bg5_w/bg1_w;
			
			var pos:Number = (cam.x)/bg1_w;
			
			bg2.x = bg2_w*pos+(cam.x*-1);
			bg3.x = bg3_w*pos+(cam.x*-1);
			bg4.x = bg4_w*pos+(cam.x*-1);
			bg5.x = bg5_w*pos+(cam.x*-1);
			
		}
		
		//-------------------------------------NEW PLAYER----------------------------------------
		protected function create_player(n: String) :MovieClip
    	{
			var mc: player = new player();
			mc.pName = n;
			trace("Player character '"+mc.pName+"' created!");
			
			mc.y = stage.stageHeight/1.5;
			mc.x = stage.stageWidth/2;
			mc.goal_x = mc.x;
			mc.goal_y = mc.y;
			mc.start_x = mc.x;
			mc.start_y = mc.y;
			mc.move_time = 0;
			mc.move_distance = 0;
			mc.addEventListener("enterFrame", player_enterFrame);
			player_scale(mc);
			
			root.camera._zoom.bg.addChild(mc);
			
			return mc
    	}
		
		//-------------------------------------UPDATE PLAYER------------------------------------
		private function player_enterFrame(e:Event){
			
			//var speedx: Number = e.target.x-e.target.goal_x;
			//var speedrx: Number = speedx/60;
			//var speedy: Number = e.target.y-e.target.goal_y;
			//var speedry: Number = speedy/60;
			
			//e.target.x -= speedrx;
			//e.target.y -= speedry;
			
			
			var lp_current: Point = new Point(e.target.x,e.target.y);
			
			var lp_start: Point = new Point(e.target.start_x,e.target.start_y);
			var lp_goal: Point = new Point(e.target.goal_x,e.target.goal_y);
			var lp_distance: Number = Point.distance(lp_current,lp_goal);
			
			if (e.target.moving){
				if (lp_distance > 5){
					e.target.move_time = e.target.move_time+e.target.speed*e.target.scaleX;
					lp_current = Point.interpolate(lp_goal,lp_start,e.target.move_time/e.target.move_distance);
				} else {
					e.target.moving = false;
				}
				e.target.x = lp_current.x;
				e.target.y = lp_current.y;
			
				player_scale(e.target);
			} else {
				e.target.move_time = 0;
				e.target.move_distance = 0;
				e.target.start_x = e.target.goal_x;
				e.target.start_y = e.target.goal_y;
				e.target.x = e.target.goal_x;
				e.target.y = e.target.goal_y;
			}
		}
		
		//-------------------------------------DEPTH SCALE--------------------------------------
		private function player_scale(e:MovieClip){
			var ground_max: Number = root.camera._zoom.bg.ground.y;
			var ground_min: Number = root.camera._zoom.bg.ground.y-root.camera._zoom.bg.ground.height;
			ground_max = ground_max - ground_min;
			var size: Number = e.y-ground_min;
			size = ((size/ground_max)*0.5)+0.5
			e.scaleX = size;
			e.scaleY = size;
		}
		
		//-------------------------------------NAVI----------------------------------------------
		private function floorCLICK_handler(e:MouseEvent){
			var pos:Point = new Point(e.stageX, e.stageY);
			pos = root.camera._zoom.bg.globalToLocal(pos);
			
			goal.x = pos.x;
			goal.y = pos.y;

			player_scale(goal);
			
			local_player.start_x = local_player.x;
			local_player.start_y = local_player.y;
			local_player.goal_x = pos.x;
			local_player.goal_y = pos.y;
			
			var lp_start: Point = new Point(local_player.start_x,local_player.start_y);
			var lp_goal: Point = new Point(local_player.goal_x,local_player.goal_y);
			
			local_player.move_distance = Point.distance(lp_start,lp_goal);
			local_player.move_time = 0;
			
			local_player.moving = true;
		}
	}
}