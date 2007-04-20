package{
	import flash.display.*;
	import flash.events.*;
	import flash.geom.*;
	import flash.ui.Mouse;
	import flash.net.*;
	import 	flash.utils.Timer;
	
	import com.threerings.ezgame.*; //-W-
	import com.whirled.WhirledGameControl; //-W-
	import com.threerings.util.Random;
	
	[SWF(width="800", height="505")]
	public class world extends MovieClip
		implements MessageReceivedListener, PropertyChangedListener
	{
		public var local_player:MovieClip;
		
		public var loadscreen:MovieClip;
		public var endscreen:MovieClip;
		
		public var game :MovieClip;
		public var preload :MovieClip;
		
		public var goal:MovieClip;
		public var hud:MovieClip;
		public var camera:MovieClip;
		public var ground_cursor:MovieClip;
		public var ground_cursor_hide:Boolean;
		public var goal_hide:Boolean;
		
		public var ground:MovieClip;
		public var bg:MovieClip;
		
		public var room_num:Number = 1;
		public var wave_num:Number = 1;
		
		//public var pc :Array;
		public var pc_count:Number = 0;
		public var npc_count:Number = 0;
		
		public var npc_killcount:Number = 0;
		
		public var darkness:Number = 0.0;
		
		//FPS Calculator Variables
		public var timer: Timer = new Timer(1000);
		public var current_fps: Number;
		
		public var poke_timer: Timer = new Timer(2000);
		
		public var time_speed:Number = 1.0;
		public var fps:Number = 20;
		public var current_mps:Number = 0;
		
		//Keyboard inputs
		public var punch_hit: Boolean;
		public var kick_hit: Boolean;
		public var block_hit: Boolean;
		public var sprint_hit: Boolean;
		
		public var punch_code: 		int = 51; 	//[3]
		public var punch_code_alt: 	int = "d".charCodeAt(0);
		
		public var kick_code: 		int = 50;	//[2]
		public var kick_code_alt: 	int = "a".charCodeAt(0);
		
		public var block_code: 		int = 52;	//[4]
		public var block_code_alt: 	int = "s".charCodeAt(0);
		
		public var sprint_code: 	int = 49;	//[1]
		public var sprint_code_alt: int = "w".charCodeAt(0);
		
		public var world_width: Number = 800;
		
		//Stats
		public var local_exp: Number = 0;
		public var local_level: Number = 1;
		public var local_energy: Number = 100;
		public var local_limiter: Boolean = false;
		public var local_score: Number = 0;
		
		public var world_clock: Timer = new Timer(33);
		public var clock: Number = 0;
		public var clock_nextupdate :Number = 0;
		public var zone_delay: Number = 0;
		public var allclear: Boolean = false;
		public var gameover: Boolean = false;
		public var boss_killed: Boolean = false;
		public var gotonextroom: Boolean = false;
		public var npc_pos_clock: Number = 0;
		
		public var death_clock: Number = 0;
		
		public var health_tick: Number = 0;
		
		public var itemcount: int = 0;
		
		public var last_attack: Number = 0;
		public var current_attack: Number = 0;
		
		public var p1_status: String = "empty";
		public var p2_status: String = "empty";
		public var p3_status: String = "empty";
		public var p4_status: String = "empty";
		public var p5_status: String = "empty";
		public var p6_status: String = "empty";
		
		public var k1_status: String = "empty";
		public var k2_status: String = "empty";
		public var k3_status: String = "empty";
		
		public var pk_status: String = "off";
		
		public var MSGBOX: MovieClip;
		
		/** Game control. */
    	protected var _control :WhirledGameControl; //-W-
		public var playerIds :Array;
		
		public function world(){

			_control = new WhirledGameControl(this); //-W-
        	_control.registerListener(this); //-W-
			_control.addEventListener(KeyboardEvent.KEY_UP, keyReleased);
			_control.addEventListener(KeyboardEvent.KEY_DOWN, keyPressed);
			
			preload = new _PRELOADER();
			root.addChild(preload);
			
			root.loaderInfo.addEventListener(Event.COMPLETE, world_Loaded);
            root.loaderInfo.addEventListener(ProgressEvent.PROGRESS, world_Loading);
        	root.loaderInfo.addEventListener(Event.UNLOAD, world_Unload);
		}
		
				
		//-------------------------------------LOAD----------------------------------------------
		protected function world_Loaded (e:Event) :void
		{
			game = new _GAME();
			root.addChildAt(game, 1);
			root.setChildIndex(preload,2);
			
			preload.fade.play();
			world_Load();
		}
		
		protected function world_Loading (e:ProgressEvent) :void
		{
			var amountLoaded:Number = e.bytesLoaded/e.bytesTotal;
            var amountLoadedStr:String = Math.round(amountLoaded * 100) + "%";
            preload.progress.text = amountLoadedStr;
            preload.bar.width = amountLoaded * pbWidth;
		}

    	protected function world_Load () :void
		{
			
			MSGBOX = new MovieClip();
			root.addChild(MSGBOX);
			
			if (_control.isConnected() && _control.amInControl()){
						var element :String = "enemyDamage";
						var table :Number = _control.get(element);
						if (table == 0) {
							table = 0;
							_control.set(element, table);
						}
						element = "playerDamage";
						table  = _control.get(element);
						if (table == 0) {
							table = 0;
							_control.set(element, table);
						}
						element = "koCount";
						table  = _control.get(element);
						if (table == 0) {
							table = 0;
							_control.set(element, table);
						}
						element = "cleartime";
						table  = _control.get(element);
						if (table == 0) {
							table = 0;
							_control.set(element, table);
						}
			}
			
			hud = game.hud;
			
			timer.addEventListener( TimerEvent.TIMER, onTimerEvent);
			timer.start();
			current_fps = 0;
			
			world_clock.addEventListener( TimerEvent.TIMER, world_tick);
			world_clock.start();
			
			poke_timer.addEventListener( TimerEvent.TIMER, player_poke);
			poke_timer.start();
			
			bg = game.camera._zoom.bg;
			ground = game.camera._zoom.bg.ground;
			ground.doubleClickEnabled = true;
			ground.addEventListener(MouseEvent.CLICK, floorCLICK_handler);
			
			
			goal = new destination();
			bg.cursor_zone.addChild(goal);
			
			ground_cursor = new cursor();
			bg.cursor_zone.addChild(ground_cursor);
			
			if (_control.isConnected()) {
				playerIds = _control.getOccupants();
				var local_name: String = _control.getOccupantName(_control.getMyId());
				local_player = create_player(local_name, "PC", 0,0, 100, 250, 0,0,0,0,0,0, _control.getMyId(), 0);
			}else{
				local_player = create_player("OFFLINE", "PC", 0,0, 100, 250 ,0,0,0,0,0,0, 0, 0);
			}
			
			if(local_player.pName == "Jessica"){
				local_exp = 300;
			}
			
			hud.zone.text = hud.levelname.text+" - ZONE "+room_num;
			load_npcs(room_num,wave_num);
			
			bg.door_next.x = ground.width;
			bg.door_next.height = ground.height;
			
			hud.addEventListener("enterFrame", hud_enterFrame);
    	}
		
		//-------------------------------------UNLOAD--------------------------------------------
    	protected function world_Unload (event :Event) :void
    	{
			//Despawn Monsters, Props, and Players.
			//************************************
			//                -W-
				if (_control.isConnected()) {
					var msg :Object = new Object;
					msg[0] = room_num;
					msg[1] = _control.getMyId();
					_control.sendMessage (p_quit, msg);
					current_mps+=1;
					msg[0] = room_num;
					msg[1] = _control.getMyId();
					msg[2] = String(local_player.pName+" left the game.");
					_control.sendMessage (p_msg, msg);
					current_mps+=1;
				}
			//                -W-
			//************************************
			_control.unregisterListener(this); //-W-
    	}
		
		//-------------------------------------TIMER---------------------------------------------
		private function onTimerEvent( e: Event):void{
			hud.fps_output.text = "FPS: "+current_fps;
			if(current_fps < 20){
				hud.fps_output.textColor = 0xFF0000;
			}else{
				hud.fps_output.textColor = 0xFFFFFF;
			}
			current_fps=0;
			
			hud.mps_output.text = "MPS: "+current_mps;
			if(current_mps >= 10){
				hud.mps_output.textColor = 0xFF0000;
			}else{
				hud.mps_output.textColor = 0xFFFFFF;
			}
			current_mps=0;
		}

		private function world_tick( e: Event):void{
			clock += 33;
			
			if (local_player.hp > 0){
				death_clock = clock+10000;
				hud.hp_warning.alpha = 1;
				hud.energy_warning.alpha = 1;
				hud.dc.text = "";
			}else{
				hud.respawn.gotoAndStop("on");
				var dc_time :String = Math.round((death_clock-clock)/1000);
				if (dc_time < 10){
					dc_time = "0"+dc_time;
				}
				hud.dc.text = dc_time;
				hud.hp_warning.alpha = 0;
				hud.energy_warning.alpha = 0;
				disable_mouse();
				disable_keys();
				if(clock >= death_clock){
					hud.respawn.gotoAndStop("off");
					player_respawn_local();
				}
			}
			
			if (local_player.energy <= 100 && local_player.hp > 0){
				if (local_player.sprinting){
						local_player.energy -= 3.333;
				}else if(local_player.animation == "block"){
						local_player.energy -= 0.667;
				}else{
					if (local_limiter){
						local_player.energy += 1.667;
					}else{
						local_player.energy += 3.333;
					}
				}
			}
			
			if (local_player && clock >= health_tick && local_player.hp > 0){
				health_tick = clock+3000;
				if (local_player.hp < local_player.maxhp){
					local_player.hp +=  local_player.maxhp/20;
				}
				if (local_player.hp > local_player.maxhp){
					local_player.hp = local_player.maxhp;
				}
			}
			
			if (zone_delay == 0){
				
			}else if(zone_delay <= clock){
				zone_delay = 0;
				next_zone();
			}
			
			//************************************
			//                -W-
				if (_control.isConnected()) {
					if (npc_pos_clock+1000 <= clock){
						npc_pos_clock = clock;
						report_npcs();
					}
				}
			//                -W-
			//************************************
			
			//************************************
			//                -W-
				if (_control.isConnected() && _control.amInControl()) {
					if (clock_nextupdate+20000 <= clock){
						clock_nextupdate = clock;
						var msg :Object = new Object;
						msg[0] = room_num;
						msg[1] = _control.getMyId();
						msg[2] = clock
						_control.sendMessage (CLOCK_UPDATE, msg);
						current_mps+=1;
					}
				}
			//                -W-
			//************************************
		}
	
		
		//-------------------------------------ENTER ZONE----------------------------------------
		private function next_zone():void{
			if (gameover){
				endgame();
			}else{
				allclear = false;
				player_move(local_player, -150,(ground.y-ground.height/2), false);
				//************************************
				//                -W-
					if (_control.isConnected()) {
						var msg :Object = new Object;
						msg[0] = room_num;
						msg[1] = _control.getMyId();
						msg[2] = 100; //X
						msg[3] = (ground.y-ground.height/2); //Y
						msg[4] = local_player.sprinting;
						msg[5] = local_player.hp;
						msg[6] = local_player.energy;
						msg[7] = false;
						_control.sendMessage (p_move, msg);
						current_mps+=1;
				}
				//                -W-
				//************************************
				room_num += 1;
				
				hud.zone.text = hud.levelname.text+" - ZONE "+room_num;
				
				bg.bg_1.gotoAndStop(room_num);
				bg.bg_2.gotoAndStop(room_num);
				bg.bg_3.gotoAndStop(room_num);
				bg.bg_4.gotoAndStop(room_num);
				bg.bg_5.gotoAndStop(room_num);
				ground.gotoAndStop(room_num);
				
				bg.door_next.x = ground.width;
				bg.door_next.height = ground.height;
	
				load_npcs(room_num,wave_num);
				
				ground = game.camera._zoom.bg.ground;
				ground.doubleClickEnabled = true;
				ground.addEventListener(MouseEvent.CLICK, floorCLICK_handler);
	
				hud.fader.gotoAndPlay("in");
				enable_ai();
				enable_mouse();
				enable_keys();
				plot_goal(local_player, 100,(ground.y-ground.height/2), clock);
			}
		}
		
		//-------------------------------------EXIT ZONE-----------------------------------------
		private function exit_zone() :void{
			plot_goal(local_player, ground.width+500, (ground.y-ground.height/2), clock);
			//************************************
			//                -W-
				if (_control.isConnected()) {
					var msg :Object = new Object;
					msg[0] = room_num;
					msg[1] = _control.getMyId();
					msg[2] =  ground.width+500;
					msg[3] = (ground.y-ground.height/2);
					msg[4] = local_player.sprinting;
					msg[5] = local_player.hp;
					msg[6] = local_player.energy;
					msg[7] = local_player.character.scaleX;
					msg[8] = clock;
					_control.sendMessage (p_goal, msg);
					current_mps+=1;
			}
			//                -W-
			//************************************
			
			hud.fader.gotoAndPlay("out");
			clear_npcs();
			disable_ai();
			disable_mouse();
			disable_keys();
			
			zone_delay = clock+2000;
		}
		
		
		//-------------------------------------END GAME------------------------------------------
		private function endgame() :void{
				//var url:String = hud.escapePlanDelta.text;
				//var request:URLRequest = new URLRequest(url);
				//navigateToURL(request, "_self")
				
				_control.removeEventListener(KeyboardEvent.KEY_UP, keyReleased);
				_control.removeEventListener(KeyboardEvent.KEY_DOWN, keyPressed);
				hud.removeEventListener("enterFrame", hud_enterFrame);
				timer.removeEventListener( TimerEvent.TIMER, onTimerEvent);
				world_clock.removeEventListener( TimerEvent.TIMER, world_tick);
				poke_timer.removeEventListener( TimerEvent.TIMER, player_poke);
				ground.removeEventListener(MouseEvent.CLICK, floorCLICK_handler);
				hud.alpha = 0;
				
				endscreen = new _ENDSCREEN();
				root.addChild(endscreen);
				
				var checkout :String = "koCount";
				var book :Number = _control.get(checkout);
				var KO :Number = Number(book);
				var KO_b:Number = 5000-(1000*(KO));
				if (KO_b < 0){
					KO_b = 0;
				}
				endscreen.stats.pko.playerkos.text = KO+" (+"+KO_b+")";
				
				
				checkout = "playerDamage";
				book = _control.get(checkout);
				var PD :Number = Math.round(Number(book));
				
				checkout = "enemyDamage";
				book = _control.get(checkout);
				var ED :Number = Math.round(Number(book));
				var D_b:Number = Math.round(ED-(PD*2));
				if(D_b < 0){
					D_b = 0;
				}
				
				endscreen.stats.dmg.enemydamage.text = "+"+D_b;

				endscreen.stats.ct.cleartime.text = hud.time.text;
				endscreen.stats.sb.bonusscore.text = "+"+Math.round(local_score);
				
				checkout = "totalMobHP";
				book = _control.get(checkout);
				var totalscore :Number = D_b+KO_b+Math.round(local_score);
				var missionpar :Number = ED+5000;
				var grade :Number = (totalscore/missionpar)*100;
				
				var rank :String;
				if(grade > 100){
					rank = "S";
				}else if(grade > 90){
					rank = "A";
				}else if(grade > 80){
					rank = "B";
				}else if(grade > 70){
					rank = "C";
				}else if(grade > 60){
					rank = "D";
				}else{
					rank = "F";
				}
				
				endscreen.stats.r.rank.text = rank+" ("+Math.round(grade)+"%)";
				
				
				if (_control.isConnected()) {
					if(grade > 99){
						grade = 99;
					}
					var flow :Number = Math.round(_control.getAvailableFlow()*(grade/100));
					endscreen.stats.f.flow.text = flow;
					_control.awardFlow(flow);
				}
				
				root.removeChild(game);
		}
		//---------------------------------------------------------------------------------------
		
		
		//-------------------------------------KEY UP--------------------------------------------
		private function keyReleased(evt:KeyboardEvent):void{
			if (keys_active){
				if (evt.keyCode == punch_code || evt.charCode == punch_code_alt){
					punch_hit = false;
				}
				if (evt.keyCode == kick_code || evt.charCode == kick_code_alt){
					kick_hit = false;
				}
				if (evt.keyCode == block_code || evt.charCode == block_code_alt){
					player_block_stop(local_player);
				}
				if (evt.keyCode == sprint_code || evt.charCode == sprint_code_alt){
					sprint_hit = false;
				}
			}
		}
		
		//-------------------------------------KEY DOWN-----------------------------------------
		private function keyPressed(evt:KeyboardEvent):void{
			if (keys_active){
				if (evt.keyCode == punch_code || evt.charCode == punch_code_alt){
					if (punch_hit == false){
							punch_hit = true;
							player_punch(local_player);
					}
				}
				if (evt.keyCode == kick_code || evt.charCode == kick_code_alt){
					if (kick_hit == false){
							kick_hit = true;
							player_kick(local_player);
					}
				}
				if (evt.keyCode == block_code || evt.charCode == block_code_alt){
					player_block(local_player);
				}
				if (evt.keyCode == sprint_code || evt.charCode == sprint_code_alt){
					if (sprint_hit == false){
							sprint_hit = true;
					}
				}
			}
		}
		
		//-------------------------------------UPDATE HUD----------------------------------------
		private function hud_enterFrame(e:Event){
			//---Keyboard Listeners

			//---------------------
			current_fps++;
			
			var clock_seconds :Number = Math.round(clock/1000);
			var minutes :Number = Math.floor(clock_seconds/60);
			var seconds :String = (clock_seconds-(minutes*60));
			if((clock_seconds-(minutes*60)) < 10){
				seconds = "0"+(clock_seconds-(minutes*60));
			}
			hud.time.text = minutes+"'"+seconds+"''";
			
			//---STAT BAR---
			hud.stats.hpnum.text = Math.round(local_player.hp);
			var hpper :Number = Math.round(local_player.hp/local_player.maxhp*100)+1;
			if (hud.stats.hp.bar.currentFrame >= hpper){
				hud.stats.hp.bar.gotoAndStop(hpper);
			}else if(hud.stats.hp.bar.currentFrame < hpper){
				hud.stats.hp.bar.nextFrame();
			}
			if (hpper < hud.stats.hp.dmg.currentFrame){
				hud.stats.hp.dmg.prevFrame();
				hud.stats.hp.gotoAndStop(1);
				//hud.stats.hp.dmg.stop();
			}else{
				hud.stats.hp.dmg.gotoAndStop(hpper);
				hud.stats.hp.gotoAndStop(2);
			}
			
			if(hpper <= 20){
				hud.hp_warning.gotoAndStop("on");
			}else{
				hud.hp_warning.gotoAndStop("off");
			}
			
			if (local_exp > 300){
				local_exp = 300;
			}else if (local_exp < 0){
				local_exp = 0;
			}
			
			if (local_player.hp <= 0){
				local_exp = 0;
				local_player.energy = 0;
			}
			
			if (hud.stats.exp.currentFrame < Math.round(local_exp)+1){
				hud.stats.exp.nextFrame();
			} else if (hud.stats.exp.currentFrame > Math.round(local_exp)+1){
				hud.stats.exp.prevFrame();
			}
			//hud.stats.exp.gotoAndStop(Math.round(local_exp)+1);
			
			if (local_exp < 100){
				local_level = 1;
			}else if (local_exp < 200){
				local_level = 2;
			}else if (local_exp < 300){
				local_level = 3;
			}else{
				local_level = 4;
			}
			
			hud.score.text = Math.round(local_score);
			
			if (local_player.energy > 100){
				local_player.energy = 100;
			}
			if (local_player.energy < 0){
				local_player.energy = 0;
			}
			
			if (local_limiter){
				hud.energy_warning.gotoAndStop("on");
				if(local_player.energy >= 99){
					local_limiter = false;   
				}
			}else{
				hud.energy_warning.gotoAndStop("off");
				if(local_player.energy <= 0){
					local_limiter = true;   
				}
			}
			
			
			var engper :int = Math.round(local_player.energy/100*100)+1;
			
			if (local_limiter){
				var neg_engper :int = engper+101;
				hud.stats.energy.gotoAndStop(neg_engper);
			}else{
				hud.stats.energy.gotoAndStop(engper);
			}
			hud.stats.energy.num.text = String(engper-1)+"%";
			//--------------
			
			var pos:Point = new Point(local_player.x, local_player.y);
			pos = localToGlobal(pos);
			var cam_x:Number = game.camera.x;
			var cam_goal: Number = (pos.x-world_width/2)*-1;
			
			cam_x = (cam_x-cam_goal)*0.333;

			game.camera.x -= cam_x;
			
			//Edge of World Checks
			var bg1_w:Number = bg.bg_1.width-world_width;
			if (game.camera.x < bg1_w*(-1)){game.camera.x = bg1_w*(-1);}
			if (game.camera.x > 0){game.camera.x = 0;}
			
			if (local_player.hp <= 0){
				hud.hp_warning.gotoAndStop("off");
				hud.energy_warning.gotoAndStop("off");
			}
			
			update_attackbar();
			update_bg();
			update_cursor();
			
			//Check For the player to enter the next zone.
			if (bg.door_next.hitTestObject(local_player.boundbox) && allclear){//npc_killcount == npc_count){
				bg.door_next.height = 0.01;
				
				//************************************
				// 
				if (_control.isConnected()) {
					var msg :Object = new Object;
					msg[0] = room_num;
					msg[1] = _control.getMyId();
					_control.sendMessage (GTNR, msg);
					current_mps+=1;
				}
				//
				//************************************ 
			}
			
			
			if (npc_killcount >= npc_count && allclear != true){
				npc_killcount = 0;
				wave_num ++;
				load_npcs(room_num,wave_num);
			}
			
			if (allclear){
				hud.go.alpha = 1;
				wave_num = 1;
			}else{
				hud.go.alpha = 0;
			}
			
			if (boss_killed){
				allclear = true;
				clear_npcs();
			}
			
			if (gotonextroom){
				gotonextroom = false;
				if(boss_killed){
					gameover = true;
				}
				exit_zone();
			}
			
			
		}
		
		//-------------------------------------UPDATE ATTACK BAR---------------------------------
		private function update_attackbar() :void{
			if(local_exp > 299){
				pk_status = "on";
			}else{
				pk_status = "off";
			}
			
			if(last_attack+2000 <= clock){
				current_attack = 0;
			}
			
			if(current_attack > 6){
				current_attack = 0;
			}
			
			p1_status = "off";
			p2_status = "off";
			p3_status = "off";
			p4_status = "off";
			p5_status = "off";
			p6_status = "off";
			k1_status = "off";
			k2_status = "off";
			k3_status = "off";
			
			switch (current_attack) {
					case 0:
						p1_status = "next";
					break;
					
					case 1:
						p1_status = "on";
						p2_status = "next";
						k1_status = "next";
					break;
					
					case 2:
						p1_status = "on";
						p2_status = "on";
						p3_status = "next";
						k1_status = "next";
					break;
					
					case 3:
						p1_status = "on";
						p2_status = "on";
						p3_status = "on";
						p4_status = "next";
						k1_status = "on";
						k2_status = "next";
					break;
					
					case 4:
						p1_status = "on";
						p2_status = "on";
						p3_status = "on";
						p4_status = "on";
						p5_status = "next";
						k1_status = "on";
						k2_status = "next";
					break;
					
					case 5:
						p1_status = "on";
						p2_status = "on";
						p3_status = "on";
						p4_status = "on";
						p5_status = "on";
						p6_status = "next";
						k1_status = "on";
						k2_status = "on";
						k3_status = "next";
					break;
					
					case 6:
						p1_status = "on";
						p2_status = "on";
						p3_status = "on";
						p4_status = "on";
						p5_status = "on";
						p6_status = "on";
						k1_status = "on";
						k2_status = "on";
						k3_status = "next";
					break;
			}
			
			hud.attacks.p1.gotoAndStop(p1_status);
			hud.attacks.p2.gotoAndStop(p2_status);
			hud.attacks.p3.gotoAndStop(p3_status);
			hud.attacks.p4.gotoAndStop(p4_status);
			hud.attacks.p5.gotoAndStop(p5_status);
			hud.attacks.p6.gotoAndStop(p6_status);
			
			hud.attacks.k1.gotoAndStop(k1_status);
			hud.attacks.k2.gotoAndStop(k2_status);
			hud.attacks.k3.gotoAndStop(k3_status);
			
			hud.attacks.pk.gotoAndPlay(pk_status);
		}
		
		//-------------------------------------UPDATE BG-----------------------------------------
		private function update_bg() :void{
			var cam: MovieClip = game.camera;
			var bg1: MovieClip = bg.bg_1;
			var bg2: MovieClip = bg.bg_2;
			var bg3: MovieClip = bg.bg_3;
			var bg4: MovieClip = bg.bg_4;
			var bg5: MovieClip = bg.bg_5;
			
			var bg1_w:Number = bg1.width-world_width;
			var bg2_w:Number = bg2.width-world_width;
			var bg3_w:Number = bg3.width-world_width;
			var bg4_w:Number = bg4.width-world_width;
			var bg5_w:Number = bg5.width-world_width;
			
			var bg2_r:Number = bg2_w/bg1_w;
			var bg3_r:Number = bg3_w/bg1_w;
			var bg4_r:Number = bg4_w/bg1_w;
			var bg5_r:Number = bg5_w/bg1_w;
			
			var pos:Number = (cam.x)/bg1_w;
			
			bg2.x = bg2_w*pos+(cam.x*-1);
			bg3.x = bg3_w*pos+(cam.x*-1);
			bg4.x = bg4_w*pos+(cam.x*-1);
			bg5.x = bg5_w*pos+(cam.x*-1);
			
			if (local_player.moving == false){
				if (goal_hide == false){
					goal.gotoAndPlay("off");
					goal_hide = true;
				}
			}
			
		}
		
		//-------------------------------------ADD SCORE-------------------------------------
		private function increase_score(n:Number) :void{
			local_score += n;
			hud.score_add.temp_score += n;
			hud.score_add.score_add.score_add.text = "+"+Math.round(hud.score_add.temp_score);
			if(hud.score_add.currentFrame < 30){
				hud.score_add.gotoAndPlay(5);
			}else{
				hud.score_add.gotoAndPlay("go");
			}
		}
		 
		//-------------------------------------ADD HIT-------------------------------------
		private function increase_hit(n:int = 1) :void{
			hud.hitcounter.temp_count += n;
			hud.hitcounter.num.hits.text = hud.hitcounter.temp_count;
			hud.hitcounter.gotoAndPlay("go");
		}
		
		//-------------------------------------UPDATE CURSOR-------------------------------------
		private function update_cursor() :void{
			var mpos: Point = new Point( root.mouseX,root.mouseY);
			if (ground.hitTestPoint(mpos.x,mpos.y)){
				//Mouse.hide();
				if (ground_cursor_hide){
					ground_cursor_hide = false;
					ground_cursor.gotoAndPlay("on");
				}
				mpos = bg.globalToLocal(mpos);
				ground_cursor.x = mpos.x;
				ground_cursor.y = mpos.y;
				player_scale(ground_cursor);
				
			}else{
				//Mouse.show();
				if (ground_cursor_hide == false){
					ground_cursor_hide = true;
					ground_cursor.gotoAndPlay("off");
				}
			}
		}
		
		//-------------------------------------NEW PLAYER----------------------------------------
		protected function create_player(n: String ="Unknown", t: String ="NPC", sX: Number =0, sY: Number =0,
										 hp: Number =100, spd: Number=1, rng: Number=100, fst: Number=2000, 
										 min: Number=1, max: Number=10, kb: Number = 0, stn: Number=0,
										 id: Number=0, wave: Number=1) :MovieClip
    	{
			var mc: player = new player();
			mc.pName = n;
			mc.flag = t;
			mc.id = id;
			
			if (mc.flag == "PC"){
				mc.name_plate.name_plate.text = mc.pName;
				mc.name = id;
				pc_count += 1;
				mc.lastupdate = clock;
			} else if (mc.flag == "NPC"){
				mc.name_plate.name_plate.text = "";
				mc.name_plate.name_plate.textColor = 0xCC0000;
				mc.name = "npc_"+npc_count;
				mc.id = npc_count;
				mc.wave = wave;
				npc_count ++;
				
				mc.ai_min = min;
				mc.ai_max = max;
				mc.ai_knockback = kb;
				mc.ai_stun = stn;
				
				if (n == "BOSS" && mc.flag == "NPC"){
					mc.removeChild(mc.character);
					mc.character = new kosmos();
					mc.addChild(mc.character);
				}
			}
			
			if (sX == 0&& sY == 0){
				sX = 100;
				sY = (ground.y-ground.height/2);
			}
			
			if (mc.flag == "PC"){
				mc.blip = new blip_pc();
			}else{
				mc.blip = new blip_npc();
			}
			hud.radar.view.addChild(mc.blip);
			mc.blip.x = (mc.x/ground.width)*200;
			mc.blip.y = 0;//(mc.y/505)*200;
			
			mc.spawn_x = sX;
			mc.spawn_y = sY;
			
			mc.ai = 1;
			mc.ai2 = 1;
			mc.ai_mode = "idle";
			mc.ai_chase = 1500;
			mc.ai_range = 1500;
			mc.ai_attrng = rng;
			mc.ai_cooldown = fst;
			mc.ai_tick = clock+mc.ai_cooldown;
			mc.ai_report = clock;
			
			mc.spd = spd;
			if (mc.spd < 0){
				mc.spd = 0;
			}
			mc.hp = hp;
			mc.maxhp = hp;
			
			mc.energy = 100;
			
			mc.y = sY;
			mc.x = sX;
			
			mc.old_x = sX;
			mc.old_y = sY;
			mc.new_x = sX;
			mc.new_y = sY;
			
			mc.goal_x = mc.x;
			mc.goal_y = mc.y;
			mc.start_x = mc.x;
			mc.start_y = mc.y;
			mc.move_time = 0;
			mc.move_distance = 0;
			
			mc.addEventListener("enterFrame", player_enterFrame);
			player_scale(mc);
			
			if (mc.pName == "Jessica" || mc.pName == "Cherub"){
				//mc.hp = 3200;
				//mc.maxhp = 3200;
				mc.removeChild(mc.character);
				//mc.character = null;
				mc.character = new kosmos();
				mc.addChild(mc.character);
			}
			
			bg.actors.addChild(mc);
			
			mc.animation = "spawn";
			mc.character.gotoAndPlay("spawn");
			
			return mc
    	}
		
		//-------------------------------------UPDATE PLAYER------------------------------------
		private function player_enterFrame(e:Event){
			if (_control.isConnected() && _control.amInControl()){
				if (e.target.lastupdate+10000 < clock){
					var msgg :Object = new Object;
					msgg[0] = room_num;
					msgg[1] = e.target.name;
					_control.sendMessage (p_quit, msgg);
					current_mps+=1;
				}
			}
			
			if (e.target.flag == "NPC"){
				if (_control.isConnected()){
					var element :String = "r"+String(room_num) + "_m"+String(e.target.id) + "_w"+String(e.target.wave);
					var table :Number = _control.get(element);
					if (table == 0){
						table = e.target.maxhp;
					}
					e.target.hp = Number(table);
					
					//e.target.name_plate.name_plate.text = "HP: "+Math.round(e.target.hp);
				}
			}
			
			if (e.target.hp <= 0){
				e.target.ai2 = e.target.ai2*(-1);
				if (e.target.ai2 == 1){
					e.target.ai = e.target.ai*(-1);
					if (e.target.ai == 1){
						e.target.character.alpha = 0;
					}else{
						e.target.character.alpha = 1;
					}
				}
				if(e.target.ai_tick <= clock){
					e.target.character.alpha = 0;
					if(e.target.flag == "NPC"){
						var temptargeth :MovieClip = bg.actors.getChildByName(e.target.ai_target);
						if(temptargeth == local_player && Math.random()*100 > 90){
							//************************************
							//                -W-
							if (_control.isConnected()) {
								var msgh :Object = new Object;
								msgh[0] = room_num;
								msgh[1] = _control.getMyId();
								msgh[2] = e.target.x;
								msgh[3] = e.target.y;
								msgh[4] = "health";
								msgh[5] = 1;
								msgh[6] = (Math.random()*30)+(-15);
								msgh[7] = (Math.random()*5)+(-2.5);
								itemcount += 1;
								msgh[8] = itemcount;
								_control.sendMessage (s_item, msgh);
								current_mps+=1;
							}
							//                -W-
							//************************************
						}
						delete_npc(e.target);
					}
				}
			}
			
			var msg :Object = new Object;
			if (e.target.hp > 0){
				//---SLIDE CODE--- MUST BE AT START ---
				e.target.new_x = e.target.x;
				e.target.new_y = e.target.y;
				e.target.new_z = e.target.z;
				//-----------------------------------
				
				//----------------------
				//---Moving to a  new location
				var lp_current: Point = new Point(e.target.x,e.target.y);
				var lp_start: Point = new Point(e.target.start_x,e.target.start_y);
				var lp_goal: Point = new Point(e.target.goal_x,e.target.goal_y);
				var lp_distance: Number = Point.distance(lp_current,lp_goal);
				
				if (e.target.moving){
					if(e.target.sprinting){
						e.target.move_O = e.target.move_A + 1000*(e.target.move_distance/((e.target.spd*e.target.scaleX)*2));
					}else{
						e.target.move_O = e.target.move_A + 1000*(e.target.move_distance/(e.target.spd*e.target.scaleX));
					}
					if (e.target.move_O > clock){
						e.target.move_time = (clock-e.target.move_A)/(e.target.move_O-e.target.move_A);
						lp_current = Point.interpolate(lp_goal,lp_start,e.target.move_time);
					} else {
						e.target.moving = false;
						if(e.target.sprinting){
							e.target.sprinting = false;
							e.target.sliding = true;
						}
					}
					e.target.x = lp_current.x;
					e.target.y = lp_current.y;
				} else {
					e.target.move_time = 0;
					e.target.move_distance = 0;
					e.target.start_x = e.target.goal_x;
					e.target.start_y = e.target.goal_y;
					e.target.move_A = 0;
					e.target.move_O = 0;
					if (e.target.sliding){
						e.target.goal_x = e.target.x;
						e.target.goal_y = e.target.y;
					}else{
						e.target.x = e.target.goal_x;
						e.target.y = e.target.goal_y;
					}
					e.target.sprinting = false;
				}
				//----------------------
				
				
				//---STUN!---
				if (e.target.stun){
					if (e.target.stun_counter <= clock){
						e.target.stun = false;
					}
					e.target.effects.gotoAndStop("stunned");
				}else{
					if (e.target.animation == "stun"){
						e.target.animation = "hurt";
					}
					e.target.effects.gotoAndStop("normal");
				}
				//-----------
				
				//---SLIDE!---
				if (e.target.sliding){
					if (ground.hitTestObject(e.target.dot)){
					}else{
						e.target.sliding = false;
					}
				}
				
				if (e.target.sliding){
					var xslide = (e.target.new_x-e.target.old_x)*0.90;
					var yslide = (e.target.new_y-e.target.old_y)*0.90;
					if (e.target.moving == false){
						if ((xslide > 0.5 || xslide < -0.5) || (yslide > 0.5 || yslide < -0.5)){
							e.target.x += xslide;
							e.target.y += yslide;
							if(e.target.knockback != true && xslide > e.target.scaleX && e.target.dustcount <= clock){
								e.target.dustcount = clock+250;
								var tempdust :MovieClip = new slide_dust();
								bg.addChild(tempdust);
								tempdust.x = e.target.x;
								tempdust.y = e.target.y;
								player_scale(tempdust);
							}
						}else{
							e.target.sliding = false;
						}
					}else{
						e.target.sliding = false;
					}
				}else if(e.target.sliding == false && e.target.sprinting == false){
					e.target.old_x = e.target.x;
					e.target.old_y = e.target.y;
					e.target.new_x = e.target.x;
					e.target.new_y = e.target.y;
				}
				
				if (e.target.sliding == false){
					e.target.knockback = false;
					if (e.target.animation == "knockback"){
						e.target.animation = "hurt";
					}
				}
				//------------
				
				
				//----------------------
				//---NPC AI Detection
				if (e.target.ai == 1){
					if (e.target.flag == "NPC" && ai_active){
						player_ai(e.target);
					}
				}
				e.target.ai = e.target.ai*(-1);
				//----------------------
				
				
				player_scale(e.target);
				player_depth(e.target);
				
				
				//----------------------
				//---Name Plate Scaling
				var nScale:Number = 1+(1-((e.target.scaleX-0.5)/0.5));
				e.target.name_plate.name_plate.scaleX = nScale;
				e.target.name_plate.name_plate.scaleY = nScale;
				e.target.name_plate.name_plate.x = -1*e.target.name_plate.width/2;
				//----------------------
				
				
				//----------------------
				//---Direction Detection
				if(e.target.flag == "NPC" && e.target.ai_target){
					if(bg.actors.getChildByName(e.target.ai_target)){
						var temptarget :MovieClip = bg.actors.getChildByName(e.target.ai_target);
						if (e.target.x < temptarget.x){
							e.target.character.scaleX = 1; //Face Right
							e.target.dmgbox.scaleX = 1;
						}else{
							e.target.character.scaleX = -1; //face Left
							e.target.dmgbox.scaleX = -1;
						}
					}
				} else if (e.target.moving){
					if (lp_start.x < lp_goal.x){
						e.target.character.scaleX = 1; //Face Right
						e.target.dmgbox.scaleX = 1;
					}else{
						e.target.character.scaleX = -1; //Face Left
						e.target.dmgbox.scaleX = -1;
					}
				}
				
				
				e.target.last_x = e.target.x;
				
				if (e.target == local_player){
					if (local_player.moving == false && local_player.hp > 0 && local_player.animation == "idle"){
						if (local_player.x < ground_cursor.x){
							local_player.character.scaleX = 1; //Face Right
							local_player.dmgbox.scaleX = 1;
						}else{
							local_player.character.scaleX = -1; //face Left
							local_player.dmgbox.scaleX = -1;
						}
					}
				}
				//----------------------
				
				
				
				
				
				
				//----------------------
				//---RADAR
				e.target.blip.x = (e.target.x/ground.width)*200;
				e.target.blip.y = 0;//(e.target.y/505)*25;
				//----------------------
				
				
				
				
				
				//----------------------
				//---Animation Detection
				if (e.target.knockback){
					e.target.animation = "knockback";
				}else if(e.target.stun){
					e.target.animation = "stun";
				}
				
				if (e.target.character.currentFrame < 10 && e.target.animation_old != "idle"){
					e.target.animation = "idle";
					e.target.animation_old = "idle";
				}
				
				if (e.target.animation == "hurt"){
				}else if (e.target.animation == "spawn"){
				}else if (e.target.animation == "knockback"){
				}else if (e.target.animation == "stun"){
				}else if (e.target.animation == "punch"){
				}else if (e.target.animation == "kick"){
				}else if (e.target.animation == "block"){
				}else{
					if (e.target.moving){
						e.target.animation = "walk";
					} else {
						e.target.animation = "idle";
					}
				}
	
				if (e.target.animation != e.target.animation_old){
					e.target.character.gotoAndPlay(e.target.animation);
					
					if (e.target == local_player){
						if (e.target.animation == "punch"){
							e.target.dmgbox.gotoAndPlay(e.target.animation);
						}else if (e.target.animation == "kick"){
							e.target.dmgbox.gotoAndPlay(e.target.animation);
						}
					}else if(e.target.flag != "PC"){
						if (e.target.animation == "punch"){
							e.target.dmgbox.gotoAndPlay(e.target.animation);
						}else if (e.target.animation == "kick"){
							e.target.dmgbox.gotoAndPlay(e.target.animation);
						}
					}
				}
				
				e.target.animation_old = e.target.animation;
				//e.target.name_plate.text = e.target.hp;
				//----------------------
				
				
				//----------------------
				//---DAMAGE!------------
				var dmg :Number = 0;
				var slidebonus :Number;
				var knock_amount :Number;
				var stun_amount :Number;
				var t:Number;
				var n:Number;
				var mov:MovieClip;
				
				
				//----FLYING BACKWARDS COLLISSION---
				if (e.target.knockback && e.target.ai_target == local_player.name){
						n = 0;
						dmg = 0;
						if (npc_count){
							t = npc_count;
							while(n <= t){
								if (bg.actors.getChildByName("npc_"+n) && bg.actors.getChildByName("npc_"+n) != e.target && xslide > 1){
									mov = bg.actors.getChildByName("npc_"+n);
									if (mov.knockback == false){
										if (mov.hp > 0){
											if (e.target.boundbox.hitTestObject(mov.boundbox)){
												if (mov.animation != "hurt"){
													dmg = (e.target.maxhp/4);
													player_hurt(bg.actors.getChildByName("npc_"+n),dmg, String(e.target.ai_target), xslide, 0);
													//************************************
													//                -W-
													if (_control.isConnected()) {
														msg[0] = room_num;
														msg[1] = _control.getMyId();
														msg[2] = n;
														msg[3] = dmg;
														msg[4] = xslide;
														msg[5] = 0;
														msg[6] = wave_num;
														msg[7] = 0;
														_control.sendMessage (p_hurt, msg);
														current_mps+=1;
													}
													//                -W-
													//************************************
												}
											}
										}
									}
								}
								n++;
							}
						}
				}
				//----------------------------------
				
				
				if (e.target.animation == "punch" || e.target.animation == "kick"){
					if (e.target.flag == "PC" && e.target == local_player){
						n = 0;
						dmg = 0;
						if (npc_count){
							t = npc_count;
							while(n <= t){
								if (bg.actors.getChildByName("npc_"+n)){
									mov = bg.actors.getChildByName("npc_"+n);
									if (mov.hp > 0){
										if (e.target.dmgbox.hitTestObject(mov.boundbox)){
											//If it hits dood, hurt dood.
											if (mov.animation != "hurt"){
												dmg = ((Math.random()*50)+10)*(local_level*0.5);
												if (e.target.animation == "kick"){
													knock_amount = 15;
												}
												if (e.target.animation == "punch"){
													//stun_amount = 1.5;
													knock_amount = 0;
												}
												if (e.target.sliding == true){
													knock_amount += xslide*2;
												}
												//local_exp += (dmg/5)/(local_level*0.5);
												var coinrnd :Number = Math.random()*100
												if(coinrnd > 75){
															coinrnd = 1;
												}else{
															coinrnd = 0;
												}
												increase_hit();
												increase_score(((dmg/10)*local_level)*hud.hitcounter.temp_count);
												player_hurt(bg.actors.getChildByName("npc_"+n),dmg, String(e.target.name), knock_amount, stun_amount, coinrnd);
												if (e.target == local_player){
													//************************************
													//                -W-
													if (_control.isConnected()) {
														msg[0] = room_num;
														msg[1] = _control.getMyId();
														msg[2] = n;
														msg[3] = dmg;
														msg[4] = knock_amount;
														msg[5] = stun_amount;
														msg[6] = wave_num;
														msg[7] = coinrnd;
														_control.sendMessage (p_hurt, msg);
														current_mps+=1;
													}
													//                -W-
													//************************************
												}
											}
										}
									}
								}
								n++;
							}
						}
					} else if (e.target.flag != "PC"){
						dmg = 0;
							if (local_player.hp > 0){
								if (e.target.dmgbox.hitTestObject(local_player.boundbox)){
									if (local_player.animation != "hurt"){
										dmg = e.target.ai_min+Math.random()*(e.target.ai_max-e.target.ai_min);
										player_hurt(local_player, dmg, String(e.target.name), e.target.ai_knockback, e.target.ai_stun);
										//************************************
										//                -W-
										if (_control.isConnected()) {
											msg[0] = room_num;
											msg[1] = _control.getMyId();
											msg[2] = "X";
											msg[3] = dmg;
											msg[4] = 0;
											msg[5] = 0;
											_control.sendMessage (n_hurt, msg);
											current_mps+=1;
										}
										//                -W-
										//************************************
									}
								}
							}
					}
				}
				//----------------------
			} else {
				e.target.animation = "dead";
				if (e.target.animation != e.target.animation_old){
					e.target.character.gotoAndPlay(e.target.animation);
					if(e.target.flag != "PC"){
						npc_killcount += 1;
						if(e.target.pName == "BOSS"){
							boss_killed = true;
						}
						
					}else if(e.target.flag == "PC"){
						if(e.target == local_player){
							var elementt :String = "koCount";
							var tablee :Number = _control.get(elementt);
								if (tablee == 0) {
									tablee = 1;
									_control.set(elementt, tablee);
								}else{
									table += 1;
									_control.set(elementt, tablee);
								}
						}
					}
					e.target.ai_tick = clock+750;
				}
			
				e.target.animation_old = e.target.animation;
			}
			
			//---SLIDE CODE--- MUST BE AT END ---
			e.target.old_x = e.target.new_x;
			e.target.old_y = e.target.new_y;
			e.target.old_z = e.target.new_z;
			//-----------------------------------
			
		}
		
		//-------------------------------------NPlayer AI---------------------------------------
		private function player_ai(mc:MovieClip){
			var msg :Object = new Object;

			if (mc.knockback || mc.stun || mc.animation == "hurt" || mc.animation == "dead"){
			}else{
				if (_control.isConnected()) {
					var pc :Array = _control.getOccupants();
					var temp_dis :Number = 0;
					var mypos :Point = new Point(mc.x,mc.y);
					var glpos :Point = new Point(mc.spawn_x, mc.spawn_y);
					var dist :Number;
					var trpos :Point;
					var player_target :MovieClip;
					
					switch (mc.ai_mode) {
						case "idle":
							//Scan for player distances
							var n :Number = 0;
							var t :Number = pc.length;
							
							while(n<t){
								if (bg.actors.getChildByName(String(pc[n]))){
									player_target = bg.actors.getChildByName(String(pc[n]));
									trpos = new Point(player_target.x,player_target.y);
									temp_dis = Point.distance(mypos,trpos);
									if (temp_dis <= mc.ai_range && player_target.hp > 0){ //Is it within target range
										if (dist){
										}else{
											dist = temp_dis+10;
										}
										if (temp_dis <= dist){ //Target closest in range
											dist = temp_dis;
											mc.ai_target = String(player_target.id);
										}
									}
								}
								n++;
							}
							//Switch to Active if target found
							if (mc.ai_target != ""){
								mc.ai_mode = "active";
							}else{
								
							}
							break;
							
						case "flee":
							dist = Point.distance(mypos,glpos);
							if (dist > mc.spd){
								plot_goal(mc, mc.spawn_x, mc.spawn_y, clock);
							}else{
								mc.ai_mode = "idle";
								mc.ai_target = "";
							}
							break;
						
						case "active":
							//Follow Target
							//if (mc.hp < mc.maxhp*0.1){
								//mc.ai_mode = "flee";
								//mc.ai_target = "";
								//_control.localChat("NPC_"+mc.id+": Low on health; breaking off!");
								//break;
							//}
							
							if (mc.ai_target != ""){
								if (bg.actors.getChildByName(mc.ai_target)){ //If target exsists...
									player_target = bg.actors.getChildByName(mc.ai_target);
										
									if (player_target.hp <= 0){ //Target dead, Breaking off.
										mc.ai_mode = "flee";
										mc.ai_target = "";
									}else{
										trpos = new Point(player_target.x,player_target.y);
										
										temp_dis = Point.distance(mypos,trpos);
										var spawn_dis :Number = Point.distance(mypos,glpos);
										
										//if (spawn_dis > mc.ai_chase ){ //Too far, break off.
											//mc.ai_mode = "flee";
											//mc.ai_target = "";
											//_control.localChat("NPC_"+mc.id+": Target too far away; breaking off.");
										//}else{
											if (mc.ai_tick <= clock){
												//---ATTACK MODE---
												if( mc.x > player_target.x){
														if(player_target == local_player){
															plot_goal(mc, player_target.x+mc.ai_attrng*mc.scaleX-mc.spd, player_target.y, clock);
														}
												}else{
														if(player_target == local_player){
															plot_goal(mc, player_target.x-mc.ai_attrng*mc.scaleX+mc.spd, player_target.y, clock);
														}
												}
												if(temp_dis <= mc.ai_attrng*mc.scaleX && (mc.y <= player_target.y+5 && mc.y >= player_target.y-5)){
													//attack!
													if(player_target == local_player){
														player_punch(mc);
													} 
													
												}
											}else{
												//---EVASIVE MODE---
												
												if (mc.moving != true && (temp_dis < mc.ai_range || temp_dis > mc.ai_range*1.5 )){
													//var seedrnd1 :Number = (mc.hp/mc.maxhp)-(player_target.hp/player_target.maxhp)*(clock/mc.ai_tick);
													//var seedrnd2 :Number = (mc.hp/mc.maxhp)*(player_target.hp/player_target.maxhp)+(clock/mc.ai_tick);
													var rnd1 :Random = new Random(mc.hp+mc.ai_tick);
													var desx :Number = (rnd1.nextNumber()*mc.ai_range)+(mc.ai_range/4);
													var flipcoin :Boolean = rnd1.nextBoolean();
													if (flipcoin){
														desx = desx*(-1);
													}
													var desy :Number = rnd1.nextNumber()*125;
													if (player_target.scaleX > 0.75){
														desy = desy*(-1);
													}
													
													var des :Point = new Point(player_target.x+desx, player_target.y+desy);
														if(player_target == local_player){
															plot_goal(mc, des.x, des.y, clock);
														}
													
												}
											}
										//}
									}
								}else{ //Can't find target, break off.
									mc.ai_mode = "flee";
									mc.ai_target = "";
									//_control.localChat("NPC_"+mc.id+": Can't find target; breaking off.");
								}
							}else{ //No target, break off.
								mc.ai_mode = "flee";
								mc.ai_target = "";
								//_control.localChat("NPC_"+mc.id+": Target is null; breaking off.");
							}
							break;
					}
				}
				
				if (mc.moving ){
				}else{
					var max_Y: Number = ground.y;
					var min_Y: Number = ground.y-ground.height;
						
					var max_X: Number = ground.width;
					var min_X: Number = 0;
						
					var pX: Number = min_X+(max_X*((Math.random()*100)/100));
					var pY: Number = min_Y+(max_Y*((Math.random()*100)/100));
					
					if (_control.isConnected()){
					}else{
						if (ground.hitTestPoint(pX,pY)){
							plot_goal(mc, pX, pY, clock);
						}
					}
					
				}
			}
		}
		
		//-------------------------------------PLAYER Z-SCALE------------------------------------
		private function player_scale(e:MovieClip){
			var ground_max: Number = ground.y;
			var ground_min: Number = ground.y-ground.height;
			ground_max = ground_max - ground_min;
			var size: Number = e.y-ground_min;
			size = ((size/ground_max)*0.5)+0.5
			e.scaleX = size;
			e.scaleY = size;
		}
		
		//-------------------------------------PLAYER Z-DEPTH-----------------------------------
		private function player_depth(e:MovieClip){
			var depth: int = ((e.scaleX-0.5)/0.5)*e.parent.numChildren;
			var cur_depth: int = e.parent.getChildIndex(e);
			
			if (depth > e.parent.numChildren-0.01){
				depth = e.parent.numChildren-0.01;
			}
			if (depth < 0){
				depth = 0;
			}
			
			var big_d :int;
			var lil_d :int;
			if (depth > cur_depth){
				big_d = depth;
				lil_d = cur_depth;
			}else{
				big_d = cur_depth;
				lil_d = depth;
			}
			
			var moo :MovieClip = e.parent.getChildAt(depth);
			if (moo){
				if (e.scaleX > moo.scaleX){
					e.parent.setChildIndex(e,big_d);
					e.parent.setChildIndex(moo,lil_d);
				}else{
					e.parent.setChildIndex(moo,big_d);
					e.parent.setChildIndex(e,lil_d);
				}
			}else{
				e.parent.setChildIndex(e,depth);
			}
		}
		
		//-------------------------------------RESPAWN PLAYER-------------------------------------
    	protected function player_respawn_local () :void
    	{
			var msg :Object
			local_player.hp = local_player.maxhp;
			local_player.energy = 100;
			local_player.knockback = false;
			local_player.stun = false;
			local_player.character.alpha = 1;
			
			player_move(local_player, 100,(ground.y-ground.height/2),true);
			//************************************
			//                -W-
				if (_control.isConnected()) {
					msg = new Object;
					msg[0] = room_num;
					msg[1] = _control.getMyId();
					msg[2] = 100; //X
					msg[3] = (ground.y-ground.height/2); //Y
					msg[4] = local_player.sprinting;
					msg[5] = local_player.hp;
					msg[6] = local_player.energy;
					msg[7] = true;
					_control.sendMessage (p_move, msg);
					current_mps+=1;
			}
			//                -W-
			//************************************
			
			plot_goal(local_player, 100,(ground.y-ground.height/2), clock);
			enable_mouse();
			enable_keys();
    	}
		
		//-------------------------------------MOVE PLAYER---------------------------------------
    	protected function player_move (mc:MovieClip, pX:Number, pY:Number, effect) :void
    	{
			mc.x = pX;
			mc.y = pY;
			mc.start_x = pX;
			mc.start_y = pY;
			mc.goal_x = pX;
			mc.goal_y = pY;
			mc.old_x = pX;
			mc.old_y = pY;
			mc.new_x = pX;
			mc.new_y = pY;
			mc.move_distance = 0;
			mc.move_time = 0;
			mc.moving = false;
			mc.sprinting = false;
			mc.sliding = false;
			
			if (effect){
				mc.character.gotoAndPlay("spawn");
				mc.animation = "spawn";
				//var ghost :death_effect = new death_effect();
				//bg.addChild(ghost);
				//ghost.x = mc.x;
				//ghost.y = mc.y;
				//player_scale(ghost);
				mc.character.alpha = 1;
			}
    	}
		
		//-------------------------------------DELETE PC-----------------------------------------
    	protected function player_delete (id:String) :void
    	{
			if (bg.actors.getChildByName(id)){
				bg.actors.getChildByName(id).removeEventListener("enterFrame", player_enterFrame);
				bg.actors.removeChild(bg.actors.getChildByName(id));
				hud.radar.view.removeChild(bg.actors.getChildByName(id).blip);
				pc_count -= 1;
			}
    	}
		
		//-------------------------------------DELETE NPC-----------------------------------------
    	protected function delete_npc (mc:MovieClip) :void
    	{
			mc.removeEventListener("enterFrame", player_enterFrame);
			hud.radar.view.removeChild(mc.blip);
			bg.actors.removeChild(mc);
			
			//var ghost :death_effect = new death_effect();
			//bg.addChild(ghost);
			//ghost.x = mc.x;
			//ghost.y = mc.y;
			//player_scale(ghost);
    	}
		
		//-------------------------------------DELETE NPCS----------------------------------------
    	protected function clear_npcs () :void
    	{
			var ghost :death_effect;
			var n:Number = 0;
			if (npc_count){
				var t:Number = npc_count;
				while(n <= t){
					if (bg.actors.getChildByName("npc_"+n)){
						bg.actors.getChildByName("npc_"+n).removeEventListener("enterFrame", player_enterFrame);
						
						ghost = new death_effect();
						bg.addChild(ghost);
						ghost.x = bg.actors.getChildByName("npc_"+n).x;
						ghost.y =bg.actors.getChildByName("npc_"+n).y;
						player_scale(ghost);
						
						hud.radar.view.removeChild(bg.actors.getChildByName("npc_"+n).blip);
						bg.actors.removeChild(bg.actors.getChildByName("npc_"+n));
					}
					n++;
				}
				npc_count = 0;
			}
    	}
		
		//-------------------------------------REPORT NPCS----------------------------------------
    	protected function report_npcs () :void
    	{
			var n:Number = 0;
			var mob: MovieClip;
			var mob_tar: MovieClip;
			
			var msg :Object = new Object;
			msg[0] = room_num;
			msg[1] = _control.getMyId();

			var m:Number = 2;
			
			if (npc_count){
				var t:Number = npc_count;
				while(n <= t){
					mob = bg.actors.getChildByName("npc_"+n);
					if (mob){
						//_control.localChat("NPC_"+1+"...");
						mob_tar = bg.actors.getChildByName(mob.ai_target);
						if(mob_tar == local_player){
							//_control.localChat("...You're target.");
							msg[m] = "npc_"+n;
							if (mob.moving){
								msg[m+1] = mob.goal_x;
								msg[m+2] = mob.goal_y;
								msg[m+4] = mob.move_A;
							}else{
								msg[m+1] = mob.x;
								msg[m+2] = mob.y;
								msg[m+4] = clock;
							}
							msg[m+3] = mob.ai_target;
							m = m+5;
						}
						
					}
					n++;
				}
			}
			_control.sendMessage (REPORT, msg);
			current_mps+=1;
    	}
		
		//-------------------------------------LOAD NPCS----------------------------------------
		private function load_npcs(room:Number, wave:Number) :void{
			clear_npcs();
			
			var mobs :MovieClip = new all_mobs();
			//mobs.gotoAndStop(room_num);
			
			var n:Number = 0;
			var t:Number = mobs.numChildren;
			var moo: MovieClip;
			
			var element :String;
			var table :Number;
			var olddmgg :Number;
			if (_control.amInControl()){
				var elementt :String = "totalMobHP";
				var tablee :Number = _control.get(elementt);
				if (tablee == 0) {
						tablee = 0;
						_control.set(elementt, tablee);
				}
			}
			
			
			var total_found :Number = 0;
			
			//_control.localChat("Spawning "+(t)+" Mobs.");
			while(n < t){
					//moo = mobs.getChildByName("mob_"+n);
					moo = mobs.getChildAt(n);
					if (moo && moo.name == "m"+String(room)+"_w"+String(wave)){
						//_control.localChat("Spawning Mob "+n+".");
						newmob = create_player(moo.mt.text, "NPC", moo.x, moo.y, moo.hp.text, moo.spd.text, moo.rng.text, moo.fst.text, moo.max.text, moo.min.text, moo.knockback.text, moo.stun.text,n, wave);
						total_found ++;
						
						if (_control.amInControl()){
							tablee = _control.get(elementt);
							tablee += moo.hp.text;
							_control.set(elementt, tablee);
						}
						
						newmob = null;
						moo = null;
					}else{
						//_control.localChat("No Child at "+n+"!");
					}
					n++;
			}
			npc_killcount = 0;
			
			if(total_found == 0){
				allclear = true;
			}
		}
		
		//-------------------------------------POKE!---------------------------------------------
		private function player_poke( e: Event):void{
			//************************************
			//                -W-
			if (_control.isConnected()) {
				var msg :Object = new Object;
				msg[0] = room_num;
        		msg[1] = _control.getMyId();
				if (local_player.moving){
					msg[2] = goal.x;
					msg[3] = goal.y;
				}else{
        			msg[2] = local_player.x;
					msg[3] = local_player.y;
				}
				msg[4] = local_player.sprinting;
				msg[5] = local_player.hp;
				msg[6] = local_player.energy;
				msg[7] = local_player.character.scaleX;
        		_control.sendMessage (p_goal, msg);
				current_mps+=1;
			}
			//                -W-
			//************************************
		}
		
		//-------------------------------------NAVI----------------------------------------------
		private function floorCLICK_handler(e:MouseEvent){
			var pos:Point = new Point(e.stageX, e.stageY);
			pos = bg.globalToLocal(pos);
			
			if (mouse_active){
				if (local_player.sprinting == false){
					goal.x = pos.x;
					goal.y = pos.y;
					player_scale(goal);
					goal.gotoAndPlay("on");
					goal_hide = false;
					
					if (sprint_hit == true && local_limiter != true){
						local_player.sprinting = true;
					}
					plot_goal(local_player, pos.x, pos.y, clock);
					
					
					//************************************
					//                -W-
					if (_control.isConnected()) {
						var msg :Object = new Object;
						msg[0] = room_num;
						msg[1] = _control.getMyId();
						msg[2] = pos.x;
						msg[3] = pos.y;
						msg[4] = local_player.sprinting;
						msg[5] = local_player.hp;
						msg[6] = local_player.energy;
						msg[7] = local_player.character.scaleX;
						msg[8] = clock;
						_control.sendMessage (p_goal, msg);
						current_mps+=1;
					}
					//                -W-
					//************************************
				}
			}
		}
		
		//-------------------------------------PLOT GOAL-----------------------------------------
		private function plot_goal(mc:MovieClip, pX:Number, pY:Number, alpha:Number){
			if ((mc.animation == "idle" || mc.animation == "walk")){ //&& mc.sliding == false){
				mc.start_x = mc.x;
				mc.start_y = mc.y;
				mc.goal_x = pX;
				mc.goal_y = pY;
				
				var lp_start: Point = new Point(mc.start_x,mc.start_y);
				var lp_goal: Point = new Point(mc.goal_x,mc.goal_y);
				
				mc.move_distance = Point.distance(lp_start,lp_goal);
				mc.move_time = 0;
				mc.move_A = alpha;
				
				mc.moving = true;
				mc.animation = "walk";
			}
		}
		
		//-------------------------------------SPAWN COIN-----------------------------------------
		private function create_coin(pX:Number, pY:Number, flag:String, count:int, amount:int = 1, 
									 sX:Number=-99, sY:Number=-99) :void{
			
			itemcount = count;
			
			if(sX == -99){
				sX = (Math.random()*30)+(-15);
			}
			if(sY == -99){
				sY = (Math.random()*5)+(-2.5);
			}
			var mc :MovieClip;
			
			for(var am:Number = 1;am <= amount; am++){
				if(flag == "coin"){
					mc = new coin();
					mc.flag = "coin";
				}else{
					mc = new health();
					mc.flag = "health";
				}
				
				mc.name = "item_"+count;
				itemcount += 1;
				
				bg.actors.addChild(mc);
				mc.x = pX;
				mc.y = pY;
				
				player_scale(mc);
				player_depth(mc);
				
				mc.old_x = pX+sX;
				mc.old_y = pY+sY;
				mc.new_x = pX;
				mc.new_y = pY;
				
				mc.birth = clock;
				mc.tog = 1;
				mc.addEventListener("enterFrame", update_coin);
			}
		}
		private function update_coin(e:Event):void{
			var mc:MovieClip;
			mc = e.target;

			mc.new_x = mc.x;
			mc.new_y = mc.y;
			
			var xslide = (mc.new_x-mc.old_x)*0.99;
			var yslide = (mc.new_y-mc.old_y)*0.99;
			if((yslide+xslide)/2 > 0.1){
				mc.x += xslide;
				mc.y += yslide;
				mc.old_x = mc.new_x;
				mc.old_y = mc.new_y;
				
				player_scale(mc);
				player_depth(mc);
			}
			
			if(mc.flag == "coin"){
				var sparkle1 :MovieClip;
				var rnd :Number = Math.random()*100;
				if (rnd > 33 && current_fps >= 20){
					sparkle1 = new coin_spark();
					bg.addChild(sparkle1);
					sparkle1.x = mc.x+Math.random()*30+(-15);
					sparkle1.y = mc.y+mc.cn.y+Math.random()*30+(-15);
				}
			}
			
			if (mc.birth+9000 < clock){
				mc.tog = mc.tog*(-1);
				if(mc.tog == 1){
					mc.alpha = 1;
				}else{
					mc.alpha = 0;
				}
				if (mc.birth+10000 < clock){
					mc.removeEventListener("enterFrame", update_coin);
					mc.parent.removeChild(mc);
				}
			}
			
			if (mc.birth+1500 < clock){
				if(mc.boundbox.hitTestObject(local_player.boundbox)){
					if (_control.isConnected()) {
						var msgg :Object = new Object;
						msgg[0] = room_num;
						msgg[1] = _control.getMyId();
						msgg[2] = mc.name;
						_control.sendMessage (d_item, msgg);
						current_mps+=1;
					}
					//                -W-
					//************************************
					
					if(mc.flag == "coin"){
						mc.cn.stop();
						
						var sparks :MovieClip;
						sparks = new coinGOT();
						bg.addChild(sparks);
						sparks.x = mc.x;
						sparks.y = mc.y;
						player_scale(sparks);
						
						mc.removeEventListener("enterFrame", update_coin);
						mc.parent.removeChild(mc);
						increase_score(5);
						local_exp += 15;
						if (_control.isConnected()) {
							var flow :Number = Math.round(_control.getAvailableFlow()*0.2);
							if(flow < 1){
								flow = 1;
							}
							_control.awardFlow(flow);
						}
					}else{
						increase_score(25);
						
						var crosses :MovieClip;
						crosses = new health_got();
						bg.addChild(crosses);
						crosses.x = mc.x;
						crosses.y = mc.y;
						player_scale(crosses);
						player_heal(local_player, (local_player.maxhp-local_player.hp))
						
						mc.removeEventListener("enterFrame", update_coin);
						mc.parent.removeChild(mc);
					}
					
				}
			}
		}
		
		
		
		
		
		
		
		public var mouse_active :Boolean = true;
		public var keys_active :Boolean = true;
		public var ai_active :Boolean = true;
		//----------------------------------------------------------------------------------------
		private function disable_mouse(){
			mouse_active = false;
		}
		private function enable_mouse(){
			mouse_active = true;
		}
		
		private function disable_keys(){
			keys_active = false;
		}
		private function enable_keys(){
			keys_active = true;
		}
		
		private function disable_ai(){
			ai_active = false;
		}
		private function enable_ai(){
			ai_active = true;
		}
		//----------------------------------------------------------------------------------------
		
		
		
		
		//----------------------------------------------------------------------------------------
		//----------------------------------------------------------------------------------------
		//-------------------------------------PUNCH!---------------------------------------------
    	protected function player_punch (mc:MovieClip) :void
    	{	
			if (mc == local_player){
				if (local_player.energy >= 25 && local_limiter != true){
						if ((mc.animation == "idle" || mc.animation == "walk") && mc.animation_old != "punch"){
							local_player.energy -= 25;
							if (mc.sprinting == true){
								mc.sliding = true;
								mc.moving = false;
								mc.sprinting =false;
								mc.move_time = 0;
								mc.move_distance = 0;
								mc.start_x = mc.goal_x;
								mc.start_y =mc.goal_y;
								mc.goal_x = mc.x;
								mc.goal_y = mc.y;
							}
							if (mc.sliding == false){
								player_move(mc, mc.x, mc.y, false);
							}
							mc.animation = "punch";
							health_tick = clock+3000;
							current_attack += 1;
							last_attack = clock;
							//************************************
							//                -W-
							if (_control.isConnected()) {
								var msg :Object = new Object;
								msg[0] = room_num;
								msg[1] = _control.getMyId();
								msg[2] = local_player.character.scaleX;
								_control.sendMessage (p_punch, msg);
								current_mps+=1;
							}
							//                -W-
							//************************************
						}
				}
			}else{
				if (mc.animation == "idle" || mc.animation == "walk"){
					if (mc.flag == "NPC"){
						mc.ai_tick = clock+mc.ai_cooldown;
						//************************************
						//                -W-
						if (_control.isConnected()) {
							var msgg :Object = new Object;
							msgg[0] = room_num;
							msgg[1] = _control.getMyId();
							msgg[3] = mc.name;
							msgg[2] = local_player.character.scaleX;
							_control.sendMessage (n_punch, msgg);
							current_mps+=1;
						}
						//                -W-
						//************************************
					}
					player_move(mc, mc.x, mc.y, false);
					mc.animation = "punch";
				}
			}
    	}
		
		//-------------------------------------KICK!----------------------------------------------
    	protected function player_kick (mc:MovieClip) :void
    	{
			if (mc == local_player){
				if (local_player.energy >= 25 && local_limiter != true){
						if ((mc.animation == "idle" || mc.animation == "walk") && mc.animation_old != "kick"){
							local_player.energy -= 25;
							if (mc.sprinting == true){
								mc.sliding = true;
								mc.moving = false;
								mc.sprinting =false;
								mc.move_time = 0;
								mc.move_distance = 0;
								mc.start_x = mc.goal_x;
								mc.start_y =mc.goal_y;
								mc.goal_x = mc.x;
								mc.goal_y = mc.y;
							}
							if (mc.sliding == false){
								player_move(mc, mc.x, mc.y, false);
							}
							mc.animation = "kick";
							health_tick = clock+3000;
							current_attack = 0;
							last_attack = clock;
							//************************************
							//                -W-
							if (_control.isConnected()) {
								var msg :Object = new Object;
								msg[0] = room_num;
								msg[1] = _control.getMyId();
								msg[2] = local_player.character.scaleX;
								_control.sendMessage (n_kick, msg);
								current_mps+=1;
							}
							//                -W-
							//************************************
						}
				}
			}else{
				if (mc.animation == "idle" || mc.animation == "walk"){
					if (mc.flag == "NPC"){
						mc.ai_tick = clock+mc.ai_cooldown;
						//************************************
						//                -W-
						if (_control.isConnected()) {
							var msgg :Object = new Object;
							msgg[0] = room_num;
							msgg[1] = _control.getMyId();
							msgg[3] = mc.name;
							msgg[2] = local_player.character.scaleX;
							_control.sendMessage (p_kick, msgg);
							current_mps+=1;
						}
						//                -W-
						//************************************
					}
					player_move(mc, mc.x, mc.y, false);
					mc.animation = "kick";
				}
			}
    	}
		
		//-------------------------------------BLOCK!---------------------------------------------
    	protected function player_block (mc:MovieClip) :void //START
    	{
			if (mc.animation == "idle" || mc.animation == "walk"){
				if (mc == local_player){
					if (mc.sprinting == true){
						mc.sliding = true;
						mc.moving = false;
						mc.sprinting =false;
						mc.move_time = 0;
						mc.move_distance = 0;
						mc.start_x = mc.goal_x;
						mc.start_y =mc.goal_y;
						mc.goal_x = mc.x;
						mc.goal_y = mc.y;
					}
					if (mc.sliding == false){
						player_move(mc, mc.x, mc.y, false);
					}
					mc.animation = "block";
					
					//************************************
					//                -W-
					if (_control.isConnected()) {
						var msg :Object = new Object;
						msg[0] = room_num;
						msg[1] = _control.getMyId();
						msg[2] = local_player.character.scaleX;
						_control.sendMessage (p_block, msg);
						current_mps+=1;
					}
					//                -W-
					//************************************
				}else{
					player_move(mc, mc.x, mc.y, false);
					mc.animation = "block";
				}
			}
    	}
    	protected function player_block_stop (mc:MovieClip) :void //STOP
    	{
			if (mc.animation == "block"){
				if (mc == local_player){
					mc.animation = "idle";
					
					//************************************
					//                -W-
					if (_control.isConnected()) {
						var msgg :Object = new Object;
						msgg[0] = room_num;
						msgg[1] = _control.getMyId();
						msgg[2] = local_player.character.scaleX;
						_control.sendMessage (p_block_stop, msgg);
						current_mps+=1;
					}
					//                -W-
					//************************************
				}else{
					mc.animation = "idle";
				}
			}
    	}
		
		//-------------------------------------HURT PLAYER----------------------------------------
		protected function player_heal (mc:MovieClip, dmg:Number) :void
    	{	
			if (mc == local_player){
				mc.hp += dmg;
				var temp_heal:MovieClip;
				temp_heal = new heal_num_player;
				temp_heal.txt.dmg.text = "+"+String(Math.round(dmg));
				bg.addChild(temp_heal);
				temp_heal.x = mc.x;
				temp_heal.y = mc.y;
				player_scale(temp_heal);
			}
		}
		
		protected function player_hurt (mc:MovieClip, dmg:Number, attacker_name:String, knockback:Number, stun:Number, coins:int=0) :void
    	{	
			var attacker :MovieClip = bg.actors.getChildByName(attacker_name);
			
			var temp_block :MovieClip;
			
			
			if (mc.animation_old == "block" || mc.animation == "block" ){
				if (mc == local_player){
						if (local_player.energy > 0){
							dmg = dmg/2;
							local_player.energy -= dmg;
							dmg = 0;
							health_tick = clock+3000;
							
							temp_block = new block();
							bg.addChild(temp_block);
							temp_block.x = mc.x+Math.random()*50;
							temp_block.y = mc.y;
							player_scale(temp_block);
							
						}else{
							mc.hp -= dmg;
							health_tick = clock+6000;
							mc.animation = "hurt";
							player_move(mc, mc.x, mc.y, false);
						}
				}
			}else{
				if (_control.isConnected()){
					if(mc == local_player){
							mc.hp -= dmg;
					}else{
						mc.ai_tick = clock+mc.ai_cooldown+2000*((mc.hp/mc.maxhp)-mc.scaleX);
						mc.ai_target = attacker_name;
						mc.ai_mode = "active";
					}
				}else{
					mc.hp -= dmg;
					mc.ai_tick = clock+mc.ai_cooldown+2000*((mc.hp/mc.maxhp)-mc.scaleX);
					mc.ai_target = attacker_name;
					mc.ai_mode = "active";
				}
				mc.animation = "hurt";
				player_move(mc, mc.x, mc.y, false);
			}
			
			if (mc == local_player){
				local_exp -= dmg;
			}
			
			if (coins > 0 && mc.ai_target == local_player.name){
					//************************************
					//                -W-
					if (_control.isConnected()) {
						var msgg :Object = new Object;
						msgg[0] = room_num;
						msgg[1] = _control.getMyId();
						msgg[2] = mc.x;
						msgg[3] = mc.y;
						msgg[4] = "coin";
						msgg[5] = coins;
						msgg[6] = (Math.random()*30)+(-15);
						msgg[7] = (Math.random()*5)+(-2.5);
						itemcount += 1;
						msgg[8] = itemcount;
						_control.sendMessage (s_item, msgg);
						current_mps+=1;
					}
					//                -W-
					//************************************
			}
			
			if (dmg > 0){
				
				if (stun > 0){
					mc.stun = true;
					mc.stun_counter = clock+(stun*1000)
				}else{
					mc.stun = false;
				}
				
				if (knockback > 0){
					mc.animation = "knockback";
					mc.knockback = true;
						mc.sliding = true;
						mc.moving = false;
						mc.sprinting =false;
						mc.move_time = 0;
						mc.move_distance = 0;
						mc.start_x = mc.goal_x;
						mc.start_y =mc.goal_y;
						mc.goal_x = mc.x;
						mc.goal_y = mc.y;
					if (attacker.x > mc.x){
						mc.old_x = mc.x+knockback;
					}else{
						mc.old_x = mc.x-knockback;
					}
					
				}
			}
			
			if (dmg > 0){
				var temp_num :MovieClip;
				var temp_snap :MovieClip;
				if (dmg > 80){
					game.camera.gotoAndPlay("x_light");
					if (mc.flag == "PC"){
						temp_num = new dmg_crit_num_player();
					}else{
						temp_num = new dmg_crit_num();
					}
				}else{
					if (mc.flag == "PC"){
						temp_num = new dmg_num_player();
					}else{
						temp_num = new dmg_num();
					}
				}
				temp_num.txt.dmg.text = "-"+String(Math.round(dmg));
				bg.addChild(temp_num);
				temp_num.x = mc.x+Math.random()*50;
				temp_num.y = mc.y;
				player_scale(temp_num);
				
				temp_snap = new dmg_snap();
				bg.addChild(temp_snap);
				temp_snap.x = mc.x;
				temp_snap.y = mc.y;
				player_scale(temp_snap);
				temp_snap.x += Math.random()*20;
				temp_snap.y += Math.random()*20;
				var snapscale :Number = 0.10-Math.random()*0.20;
				temp_snap.scaleX += snapscale;
				temp_snap.scaleY += snapscale;
				temp_snap.snap.rotate = Math.random()*360;
				
				
				
			}
			
			
    	}
		//----------------------------------------------------------------------------------------
		//----------------------------------------------------------------------------------------
		//----------------------------------------------------------------------------------------
		
		
		
		protected function sendmsg (n:String, msg:Object) :void
    	{
			if(current_mps < 8){ //Below threshold
				current_mps++;
				//************************************
				//                -W-
				if (_control.isConnected()) {
						_control.sendMessage (n, msg);
						current_mps+=1;
				}
				//                -W-
				//************************************
				
			}else{ //Too many messages sent! Wait!
				var TBS: _MSG = new _MSG();
				TBS.msg = msg;
				TBS.name = n;
				MSGBOX.addChild(TBS);
				TBS.addEventListener("enterFrame", update_MSG);
				
			}
    	}
		protected function update_MSG (e:Event) :void
    	{
			var mc:MovieClip = e.target;
			if(current_mps < 8){
				current_mps++;
				//************************************
				//                -W-
				if (_control.isConnected()) {
						_control.sendMessage (mc.name, mc.msg);
						current_mps+=1;
				}
				//                -W-
				//************************************
				mc.removeEventListener("enterFrame", update_MSG);
				MSGBOX.removeChild(mc);
			}
    	}
		
		
		
		
		public static const p_goal :String = "New Player Goal";
		public static const n_goal :String = "New NPC Goal";
		public static const p_move :String = "Move Player";
		public static const p_punch :String = "Player Punched";
		public static const p_kick :String = "Player Kicked";
		public static const p_block :String = "Player Blocked";
		public static const p_block_stop :String = "Player Stopped Blocking";
		
		public static const n_punch :String = "Enemy Punched";
		public static const n_kick :String = "Enemy Kicked";
		
		public static const p_hurt :String = "This guy took damage!";
		public static const n_hurt :String = "This guy hit me!";
		
		public static const s_item :String = "Spawn an item!";
		public static const d_item :String = "Delete an item!";
		
		public static const p_quit :String = "Player quit the game";
		
		public static const p_msg :String = "Player Sent a Message";
		
		public static const GTNR :String = "Go to the next room!";
		
		public static const CLOCK_UPDATE :String = "Hey, update your clock!";
		
		public static const REPORT :String = "Current status of the NPC via the Host";
		//----------------------------------------------------------------------------------------
		//------------------------------------NETWORKING------------------------------------------
		//----------------------------------------------------------------------------------------
		// from MessageReceivedListener 
		public function messageReceived (event :MessageReceivedEvent) :void
		{
			if(gameover){
				
			}else{
				var room :Number = int(event.value[0]);
				var id :int = int(event.value[1]);
				var moo_id: String = id;
				var moo: MovieClip;
				
				var element :String;
				var table :Number;
				
				//---PUBLIC---
				switch (event.name) {
					case s_item:
						create_coin( Number(event.value[2]),  Number(event.value[3]),  String(event.value[4]), int(event.value[8]),  int(event.value[5]),  Number(event.value[6]),  Number(event.value[7]));
						break;
					
					case GTNR:
						room_num = int(event.value[0]);
						gotonextroom = true;
						break;
							
					case CLOCK_UPDATE:
						clock = int(event.value[2]);
						break;
							
					case p_msg:
						var chat :String = String(event.value[2]);
						_control.localChat(chat);
						break;
							
					case p_quit:
						player_delete(moo_id);
						break;
							
					case p_hurt:
						var mobb :Number = event.value[2];
						var dmgg :Number = Number(event.value[3]);
						if (_control.isConnected() && _control.amInControl()){
							element = "r"+String(room)+"_m"+mobb+"_w"+String(event.value[6]);
							table = _control.get(element);
							if (table == 0) {
								//_control.localChat(element+" does not exsist: Creating...");
								table = bg.actors.getChildByName("npc_"+mobb).maxhp;
							}
							table -= dmgg;
							//_control.localChat("S: "+element+" = "+table);
							_control.set(element, table);
							
							element = "enemyDamage";
							table = _control.get(element);
							if (table == 0) {
								table = dmgg;
								_control.set(element, table);
							}else{
								table += dmgg;
								_control.set(element, table);
							}
						}
						break;
						
					case n_hurt:
						var ndmgg :Number = Number(event.value[3]);
						if (_control.isConnected() && _control.amInControl()){
							element = "playerDamage";
							table = _control.get(element);
							if (table == 0) {
								table = ndmgg;
								_control.set(element, table);
							}else{
								table += ndmgg;
								_control.set(element, table);
							}
						}
						break;
						
					case n_goal:
							gX = Number(event.value[2]);
							gY = Number(event.value[3]);
							moo = bg.actors.getChildByName(String(event.value[4]));
							if (moo){
								moo.ai_target = String(event.value[5]);
								plot_goal(moo, gX, gY, clock);//Number(event.value[6]));
							}
							break;
						
					case REPORT:
							var m:Number = 2;
							var n:Number = 0;
							var moob :MovieClip;
								var t:Number = npc_count;
								while(event.value[m]){
									moob = bg.actors.getChildByName(String(event.value[m]));
									if (moob){
										moob.ai_target = String(event.value[m+3]);
										plot_goal(moob, Number(event.value[m+1]), Number(event.value[m+2]), clock);//Number(event.value[m+4]));
										//_control.localChat(moob.name+" reported");
										moob = null;
									}
									m = m+5
								}
				}
				
				
				//---LOCAL---
				if (room == room_num && id != _control.getMyId()){
					if (bg.actors.getChildByName(moo_id)){
					}else{
						var local_name: String = _control.getOccupantName(id);
						moo = create_player(local_name, "PC", 0, 0, 100, 250, 0,0,0,0,0,0, moo_id, 0);
						if (_control.amInControl()) {
							clock_nextupdate = clock;
							var msgr :Object = new Object;
							msgr[0] = room_num;
							msgr[1] = _control.getMyId();
							msgr[2] = clock
							_control.sendMessage (CLOCK_UPDATE, msgr);
							current_mps+=1;
						}
					}
					
					var gX :Number;
					var gY :Number;
					switch (event.name){
						
						case p_hurt:
							var mob :Number = Number(event.value[2]);
							var dmg :Number = Number(event.value[3]);
							if(bg.actors.getChildByName("npc_"+mob)){
								player_hurt (bg.actors.getChildByName("npc_"+mob), dmg, String(id), Number(event.value[4]), Number(event.value[5]), int(event.value[7]));
							}
							break;
						
						case d_item:
							var tempitem:MovieClip = bg.actors.getChildByName(String(event.value[2]));
							if(tempitem){
								tempitem.removeEventListener("enterFrame", update_coin);
								tempitem.parent.removeChild(tempitem);
							}
							break;
						
						case p_goal:
							gX = Number(event.value[2]);
							gY = Number(event.value[3]);
							moo = bg.actors.getChildByName(moo_id);
							moo.sprinting = event.value[4];
							moo.hp = event.value[5];
							moo.energy = event.value[6];
							moo.character.scaleX = Number(event.value[7]);
							plot_goal(moo, gX, gY, clock);// Number(event.value[8]));
							break;
								
						case p_move:
							gX = Number(event.value[2]);
							gY = Number(event.value[3]);
							moo = bg.actors.getChildByName(moo_id);
							moo.sprinting = event.value[4];
							moo.hp = event.value[5];
							moo.energy = event.value[6];
							player_move(moo, gX, gY, Boolean(event.value[7]));
							break;
							
						case n_hurt:
							player_hurt (bg.actors.getChildByName(moo_id), Number(event.value[3]), "npc_x", Number(event.value[4]), Number(event.value[5]));
							break;
						
						case n_punch:
							moo = bg.actors.getChildByName(String(event.value[3]));
							moo.character.scaleX = Number(event.value[2]);
							player_punch(moo);
							break;
								
						case n_kick:
							moo = bg.actors.getChildByName(String(event.value[3]));
							moo.character.scaleX = Number(event.value[2]);
							player_kick(moo);
							break;
						
						case p_punch:
							moo = bg.actors.getChildByName(moo_id);
							moo.character.scaleX = Number(event.value[2]);
							player_punch(moo);
							break;
								
						case p_kick:
							moo = bg.actors.getChildByName(moo_id);
							moo.character.scaleX = Number(event.value[2]);
							player_kick(moo);
							break;
								
						case p_block:
							moo = bg.actors.getChildByName(moo_id);
							moo.character.scaleX = Number(event.value[2]);
							player_block(moo);
							break;
								
						case p_block_stop:
							moo = bg.actors.getChildByName(moo_id);
							moo.character.scaleX = Number(event.value[2]);
							player_block_stop(moo);
							break;
					}
				}else if (room != room_num && id != _control.getMyId()){
					//player_delete(moo_id);
				}
				
				
				//---IDLE OUT---
				if (bg.actors.getChildByName(moo_id)){
					bg.actors.getChildByName(moo_id).lastupdate = clock;
				}
			}
		}
		 	
		public function propertyChanged( event :PropertyChangedEvent):void
		{
		}
		//----------------------------------------------------------------------------------------
		//----------------------------------------------------------------------------------------
		//----------------------------------------------------------------------------------------
	}
}