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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.CorrosionParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ScholarParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.ColorMath;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

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
	public int bonusRange () {return (super.bonusRange()) + 2;}
	@Override
	public int scholarTurnCount(){
		return super.scholarTurnCount() + 8;
	}
	@Override
	public void scholarAbility(Ballistica bolt, int cell){
		super.scholarAbility(bolt,cell);
		int pos = bolt.collisionPos;
		setVent(pos);
	}

	public void setVent (int pos) {
		if (terrCheck(pos, Terrain.EMPTY_SP)) {
			if (Dungeon.level.heroFOV[pos]) {
				CellEmitter.get( pos ).burst( Speck.factory( Speck.WOOL ), 15);
			}
			//trap
			Dungeon.level.setTrap(new GasVent().reveal(), pos);
			Painter.set(Dungeon.level, pos, Terrain.INACTIVE_TRAP);
			GameScene.updateMap(pos);
			GameScene.updateFog();

			//vent
			int t = scholarTurnCount();
			GasVentSeed g = (GasVentSeed) Dungeon.level.blobs.get(GasVentSeed.class);
			for (int i = 0; i < Dungeon.level.length(); i++){
				if (g != null && g.volume > 0 && g.cur[i] > 0){
					g.cur[i] = turnCount(t,200) - (g.getCount(i) * t);
				}
			}

			GasVentSeed c = (GasVentSeed)Blob.seed(pos, turnCount(t,200), GasVentSeed.class, Dungeon.level);
			c.setCorrosionSeed(bonusRange(), t, buffedLvl());
			GameScene.add(c);
		}
	}

	public int turnCount (int turn, int amount) {

		return amount - (amount % turn);
	}

	public static class GasVentSeed extends Blob {
		{
			actPriority = BLOB_PRIO + 1;
		}

		public int count;
		int turn, buffedLvl;
		@Override
		protected void evolve() {
			int cell;

			CorrosiveGas gas = (CorrosiveGas) Dungeon.level.blobs.get(CorrosiveGas.class);
			for (int i=area.top-1; i <= area.bottom; i++) {
				for (int j = area.left-1; j <= area.right; j++) {
					cell = j + i* Dungeon.level.width();

					if (cur[cell] > 0) {

						off[cell] = cur[cell] - 1;
						volume += off[cell];

						if (Math.max(getCount(cell), 0) >= count
								|| Dungeon.level.map[cell] != Terrain.INACTIVE_TRAP){
							off[cell] = 0;
							setTerr(cell);
							continue;
						}

						boolean emit = off[cell] % turn == 0;

						if (emit) {
							// 부식 막대의 2/5
							int amountGas = 20 + 10 * buffedLvl;

							if (gas == null || gas.volume == 0) {

							} else if (gas.cur[cell] < amountGas) {
								//부식 가스가 있을때
								amountGas += gas.cur[cell];
								gas.clear(cell);
							}

							if (Dungeon.hero.fieldOfView[cell]) {
								Sample.INSTANCE.play(Assets.Sounds.GAS);
								CellEmitter.get( cell ).burst( ScholarParticle.YELLOW, 15);
							}

							CorrosiveTerrGas c = Blob.seed(cell, amountGas, CorrosiveTerrGas.class);
							c.setStrength(buffedLvl + 2);
							GameScene.add(c);
						}
					}
				}
			}
		}

		/** count = emit count, turn = time to emit, buffedLvl = Corrosive lvl */
		public void setCorrosionSeed(int count, int turn, int buffedLvl) {
			this.count = count;
			this.turn = turn;
			this.buffedLvl = buffedLvl;
		}

		public void setTerr(int cell) {
			Trap t = Dungeon.level.traps.get(cell);
			if (t != null) Dungeon.level.traps.remove(cell);

			if (Dungeon.level.map[cell] == Terrain.INACTIVE_TRAP) {
				if (Dungeon.level.heroFOV[cell]) {
					CellEmitter.get( cell ).burst( Speck.factory( Speck.WOOL ), 15);
				}
				Level.set(cell, Terrain.EMPTY);
			}

			GameScene.updateMap(cell);
			GameScene.updateFog();
			clear(cell);
		}

		public int getCount (int cell){
			int startAmount = 200 - (200 % turn);
			float emitCount = (startAmount - cur[cell]) / turn;
			return (int) Math.floor(emitCount);
		}

		@Override
		public void use( BlobEmitter emitter ) {
			super.use( emitter );
			emitter.pour(ScholarParticle.YELLOW, 0.35f );
		}

		private static final String COUNT = "count";
		private static final String TURN = "turn";
		private static final String BUFF_LVL = "buff_lvl";
		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle( bundle );
			bundle.put(COUNT, count);
			bundle.put(TURN, turn);
			bundle.put(BUFF_LVL, buffedLvl);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			count = bundle.getInt(COUNT);
			turn = bundle.getInt(TURN);
			buffedLvl = bundle.getInt(BUFF_LVL);
		}
	}

	public static class CorrosiveTerrGas extends CorrosiveGas {
		@Override
		protected void evolve() {
			super.evolve();

			int cell;

			for (int i = area.left; i < area.right; i++){
				for (int j = area.top; j < area.bottom; j++){
					cell = i + j*Dungeon.level.width();
					if (cur[cell] > 0) {
						int terr = Dungeon.level.map[cell];
						if (Dungeon.level.setCellToWater(false, cell)) {
							if (Dungeon.level.heroFOV[cell] && terr != Terrain.EMPTY) {
								CellEmitter.get( cell ).burst( Speck.factory( Speck.WOOL ), 15);
							}
							Level.set(cell, Terrain.EMPTY);
							GameScene.updateMap(cell);
							GameScene.updateFog();
						}
					}
				}
			}
		}
	}
	public static class GasVent extends Trap {

		{
			color = GREY;
			shape = GRILL;

			canBeHidden = false;
			active = false;
		}

		@Override
		public void activate() {
			int turn = Math.max(scalingDepth()/2, 8);
			int turnCount = 200 - (200 % turn);
			GasVentSeed g = (GasVentSeed) Dungeon.level.blobs.get(GasVentSeed.class);
			for (int i = 0; i < Dungeon.level.length(); i++){
				if (g != null && g.volume > 0 && g.cur[i] > 0){
					g.cur[i] = turnCount - (g.getCount(i) * turn);
				}
			}

			int bonusRange = Dungeon.hero.pointsInTalent(Talent.WIDE_SUMMON);
			GasVentSeed c = (GasVentSeed)Blob.seed(pos, turnCount, GasVentSeed.class, Dungeon.level);
			c.setCorrosionSeed(bonusRange + 2, turn, 1+scalingDepth()/4);
			Sample.INSTANCE.play(Assets.Sounds.GAS);
		}
	}
}
