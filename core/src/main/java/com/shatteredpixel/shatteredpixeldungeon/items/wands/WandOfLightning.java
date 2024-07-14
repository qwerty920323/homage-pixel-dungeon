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
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Lightning;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Shocking;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.utils.BArray;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class WandOfLightning extends DamageWand {

	{
		image = ItemSpriteSheet.WAND_LIGHTNING;
	}
	
	private ArrayList<Char> affected = new ArrayList<>();

	private ArrayList<Lightning.Arc> arcs = new ArrayList<>();

	public int min(int lvl){
		return 5+lvl;
	}

	public int max(int lvl){
		return 10+5*lvl;
	}
	
	@Override
	public void onZap(Ballistica bolt) {

		//lightning deals less damage per-target, the more targets that are hit.
		float multiplier = 0.4f + (0.6f/affected.size());
		//if the main target is in water, all affected take full damage
		if (Dungeon.level.water[bolt.collisionPos]) multiplier = 1f;

		for (Char ch : affected){
			if (ch == Dungeon.hero) PixelScene.shake( 2, 0.3f );
			ch.sprite.centerEmitter().burst( SparkParticle.FACTORY, 3 );
			ch.sprite.flash();

			if (ch != curUser && ch.alignment == curUser.alignment && ch.pos != bolt.collisionPos){
				continue;
			}
			wandProc(ch, chargesPerCast());
			if (ch == curUser && ch.isAlive()) {
				ch.damage(Math.round(damageRoll() * multiplier * 0.5f), this);
				if (!curUser.isAlive()) {
					Badges.validateDeathFromFriendlyMagic();
					Dungeon.fail( this );
					GLog.n(Messages.get(this, "ondeath"));
				}
			} else {
				ch.damage(Math.round(damageRoll() * multiplier), this);
			}
		}

		//scholar
		for (int i = 0; i < affected.size()-1; i++){
			Char ch = affected.get(i);
			Char hit = affected.get(i+1);
			resetTrap(ch, hit);
		}
	}

	@Override
	public void onHit(MagesStaff staff, Char attacker, Char defender, int damage) {
		//acts like shocking enchantment
		new LightningOnHit().proc(staff, attacker, defender, damage);
	}

	public static class LightningOnHit extends Shocking {
		@Override
		protected float procChanceMultiplier(Char attacker) {
			return Wand.procChanceMultiplier(attacker);
		}
	}

	private void arc( Char ch ) {

		int dist = Dungeon.level.water[ch.pos] ? 2 : 1;

		ArrayList<Char> hitThisArc = new ArrayList<>();
		PathFinder.buildDistanceMap( ch.pos, BArray.not( Dungeon.level.solid, null ), dist );
		for (int i = 0; i < PathFinder.distance.length; i++) {
			if (PathFinder.distance[i] < Integer.MAX_VALUE){
				Char n = Actor.findChar( i );
				if (n == Dungeon.hero && PathFinder.distance[i] > 1)
					//the hero is only zapped if they are adjacent
					continue;
				else if (n != null && !affected.contains( n )) {
					hitThisArc.add(n);
				}
			}
		}
		
		affected.addAll(hitThisArc);
		for (Char hit : hitThisArc){
			arcs.add(new Lightning.Arc(ch.sprite.center(), hit.sprite.center()));
			arc(hit);
		}
	}
	
	@Override
	public void fx(Ballistica bolt, Callback callback) {

		affected.clear();
		arcs.clear();

		int cell = bolt.collisionPos;

		Char ch = Actor.findChar( cell );
		if (ch != null) {
			if (ch instanceof DwarfKing){
				Statistics.qualifiedForBossChallengeBadge = false;
			}

			affected.add( ch );
			arcs.add( new Lightning.Arc(curUser.sprite.center(), ch.sprite.center()));
			arc(ch);
		} else {
			arcs.add( new Lightning.Arc(curUser.sprite.center(), DungeonTilemap.raisedTileCenterToWorld(bolt.collisionPos)));
			CellEmitter.center( cell ).burst( SparkParticle.FACTORY, 3 );
		}

		//don't want to wait for the effect before processing damage.
		curUser.sprite.parent.addToFront( new Lightning( arcs, null ) );
		Sample.INSTANCE.play( Assets.Sounds.LIGHTNING );
		callback.call();
	}

	@Override
	public void staffFx(MagesStaff.StaffParticle particle) {
		particle.color(0xFFFFFF);
		particle.am = 0.6f;
		particle.setLifespan(0.6f);
		particle.acc.set(0, +10);
		particle.speed.polar(-Random.Float(3.1415926f), 6f);
		particle.setSize(0f, 1.5f);
		particle.sizeJitter = 1f;
		particle.shuffleXY(1f);
		float dst = Random.Float(1f);
		particle.x -= dst;
		particle.y += dst;
	}

	//scholar
	@Override
	public int bonusRange () {return super.bonusRange()+2;} // 실제로는 0
	@Override
	public int scholarTurnCount(){
		return super.scholarTurnCount() + 1;
	}
	@Override
	public void scholarAbility(Ballistica bolt, int cell){
		super.scholarAbility(bolt,cell);
		for (int pos : bolt.path){
			Trap t = Dungeon.level.traps.get(pos);

			if (t != null && t.visible && !t.active) {
				CellEmitter.get(pos).burst(SparkParticle.FACTORY, 8);
				Sample.INSTANCE.play(Assets.Sounds.LIGHTNING);
				t.active = true;
				Level.set(pos, Terrain.TRAP);
				GameScene.updateMap(pos);
				GameScene.updateFog();
			}
		}

		ShockerBlob s = (ShockerBlob) Dungeon.level.blobs.get(ShockerBlob.class);
		if (s != null) s.fullyClear();

		ShockerBlob shockerBlob = Blob.seed(bolt.collisionPos, 1, ShockerBlob.class);
		GameScene.add(shockerBlob);
		shockerBlob.left = scholarTurnCount();
	}

	public void resetTrap(Char ch, Char hit){
		Ballistica bolt = new Ballistica(ch.pos, hit.pos, Ballistica.MAGIC_BOLT);
		for (int pos : bolt.path){
			Trap t = Dungeon.level.traps.get(pos);

			if (t != null && t.visible && !t.active) {
				CellEmitter.get(pos).burst(SparkParticle.FACTORY, 8);
				Sample.INSTANCE.play(Assets.Sounds.LIGHTNING);
				t.active = true;
				Level.set(pos, Terrain.TRAP);
				GameScene.updateMap(pos);
				GameScene.updateFog();
			}
		}
	}


	public static class ShockerBlob extends Blob {

		{
			alwaysVisible = true;
		}
		private int targetNeighbor = Random.Int(8);
		int left;
		@Override
		protected void evolve() {
			int cell;
			for (int i = area.left; i < area.right; i++){
				for (int j = area.top; j < area.bottom; j++){
					cell = i + j* Dungeon.level.width();
					off[cell] = cur[cell];
					volume += off[cell];

					if (off[cell] > 0) {
						shockAround(cell, left);
					}
				}
			}
		}

		private void shockAround(int pos, int left){
			int bonusRange = Dungeon.hero.pointsInTalent(Talent.WIDE_SUMMON);

			ArrayList<Integer> shockCells = new ArrayList<>();

			//shockCells.add(pos);
			shockCells.add(pos + PathFinder.CIRCLE8[targetNeighbor]);       //shockCells.get(0)
			shockCells.add(pos + PathFinder.CIRCLE8[(targetNeighbor+4)%8]); //get(1)

			if (bonusRange>0) {
				shockCells.add(shockCells.get(0) + PathFinder.CIRCLE8[targetNeighbor]); //get(2)

				if (bonusRange>=2) {
					shockCells.add(shockCells.get(1) + PathFinder.CIRCLE8[(targetNeighbor+4)%8]);
				}
				if (bonusRange>=3) {
					shockCells.add(shockCells.get(2) + PathFinder.CIRCLE8[targetNeighbor]);
				}
			}

			for (int cell : shockCells) {
				if (Dungeon.hero.fieldOfView[cell]) {
					Dungeon.hero.sprite.parent.add(new Lightning(DungeonTilemap.raisedTileCenterToWorld(pos),
							DungeonTilemap.raisedTileCenterToWorld(cell), null));
					CellEmitter.get(cell).burst(SparkParticle.FACTORY, 3);

				}

				Char ch = Actor.findChar(cell);

				if (ch != null && ch.buff(Paralysis.class) == null) {
					//Buff.affect(ch, Paralysis.class, left);
					ch.sprite.flash();

					if (Dungeon.hero.fieldOfView[ch.pos])
						Sample.INSTANCE.play(Assets.Sounds.LIGHTNING);
				}

				Trap t = Dungeon.level.traps.get(cell);

				if (t != null && t.visible && !t.active) {
					CellEmitter.get(cell).burst(SparkParticle.FACTORY, 8);
					Sample.INSTANCE.play(Assets.Sounds.LIGHTNING);
					t.active = true;
					Level.set(cell, Terrain.TRAP);
					GameScene.updateMap(cell);
					GameScene.updateFog();
				}
			}

			targetNeighbor = (targetNeighbor+1)%8;

		}

		@Override
		public void use(BlobEmitter emitter) {
			super.use(emitter);
			emitter.pour( SparkParticle.STATIC, 0.10f );
		}

		@Override
		public String tileDesc() {
			return Messages.get(this, "desc");
		}

		private static final String NEIGHBER = "neighber";
		private static final String LEFT = "left";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(NEIGHBER, targetNeighbor);
			bundle.put(LEFT, left);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			targetNeighbor = bundle.getInt(NEIGHBER);
			left = bundle.getInt(LEFT);
		}
	}
	// scholar
}
