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
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blizzard;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Freezing;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WildMagic;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ElmoParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blazing;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.GameMath;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class WandOfFireblast extends DamageWand {

	{
		image = ItemSpriteSheet.WAND_FIREBOLT;

		//only used for targeting, actual projectile logic is Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID
		collisionProperties = Ballistica.WONT_STOP;
	}

	//1/2/3 base damage with 1/2/3 scaling based on charges used
	public int min(int lvl){
		return (1+lvl) * chargesPerCast();
	}

	//2/8/18 base damage with 2/4/6 scaling based on charges used
	public int max(int lvl){
		switch (chargesPerCast()){
			case 1: default:
				return 2 + 2*lvl;
			case 2:
				return 2*(4 + 2*lvl);
			case 3:
				return 3*(6+2*lvl);
		}
	}

	ConeAOE cone;

	@Override
	public void onZap(Ballistica bolt) {

		ArrayList<Char> affectedChars = new ArrayList<>();
		ArrayList<Integer> adjacentCells = new ArrayList<>();
		ArrayList<Integer> fireCells = new ArrayList<>();
		for( int cell : cone.cells ){

			//ignore caster cell
			if (cell == bolt.sourcePos){
				continue;
			}

			//knock doors open
			if (Dungeon.level.map[cell] == Terrain.DOOR){
				Level.set(cell, Terrain.OPEN_DOOR);
				GameScene.updateMap(cell);
			}

			//only ignite cells directly near caster if they are flammable or solid
			if (Dungeon.level.adjacent(bolt.sourcePos, cell)
					&& !(Dungeon.level.flamable[cell] || Dungeon.level.solid[cell])){
				adjacentCells.add(cell);
				//do burn any heaps located here though
				if (Dungeon.level.heaps.get(cell) != null){
					Dungeon.level.heaps.get(cell).burn();
				}
			} else {
				GameScene.add( Blob.seed( cell, 1+chargesPerCast(), Fire.class ) );
				//scholar
				if (Actor.findChar(cell) == null && Dungeon.level.passable[cell])
					fireCells.add(cell);
			}

			Char ch = Actor.findChar( cell );
			if (ch != null) {
				affectedChars.add(ch);
			}

		}

		//if wand was shot right at a wall
		if (cone.cells.isEmpty()){
			adjacentCells.add(bolt.sourcePos);
		}

		setBonusFire(fireCells, affectedChars, bonusRange()-1); //scholar

		//ignite cells that share a side with an adjacent cell, are flammable, and are closer to the collision pos
		//This prevents short-range casts not igniting barricades or bookshelves
		for (int cell : adjacentCells){
			for (int i : PathFinder.NEIGHBOURS8){
				if (Dungeon.level.trueDistance(cell+i, bolt.collisionPos) < Dungeon.level.trueDistance(cell, bolt.collisionPos)
						&& Dungeon.level.flamable[cell+i]
						&& Fire.volumeAt(cell+i, Fire.class) == 0){
					GameScene.add( Blob.seed( cell+i, 1+chargesPerCast(), Fire.class ) );
				}
			}
		}

		for ( Char ch : affectedChars ){
			wandProc(ch, chargesPerCast());
			ch.damage(damageRoll(), this);
			if (ch.isAlive()) {
				Buff.affect(ch, Burning.class).reignite(ch);
				switch (chargesPerCast()) {
					case 1:
						break; //no effects
					case 2:
						Buff.affect(ch, Cripple.class, 4f);
						break;
					case 3:
						Buff.affect(ch, Paralysis.class, 4f);
						break;
				}
			}
		}
	}

	@Override
	public void onHit(MagesStaff staff, Char attacker, Char defender, int damage) {
		//acts like blazing enchantment
		new FireBlastOnHit().proc( staff, attacker, defender, damage);
	}

	public static class FireBlastOnHit extends Blazing {
		@Override
		protected float procChanceMultiplier(Char attacker) {
			return Wand.procChanceMultiplier(attacker);
		}
	}

	@Override
	public void fx(Ballistica bolt, Callback callback) {
		//need to perform flame spread logic here so we can determine what cells to put flames in.

		// 5/7/9 distance
		int maxDist = 3 + 2*chargesPerCast();

		cone = new ConeAOE( bolt,
				maxDist,
				30 + 20*chargesPerCast(),
				Ballistica.STOP_TARGET | Ballistica.STOP_SOLID |
						     Ballistica.IGNORE_SOFT_SOLID | Ballistica.IGNORE_SOLID);


		//cast to cells at the tip, rather than all cells, better performance.
		Ballistica longestRay = null;
		for (Ballistica ray : cone.outerRays){
			if (longestRay == null || ray.dist > longestRay.dist){
				longestRay = ray;
			}
			((MagicMissile)curUser.sprite.parent.recycle( MagicMissile.class )).reset(
					MagicMissile.FIRE_CONE,
					curUser.sprite,
					ray.path.get(ray.dist),
					null
			);
		}

		//final zap at half distance of the longest ray, for timing of the actual wand effect
		MagicMissile.boltFromChar( curUser.sprite.parent,
				MagicMissile.FIRE_CONE,
				curUser.sprite,
				longestRay.path.get(longestRay.dist/2),
				callback );
		Sample.INSTANCE.play( Assets.Sounds.ZAP );
		Sample.INSTANCE.play( Assets.Sounds.BURNING );
	}

	@Override
	protected int chargesPerCast() {
		if (cursed || charger != null && charger.target.buff(WildMagic.WildMagicTracker.class) != null){
			return 1;
		}
		//consumes 30% of current charges, rounded up, with a min of 1 and a max of 3.
		return (int) GameMath.gate(1, (int)Math.ceil(curCharges*0.3f), 3);
	}

	@Override
	public String statsDesc() {
		if (levelKnown)
			return Messages.get(this, "stats_desc", chargesPerCast(), min(), max());
		else
			return Messages.get(this, "stats_desc", chargesPerCast(), min(0), max(0));
	}

	@Override
	public void staffFx(MagesStaff.StaffParticle particle) {
		particle.color( 0xEE7722 );
		particle.am = 0.5f;
		particle.setLifespan(0.6f);
		particle.acc.set(0, -40);
		particle.setSize( 0f, 3f);
		particle.shuffleXY( 1.5f );
	}

	//scholar
	@Override
	public int bonusRange () {return super.bonusRange()+2;} // 실제로는 +1
	@Override
	public int scholarTurnCount(){
		return super.scholarTurnCount() + 2;
	}

	@Override
	public void scholarAbility(Ballistica bolt, int cell){
		super.scholarAbility(bolt,cell);
		firePosition(bolt.collisionPos);
	}

	public int firePosition (int cell) {
		int maxDist = 3 + 2*chargesPerCast();
		int ball = cursed ? Ballistica.MAGIC_BOLT : Ballistica.PROJECTILE;

		MiniEternalFire miniFire = (MiniEternalFire)Dungeon.level.blobs.get( MiniEternalFire.class );
		if (miniFire != null) {
			miniFire.fullyClear();
		}

		if (this.cursed) setCursedFire(cell, bonusRange()-1);

		Ballistica ballistica = new Ballistica(curUser.pos, cell, ball | Ballistica.IGNORE_SOLID);
		int pos = ballistica.collisionPos;

		int dist = ballistica.dist - maxDist;
		if (dist > 0 && !this.cursed) {
			pos = ballistica.path.get(ballistica.dist-dist);
		}

		if (Dungeon.level.passable[pos]) {

			int backTrace = ballistica.dist - 1;
			while (Actor.findChar(pos) != null && pos != curUser.pos) {
				pos = ballistica.path.get(backTrace);
				backTrace--;
			}

			if (pos != curUser.pos) {
				MiniEternalFire fire = Blob.seed(pos, scholarTurnCount(), MiniEternalFire.class);
				GameScene.add(fire);
				fire.nearbyHero++;
			} else {
				setCursedFire(pos, 1);
			}
		}

		return pos;
	}
	public void setCursedFire (int cell, int count){
		int pos =  new Ballistica(curUser.pos, cell, Ballistica.MAGIC_BOLT | Ballistica.IGNORE_SOLID).collisionPos;

		ArrayList<Integer> spawnPoints = new ArrayList<>();
		for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++) {
			int p = pos + PathFinder.NEIGHBOURS8[i];
			if (Dungeon.level.passable[p]
					&& Actor.findChar(p) == null
					&& !(Dungeon.level.adjacent(curUser.pos, p))) {
				spawnPoints.add(p);
			}
		}

		ArrayList<Integer> respawnPoints = new ArrayList<>();

		while (count > 0 && spawnPoints.size() > 0) {
			int index = Random.index( spawnPoints );

			respawnPoints.add( spawnPoints.remove( index ) );
			count--;
		}

		for (Integer cells : respawnPoints) {
			MiniEternalFire fire = Blob.seed(cells, scholarTurnCount(), MiniEternalFire.class);
			GameScene.add(fire);
			fire.nearbyHero++;
		}
	}

	public void setBonusFire (ArrayList<Integer> fireCells, ArrayList<Char> affectedChars, int count) {
		if (((Hero) curUser).subClass != HeroSubClass.SCHOLAR) {
			return;
		}

		ArrayList<Integer> result = new ArrayList<>();
		if (!affectedChars.isEmpty()) {
			Char enemy = affectedChars.get(Random.Int(affectedChars.size() - 1));

			ArrayList<Integer> spawnPoints = new ArrayList<>();

			if (Dungeon.level.adjacent(enemy.pos, curUser.pos)) {
				for (int cell : cone.cells) {
					if (Dungeon.level.distance(enemy.pos, cell) == 1
							&& Actor.findChar(cell) == null && Dungeon.level.passable[cell]) {
						spawnPoints.add(cell);
					}
				}
			} else {
				for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++) {
					int p = enemy.pos + PathFinder.NEIGHBOURS8[i];
					if (Actor.findChar(p) == null && Dungeon.level.passable[p]
							&& Dungeon.level.trueDistance(p, curUser.pos) < Dungeon.level.trueDistance(enemy.pos, curUser.pos)) {
						spawnPoints.add(p);
					}
				}
			}

			while (count > 0 && !spawnPoints.isEmpty()) {
				result.add(spawnPoints.remove(Random.index(spawnPoints)));
				count--;
			}
		}

		while (count > 0 && !fireCells.isEmpty()) {
			result.add(fireCells.remove(Random.index(fireCells)));
			count--;
		}

		while (!result.isEmpty()) {
			int pos = result.remove( Random.index(result) );

			if (MiniEternalFire.volumeAt(pos, MiniEternalFire.class) == 0) {
				MiniEternalFire fire = Blob.seed(pos, scholarTurnCount() , MiniEternalFire.class);
				GameScene.add(fire);
				fire.nearbyHero++;
			}
		}

	}


	public static class MiniEternalFire extends Blob {
		public int nearbyHero;
		@Override
		protected void evolve() {

			int cell;

			Freezing freeze = (Freezing)Dungeon.level.blobs.get( Freezing.class );
			Blizzard bliz = (Blizzard)Dungeon.level.blobs.get( Blizzard.class );

			Fire fire = (Fire)Dungeon.level.blobs.get( Fire.class );

			Level l = Dungeon.level;
			for (int i = area.left - 1; i <= area.right; i++){
				for (int j = area.top - 1; j <= area.bottom; j++){
					cell = i + j*l.width();

					if (cur[cell] > 0){
						//evaporates in the presence of water, frost, or blizzard
						//this blob is not considered interchangeable with fire, so those blobs do not interact with it otherwise
						//potion of purity can cleanse it though
						if (l.water[cell] || l.map[cell] == Terrain.ICE){
							off[cell] = cur[cell] = 0;

						}
						//overrides fire
						if (fire != null && fire.volume > 0 && fire.cur[cell] > 0){
							fire.clear(cell);
						}

						//clears itself if there is frost/blizzard on or next to it
						for (int k : PathFinder.NEIGHBOURS9) {

							if (freeze != null && freeze.volume > 0 && freeze.cur[cell + k] > 0) {
								freeze.clear(cell);
								off[cell] = cur[cell] = 0;

							}
							if (bliz != null && bliz.volume > 0 && bliz.cur[cell + k] > 0) {
								bliz.clear(cell);
								off[cell] = cur[cell] = 0;

							}

							//spread fire to nearby flammable cells
							if (Dungeon.level.flamable[cell + k]
									&& (fire == null || fire.volume == 0 || fire.cur[cell + k] == 0)) {
								GameScene.add(Blob.seed(cell + k, 4, Fire.class));
							}

							//ignite adjacent chars, but only on outside and non-water cells
							Char ch = Actor.findChar(cell + k);
							if (ch != null
									&& !ch.isImmune(getClass())
									&& ch.buff(Burning.class) == null
									&& (Dungeon.level.map[cell + k] != Terrain.WATER
									|| Dungeon.level.map[cell + k] != Terrain.ICE)) {
								if (ch instanceof Hero){
									if (nearbyHero <= 0)Buff.affect(ch, Burning.class).reignite(ch, 4);

								} else {
									Buff.affect(ch, Burning.class).reignite(ch, 4f);

								}
							}

							//burn adjacent heaps, but only on outside and non-water cells
							if (Dungeon.level.heaps.get(cell + k) != null
									&& (Dungeon.level.map[cell + k] != Terrain.WATER
									|| Dungeon.level.map[cell + k] != Terrain.ICE)) {
								Dungeon.level.heaps.get(cell + k).burn();
							}

						}

						if (nearbyHero > 0) nearbyHero--;

					}

					off[cell] = cur[cell] > 0 ? cur[cell] - 1 : 0;

					volume += off[cell];

					l.passable[cell] = cur[cell] == 0 && (Terrain.flags[l.map[cell]] & Terrain.PASSABLE) != 0;

					if (cur[cell] <= 0) Dungeon.level.buildFlagMaps();

				}
			}
		}

		@Override
		public void seed(Level level, int cell, int amount) {
			super.seed(level, cell, amount);
			level.passable[cell] = cur[cell] == 0 && (Terrain.flags[level.map[cell]] & Terrain.PASSABLE) != 0;
		}

		@Override
		public void use( BlobEmitter emitter ) {
			super.use( emitter );
			emitter.pour( ElmoParticle.FACTORY, 0.02f );
		}

		@Override
		public void clear(int cell) {
			super.clear(cell);
			Dungeon.level.buildFlagMaps();
		}
		@Override
		public void fullyClear() {
			super.fullyClear();
			Dungeon.level.buildFlagMaps();
		}

		@Override
		public String tileDesc() {
			return Messages.get(this, "desc");
		}

		@Override
		public void onBuildFlagMaps( Level l ) {
			if (volume > 0){
				for (int i=0; i < l.length(); i++) {
					l.passable[i] = l.passable[i] && cur[i] == 0;
				}
			}
		}
	}
	//scholar
}
