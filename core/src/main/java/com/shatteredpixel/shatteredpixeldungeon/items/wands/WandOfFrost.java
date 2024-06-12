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
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.NaturesPower;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SnowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.MagicalFireRoom;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Iterator;

public class WandOfFrost extends DamageWand {

	{
		image = ItemSpriteSheet.WAND_FROST;
	}

	public int min(int lvl){
		return 2+lvl;
	}

	public int max(int lvl){
		return 8+5*lvl;
	}

	@Override
	public void onZap(Ballistica bolt) {

		Heap heap = Dungeon.level.heaps.get(bolt.collisionPos);
		if (heap != null) {
			heap.freeze();
		}

		Fire fire = (Fire) Dungeon.level.blobs.get(Fire.class);
		if (fire != null && fire.volume > 0) {
			fire.clear( bolt.collisionPos );
		}

		MagicalFireRoom.EternalFire eternalFire = (MagicalFireRoom.EternalFire)Dungeon.level.blobs.get(MagicalFireRoom.EternalFire.class);
		if (eternalFire != null && eternalFire.volume > 0) {
			eternalFire.clear( bolt.collisionPos );
			//bolt ends 1 tile short of fire, so check next tile too
			if (bolt.path.size() > bolt.dist+1){
				eternalFire.clear( bolt.path.get(bolt.dist+1) );
			}

		}

		WandOfFireblast.MiniEternalFire miniEternalFire = (WandOfFireblast.MiniEternalFire)Dungeon.level.blobs.get(WandOfFireblast.MiniEternalFire.class);
		if (miniEternalFire != null && miniEternalFire.volume > 0) {
			miniEternalFire.clear( bolt.collisionPos );
			//bolt ends 1 tile short of fire, so check next tile too
			if (bolt.path.size() > bolt.dist+1){
				miniEternalFire.clear( bolt.path.get(bolt.dist+1) );
			}

		}

		Char ch = Actor.findChar(bolt.collisionPos);
		if (ch != null){

			int damage = damageRoll();

			if (ch.buff(Frost.class) != null){
				return; //do nothing, can't affect a frozen target
			}
			if (ch.buff(Chill.class) != null){
				//6.67% less damage per turn of chill remaining, to a max of 10 turns (50% dmg)
				float chillturns = Math.min(10, ch.buff(Chill.class).cooldown());
				damage = (int)Math.round(damage * Math.pow(0.9333f, chillturns));
			} else {
				ch.sprite.burst( 0xFF99CCFF, buffedLvl() / 2 + 2 );
			}

			wandProc(ch, chargesPerCast());
			ch.damage(damage, this);
			Sample.INSTANCE.play( Assets.Sounds.HIT_MAGIC, 1, 1.1f * Random.Float(0.87f, 1.15f) );

			if (ch.isAlive()){
				if (Dungeon.level.water[ch.pos])
					Buff.affect(ch, Chill.class, 4+buffedLvl());
				else
					Buff.affect(ch, Chill.class, 2+buffedLvl());
			}
		} else {
			Dungeon.level.pressCell(bolt.collisionPos);
		}
	}

	@Override
	public void fx(Ballistica bolt, Callback callback) {
		MagicMissile.boltFromChar(curUser.sprite.parent,
				MagicMissile.FROST,
				curUser.sprite,
				bolt.collisionPos,
				callback);
		Sample.INSTANCE.play(Assets.Sounds.ZAP);
	}

	@Override
	public void onHit(MagesStaff staff, Char attacker, Char defender, int damage) {
		Chill chill = defender.buff(Chill.class);

		if (chill != null) {

			//1/9 at 2 turns of chill, scaling to 9/9 at 10 turns
			float procChance = ((int)Math.floor(chill.cooldown()) - 1)/9f;
			procChance *= procChanceMultiplier(attacker);

			if (Random.Float() < procChance) {

				float powerMulti = Math.max(1f, procChance);

				//need to delay this through an actor so that the freezing isn't broken by taking damage from the staff hit.
				new FlavourBuff() {
					{
						actPriority = VFX_PRIO;
					}

					public boolean act() {
						Buff.affect(target, Frost.class, Math.round(Frost.DURATION * powerMulti));
						return super.act();
					}
				}.attachTo(defender);
			}
		}
	}

	@Override
	public void staffFx(MagesStaff.StaffParticle particle) {
		particle.color(0x88CCFF);
		particle.am = 0.6f;
		particle.setLifespan(2f);
		float angle = Random.Float(PointF.PI2);
		particle.speed.polar( angle, 2f);
		particle.acc.set( 0f, 1f);
		particle.setSize( 0f, 1.5f);
		particle.radiateXY(Random.Float(1f));
	}

	//scholar
	@Override
	public int bonusRange () {return 4 + 2 * super.bonusRange();}
	@Override
	public int scholarTurnCount(){
		return super.scholarTurnCount() + 10;
	}
	@Override
	public void scholarAbility(Ballistica bolt, int cell) {
		super.scholarAbility(bolt,cell);
		int iceToPlace = bonusRange(); //범위

		ArrayList<Integer> cells = new ArrayList<>(bolt.path);
		ArrayList<Integer> integers = new ArrayList<>();

		for (Iterator<Integer> i = cells.iterator(); i.hasNext(); ) {
			int pos = i.next();
			int terr = Dungeon.level.map[pos];
			if (!(terr == Terrain.EMPTY || terr == Terrain.EMPTY_DECO || terr == Terrain.WATER)) {
				i.remove();
			} else if (Char.hasProp(Actor.findChar(pos), Char.Property.IMMOVABLE)) {
				i.remove();
			} else if (Dungeon.level.plants.get(pos) != null) {
				i.remove();
			} else if (Dungeon.level.distance(curUser.pos, pos) > bolt.dist) {
				i.remove();
			} else {
				Char ch = Actor.findChar(pos);
				if (ch == Dungeon.hero) i.remove();
			}
		}

		for (int pos : bolt.path) {
			if (iceToPlace > 0 && cells.contains(pos)) {
				if (!Dungeon.level.solid[pos]) {
					if (Dungeon.level.water[pos]) {

					}
					integers.add(pos);
					CellEmitter.get(pos).burst(SnowParticle.FACTORY, 8);
					Level.set(pos, Terrain.ICE);
				}

				GameScene.updateMap(pos);
				iceToPlace--;
				//moves cell to the back
				cells.remove((Integer) pos);
				cells.add(pos);
			}
		}

		Iceing ice = Buff.append(Dungeon.hero, Iceing.class);
		ice.set(scholarTurnCount()+1, Dungeon.depth, Dungeon.branch);
        ice.cells.addAll(integers);
	}

	public static class Iceing extends Buff {

		{
			revivePersists = true;
		}
		protected float left;
		int depth;
		int branch;

		protected ArrayList<Integer> cells = new ArrayList<>();

		@Override
		public boolean act() {

			spend(TICK);

			if (depth == Dungeon.depth && branch == Dungeon.branch)
				left -= TICK;

			if (left <= 0) {
				detach();
			}

			return true;
		}

		@Override
		public void detach() {
			if (depth == Dungeon.depth && branch == Dungeon.branch) {
				for (int j = 0; j <cells.size(); j++) {
					int cell = cells.get(j);
					if (Dungeon.level.map[cell] == Terrain.ICE) {
						CellEmitter.get(cell).burst(SnowParticle.FACTORY, 12);

						Dungeon.level.setCellToWater(true, cell);
						GameScene.updateMap(cell);
					}
				}
			}
			super.detach();
		}

		public void set(float left, int depth, int branch){
			this.left = left;
			this.depth = depth;
			this.branch = branch;
		}

		private static final String DEPTH  = "depth";
		private static final String BRANCH = "branch";
		private static final String CELLS  = "cells";
		private static final String LEFT   = "left";
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			int[] values = new int[cells.size()];
			for (int i = 0; i < values.length; i++)
				values[i] = cells.get(i);
			bundle.put(CELLS, values);
			bundle.put(DEPTH, depth);
			bundle.put(BRANCH, branch);
			bundle.put(LEFT, left);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			int[] values = bundle.getIntArray( CELLS );
			for (int value : values)
				cells.add(value);
			depth = bundle.getInt(DEPTH);
			branch = bundle.getInt(BRANCH);
			left = bundle.getInt(LEFT);
		}

	}
}
