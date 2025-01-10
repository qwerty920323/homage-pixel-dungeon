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
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlashDots;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Light;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Shopkeeper;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TalismanOfForesight;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.effects.Beam;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.RainbowParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class WandOfPrismaticLight extends DamageWand {

	{
		image = ItemSpriteSheet.WAND_PRISMATIC_LIGHT;

		collisionProperties = Ballistica.MAGIC_BOLT;
	}

	public int min(int lvl){
		return 1+lvl;
	}

	public int max(int lvl){
		return 5+3*lvl;
	}

	@Override
	public void onZap(Ballistica beam) {
		affectMap(beam);
		
		if (Dungeon.level.viewDistance < 6 ){
			if (Dungeon.isChallenged(Challenges.DARKNESS)){
				Buff.prolong( curUser, Light.class, 2f + buffedLvl());
			} else {
				Buff.prolong( curUser, Light.class, 10f+buffedLvl()*5);
			}
		}
		
		Char ch = Actor.findChar(beam.collisionPos);
		if (ch != null){
			wandProc(ch, chargesPerCast());
			affectTarget(ch);
		}
	}

	private void affectTarget(Char ch){
		int dmg = damageRoll();

		//three in (5+lvl) chance of failing
		if (Random.Int(5+buffedLvl()) >= 3) {
			Buff.prolong(ch, Blindness.class, 2f + (buffedLvl() * 0.333f));
			ch.sprite.emitter().burst(Speck.factory(Speck.LIGHT), 6 );
		}

		if (ch.properties().contains(Char.Property.DEMONIC) || ch.properties().contains(Char.Property.UNDEAD)){
			ch.sprite.emitter().start( ShadowParticle.UP, 0.05f, 10+buffedLvl() );
			Sample.INSTANCE.play(Assets.Sounds.BURNING);

			ch.damage(Math.round(dmg*1.333f), this);
		} else {
			ch.sprite.centerEmitter().burst( RainbowParticle.BURST, 10+buffedLvl() );

			ch.damage(dmg, this);
		}

	}

	private void affectMap(Ballistica beam){
		boolean noticed = false;
		for (int c : beam.subPath(0, beam.dist)){
			if (!Dungeon.level.insideMap(c)){
				continue;
			}
			for (int n : PathFinder.NEIGHBOURS9){
				int cell = c+n;

				if (Dungeon.level.discoverable[cell])
					Dungeon.level.mapped[cell] = true;

				int terr = Dungeon.level.map[cell];
				if ((Terrain.flags[terr] & Terrain.SECRET) != 0) {

					Dungeon.level.discover( cell );

					GameScene.discoverTile( cell, terr );
					ScrollOfMagicMapping.discover(cell);

					noticed = true;
				}
			}

			CellEmitter.center(c).burst( RainbowParticle.BURST, Random.IntRange( 1, 2 ) );
		}
		if (noticed)
			Sample.INSTANCE.play( Assets.Sounds.SECRET );

		GameScene.updateFog();
	}

	@Override
	public String upgradeStat2(int level) {
		return Messages.decimalFormat("#", 100*(1-(3/(float)(5+level)))) + "%";
	}

	@Override
	public String upgradeStat3(int level) {
		if (Dungeon.isChallenged(Challenges.DARKNESS)){
			return Integer.toString(2 + level);
		} else {
			return Integer.toString(10 + 5*level);
		}
	}

	@Override
	public void fx(Ballistica beam, Callback callback) {
		curUser.sprite.parent.add(
				new Beam.LightRay(curUser.sprite.center(), DungeonTilemap.raisedTileCenterToWorld(beam.collisionPos)));
		callback.call();
	}

	@Override
	public void onHit(MagesStaff staff, Char attacker, Char defender, int damage) {
		//cripples enemy
		Buff.prolong( defender, Cripple.class, Math.round((1+staff.buffedLvl())*procChanceMultiplier(attacker)));
	}

	@Override
	public void staffFx(MagesStaff.StaffParticle particle) {
		particle.color( Random.Int( 0x1000000 ) );
		particle.am = 0.5f;
		particle.setLifespan(1f);
		particle.speed.polar(Random.Float(PointF.PI2), 2f);
		particle.setSize( 1f, 2f);
		particle.radiateXY( 0.5f);
	}

	//scholar
	@Override
	public int scholarTurnCount(){
		return super.scholarTurnCount() + 5;
	}

	@Override
	public void scholarAbility(Ballistica bolt, int cell) {
		super.scholarAbility(bolt,cell);

		int distance = Dungeon.level.distance(curUser.pos,bolt.collisionPos); //거리
		int maxDist = distance;
		int count = bonusRange();
		int amount = 250;

		for (int pos : bolt.path) {

			ArrayList<Integer> spawnPoints = new ArrayList<>();
			for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++) {
				int p = pos + PathFinder.NEIGHBOURS8[i];
				if (Dungeon.level.passable[p] || Dungeon.level.avoid[p]) {
					spawnPoints.add(p);
				}
			}

			ArrayList<Integer> respawnPoints = new ArrayList<>();

			while (count > 0 && spawnPoints.size() > 0) {
				int index = Random.index(spawnPoints);

				respawnPoints.add(spawnPoints.remove(index));
				count--;
			}

			Char ch = Actor.findChar(pos);

			if (maxDist > 0 && ch != Dungeon.hero) {
				if (!Dungeon.level.solid[pos]) {
					amount *= (maxDist + distance) / distance;

					FireFlyBlobs flyLight = Blob.seed(pos, amount, FireFlyBlobs.class);
					flyLight.turn(scholarTurnCount());
					GameScene.add(flyLight);
					amount -= 15;

					for (Integer bonuscell : respawnPoints) {
						FireFlyBlobs f = Blob.seed(bonuscell, amount, FireFlyBlobs.class);
						f.turn(scholarTurnCount());
						GameScene.add(f);
					}
				}

				GameScene.updateMap(pos);
				maxDist--;
			}
		}
	}

	public static class FireFlyBlobs extends Blob {

		{
			alwaysVisible = true;
		}
		private static float turnLeft = 0;
		@Override
		protected void evolve() {
			super.evolve();

			int cell;
			if (volume == 0){
				turnLeft = 0;

			} else {
				for (int i = area.left - 1; i <= area.right; i++) {
					for (int j = area.top - 1; j <= area.bottom; j++) {
						cell = i + j * Dungeon.level.width();
						if (cur[cell] > 0) {

							off[cell] = 2 * cur[cell] / 3;
							shine(cell);

						}
						volume += off[cell];
					}
				}
			}

		}

		public static void shine(int cell ){
			Char ch = Actor.findChar( cell );
			if (ch != null
					&& !(ch instanceof Shopkeeper)
					&& !ch.isImmune(FireFlyBlobs.class)
					&& ch.buff(FireFly.class) == null) {
				Buff.affect( ch, FireFly.class ).set(turnLeft);
			}

			Heap h = Dungeon.level.heaps.get(cell);
			if (h != null){
				Buff.append(Dungeon.hero, TalismanOfForesight.HeapAwareness.class, turnLeft).pos = h.pos;
			}
		}

		public FireFlyBlobs turn(int left){
			turnLeft = left;
			return this;
		}

		@Override
		public void use( BlobEmitter emitter ) {
			super.use( emitter );
			emitter.pour( FlashDots.GREEN, 0.85f );
		}

		@Override
		public String tileDesc() {
			return Messages.get(this, "desc");
		}

		private static final String LEFT = "left";
		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle( bundle );
			bundle.put( LEFT, turnLeft );
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle( bundle );
			turnLeft = bundle.getFloat( LEFT );
		}

	}

	public static class FireFly extends Buff implements Hero.Doom{
		private float duration;

		{
			type = buffType.NEGATIVE;
			announced = true;
		}

		private float left;

		public void set( float duration ) {
			this.duration = Math.max(duration, left);
			left = Math.max(duration, left);
		}

		private static final String LEFT	 = "left";
		private static final String DURATION =  "left";
		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle( bundle );
			bundle.put( DURATION, duration );
			bundle.put( LEFT, left );
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			duration = bundle.getFloat( DURATION );
			left = bundle.getFloat( LEFT );
		}

		@Override
		public boolean act() {
			if (target.isAlive()) {
				if (target.fieldOfView == null || target.fieldOfView.length != Dungeon.level.length()){
					target.fieldOfView = new boolean[Dungeon.level.length()];
				}
				if (target.fieldOfView != null) {
					Dungeon.level.updateFieldOfView(target, target.fieldOfView);
				}
				GameScene.updateFog(target.pos, target.viewDistance+(int)Math.ceil(target.speed()));

				if ((target.properties().contains(Char.Property.UNDEAD)
						|| target.properties().contains(Char.Property.DEMONIC))) {
					int damage = Hero.heroDamageIntRange( 1, 1+Dungeon.scalingDepth()/6 );
					target.damage( damage, this );
					target.sprite.emitter().start( ShadowParticle.UP, 0.05f, 8 + damage );
				}
			}

			spend( TICK );
			left -= TICK;

			if (left <= 0) {
				detach();
			}

			return true;
		}
		@Override
		public void detach() {
			Actor.add(new Actor() {
				@Override
				protected boolean act() {
					Dungeon.observe();
					GameScene.updateFog();
					Actor.remove(this);
					return true;
				}
			});
			super.detach();
		}

		@Override
		public int icon() {
			return BuffIndicator.SCHOLAR_BUFF;
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (duration - left) / duration);
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString((int)left);
		}

		@Override
		public void fx(boolean on) {
			if (on) target.sprite.add(CharSprite.State.FIREFLY);
			else target.sprite.remove(CharSprite.State.FIREFLY);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", 1, 1+(Dungeon.scalingDepth()/6),(int)left);
		}

		@Override
		public void onDeath() {
			Badges.validateDeathFromFriendlyMagic();
			Dungeon.fail(this);
			GLog.n(Messages.get(this, "ondeath"));
		}
	}
}
