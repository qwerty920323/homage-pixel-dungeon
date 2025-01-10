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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Lightning;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Shocking;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.BArray;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.GameMath;
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
			spawnCell(ch.pos, hit.pos);
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
	public int bonusRange () {return 3 * super.bonusRange() +6;}
	@Override
	public int scholarTurnCount(){
		return super.scholarTurnCount() + 6;
	}
	@Override
	public void scholarAbility(Ballistica bolt, int cell){
		super.scholarAbility(bolt,cell);

		spawnCell(curUser.pos, bolt.collisionPos);

	}

	public void spawnShocker (int pos) {
		ShockerBlob shockerBlob = Blob.seed(pos, scholarTurnCount(), ShockerBlob.class);
		GameScene.add(shockerBlob);
		shockerBlob.range = bonusRange();
	}

	public void spawnCell (int start, int end){
		if (((Hero)curUser).subClass != HeroSubClass.SCHOLAR) return;

		Ballistica bolt = new Ballistica( start, end, Ballistica.MAGIC_BOLT);
		int dist = Dungeon.level.distance( start, end );

		for (int pos : bolt.subPath(0, dist)) {
			spawnShocker(pos);
		}
	}

	public static class ShockerBlob extends Blob {

		{
			alwaysVisible = true;
		}

		private int targetNeighbor = Random.Int(8);
		int range;
		@Override
		protected void evolve() {
			int cell;
			boolean on = false;
			
			for (int i = area.left; i < area.right; i++){
				for (int j = area.top; j < area.bottom; j++){
					cell = i + j* Dungeon.level.width();

					if (cur[cell] > 0) {
						off[cell] = cur[cell] - 1;
						volume += off[cell];

						if (Dungeon.level.distance(Dungeon.hero.pos, cell) > range) {
							off[cell] = 0;
						}

						on = true;

					} else {
						off[cell] = 0;
					}
				}
			}

			if (on) {
				energySourceSprite = null;
				targetNeighbor = (targetNeighbor+1)%8;
			}
		}

		private void shockCells(int pos){
			ArrayList<Integer> shockCells = new ArrayList<>();

			shockCells.add(pos);
			shockCells.add(pos + PathFinder.CIRCLE8[targetNeighbor]);
			shockCells.add(pos + PathFinder.CIRCLE8[(targetNeighbor+2)%8]);
			shockCells.add(pos + PathFinder.CIRCLE8[(targetNeighbor+4)%8]);
			shockCells.add(pos + PathFinder.CIRCLE8[(targetNeighbor+6)%8]);

			float charge = (Random.Int(range) +1)/10f;

			for (int cell : shockCells) {
				int p = new Ballistica(pos, cell, Ballistica.STOP_SOLID | Ballistica.STOP_TARGET).collisionPos;

				if (p != cell) continue;

				if (Dungeon.hero.fieldOfView[cell] && !Dungeon.level.solid[cell]) {
					Dungeon.hero.sprite.parent.add(new Lightning(DungeonTilemap.raisedTileCenterToWorld(pos),
							DungeonTilemap.raisedTileCenterToWorld(cell), null));
					CellEmitter.get(cell).burst(SparkParticle.FACTORY, 3);
				}
			}
		}

		private static CharSprite energySourceSprite = null;

		private static Emitter.Factory DIRECTED_SPARKS = new Emitter.Factory() {
			@Override
			public void emit(Emitter emitter, int index, float x, float y) {
				if (energySourceSprite == null){
					Char ch = null;
					for (Char c : Actor.chars()){
						if (c instanceof Hero) continue;

						if (ch == null) ch = c;

						else if (Dungeon.level.distance(Dungeon.hero.pos, c.pos)
								< Dungeon.level.distance(Dungeon.hero.pos, ch.pos)){
							ch = c;
						}
					}

					if (ch != null) energySourceSprite = ch.sprite;

					else energySourceSprite = Dungeon.hero.sprite;

				}

				float dist = (float)Math.max( Math.abs(energySourceSprite.x - x), Math.abs(energySourceSprite.y - y) );
				dist = GameMath.gate(0, dist-40, 320);
				//more sparks closer up
				if (Random.Float(360) > dist) {

					SparkParticle s = ((SparkParticle) emitter.recycle(SparkParticle.class));
					s.resetAttracting(x, y, energySourceSprite);
				}
			}

			@Override
			public boolean lightMode() {
				return true;
			}
		};

		@Override
		public void fullyClear() {
			super.fullyClear();
			energySourceSprite = null;
		}

		@Override
		public void use(BlobEmitter emitter) {
			super.use(emitter);
			energySourceSprite = null;
			emitter.pour(DIRECTED_SPARKS, 0.15f);
		}

		@Override
		public String tileDesc() {
			return Messages.get(this, "desc");
		}

		private static final String NEIGHBER = "neighber";
		private static final String RANGE = "range";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(NEIGHBER, targetNeighbor);
			bundle.put(RANGE, range);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			targetNeighbor = bundle.getInt(NEIGHBER);
			range = bundle.getInt(RANGE);
		}
	}
	// scholar
}
