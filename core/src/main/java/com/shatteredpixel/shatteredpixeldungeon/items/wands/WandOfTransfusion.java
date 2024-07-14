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
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.WellWater;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Beam;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BloodParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShaftParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.journal.Notes;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Point;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

public class WandOfTransfusion extends DamageWand {

	{
		image = ItemSpriteSheet.WAND_TRANSFUSION;

		collisionProperties = Ballistica.PROJECTILE;
	}

	@Override
	public int min(int level) {
		return 3 + level;
	}

	@Override
	public int max(int level) {
		return 6 + 2*level;
	}

	private boolean freeCharge = false;

	@Override
	public void onZap(Ballistica beam) {
		for (int c : beam.subPath(0, beam.dist))
			CellEmitter.center(c).burst( BloodParticle.BURST, 1 );

		int cell = beam.collisionPos;

		Char ch = Actor.findChar(cell);

		if (ch instanceof Mob){
			
			wandProc(ch, chargesPerCast());
			
			//this wand does different things depending on the target.
			
			//heals/shields an ally or a charmed enemy while damaging self
			if (ch.alignment == Char.Alignment.ALLY || ch.buff(Charm.class) != null){
				
				// 5% of max hp
				int selfDmg = Math.round(curUser.HT*0.05f);
				
				int healing = selfDmg + 3*buffedLvl();
				int shielding = (ch.HP + healing) - ch.HT;
				if (shielding > 0){
					healing -= shielding;
					Buff.affect(ch, Barrier.class).setShield(shielding);
				} else {
					shielding = 0;
				}
				
				ch.HP += healing;
				
				ch.sprite.emitter().burst(Speck.factory(Speck.HEALING), 2 + buffedLvl() / 2);
				if (healing > 0) {
					ch.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healing), FloatingText.HEALING);
				}
				if (shielding > 0){
					ch.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shielding), FloatingText.SHIELDING);
				}
				
				if (!freeCharge) {
					damageHero(selfDmg);
				} else {
					freeCharge = false;
				}

			//for enemies...
			//(or for mimics which are hiding, special case)
			} else if (ch.alignment == Char.Alignment.ENEMY || ch instanceof Mimic) {

				//grant a self-shield, and...
				Buff.affect(curUser, Barrier.class).setShield((5 + buffedLvl()));
				curUser.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(5+buffedLvl()), FloatingText.SHIELDING);
				
				//charms living enemies
				if (!ch.properties().contains(Char.Property.UNDEAD)) {
					Charm charm = Buff.affect(ch, Charm.class, Charm.DURATION/2f);
					charm.object = curUser.id();
					charm.ignoreHeroAllies = true;
					ch.sprite.centerEmitter().start( Speck.factory( Speck.HEART ), 0.2f, 3 );
				
				//harms the undead
				} else {
					ch.damage(damageRoll(), this);
					ch.sprite.emitter().start(ShadowParticle.UP, 0.05f, 10 + buffedLvl());
					Sample.INSTANCE.play(Assets.Sounds.BURNING);
				}

			}
			
		}
		
	}

	//this wand costs health too
	private void damageHero(int damage){
		
		curUser.damage(damage, this);

		if (!curUser.isAlive()){
			Badges.validateDeathFromFriendlyMagic();
			Dungeon.fail( this );
			GLog.n( Messages.get(this, "ondeath") );
		}
	}

	@Override
	public void onHit(MagesStaff staff, Char attacker, Char defender, int damage) {
		if (defender.buff(Charm.class) != null && defender.buff(Charm.class).object == attacker.id()){
			//grants a free use of the staff and shields self
			freeCharge = true;
			int shieldToGive = Math.round((2*(5 + buffedLvl()))*procChanceMultiplier(attacker));
			Buff.affect(attacker, Barrier.class).setShield(shieldToGive);
			attacker.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shieldToGive), FloatingText.SHIELDING);
			GLog.p( Messages.get(this, "charged") );
			attacker.sprite.emitter().burst(BloodParticle.BURST, 20);
		}
	}

	@Override
	public void fx(Ballistica beam, Callback callback) {
		curUser.sprite.parent.add(
				new Beam.HealthRay(curUser.sprite.center(), DungeonTilemap.raisedTileCenterToWorld(beam.collisionPos)));
		callback.call();
	}

	@Override
	public void staffFx(MagesStaff.StaffParticle particle) {
		particle.color( 0xCC0000 );
		particle.am = 0.6f;
		particle.setLifespan(1f);
		particle.speed.polar( Random.Float(PointF.PI2), 2f );
		particle.setSize( 1f, 2f);
		particle.radiateXY(0.5f);
	}

	@Override
	public String statsDesc() {
		int selfDMG = Math.round(Dungeon.hero.HT*0.05f);
		if (levelKnown)
			return Messages.get(this, "stats_desc", selfDMG, selfDMG + 3*buffedLvl(), 5+buffedLvl(), min(), max());
		else
			return Messages.get(this, "stats_desc", selfDMG, selfDMG, 5, min(0), max(0));
	}

	private static final String FREECHARGE = "freecharge";

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		freeCharge = bundle.getBoolean( FREECHARGE );
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put( FREECHARGE, freeCharge );
	}

	//scholar
	@Override
	public int bonusRange(){
		return scholarTurnCount() + super.bonusRange() + 2;
	}
	@Override
	public int scholarTurnCount(){
		int result = Math.round((20 + 5*(Dungeon.hero.lvl-1))* 0.05f);
		return result + super.scholarTurnCount();
	}
	@Override
	public void scholarAbility(Ballistica bolt, int cell) {
		super.scholarAbility(bolt,cell);

		int pos = bolt.collisionPos;
		int terr = Dungeon.level.map[pos];

		WaterOfMiniHealth miniHealth = (WaterOfMiniHealth) Dungeon.level.blobs.get(WaterOfMiniHealth.class);
		int healthPos = -1;
		for (int i = 0; i < Dungeon.level.length(); i++) {
			if (miniHealth != null && miniHealth.volume > 0 && miniHealth.cur[i] > 0) {
				healthPos = i;
			}
		}

		int range = Random.Int(scholarTurnCount(), bonusRange());

		if (healthPos>0) {
			if (healthPos == pos && terr == Terrain.WELL) {
				setWell(pos, range,-1);
			} else if (terrCheck(pos, Terrain.EMPTY_WELL)) {

				if (healthPos != pos) {
					CellEmitter.get(healthPos).start(Speck.factory(Speck.LIGHT), 0.2f, 4);
					Level.set(healthPos, miniHealth.terrian);
					GameScene.updateMap(healthPos);
					miniHealth.clear(healthPos);
				}

				setWell(pos, range, terr);

			}
		} else if (terrCheck(pos, Terrain.EMPTY_WELL)){
			setWell(pos, range, terr);
		}
	}
	public void setWell (int pos, int range, int terr) {
		Level.set(pos, Terrain.WELL);

		WaterOfMiniHealth water = Blob.seed(pos, 1, WaterOfMiniHealth.class);
		water.setHealth(range);
		if (terr >= 0) water.terrian = terr;

		CellEmitter.get(pos).start( Speck.factory( Speck.LIGHT ), 0.2f , 4 );
		GameScene.add(water);
		GameScene.updateMap(pos);
	}



	public static class WaterOfMiniHealth extends WellWater {
		private int turnLeft = 0;
		public int terrian = -1;

		@Override
		protected void evolve() {
			super.evolve();

			int cell;
			for (int i=area.top-1; i <= area.bottom; i++) {
				for (int j = area.left-1; j <= area.right; j++) {
					cell = j + i* Dungeon.level.width();
					if (Dungeon.level.insideMap(cell)) {
						if (Dungeon.level.map[cell] != Terrain.WELL)
							cur[cell] = 0;
					}
					Notes.remove(record());
				}
			}
		}
		@Override
		protected boolean affectHero( Hero hero ) {

			if (!hero.isAlive()) return false;

			Sample.INSTANCE.play( Assets.Sounds.DRINK );
			//health
			Buff.affect(hero, Health.class).boost(turnLeft);

			CellEmitter.get( hero.pos ).start( ShaftParticle.FACTORY, 0.2f , 3 );
			hero.sprite.emitter().start( Speck.factory( Speck.HEALING ), 0.4f, 4 );
			turnLeft = 0;

			Dungeon.hero.interrupt();

			GLog.p( Messages.get(this, "procced") );

			return true;
		}

		public WaterOfMiniHealth setHealth (int left){
			this.turnLeft += left;
			return this;
		}

		@Override
		protected Item affectItem(Item item, int pos) {
			return null;
		}

		@Override
		public void use( BlobEmitter emitter ) {
			super.use( emitter );
			float interval = Dungeon.hero.HT;
			emitter.start( Speck.factory( Speck.HEALING ), 0.6f - (0.2f * turnLeft/interval), 0 );
		}

		@Override
		public String tileDesc() {
			return Messages.get(this, "desc", turnLeft);
		}

		@Override
		protected Notes.Landmark record() {
			return Notes.Landmark.WELL_OF_MINI_HEALTH;
		}

		private static final String TURNLEFT = "turnleft";
		private static final String TERRIAN	= "terrian";
		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			turnLeft = bundle.getInt( TURNLEFT );
			terrian = bundle.getInt( TERRIAN );
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put( TURNLEFT, turnLeft);
			bundle.put( TERRIAN, terrian );
		}
	}

	public static class Health extends Buff {
		private static final float STEP = 1f;
		private float duration, left, partialHeal;
		private int pos;

		{
			type = buffType.POSITIVE;
			announced = true;
		}

		@Override
		public boolean act() {
			if (target.pos != pos) {
				detach();
			}

			//for the hero, full heal takes ~50/93/111/120 turns at levels 1/10/20/30
			partialHeal += (40 + target.HT)/150f;

			if (partialHeal > 1){
				int healThisTurn = (int)partialHeal;
				partialHeal -= healThisTurn;
				left -= healThisTurn;

				if (target.HP < target.HT) {

					target.HP += healThisTurn;
					target.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healThisTurn), FloatingText.HEALING);

					if (target.HP >= target.HT) {
						target.HP = target.HT;
						if (target instanceof Hero) {
							((Hero) target).resting = false;
						}
					}
				}
			}

			if (left <= 0) {
				detach();
				if (target instanceof Hero){
					((Hero)target).resting = false;
				}
			}
			spend( STEP );
			return true;
		}

		public void boost( int amount ){
			if (target != null) {
				this.duration = left = Math.max(left, amount);
				pos = target.pos;
			}
		}
		@Override
		public int icon() {
			return BuffIndicator.HERB_HEALING;
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
		public String desc() {
			return Messages.get(this, "desc", (int)left);
		}

		private static final String POS	= "pos";
		private static final String PARTIAL = "partial_heal";
		private static final String DURATION = "duration";
		private static final String LEFT = "left";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle( bundle );
			bundle.put( POS, pos );
			bundle.put( PARTIAL, partialHeal );
			bundle.put( DURATION, duration );
			bundle.put( LEFT, left );
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle( bundle );
			pos = bundle.getInt( POS );
			partialHeal = bundle.getFloat( PARTIAL );
			duration = bundle.getFloat( DURATION );
			left = bundle.getFloat( LEFT );
		}
	}
}
