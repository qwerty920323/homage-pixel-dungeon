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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Amok;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AscensionChallenge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ChampionEnemy;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corrosion;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corruption;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Daze;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Doom;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Dread;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Drowsy;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hex;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicalSleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SoulMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ArmoredStatue;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Bee;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.CrystalMimic;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EbonyMimic;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GoldenMimic;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Piranha;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Statue;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Swarm;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Wraith;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SacrificialParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.GuardianTrap;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.BArray;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Point;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.HashMap;

public class WandOfCorruption extends Wand {

	{
		image = ItemSpriteSheet.WAND_CORRUPTION;
	}
	
	//Note that some debuffs here have a 0% chance to be applied.
	// This is because the wand of corruption considers them to be a certain level of harmful
	// for the purposes of reducing resistance, but does not actually apply them itself
	
	private static final float MINOR_DEBUFF_WEAKEN = 1/4f;
	private static final HashMap<Class<? extends Buff>, Float> MINOR_DEBUFFS = new HashMap<>();
	static{
		MINOR_DEBUFFS.put(Weakness.class,       2f);
		MINOR_DEBUFFS.put(Vulnerable.class,     2f);
		MINOR_DEBUFFS.put(Cripple.class,        1f);
		MINOR_DEBUFFS.put(Blindness.class,      1f);
		MINOR_DEBUFFS.put(Terror.class,         1f);

		MINOR_DEBUFFS.put(Chill.class,          0f);
		MINOR_DEBUFFS.put(Ooze.class,           0f);
		MINOR_DEBUFFS.put(Roots.class,          0f);
		MINOR_DEBUFFS.put(Vertigo.class,        0f);
		MINOR_DEBUFFS.put(Drowsy.class,         0f);
		MINOR_DEBUFFS.put(Bleeding.class,       0f);
		MINOR_DEBUFFS.put(Burning.class,        0f);
		MINOR_DEBUFFS.put(Poison.class,         0f);
	}

	private static final float MAJOR_DEBUFF_WEAKEN = 1/2f;
	private static final HashMap<Class<? extends Buff>, Float> MAJOR_DEBUFFS = new HashMap<>();
	static{
		MAJOR_DEBUFFS.put(Amok.class,           3f);
		MAJOR_DEBUFFS.put(Slow.class,           2f);
		MAJOR_DEBUFFS.put(Hex.class,            2f);
		MAJOR_DEBUFFS.put(Paralysis.class,      1f);

		MAJOR_DEBUFFS.put(Daze.class,           0f);
		MAJOR_DEBUFFS.put(Dread.class,          0f);
		MAJOR_DEBUFFS.put(Charm.class,          0f);
		MAJOR_DEBUFFS.put(MagicalSleep.class,   0f);
		MAJOR_DEBUFFS.put(SoulMark.class,       0f);
		MAJOR_DEBUFFS.put(Corrosion.class,      0f);
		MAJOR_DEBUFFS.put(Frost.class,          0f);
		MAJOR_DEBUFFS.put(Doom.class,           0f);
	}
	
	@Override
	public void onZap(Ballistica bolt) {
		Char ch = Actor.findChar(bolt.collisionPos);

		if (ch != null){
			
			if (!(ch instanceof Mob)){
				return;
			}

			Mob enemy = (Mob) ch;

			if (enemy instanceof DwarfKing){
				Statistics.qualifiedForBossChallengeBadge = false;
			}

			float corruptingPower = 3 + buffedLvl()/3f;
			
			//base enemy resistance is usually based on their exp, but in special cases it is based on other criteria
			float enemyResist;
			if (ch instanceof Mimic || ch instanceof Statue){
				enemyResist = 1 + Dungeon.depth;
			} else if (ch instanceof Piranha || ch instanceof Bee) {
				enemyResist = 1 + Dungeon.depth/2f;
			} else if (ch instanceof Wraith) {
				//divide by 5 as wraiths are always at full HP and are therefore ~5x harder to corrupt
				enemyResist = (1f + Dungeon.scalingDepth()/4f) / 5f;
			} else if (ch instanceof Swarm){
				//child swarms don't give exp, so we force this here.
				enemyResist = 1 + AscensionChallenge.AscensionCorruptResist(enemy);
				if (enemyResist == 1) enemyResist = 1 + 3;
			} else {
				enemyResist = 1 + AscensionChallenge.AscensionCorruptResist(enemy);
			}
			
			//100% health: 5x resist   75%: 3.25x resist   50%: 2x resist   25%: 1.25x resist
			enemyResist *= 1 + 4*Math.pow(enemy.HP/(float)enemy.HT, 2);
			
			//debuffs placed on the enemy reduce their resistance
			for (Buff buff : enemy.buffs()){
				if (MAJOR_DEBUFFS.containsKey(buff.getClass()))         enemyResist *= (1f-MAJOR_DEBUFF_WEAKEN);
				else if (MINOR_DEBUFFS.containsKey(buff.getClass()))    enemyResist *= (1f-MINOR_DEBUFF_WEAKEN);
				else if (buff.type == Buff.buffType.NEGATIVE)           enemyResist *= (1f-MINOR_DEBUFF_WEAKEN);
			}
			
			//cannot re-corrupt or doom an enemy, so give them a major debuff instead
			if(enemy.buff(Corruption.class) != null || enemy.buff(Doom.class) != null){
				corruptingPower = enemyResist - 0.001f;
			}
			
			if (corruptingPower > enemyResist){
				corruptEnemy( enemy );
			} else {
				float debuffChance = corruptingPower / enemyResist;
				if (Random.Float() < debuffChance){
					debuffEnemy( enemy, MAJOR_DEBUFFS);
				} else {
					debuffEnemy( enemy, MINOR_DEBUFFS);
				}
			}

			wandProc(ch, chargesPerCast());
			Sample.INSTANCE.play( Assets.Sounds.HIT_MAGIC, 1, 0.8f * Random.Float(0.87f, 1.15f) );
			
		} else {
			Dungeon.level.pressCell(bolt.collisionPos);
		}
	}
	
	private void debuffEnemy( Mob enemy, HashMap<Class<? extends Buff>, Float> category ){
		
		//do not consider buffs which are already assigned, or that the enemy is immune to.
		HashMap<Class<? extends Buff>, Float> debuffs = new HashMap<>(category);
		for (Buff existing : enemy.buffs()){
			if (debuffs.containsKey(existing.getClass())) {
				debuffs.put(existing.getClass(), 0f);
			}
		}
		for (Class<?extends Buff> toAssign : debuffs.keySet()){
			 if (debuffs.get(toAssign) > 0 && enemy.isImmune(toAssign)){
			 	debuffs.put(toAssign, 0f);
			 }
		}
		
		//all buffs with a > 0 chance are flavor buffs
		Class<?extends FlavourBuff> debuffCls = (Class<? extends FlavourBuff>) Random.chances(debuffs);
		
		if (debuffCls != null){
			Buff.append(enemy, debuffCls, 6 + buffedLvl()*3);
		} else {
			//if no debuff can be applied (all are present), then go up one tier
			if (category == MINOR_DEBUFFS)          debuffEnemy( enemy, MAJOR_DEBUFFS);
			else if (category == MAJOR_DEBUFFS)     corruptEnemy( enemy );
		}
	}
	
	private void corruptEnemy( Mob enemy ){
		//cannot re-corrupt or doom an enemy, so give them a major debuff instead
		if(enemy.buff(Corruption.class) != null || enemy.buff(Doom.class) != null){
			GLog.w( Messages.get(this, "already_corrupted") );
			return;
		}
		
		if (!enemy.isImmune(Corruption.class)){
			Corruption.corruptionHeal(enemy);
			AllyBuff.affectAndLoot(enemy, curUser, Corruption.class);
		} else {
			Buff.affect(enemy, Doom.class);
		}
	}

	@Override
	public void onHit(MagesStaff staff, Char attacker, Char defender, int damage) {
		int level = Math.max( 0, buffedLvl() );

		// lvl 0 - 16%
		// lvl 1 - 28.5%
		// lvl 2 - 37.5%
		float procChance = (level+1f)/(level+6f) * procChanceMultiplier(attacker);
		if (Random.Float() < procChance) {

			float powerMulti = Math.max(1f, procChance);

			Buff.prolong( defender, Amok.class, Math.round((4+level*2) * powerMulti));
		}
	}

	@Override
	public void fx(Ballistica bolt, Callback callback) {
		MagicMissile.boltFromChar( curUser.sprite.parent,
				MagicMissile.SHADOW,
				curUser.sprite,
				bolt.collisionPos,
				callback);
		Sample.INSTANCE.play( Assets.Sounds.ZAP );
	}

	@Override
	public void staffFx(MagesStaff.StaffParticle particle) {
		particle.color( 0 );
		particle.am = 0.6f;
		particle.setLifespan(2f);
		particle.speed.set(0, 5);
		particle.setSize( 0.5f, 2f);
		particle.shuffleXY(1f);
	}

	//scholar

	@Override
	public String info() {
		String desc = super.info();
		if (Dungeon.hero.subClass == HeroSubClass.SCHOLAR){
			//desc += " " + Messages.get(this, "mob_count",scholarTurnCount());
		}

		return desc;
	}
	@Override
	public int bonusRange () {return 2 + super.bonusRange();}
	@Override
	public int scholarTurnCount(){
		return super.scholarTurnCount() + 1;
	}
	@Override
	public void scholarAbility(Ballistica bolt, int cell) {
		super.scholarAbility(bolt,cell);

		int pos = bolt.collisionPos;
		int terr = Dungeon.level.map[pos];

		MiniSacrificialFire miniSacrificial = (MiniSacrificialFire) Dungeon.level.blobs.get(MiniSacrificialFire.class);
		int sacrificialPos = -1;
		for (int i = 0; i < Dungeon.level.length(); i++) {
			if (miniSacrificial != null && miniSacrificial.volume > 0 && miniSacrificial.cur[i] > 0) {
				sacrificialPos = i;
			}
		}

		//int turn = Math.max(1, Dungeon.scalingDepth()/4);
		int turn = scholarTurnCount();

		if (sacrificialPos>0) {
			if (sacrificialPos == pos && terr == Terrain.PEDESTAL) {
				setPedestal(pos, turn);
			} else if (terrCheck(pos, Terrain.EMPTY)) {

				if (sacrificialPos != pos) {
					CellEmitter.get(sacrificialPos).start(Speck.factory(Speck.LIGHT), 0.2f, 4);
					Level.set(sacrificialPos, Terrain.EMBERS);
					GameScene.updateMap(sacrificialPos);
					miniSacrificial.fullyClear();
				}

				setPedestal(pos, turn);

			}
		} else if (terrCheck(pos, Terrain.EMPTY)){
			setPedestal(pos, turn);
		}
	}
	public void setPedestal(int pos, int turn) {
		for (int p : PathFinder.NEIGHBOURS8){
			if (terrCheck(pos+p, Terrain.EMPTY) && Dungeon.level.map[pos+p] != Terrain.EMBERS) {
				Dungeon.level.pressCell(pos + p);
				Level.set(pos + p, Terrain.EMBERS);
				CellEmitter.get( pos+p ).start( SacrificialParticle.FACTORY, 0.01f, 20 );
			}
		}

		Dungeon.level.pressCell(pos);
		Level.set(pos, Terrain.PEDESTAL);

		MiniSacrificialFire miniSacrificial = Blob.seed(pos, scholarTurnCount(), MiniSacrificialFire.class);
		miniSacrificial.setSpawn(turn);

		CellEmitter.get( pos ).start( SacrificialParticle.FACTORY, 0.01f, 10 );
		GameScene.add(miniSacrificial);
		GameScene.updateMap(pos);
	}



	public static class MiniSacrificialFire extends Blob {
		BlobEmitter curEmitter;

		{
			//acts after mobs, so they can get marked as they move
			actPriority = MOB_PRIO-1;
		}
		private int turn ;
		@Override
		protected void evolve() {
			int cell;
			for (int i=area.top-1; i <= area.bottom; i++) {
				for (int j = area.left-1; j <= area.right; j++) {
					cell = j + i* Dungeon.level.width();
					if (Dungeon.level.insideMap(cell)) {
						off[cell] = cur[cell];
						volume += off[cell];
						if (off[cell] > 0){
							for (int k : PathFinder.NEIGHBOURS9){
								Char ch = Actor.findChar( cell+k );
								if (ch != null && !(ch instanceof NPC)){
									if (Dungeon.level.heroFOV[cell+k] && ch.buff( Marked.class ) == null) {
										CellEmitter.get(cell+k).burst( SacrificialParticle.FACTORY, 5 );
									}
									Buff.prolong( ch, Marked.class, Marked.DURATION );
								}
							}
							if (off[cell] > 0 && off[Dungeon.hero.pos] > 0) {
								int spawn = 2+Dungeon.hero.pointsInTalent(Talent.WIDE_SUMMON);
								if (setMob() != null && mobCount() < turn)
									spawnMob(spawn, setMob());
							}

							Heap heap = Dungeon.level.heaps.get(cell);
							if (heap != null && heap.type == Heap.Type.HEAP) {
								ArrayList<Integer> candidates = new ArrayList<>();
								for (int n : PathFinder.NEIGHBOURS8) {
									if (Dungeon.level.passable[cell + n]) {
										candidates.add(cell + n);
									}
								}
								Item item = heap.pickUp();
								Dungeon.level.drop(item, Random.element(candidates)).sprite.drop(cell);
							}
						}
					}
				}
			}

			int max = 6 + turn * 2;
			curEmitter.pour( SacrificialParticle.FACTORY, 0.01f + ((mobCount()+1f / (float)max) * 0.09f) );
		}

		public void setSpawn(int turn){
			this.turn = turn;
		}

		public Mob setMob () {
			if (Dungeon.hero.buff(MarkedMob.class) != null)
				return Dungeon.hero.buff(MarkedMob.class).mob;
			else
				return null;
		}

		public int mobCount () {
			ArrayList<Mob> mobs = new ArrayList<>();
			for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
				if (mob.buff(Spawned.class) != null) {
					mobs.add(mob);
				}
			}
			return mobs.size();
		}

		public boolean spawnMob(int disLimit, Mob mobs){
			PathFinder.buildDistanceMap( Dungeon.hero.pos, BArray.not( Dungeon.level.solid, null ), disLimit );

			Mob mob = Reflection.newInstance(mobs.getClass());
			ChampionEnemy.rollForChampion(mob);
			mob.state = mob.WANDERING;
			mob.EXP = (int) (mob.EXP / Math.pow(1.22f,(turn-mobCount()))); //ez corruption
			mob.maxLvl = -5; //no exp, item
			mob.pos = -1;

			ArrayList <Integer> cells = new ArrayList<>();
			for (int i = 0; i < Dungeon.level.length(); i++){
				if (Actor.findChar(i) == null
						&& PathFinder.distance[i] < Integer.MAX_VALUE
						&& Dungeon.level.passable[i] && !Dungeon.level.solid[i])
					cells.add(i);
			}

			while (!cells.isEmpty()){
				int index = cells.remove(Random.index(cells));
				if (Dungeon.level.distance(Dungeon.hero.pos, index) <= disLimit) {
					mob.pos = index;
					break;
				}
			}

			if (Dungeon.hero.isAlive() && mob.pos != -1) {
				Mob m = mobChoice(mob);
				GameScene.add(m,1f);
				Buff.affect(m, Spawned.class);

				GLog.w( Messages.get(this, "respawn"));

				if (Dungeon.level.heroFOV[mob.pos]) {
					CellEmitter.get(mob.pos).burst(SacrificialParticle.FACTORY, 20);
					Sample.INSTANCE.play(Assets.Sounds.BURNING);
					Sample.INSTANCE.play(Assets.Sounds.BURNING);
					Sample.INSTANCE.play(Assets.Sounds.BURNING);
				}

				if (!mob.buffs(ChampionEnemy.class).isEmpty()){
					GLog.w(Messages.get(ChampionEnemy.class, "warn"));
				}
				return true;
			} else {
				return false;
			}
		}

		public Mob mobChoice(Mob mob){
			Mob m;
			m = mob;
			if (mob instanceof Bee){
				Bee bee = new Bee();
				bee.spawn( Dungeon.depth );
				bee.HP = bee.HT;
				bee.pos = mob.pos;

				bee.setPotInfo(mob.pos, null);
				m = bee;
			} else if (mob instanceof Mimic){
				m = Mimic.spawnAt(mob.pos, mob.getClass(), false);
				((Mimic)m).stopHiding();
				m.alignment = Char.Alignment.ENEMY;
				((Mimic)m).items = null;
			} else if (mob instanceof Statue){
				Statue statue = new Statue();

				if (mob.getClass() == GuardianTrap.Guardian.class ){
					statue = new GuardianTrap.Guardian();
				} else if (mob.getClass() == ArmoredStatue.class){
					statue = new ArmoredStatue();
				}

				statue.createWeapon(false);
				statue.pos = mob.pos;
				m = statue;
			}
			return m;
		}


		@Override
		public void use( BlobEmitter emitter ) {
			super.use( emitter );
			curEmitter = emitter;

			int max = 6 + turn * 2;
			curEmitter.pour( SacrificialParticle.FACTORY, 0.01f + ((mobCount()+1f / (float)max) * 0.09f) );
		}

		public String mobName (){
			if (setMob() != null)
				return setMob().name();
			return Messages.get(this, "null");
		}

		@Override
		public String tileDesc() {
			return Messages.get(this, "desc", turn - mobCount(), mobName());
		}

		private static final String TURN = "turn";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put( TURN, turn);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			turn = bundle.getInt( TURN );
		}

		public void sacrifice( Char ch ) {
			int firePos = -1;
			for (int i : PathFinder.NEIGHBOURS9){
				if (volume > 0 && cur[ch.pos+i] > 0){
					firePos = ch.pos+i;
					break;
				}
			}

			if (firePos != -1) {
				if (ch instanceof Mob
						&& !ch.isImmune(Corruption.class)
						&& ch.alignment == Char.Alignment.ENEMY
						//&& !(ch instanceof Piranha)
				    ) {

					Buff.affect(Dungeon.hero, MarkedMob.class).setMobClass((Mob)ch);
					CellEmitter.get(firePos).burst(SacrificialParticle.FACTORY, 20);
					Sample.INSTANCE.play(Assets.Sounds.BURNING);
					GLog.w(Messages.get(this, "worthy"));
				} else {
					GLog.w( Messages.get(this, "unworthy"));
				}
			}
		}

		public static class Marked extends FlavourBuff {

			public static final float DURATION	= 2f;

			@Override
			public void detach() {
				if (!target.isAlive()) {
					MiniSacrificialFire fire = (MiniSacrificialFire) Dungeon.level.blobs.get(MiniSacrificialFire.class);
					if (fire != null) {
						fire.sacrifice(target);
					}
				}
				super.detach();
			}
		}
	}

	public static class MarkedMob extends Buff {
		{
			revivePersists = true;
		}
		Mob mob = null;
		private void setMobClass (Mob mob){
			this.mob = mob;
		}
		private static final String MOB = "mob";
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put( MOB, mob);
		}
		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			if (bundle.contains(MOB)) mob = (Mob) bundle.get(MOB);
		}
	}

	public static class Spawned extends Buff {
		{type = buffType.NEGATIVE;}
		@Override
		public void fx(boolean on) {
			if (on) target.sprite.add(CharSprite.State.MARKED);
			else target.sprite.remove(CharSprite.State.MARKED);
		}
	}
}
