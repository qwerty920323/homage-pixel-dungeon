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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.CorrosiveGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.BlobImmunity;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Lightning;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.CorrosionParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.EarthParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SmokeParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.curses.Corrosion;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.BArray;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.ColorMath;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.HashSet;

public class WandOfCorrosion extends Wand {

	{
		image = ItemSpriteSheet.WAND_CORROSION;

		collisionProperties = Ballistica.STOP_TARGET | Ballistica.STOP_SOLID;
	}

	@Override
	public void onZap(Ballistica bolt) {
		CorrosiveGas gas = Blob.seed(bolt.collisionPos, 50 + 10 * buffedLvl(), CorrosiveGas.class);
		CellEmitter.get(bolt.collisionPos).burst(Speck.factory(Speck.CORROSION), 10 );
		gas.setStrength(2 + buffedLvl(), getClass());
		GameScene.add(gas);
		Sample.INSTANCE.play(Assets.Sounds.GAS);

		for (int i : PathFinder.NEIGHBOURS9) {
			Char ch = Actor.findChar(bolt.collisionPos + i);
			if (ch != null) {
				wandProc(ch, chargesPerCast());

				if (i == 0 && ch instanceof DwarfKing){
					Statistics.qualifiedForBossChallengeBadge = false;
				}
			}
		}
		
		if (Actor.findChar(bolt.collisionPos) == null){
			Dungeon.level.pressCell(bolt.collisionPos);
		}
	}

	@Override
	public void fx(Ballistica bolt, Callback callback) {
		MagicMissile.boltFromChar(
				curUser.sprite.parent,
				MagicMissile.CORROSION,
				curUser.sprite,
				bolt.collisionPos,
				callback);
		Sample.INSTANCE.play(Assets.Sounds.ZAP);
	}

	@Override
	public void onHit(MagesStaff staff, Char attacker, Char defender, int damage) {
		int level = Math.max( 0, buffedLvl() );

		// lvl 0 - 33%
		// lvl 1 - 50%
		// lvl 2 - 60%
		float procChance = (level+1f)/(level+3f) * procChanceMultiplier(attacker);
		if (Random.Float() < procChance) {

			float powerMulti = Math.max(1f, procChance);
			
			Buff.affect( defender, Ooze.class ).set( Ooze.DURATION * powerMulti );
			CellEmitter.center(defender.pos).burst( CorrosionParticle.SPLASH, 5 );
			
		}
	}

	@Override
	public void staffFx(MagesStaff.StaffParticle particle) {
		particle.color( ColorMath.random( 0xAAAAAA, 0xFF8800) );
		particle.am = 0.6f;
		particle.setLifespan( 1f );
		particle.acc.set(0, 20);
		particle.setSize( 0.5f, 3f );
		particle.shuffleXY( 1f );
	}

	@Override
	public String statsDesc() {
		if (levelKnown)
			return Messages.get(this, "stats_desc", 2+buffedLvl());
		else
			return Messages.get(this, "stats_desc", 2);
	}

	//scholar
	@Override
	public int bonusRange () {return  super.bonusRange()+2;}
	@Override
	public int scholarTurnCount(){
		return super.scholarTurnCount() + 5;
	}
	@Override
	public void scholarAbility(Ballistica bolt, int cell){
		super.scholarAbility(bolt,cell);
		Buff.append( curUser, CorrosionArea.class ).set( scholarTurnCount(), bonusRange(), bolt.collisionPos );
	}

	public static class CorrosionArea extends Buff {

		{
			type = buffType.POSITIVE;
			revivePersists = true;
		}

		private float left;
		int pos, range;

		public void set( float duration , int range, int cell) {
			this.left = Math.max(duration, left);
			this.range = range;
			this.pos = cell;
		}

		private static final String POS    = "pos";
		private static final String LEFT   = "left";
		private static final String RANGE  = "range";


		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle( bundle );
			bundle.put( LEFT, left );
			bundle.put(POS, pos);
			bundle.put(RANGE, range);

		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			left = bundle.getFloat( LEFT );
			pos = bundle.getInt(POS);
			range = bundle.getInt(RANGE);

		}

		@Override
		public boolean act() {

			spend(TICK);

			left -= TICK;

			PathFinder.buildDistanceMap(pos, BArray.not(Dungeon.level.solid, null), range);

			for (int i = 0; i < Dungeon.level.length(); i++) {
				if (PathFinder.distance[i] < Integer.MAX_VALUE) {

					CorrosiveGas gas = (CorrosiveGas) Dungeon.level.blobs.get(CorrosiveGas.class);
					if (gas != null && gas.volume > 0 && gas.cur[i] > 0) {
						setCellToEmpty(i);
					}
				}
			}

			if (left <= 0) {
				detach();
			}

			return true;
		}


		public boolean setCellToEmpty( int cell ){
			Point p = Dungeon.level.cellToPoint(cell);

			//if a custom tilemap is over that cell, don't put water there
			for (CustomTilemap cust : Dungeon.level.customTiles){
				Point custPoint = new Point(p);
				custPoint.x -= cust.tileX;
				custPoint.y -= cust.tileY;
				if (custPoint.x >= 0 && custPoint.y >= 0
						&& custPoint.x < cust.tileW && custPoint.y < cust.tileH){
					if (cust.image(custPoint.x, custPoint.y) != null){
						return false;
					}
				}
			}

			int terr = Dungeon.level.map[cell];

			if ((terr == Terrain.INACTIVE_TRAP || terr == Terrain.EMBERS)) {

				if (terr == Terrain.INACTIVE_TRAP)
					Dungeon.level.traps.remove(cell);

				Level.set(cell, Terrain.EMPTY);

				if (Dungeon.level.heroFOV[cell]) {
					CellEmitter.get( cell ).burst( Speck.factory( Speck.WOOL ), 15);

					Emitter e = CellEmitter.get(cell);
					e.y -= DungeonTilemap.SIZE * 0.2f;
					e.height *= 0.8f;
					e.start(EarthParticle.FALLING, 0.01f,30);
				}

				GameScene.updateMap(cell);
				return true;
			}

			return false;
		}

		@Override
		public void detach() {

			super.detach();
		}
	}
}
