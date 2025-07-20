package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.potionist;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AlchemistBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.BlobImmunity;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Drowsy;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hex;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.DirectableAlly;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SacrificialParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SmokeParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Vial;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TalismanOfForesight;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.Chasm;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ShadowCaster;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.GooSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.HomunculusSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class Homunculus extends ArmorAbility {

    {
        baseChargeUse = 35f;
    }

    @Override
    public String targetingPrompt() {
        if (getAlly() == null) {
            return super.targetingPrompt();
        } else {
            return Messages.get(this, "prompt");
        }
    }

    @Override
    public boolean useTargeting(){
        return false;
    }

    @Override
    public float chargeUse(Hero hero) {
        if (getAlly() == null) {
            return super.chargeUse(hero);
        } else {
            return 0;
        }
    }

    @Override
    protected void activate(ClassArmor armor, Hero hero, Integer target) {
        HomunculusAlly ally = getAlly();

        if (ally != null){
            if (target == null){
                return;
            } else {
                ally.directTocell(target);
            }
        } else {
            ArrayList<Integer> spawnPoints = new ArrayList<>();
            for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++) {
                int p = hero.pos + PathFinder.NEIGHBOURS8[i];
                if (Actor.findChar(p) == null && Dungeon.level.passable[p]) {
                    spawnPoints.add(p);
                }
            }

            if (!spawnPoints.isEmpty()){
                Vial v = hero.belongings.getItem(Vial.class);
                AlchemistBuff.State state;
                if (v != null) {
                    state = AlchemistBuff.potions.get(v.potion().getClass());
                } else {
                    GLog.w(Messages.get(VialBrewing.class, "no_vial"));
                    return;
                }

                armor.charge -= chargeUse(hero);
                armor.updateQuickslot();

                ally = ally(state);
                ally.pos = Random.element(spawnPoints);
                GameScene.add(ally);

                ally.setColor(AlchemistBuff.getColor(v.potion()));
                ally.setColor();

                ScrollOfTeleportation.appear(ally, ally.pos);
                Dungeon.observe();

                Invisibility.dispel();
                hero.spendAndNext(Actor.TICK);
                hero.sprite.operate(hero.pos);

            } else {
                GLog.w(Messages.get(this, "no_space"));
            }
        }

    }

    private static HomunculusAlly getAlly(){
        for (Char ch : Actor.chars()){
            if (ch instanceof HomunculusAlly){
                return (HomunculusAlly) ch;
            }
        }
        return null;
    }

    public HomunculusAlly ally (AlchemistBuff.State state){
        HomunculusAlly ally;
        switch (state) {
            //red
            case FLAME:
            case STRENGTH:
                ally = new RedAlly();
                break;
            //yellow
            case HASTE:
            case PARALYTIC:
                ally = new YellowAlly();
                break;
            //green
            case HEAL:
            case TOXIC_GAS:
                ally = new GreenAlly();
                break;
            //blue
            case FROST:
            case INVISIBLE:
                ally = new BlueAlly();
                break;
            //purple
            case PURITY:
            case MIND_VISION:
                ally = new PurpleAlly();
                break;
            //white
            default:
            case LEVITATE:
            case EXPERIENCE:
                ally = new WhiteAlly();
                break;
        }
        return ally;
    }


    @Override
    public int icon() { return HeroIcon.HOMUNCULUS; }

    @Override
    public Talent[] talents() {
        return new Talent[]{Talent.PHEROMONE_SPRAY, Talent.ENERGY_INCREASE, Talent.ENHANCED_GLAND, Talent.HEROIC_ENERGY};
    }

    public static abstract class HomunculusAlly extends DirectableAlly {
        public int color;

        {
            HP = HT = HT();

            viewDistance = 6;
            baseSpeed = 2f;

            immunities.addAll(new BlobImmunity().immunities());
            immunities.add(AllyBuff.class);

            properties.add(Property.INORGANIC);
        }

        //public boolean interact(Char c){die(Dungeon.hero);return true;}

        public void setColor (int color) { this.color = color;}

        public int HT () {return 3*Dungeon.hero.HT/4;}
        @Override
        public int attackSkill(Char target) { return Dungeon.hero.attackSkill(target); }
        @Override
        public int defenseSkill(Char enemy) { return Dungeon.hero.defenseSkill(enemy); }
        @Override
        public int damageRoll() {
            return Random.NormalIntRange(5, 10);
        }

        @Override
        public int attackProc(Char enemy, int damage) {
            for (Mob m : Dungeon.level.mobs.toArray( new Mob[0] )){
                if ((m.state != m.PASSIVE
                        && m.alignment == Alignment.ENEMY
                        && Dungeon.level.distance(m.pos, pos) <= Dungeon.hero.pointsInTalent(Talent.PHEROMONE_SPRAY))){
                    m.aggro(this);

                    if (Dungeon.level.heroFOV[pos])
                        this.sprite.centerEmitter().start( Speck.factory( Speck.DUST ), 0.06f, 8 );
                }
            }

            return super.attackProc( enemy, damage );
        }

        @Override
        protected boolean act() {
            int oldPos = pos;
            boolean result = super.act();
            //partially simulates how the hero switches to idle animation
            if ((pos == target || oldPos == pos) && sprite.looping()){
                sprite.idle();
            }
            return result;
        }

        @Override
        public void die(Object cause) {
            if (cause instanceof Chasm) {
                super.die(cause);
                return;
            }

            explode();
            Dungeon.observe();
            GameScene.updateFog();
            super.die(cause);
        }

        public void explode () {
            Sample.INSTANCE.play( Assets.Sounds.BLAST );
            Sample.INSTANCE.play( Assets.Sounds.BLAST );

            boolean circular = Dungeon.hero.pointsInTalent(Talent.ENERGY_INCREASE)%2 == 1;
            int distance = 1 + Math.round(Dungeon.hero.pointsInTalent(Talent.ENERGY_INCREASE)/2f);

            Point c = Dungeon.level.cellToPoint(pos);

            int[] rounding = ShadowCaster.rounding[distance];

            int left, right;
            int cell;
            for (int y = Math.max(0, c.y - distance); y <= Math.min(Dungeon.level.height()-1, c.y + distance); y++) {
                if (!circular){
                    left = c.x - distance;
                } else if (rounding[Math.abs(c.y - y)] < Math.abs(c.y - y)) {
                    left = c.x - rounding[Math.abs(c.y - y)];
                } else {
                    left = distance;
                    while (rounding[left] < rounding[Math.abs(c.y - y)]){
                        left--;
                    }
                    left = c.x - left;
                }
                right = Math.min(Dungeon.level.width()-1, c.x + c.x - left);
                left = Math.max(0, left);
                for (cell = left + y * Dungeon.level.width(); cell <= right + y * Dungeon.level.width(); cell++){

                    CellEmitter.get(cell).burst(SacrificialParticle.FACTORY, 15);
                    CellEmitter.get(cell).burst(SmokeParticle.FACTORY, 4);

                    Char ch = Actor.findChar(cell);
                    if (ch != null && ch.alignment != Alignment.ALLY) {
                        if (!ch.isAlive()){
                            continue;
                        }

                        affects(ch, 4f + 2f*Dungeon.hero.pointsInTalent(Talent.ENHANCED_GLAND));
                        int dmg = Random.NormalIntRange(15, 30);
                        dmg -= ch.drRoll();

                        if (dmg > 0) {
                            ch.damage(dmg, this);
                        }
                    }
                }
            }
        }

        public AlchemistBuff.State state (){
            for (AlchemistBuff.State state : AlchemistBuff.buffColor.keySet()) {
                if (AlchemistBuff.buffColor.get(state) == color) {
                    return state;
                }
            }
            return null;
        }

        protected abstract void affects (Char ch, float duration);
        protected void setColor () { ((HomunculusSprite)sprite).setEmitter(); }
        @Override
        public void defendPos(int cell) {
            GLog.i(Messages.get(this, "direct_defend"));
            super.defendPos(cell);
        }

        @Override
        public void followHero() {
            GLog.i(Messages.get(this, "direct_follow"));
            super.followHero();
        }

        @Override
        public void targetChar(Char ch) {
            GLog.i(Messages.get(this, "direct_attack"));
            super.targetChar(ch);
        }

        @Override
        public String description() {
            String message = Messages.get(this, "desc",15,30);
            if (Actor.chars().contains(this)){
                int dura = 4 + 2*Dungeon.hero.pointsInTalent(Talent.ENHANCED_GLAND);
                if (state() == AlchemistBuff.State.PURITY) dura = Math.max(11 - dura, 0);
                if (state() == AlchemistBuff.State.HEAL) dura *= 2;
                message += "\n\n" + Messages.get(this,""+state(), dura);
            }
            return message;
        }

        private static final String COLOR = "color";

        @Override
        public void storeInBundle(Bundle bundle) {
            super.storeInBundle(bundle);
            bundle.put(COLOR, color);
        }

        @Override
        public void restoreFromBundle(Bundle bundle) {
            super.restoreFromBundle(bundle);
            color = bundle.getInt(COLOR);
        }
    }

    public static class RedAlly extends HomunculusAlly {
        {
            spriteClass = HomunculusSprite.Red.class;
        }

        @Override
        protected void affects (Char ch, float duration) {
            if (state() == AlchemistBuff.State.FLAME) {
                if (!ch.isImmune(Burning.class)) {
                    Buff.affect(ch, Burning.class).reignite(ch, duration);
                }
                return;
            }

            if (state() == AlchemistBuff.State.STRENGTH) {
                Buff.prolong( ch, Weakness.class, duration );
            }
        }
    }

    public static class YellowAlly extends HomunculusAlly {
        {
            spriteClass = HomunculusSprite.Yellow.class;
        }
        @Override
        protected void affects (Char ch, float duration) {
            if (state() == AlchemistBuff.State.HASTE) {
                Buff.prolong(ch, Cripple.class, duration);
                return;
            }

            if (state() == AlchemistBuff.State.PARALYTIC) {
                if (!ch.isImmune(Paralysis.class)) {
                    Buff.affect(ch, Paralysis.class, duration);
                }
            }
        }
    }

    public static class GreenAlly extends HomunculusAlly {
        {
            spriteClass = HomunculusSprite.Green.class;
        }
        @Override
        protected void affects (Char ch, float duration) {
            if (state() == AlchemistBuff.State.HEAL) {
                if (!ch.isImmune(Bleeding.class)) {
                    Bleeding bleeding = ch.buff(Bleeding.class);
                    if (bleeding != null) duration += (int) bleeding.level();

                    Buff.affect(ch, Bleeding.class).set(2*duration);
                }
                return;
            }

            if (state() == AlchemistBuff.State.TOXIC_GAS) {
                if (!ch.isImmune(Poison.class)) {
                    Buff.affect(ch, Poison.class).extend(duration);
                }
            }
        }
    }

    public static class BlueAlly extends HomunculusAlly {
        {
            spriteClass = HomunculusSprite.Blue.class;
        }
        @Override
        protected void affects (Char ch, float duration) {
            if (state() == AlchemistBuff.State.FROST) {
                if (!ch.isImmune(Chill.class)) {
                    Buff.affect(ch, Chill.class, duration);
                }
                return;
            }

            if (state() == AlchemistBuff.State.INVISIBLE) {
                Buff.prolong( ch, Blindness.class, duration);
            }
        }
    }

    public static class PurpleAlly extends HomunculusAlly {
        {
            spriteClass = HomunculusSprite.Purple.class;
        }
        @Override
        protected void affects (Char ch, float duration) {
            if (state() == AlchemistBuff.State.PURITY) {
                Buff.affect( ch, Drowsy.class, Math.max(10 - duration, 0));
                ch.sprite.centerEmitter().start( Speck.factory( Speck.NOTE ), 0.3f, 5 );
                return;
            }

            if (state() == AlchemistBuff.State.MIND_VISION) {
                Buff.append(Dungeon.hero, TalismanOfForesight.CharAwareness.class, duration).charID = ch.id();
            }
        }
    }

    public static class WhiteAlly extends HomunculusAlly {
        {
            spriteClass = HomunculusSprite.White.class;
        }
        @Override
        protected void affects (Char ch, float duration) {
            if (state() == AlchemistBuff.State.LEVITATE) {
                if (!ch.isImmune(Vertigo.class)) {
                    Buff.prolong(ch, Vertigo.class, duration);
                }
                return;
            }

            if (state() == AlchemistBuff.State.EXPERIENCE) {
                Buff.affect(ch, Hex.class, duration);
            }
        }
    }
}
