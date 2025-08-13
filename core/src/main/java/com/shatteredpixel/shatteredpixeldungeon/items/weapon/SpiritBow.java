/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2025 Evan Debenham
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

package com.shatteredpixel.shatteredpixeldungeon.items.weapon;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArrowBlast;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CounterBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PinArrow;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RevealedArea;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.NaturesPower;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Splash;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfSharpshooting;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Projecting;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.plants.Blindweed;
import com.shatteredpixel.shatteredpixeldungeon.plants.Firebloom;
import com.shatteredpixel.shatteredpixeldungeon.plants.Icecap;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import com.shatteredpixel.shatteredpixeldungeon.plants.Sorrowmoss;
import com.shatteredpixel.shatteredpixeldungeon.plants.Stormvine;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

public class SpiritBow extends Weapon {
	
	public static final String AC_SHOOT		= "SHOOT";
	
	{
		image = ItemSpriteSheet.SPIRIT_BOW;
		
		defaultAction = AC_SHOOT;
		usesTargeting = true;
		
		unique = true;
		bones = false;
	}
	
	public boolean sniperSpecial = false;
	public float sniperSpecialBonusDamage = 0f;
	
	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.remove(AC_EQUIP);
		actions.add(AC_SHOOT);
		return actions;
	}
	
	@Override
	public void execute(Hero hero, String action) {
		
		super.execute(hero, action);
		
		if (action.equals(AC_SHOOT)) {
			
			curUser = hero;
			curItem = this;
			GameScene.selectCell( shooter );
			
		}
	}

	private static Class[] harmfulPlants = new Class[]{
			Blindweed.class, Firebloom.class, Icecap.class, Sorrowmoss.class,  Stormvine.class
	};

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		//arrow blast add only enchantment without anything else
		if (attacker.buff(Talent.EnchantBlastTracker.class) != null) {
			return super.proc(attacker, defender, damage);
		}

		if (attacker.buff(NaturesPower.naturesPowerTracker.class) != null && !sniperSpecial){

			Actor.add(new Actor() {
				{
					actPriority = VFX_PRIO;
				}

				@Override
				protected boolean act() {

					if (Random.Int(12) < ((Hero)attacker).pointsInTalent(Talent.NATURES_WRATH)){
						Plant plant = (Plant) Reflection.newInstance(Random.element(harmfulPlants));
						plant.pos = defender.pos;
						plant.activate( defender.isAlive() ? defender : null );
					}

					if (!defender.isAlive()){
						NaturesPower.naturesPowerTracker tracker = attacker.buff(NaturesPower.naturesPowerTracker.class);
						if (tracker != null){
							tracker.extend(((Hero) attacker).pointsInTalent(Talent.WILD_MOMENTUM));
						}
					}

					Actor.remove(this);
					return true;
				}
			});

		}

		if (((Hero)attacker).subClass == HeroSubClass.RANGER){
			Buff.prolong(attacker, ArrowBlast.class, ArrowBlast.DURATION);
			//ranger pinArrow
			if (defender.isAlive()) {
				PinArrow.prolong(defender, PinArrow.class, PinArrow.DURATION).count++;
			}
		}

		return super.proc(attacker, defender, damage);
	}

	@Override
	public String info() {
		String info = super.info();
		
		info += "\n\n" + Messages.get( SpiritBow.class, "stats",
				Math.round(augment.damageFactor(min())),
				Math.round(augment.damageFactor(max())),
				STRReq());
		
		if (STRReq() > Dungeon.hero.STR()) {
			info += " " + Messages.get(Weapon.class, "too_heavy");
		} else if (Dungeon.hero.STR() > STRReq()){
			info += " " + Messages.get(Weapon.class, "excess_str", Dungeon.hero.STR() - STRReq());
		}
		
		switch (augment) {
			case SPEED:
				info += "\n\n" + Messages.get(Weapon.class, "faster");
				break;
			case DAMAGE:
				info += "\n\n" + Messages.get(Weapon.class, "stronger");
				break;
			case NONE:
		}

		if (enchantment != null && (cursedKnown || !enchantment.curse())){
			info += "\n\n" + Messages.capitalize(Messages.get(Weapon.class, "enchanted", enchantment.name()));
			if (enchantHardened) info += " " + Messages.get(Weapon.class, "enchant_hardened");
			info += " " + enchantment.desc();
		} else if (enchantHardened){
			info += "\n\n" + Messages.get(Weapon.class, "hardened_no_enchant");
		}
		
		if (cursed && isEquipped( Dungeon.hero )) {
			info += "\n\n" + Messages.get(Weapon.class, "cursed_worn");
		} else if (cursedKnown && cursed) {
			info += "\n\n" + Messages.get(Weapon.class, "cursed");
		} else if (!isIdentified() && cursedKnown){
			info += "\n\n" + Messages.get(Weapon.class, "not_cursed");
		}
		
		info += "\n\n" + Messages.get(MissileWeapon.class, "distance");
		
		return info;
	}
	
	@Override
	public int STRReq(int lvl) {
		return STRReq(1, lvl); //tier 1
	}
	
	@Override
	public int min(int lvl) {
		int dmg = 1 + Dungeon.hero.lvl/5
				+ RingOfSharpshooting.levelDamageBonus(Dungeon.hero)
				+ (curseInfusionBonus ? 1 + Dungeon.hero.lvl/30 : 0);
		return Math.max(0, dmg);
	}
	
	@Override
	public int max(int lvl) {
		int dmg = 6 + (int)(Dungeon.hero.lvl/2.5f)
				+ 2*RingOfSharpshooting.levelDamageBonus(Dungeon.hero)
				+ (curseInfusionBonus ? 2 + Dungeon.hero.lvl/15 : 0);
		return Math.max(0, dmg);
	}

	@Override
	public int targetingPos(Hero user, int dst) {
		return knockArrow().targetingPos(user, dst);
	}
	
	private int targetPos;
	
	@Override
	public int damageRoll(Char owner) {
		int damage = augment.damageFactor(super.damageRoll(owner));
		
		if (owner instanceof Hero) {
			int exStr = ((Hero)owner).STR() - STRReq();
			if (exStr > 0) {
				damage += Hero.heroDamageIntRange( 0, exStr );
			}
		}

		if (sniperSpecial){
			damage = Math.round(damage * (1f + sniperSpecialBonusDamage));

			switch (augment){
				case NONE:
					damage = Math.round(damage * 0.667f);
					break;
				case SPEED:
					damage = Math.round(damage * 0.5f);
					break;
				case DAMAGE:
					//as distance increases so does damage, capping at 3x:
					//1.20x|1.35x|1.52x|1.71x|1.92x|2.16x|2.43x|2.74x|3.00x
					int distance = Dungeon.level.distance(owner.pos, targetPos) - 1;
					float multiplier = Math.min(3f, 1.2f * (float)Math.pow(1.125f, distance));
					damage = Math.round(damage * multiplier);
					break;
			}
		}
		
		return damage;
	}
	
	@Override
	protected float baseDelay(Char owner) {
		if (sniperSpecial){
			switch (augment){
				case NONE: default:
					return 0f;
				case SPEED:
					return 1f;
				case DAMAGE:
					return 2f;
			}
		} else{
			return super.baseDelay(owner);
		}
	}

	@Override
	protected float speedMultiplier(Char owner) {
		float speed = super.speedMultiplier(owner);
		if (owner.buff(NaturesPower.naturesPowerTracker.class) != null){
			// +33% speed to +50% speed, depending on talent points
			speed += ((8 + ((Hero)owner).pointsInTalent(Talent.GROWING_POWER)) / 24f);
		}
		return speed;
	}

	@Override
	public int level() {
		int level = Dungeon.hero == null ? 0 : Dungeon.hero.lvl/5;
		if (curseInfusionBonus) level += 1 + level/6;
		return level;
	}

	@Override
	public int buffedLvl() {
		//level isn't affected by buffs/debuffs
		return level();
	}
	
	@Override
	public boolean isUpgradable() {
		return false;
	}
	
	public SpiritArrow knockArrow(){
		return new SpiritArrow();
	}
	
	public class SpiritArrow extends MissileWeapon {
		
		{
			image = ItemSpriteSheet.SPIRIT_ARROW;

			hitSound = Assets.Sounds.HIT_ARROW;

			setID = 0;
		}

		@Override
		public int defaultQuantity() {
			return 1;
		}

		@Override
		public Emitter emitter() {
			if (Dungeon.hero.buff(NaturesPower.naturesPowerTracker.class) != null && !sniperSpecial){
				Emitter e = new Emitter();
				e.pos(5, 5);
				e.fillTarget = false;
				e.pour(LeafParticle.GENERAL, 0.01f);
				return e;
			} else {
				return super.emitter();
			}
		}

		@Override
		public int damageRoll(Char owner) {
			return SpiritBow.this.damageRoll(owner);
		}
		
		@Override
		public boolean hasEnchant(Class<? extends Enchantment> type, Char owner) {
			return SpiritBow.this.hasEnchant(type, owner);
		}
		
		@Override
		public int proc(Char attacker, Char defender, int damage) {
			return SpiritBow.this.proc(attacker, defender, damage);
		}
		
		@Override
		public float delayFactor(Char user) {
			return SpiritBow.this.delayFactor(user);
		}
		
		@Override
		public float accuracyFactor(Char owner, Char target) {
			if (sniperSpecial && SpiritBow.this.augment == Augment.DAMAGE){
				return Float.POSITIVE_INFINITY;
			} else {
				return super.accuracyFactor(owner, target);
			}
		}
		
		@Override
		public int STRReq(int lvl) {
			return SpiritBow.this.STRReq();
		}

		@Override
		protected void onThrow( int cell ) {
			Char enemy = Actor.findChar( cell );
			if (enemy == null || enemy == curUser) {
				parent = null;
				Splash.at(cell, 0xCC99FFFF, 1);
			} else {
				if (!curUser.shoot( enemy, this )) {
					Splash.at(cell, 0xCC99FFFF, 1);
				}
				if (sniperSpecial && SpiritBow.this.augment != Augment.SPEED) sniperSpecial = false;
			}
		}

		@Override
		public void throwSound() {
			Sample.INSTANCE.play( Assets.Sounds.ATK_SPIRITBOW, 1, Random.Float(0.87f, 1.15f) );
		}

		int flurryCount = -1;
		Actor flurryActor = null;

		@Override
		public void cast(final Hero user, final int dst) {
			final int cell = throwPos( user, dst );
			SpiritBow.this.targetPos = cell;
			if (sniperSpecial && SpiritBow.this.augment == Augment.SPEED){
				if (flurryCount == -1) flurryCount = 3;
				
				final Char enemy = Actor.findChar( cell );
				
				if (enemy == null){
					if (user.buff(Talent.LethalMomentumTracker.class) != null){
						user.buff(Talent.LethalMomentumTracker.class).detach();
						user.next();
					} else {
						user.spendAndNext(castDelay(user, cell));
					}
					sniperSpecial = false;
					flurryCount = -1;

					if (flurryActor != null){
						flurryActor.next();
						flurryActor = null;
					}
					return;
				}

				QuickSlotButton.target(enemy);
				
				user.busy();
				
				throwSound();

				user.sprite.zap(cell);
				((MissileSprite) user.sprite.parent.recycle(MissileSprite.class)).
						reset(user.sprite,
								cell,
								this,
								new Callback() {
									@Override
									public void call() {
										if (enemy.isAlive()) {
											curUser = user;
											onThrow(cell);
										}

										flurryCount--;
										if (flurryCount > 0){
											Actor.add(new Actor() {

												{
													actPriority = VFX_PRIO-1;
												}

												@Override
												protected boolean act() {
													flurryActor = this;
													int target = QuickSlotButton.autoAim(enemy, SpiritArrow.this);
													if (target == -1) target = cell;
													cast(user, target);
													Actor.remove(this);
													return false;
												}
											});
											curUser.next();
										} else {
											if (user.buff(Talent.LethalMomentumTracker.class) != null){
												user.buff(Talent.LethalMomentumTracker.class).detach();
												user.next();
											} else {
												user.spendAndNext(castDelay(user, cell));
											}
											sniperSpecial = false;
											flurryCount = -1;
										}

										if (flurryActor != null){
											flurryActor.next();
											flurryActor = null;
										}
									}
								});
				
			} else {

				if (user.hasTalent(Talent.SEER_SHOT)
						&& user.buff(Talent.SeerShotCooldown.class) == null){
					int shotPos = throwPos(user, dst);
					if (Actor.findChar(shotPos) == null) {
						RevealedArea a = Buff.affect(user, RevealedArea.class, 5 * user.pointsInTalent(Talent.SEER_SHOT));
						a.depth = Dungeon.depth;
						a.branch = Dungeon.branch;
						a.pos = shotPos;
						Buff.affect(user, Talent.SeerShotCooldown.class, 20f);
					}
				}

				/** ranger */
				Char enemy = Actor.findChar( cell );
				if (enemy == user){
					//ranger arrow change
					user.sprite.operate(cell);
					if (user.buff(WideArrow.class) != null) {
						Buff.affect(curUser, PiercingArrow.class).countUp(user.buff(WideArrow.class).count());
						user.buff(WideArrow.class).detach();

					} else if (user.buff(PiercingArrow.class) != null) {
						Buff.affect(user, WideArrow.class).countUp(user.buff(PiercingArrow.class).count());
						user.buff(PiercingArrow.class).detach();
					} else {
						super.cast(user, dst);
					}
					return;
				}

				if (user.buff(WideArrow.class) != null) {
					super.cast(user, dst);
					wideShot(user, cell, checkedWideCell(user, cell));
					wideShot(user, cell,checkedWideCell(user, cell)+4);

				} else if (user.buff(PiercingArrow.class) != null && enemy != null){
					QuickSlotButton.target(enemy);
					user.busy();

					throwSound();
					user.sprite.zap(cell);

					pierceingShot(user, dst);

				} else {
					super.cast(user, dst);
				}

				//super.cast(user, dst);
			}
		}
		public int checkedWideCell(Hero user, int cell) {
			Ballistica beam = new Ballistica(user.pos, cell, Ballistica.PROJECTILE);
			for (int i = 0; i<8; i++) {
				if (beam.path.get(1) == user.pos + PathFinder.CIRCLE8[i]) {
					return i;
				}
			}
			return -1;
		}

		public void wideShot (Hero user, int cell, int circle) {
			final int end = throwPos( user, cell + PathFinder.CIRCLE8[(circle + 2) % 8] );

			Char ch = Actor.findChar(end);
			if (ch != null) {
				((MissileSprite) user.sprite.parent.recycle(MissileSprite.class)).
						reset(user.sprite,
								ch.sprite,
								this,
								new Callback() {
									@Override
									public void call() {
										attackArrow(user, ch);
									}
								});
			} else {
				((MissileSprite) user.sprite.parent.recycle(MissileSprite.class)).
						reset(user.sprite,
								end,
								this,
								new Callback() {
									@Override
									public void call() {
										Splash.at(end, 0xCC99FFFF, 1);
									}
								});
			}
		}

		public void attackArrow(Hero hero, Char enemy){
			if (enemy == hero) return;

			float dmgMulti = 0.2f;

			if (hero.hasTalent(Talent.ENCHANT_BLAST))
				dmgMulti += 0.0667f * hero.pointsInTalent(Talent.ENCHANT_BLAST);

			hero.belongings.thrownWeapon = this;
			boolean hit = hero.attack(enemy, dmgMulti, 0, 1);
			Invisibility.dispel();
			hero.belongings.thrownWeapon = null;

			if (hit) {
				if (hero.buff(WideArrow.class) != null)
					hero.buff(WideArrow.class).countDown(1);

				if (hero.buff(PiercingArrow.class) != null)
					hero.buff(PiercingArrow.class).countDown(1);
			}
		}

		public void pierceingShot (Hero user, int dst) {
			int ballistica = hasEnchant(Projecting.class, user) ? Ballistica.WONT_STOP : Ballistica.STOP_SOLID;
			Ballistica beam = new Ballistica(user.pos, dst, ballistica);

			int dist = 1;

			ArrayList<Char> chars = new ArrayList<>();
			for (int c : beam.subPath(1, beam.dist)) {
				//when beyond the wall
				if (Dungeon.level.distance(user.pos, c) > new Ballistica(user.pos, dst, Ballistica.STOP_SOLID).dist
						&& Dungeon.level.distance(user.pos, c) > Math.round(4 * Enchantment.genericProcChanceMultiplier(user))) {
					dist = Dungeon.level.distance(user.pos, c)-1;
					break;
				}

				Char ch;
				if ((ch = Actor.findChar(c)) != null) {

					if (ch instanceof Mob && ((Mob) ch).state == ((Mob) ch).PASSIVE
							&& !(Dungeon.level.mapped[c] || Dungeon.level.visited[c])) {
						//avoid harming undiscovered passive chars
					} else {
						if (!chars.contains(ch)) chars.add(ch);

						dist = Math.min(Dungeon.level.distance(user.pos, c)+1, beam.dist);
						if (chars.size() >= 3) {
							dist = Dungeon.level.distance(user.pos, c);
							break;
						}
					}
				}
			}

			int lastCell = beam.path.get(dist);

			Char ch = Actor.findChar(lastCell);
			if (ch == null) {
				((MissileSprite) user.sprite.parent.recycle(MissileSprite.class)).
						reset(user.sprite,
								lastCell,
								this,
								new Callback() {
									@Override
									public void call() {
										for (Char ch : chars) {
											if (ch == chars.get(0)) {
												onThrow(ch.pos);
												continue;
											}
											attackArrow(user, ch);
										}

										if (user.buff(Talent.LethalMomentumTracker.class) != null) {
											user.buff(Talent.LethalMomentumTracker.class).detach();
											user.next();
										} else {
											user.spendAndNext(castDelay(user, dst));
										}
										Splash.at(lastCell, 0xCC99FFFF, 1);
									}
								});
			} else {
				((MissileSprite) user.sprite.parent.recycle(MissileSprite.class)).
						reset(user.sprite,
								ch.sprite,
								this,
								new Callback() {
									@Override
									public void call() {
										for (Char ch : chars) {
											if (ch == chars.get(0)) {
												onThrow(ch.pos);
												continue;
											}
											attackArrow(user, ch);
										}

										if (user.buff(Talent.LethalMomentumTracker.class) != null) {
											user.buff(Talent.LethalMomentumTracker.class).detach();
											user.next();
										} else {
											user.spendAndNext(castDelay(user, dst));
										}
									}
								});
			}
		}
	}

	public static class WideArrow extends RangerArrow { public int icon() { return BuffIndicator.WIDE_ARROW; }}
	public static class PiercingArrow extends RangerArrow { public int icon() { return BuffIndicator.PIERCE_ARROW; }}
	public static class RangerArrow extends CounterBuff {
		{announced = true;}
		@Override
		public float iconFadePercent() { return Math.max(0, (10f - (int)count()) / 10f); }
		@Override
		public String iconTextDisplay() { return Integer.toString((int)count()); }
		public String desc() { return Messages.get(this, "desc", (int)count()); }
		@Override
		public void countDown( float inc ){
			super.countDown(inc);
			if (count() <= 0) detach();
		}
	}
	/** ~ ranger end */
	private CellSelector.Listener shooter = new CellSelector.Listener() {
		@Override
		public void onSelect( Integer target ) {
			if (target != null) {
				knockArrow().cast(curUser, target);
			}
		}
		@Override
		public String prompt() {
			return Messages.get(SpiritBow.class, "prompt");
		}
	};
}
