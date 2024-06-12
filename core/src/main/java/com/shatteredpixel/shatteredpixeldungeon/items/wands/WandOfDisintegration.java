/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2024 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.items.wands;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Web;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Beam;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.PurpleParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ScholarParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class WandOfDisintegration extends DamageWand {

	{
		image = ItemSpriteSheet.WAND_DISINTEGRATION;

		collisionProperties = Ballistica.WONT_STOP;
	}


	public int min(int lvl){
		return 2+lvl;
	}

	public int max(int lvl){
		return 8+4*lvl;
	}
	
	@Override
	public int targetingPos(Hero user, int dst) {
		return dst;
	}

	@Override
	public void onZap(Ballistica beam) {
		
		boolean terrainAffected = false;
		
		int level = buffedLvl();
		
		int maxDistance = Math.min(distance(), beam.dist);
		
		ArrayList<Char> chars = new ArrayList<>();
		//scholar
		int disinter = 0;
		ArrayList<Integer> solidCells = new ArrayList<>();

		Blob web = Dungeon.level.blobs.get(Web.class);

		int terrainPassed = 2, terrainBonus = 0;
		for (int c : beam.subPath(1, maxDistance)) {
			
			Char ch;
			if ((ch = Actor.findChar( c )) != null) {

				//we don't want to count passed terrain after the last enemy hit. That would be a lot of bonus levels.
				//terrainPassed starts at 2, equivalent of rounding up when /3 for integer arithmetic.
				terrainBonus += terrainPassed/3;
				terrainPassed = terrainPassed%3;

				if (ch instanceof Mob && ((Mob) ch).state == ((Mob) ch).PASSIVE
						&& !(Dungeon.level.mapped[c] || Dungeon.level.visited[c])){
					//avoid harming undiscovered passive chars
				} else {
					chars.add(ch);
				}
			}

			if (Dungeon.level.solid[c]) {
				terrainPassed++;

				//scholar
				if (Dungeon.level.disinter[c]) {
					terrainPassed++;
				}

				disinter++;
				if (disinter <= bonusRange()
						&& Dungeon.level.distance(curUser.pos,c) <= bonusRange()){
					solidCells.add(c);
				}
			}

			if (Dungeon.level.flamable[c]) {

				Dungeon.level.destroy( c );
				GameScene.updateMap( c );
				terrainAffected = true;
				
			}

			
			CellEmitter.center( c ).burst( PurpleParticle.BURST, Random.IntRange( 1, 2 ) );
		}
		
		if (terrainAffected) {
			Dungeon.observe();
		}
		
		int lvl = level + (chars.size()-1) + terrainBonus;
		for (Char ch : chars) {
			wandProc(ch, chargesPerCast());
			ch.damage( damageRoll(lvl), this );
			ch.sprite.centerEmitter().burst( PurpleParticle.BURST, Random.IntRange( 1, 2 ) );
			ch.sprite.flash();
		}

		if (!solidCells.isEmpty()) disintergration(solidCells); //scholar
	}

	@Override
	public void onHit(MagesStaff staff, Char attacker, Char defender, int damage) {
		//no direct effect, see magesStaff.reachfactor
	}

	private int distance() {
		return buffedLvl()*2 + 6;
	}
	
	@Override
	public void fx(Ballistica beam, Callback callback) {
		
		int cell = beam.path.get(Math.min(beam.dist, distance()));
		curUser.sprite.parent.add(new Beam.DeathRay(curUser.sprite.center(), DungeonTilemap.raisedTileCenterToWorld( cell )));
		callback.call();
	}

	@Override
	public void staffFx(MagesStaff.StaffParticle particle) {
		particle.color(0x220022);
		particle.am = 0.6f;
		particle.setLifespan(1f);
		particle.acc.set(10, -10);
		particle.setSize( 0.5f, 3f);
		particle.shuffleXY(1f);
	}

	//scholar
	@Override
	public int bonusRange () {return super.bonusRange()+2;}
	@Override
	public int scholarTurnCount(){
		return super.scholarTurnCount() + 2;
	}
	@Override
	public void scholarAbility(Ballistica bolt, int cell) {
		super.scholarAbility(bolt,cell);
		if (Actor.findChar(bolt.collisionPos) != null
				&& ((Hero)curUser).subClass == HeroSubClass.SCHOLAR){
			chargeForChar(Actor.findChar(bolt.collisionPos));
		}
	}

	public void disintergration (ArrayList<Integer> cells) {
		if (((Hero)curUser).subClass == HeroSubClass.SCHOLAR){
			for (Piercing p : curUser.buffs(Piercing.class)){
				if (!p.piercingPositions.isEmpty()
						&& p.depth == Dungeon.depth
						&& p.branch == Dungeon.branch){
					p.alreadySet(cells);
				}
			}
			Buff.append( curUser, Piercing.class ).set( scholarTurnCount()+1,Dungeon.depth, Dungeon.branch, cells );
		}
	}

	public static class Piercing extends Buff {
		private ArrayList<Integer> piercingPositions = new ArrayList<>();
		private ArrayList<Emitter> piercingEmitters = new ArrayList<>();

		{
			revivePersists = true;
		}

		private float left;
		int depth, branch;
		public void set( float duration ,int depth, int branch, ArrayList<Integer> cells) {
			this.left = Math.max(duration, left);

			piercingPositions.addAll(cells);
			this.depth = depth;
			this.branch = branch;

			for (int i : cells)
				Dungeon.level.disinter[i] = true;

			if (target != null) {
				fx(false);
				fx(true);
			}
		}

		public void alreadySet( ArrayList<Integer> cells) {
			ArrayList<Integer> pass = new ArrayList<>();
			for (int p : piercingPositions)
				if (cells.contains(p)) pass.add(p);

			remove(pass);
		}

		public void depthCheck () {
			if (depth == Dungeon.depth && branch == Dungeon.branch){
				return;
			}

			if (target != null)
				fx(false);
		}

		private static final String BRANCH = "branch";
		private static final String DEPTH  = "depth";
		private static final String LEFT   = "left";
		private static final String PIERCING_POS = "piercing_pos";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle( bundle );
			bundle.put(LEFT, left);
			bundle.put(DEPTH, depth);
			bundle.put(BRANCH, branch);

			int[] values = new int[piercingPositions.size()];
			for (int i = 0; i < values.length; i ++)
				values[i] = piercingPositions.get(i);
			bundle.put(PIERCING_POS, values);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			left = bundle.getFloat(LEFT);
			depth = bundle.getInt(DEPTH);
			branch = bundle.getInt(BRANCH);

			int[] values = bundle.getIntArray(PIERCING_POS);
			for (int value : values) {
				piercingPositions.add(value);
			}
		}

		@Override
		public boolean act() {

			spend( TICK );

			if (depth == Dungeon.depth && branch == Dungeon.branch) {
				left -= TICK;

				ArrayList<Integer> pass = new ArrayList<>();
				for (int i : piercingPositions) {
					if (!Dungeon.level.solid[i]) {
						Dungeon.level.disinter[i] = false;
						pass.add(i);
					}
				}
				remove(pass);

				if (left <= 0 || piercingPositions.isEmpty()) {
					detach();
				}

			}

			return true;
		}

		public void remove (ArrayList<Integer> pass){
			if (!pass.isEmpty()){
				for (int p : pass) piercingPositions.remove(Integer.valueOf(p));

				if (target != null) {
					fx(false);
					fx(true);
				}
			}
		}

		@Override
		public void detach() {
			for (int i : piercingPositions){
				Dungeon.level.disinter[i] = false;
			}
			super.detach();
		}
		@Override
		public void fx(boolean on) {
			if (on){
				for (int i : piercingPositions){
					Emitter e = CellEmitter.get(i);
					e.pour(ScholarParticle.YELLOW, 0.05f);
					piercingEmitters.add(e);
				}
			} else {
				for (Emitter e : piercingEmitters){
					e.on = false;
				}
				piercingEmitters.clear();
			}
		}
	}
}
