package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ConfusionGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.CorrosiveGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Electricity;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ParalyticGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Lightning;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfExperience;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfFrost;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHaste;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfInvisibility;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLevitation;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLiquidFlame;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfMindVision;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfParalyticGas;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfPurity;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.Elixir;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfAquaticRejuvenation;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfArcaneArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfDragonsBlood;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfFeatherFall;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfHoneyedHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfIcyTouch;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfMight;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfToxicEssence;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.ExoticPotion;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class AlchemistBuff extends FlavourBuff {

    {
        type = buffType.POSITIVE;
        revivePersists = true;
    }

    public static final HashMap<Class <?extends Potion>, State> potions = new HashMap<>();
    static {
        potions.put(PotionOfHealing.class,      State.HEAL);
        potions.put(PotionOfInvisibility.class, State.INVISIBLE);
        potions.put(PotionOfMindVision.class,   State.MIND_VISION);
        potions.put(PotionOfHaste.class,        State.HASTE);
        potions.put(PotionOfPurity.class,       State.PURITY);
        potions.put(PotionOfLevitation.class,   State.LEVITATE);

        potions.put(PotionOfFrost.class,        State.FROST);
        potions.put(PotionOfLiquidFlame.class,  State.FLAME);
        potions.put(PotionOfToxicGas.class,     State.TOXIC_GAS);
        potions.put(PotionOfParalyticGas.class, State.PARALYTIC);

        potions.put(PotionOfStrength.class,     State.STRENGTH);
        potions.put(PotionOfExperience.class,   State.EXPERIENCE);
    }

    public static final HashMap<State, Integer> buffColor = new HashMap<>();
    static {
        buffColor.put(State.HEAL,       0x2EE62E);
        buffColor.put(State.INVISIBLE,  0x195D80);
        buffColor.put(State.MIND_VISION,0xC669C6);
        buffColor.put(State.HASTE,      0xFFFF66);
        buffColor.put(State.PURITY,     0xA15CE5);
        buffColor.put(State.LEVITATE,   0XD9D9D9);

        buffColor.put(State.FROST,      0x66B3FF);
        buffColor.put(State.FLAME,      0xCC0022);
        buffColor.put(State.TOXIC_GAS,  0x005826);
        buffColor.put(State.PARALYTIC,  0xB2A600);

        buffColor.put(State.STRENGTH,   0xFF7F00);
        buffColor.put(State.EXPERIENCE, 0x404040);
    }

    public static final LinkedHashMap<Class<?extends Elixir>, Class<?extends Potion>> eliToReg = new LinkedHashMap<>();
    static{
        //flame
        eliToReg.put(ElixirOfDragonsBlood.class, PotionOfLiquidFlame.class);
        //frost
        eliToReg.put(ElixirOfIcyTouch.class, PotionOfFrost.class);
        //levitate
        eliToReg.put(ElixirOfFeatherFall.class, PotionOfLevitation.class);
        //toxic gas
        eliToReg.put(ElixirOfToxicEssence.class, PotionOfToxicGas.class);
        //paralytic
        eliToReg.put(ElixirOfArcaneArmor.class, PotionOfParalyticGas.class);

        //strength
        eliToReg.put(ElixirOfMight.class, PotionOfStrength.class);
        //heal
        eliToReg.put(ElixirOfHoneyedHealing.class, PotionOfHealing.class);
        //heal
        eliToReg.put(ElixirOfAquaticRejuvenation.class, PotionOfHealing.class);
    }

    public enum State{
        HEAL, INVISIBLE, MIND_VISION, HASTE, PURITY, LEVITATE, // beneficial
        FROST, FLAME, TOXIC_GAS, PARALYTIC,                    // harmful
        STRENGTH, EXPERIENCE                                   // unique
    }
    public static final float DURATION	= 20f;
    protected ArrayList<Class> bonusImmune = new ArrayList<>();

    protected State state = null;
    private static final String STATE  = "state";
    private static final String IMMUNE = "immune";

    @Override
    public void storeInBundle( Bundle bundle ) {
        super.storeInBundle( bundle );
        bundle.put( STATE, state );
        bundle.put( IMMUNE, bonusImmune.toArray(new Class[bonusImmune.size()]) );
    }

    @Override
    public void restoreFromBundle( Bundle bundle ) {
        super.restoreFromBundle( bundle );
        state = bundle.getEnum( STATE, State.class );

        bonusImmune.clear();
        if (bundle.contains(IMMUNE)) {
            Collections.addAll(bonusImmune, bundle.getClassArray(IMMUNE));
        }
    }

    public State state() {
        return state;
    }

    public void set( Potion p , float cooldown) {
        if (((Hero)target).hasTalent(Talent.DRUG_INTERACTION) && !immunities.isEmpty()) {
            bonusImmune.addAll(immunities);

            float shield = 0.25f*((Hero)target).pointsInTalent(Talent.DRUG_INTERACTION);
            Buff.affect(target, Barrier.class).setShield(Math.round(cooldown * (shield+0.25f)));
            target.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(Math.round(cooldown * (shield+0.25f))), FloatingText.SHIELDING );
        }

        Potion potion = p;
        if (p instanceof ExoticPotion) {
            potion = Reflection.newInstance(ExoticPotion.exoToReg.get(p.getClass()));
        }
        if (p instanceof Elixir) {
            potion = Reflection.newInstance(eliToReg.get(p.getClass()));
        }

        //strength
        if (potion instanceof PotionOfStrength) {
            Buff.affect(Dungeon.hero, AdrenalineSurge.class).reset(1, AdrenalineSurge.DURATION);
        }

        state = potions.get(potion.getClass());
        stateImmunities();
        BuffIndicator.refreshHero();
    }

    public void stateImmunities () {
        this.immunities.clear();

        switch (state) {
            case FROST:
                immunities.add(Chill.class);
                immunities.add(Frost.class);
                Buff.detach(target, Chill.class);
                Buff.detach(target, Frost.class);
                break;

            case FLAME:
                immunities.add(Burning.class);
                Buff.detach(target, Burning.class);
                break;

            case TOXIC_GAS:
                immunities.add(CorrosiveGas.class);
                immunities.add(ToxicGas.class);
                Buff.detach(target, Corrosion.class);
                break;

            case PARALYTIC:
                immunities.add(ParalyticGas.class);
                Buff.detach(target, Paralysis.class);
                break;

            case LEVITATE:
                immunities.add(ConfusionGas.class);
                Buff.detach(target, Vertigo.class);
                break;
        }

        if (!bonusImmune.isEmpty()) {
            //compare starting from last index
            for (int i = bonusImmune.size()-1; i >= 0; i--) {
                Class c = bonusImmune.get(i);

                if (!immunities.contains(c))
                    immunities.add(c);
                else
                    bonusImmune.remove(c);
            }
        }
    }

    public int attackProc (Char enemy, int damage){
        boolean leftBonus = (int)visualcooldown() <= 20f;

        switch (state) {
            case FROST:
                enemy.damage(Random.NormalIntRange(2,2+Dungeon.scalingDepth()/5), new Chill());
                break;

            case TOXIC_GAS:
                Buff.affect(enemy, Poison.class).extend(Random.NormalIntRange(2,2+Dungeon.scalingDepth()/5));
                break;

            case INVISIBLE:
                if (target.invisible > 0 && ((Mob)enemy).surprisedBy(target)) {
                    damage = Math.round(damage * 1.5f);
                }
                break;

            case STRENGTH:
                AdrenalineSurge buff = target.buff(AdrenalineSurge.class);
                if (buff != null){
                    buff.reset(1, 2f + buff.cooldown());
                }
                break;

            case MIND_VISION: //hero accuracy
            case HASTE:       //hero attack speed
            case EXPERIENCE:  //weapon, armor buffedlvl
                break;

            default:
                leftBonus = false;
                break;
        }

        if (((Hero)target).hasTalent(Talent.CORRECT_USAGE) && leftBonus) {
            spend(0.5f*((Hero)target).pointsInTalent(Talent.CORRECT_USAGE));
        }

        return damage;
    }

    public int defenseProc (int damage, Object src) {
        boolean leftBonus = (int)visualcooldown() <= 20f;
        if (!(src instanceof Char)) {
            return damage;
        }

        Char enemy = ((Char)src);

        switch (state) {
            default:
                leftBonus = false;
                break;

            case HEAL:
                if (target.HP < target.HT) {
                    target.HP = Math.min(target.HT, target.HP + damage / 2);
                }
                target.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(damage / 2), FloatingText.HEALING);
                break;

            case PURITY:
                leftBonus = false;

                ArrayList <Buff> buffs = new ArrayList<>();
                for (Buff b : target.buffs()){
                    if (b.type == Buff.buffType.NEGATIVE
                            && !(b instanceof AllyBuff)
                            && !(b instanceof LostInventory)){
                        buffs.add(b);
                    }
                }

                while (!buffs.isEmpty()) {
                    buffs.get(0).detach();
                    buffs.remove(buffs.get(0));
                    leftBonus = (int)visualcooldown() <= 20f;
                }
                break;

            case FLAME:
                MagicMissile.boltFromChar(target.sprite.parent, MagicMissile.FIRE,
                        target.sprite, enemy.pos,
                        new Callback() {
                            @Override
                            public void call() {
                                if (!enemy.isImmune(Burning.class))
                                    Buff.affect( enemy, Burning.class ).reignite( enemy , Random.NormalIntRange(2,2+Dungeon.scalingDepth()/5) );
                            }
                        });

                break;

            case PARALYTIC:
                target.sprite.parent.add(new Lightning(target.sprite.center(),
                        DungeonTilemap.raisedTileCenterToWorld(enemy.pos), null));
                CellEmitter.get(enemy.pos).burst(SparkParticle.FACTORY, 3);

                enemy.sprite.flash();
                enemy.damage(Random.NormalIntRange(2,2+Dungeon.scalingDepth()/5), new Electricity());

                break;
            case EXPERIENCE:
                //only spend time
                break;
        }

        if (((Hero)target).hasTalent(Talent.CORRECT_USAGE) && leftBonus) {
            spend(0.5f*((Hero)target).pointsInTalent(Talent.CORRECT_USAGE));
        }

        return damage;
    }

    public void defenseSkill () {
        boolean leftBonus = (int) visualcooldown() <= 20f;

        switch (state) {
            case LEVITATE:
                break;

            default:
                leftBonus = false;
                break;
        }

        if (Dungeon.hero.hasTalent(Talent.CORRECT_USAGE) && leftBonus) {
            spend(0.5f*Dungeon.hero.pointsInTalent(Talent.CORRECT_USAGE));
        }
    }

    public static int getColor (Potion p) {
        return buffColor.get(potions.get(p.getClass()));
    }

    @Override
    public int icon() { return BuffIndicator.POTION_UP; }

    @Override
    public void tintIcon(Image icon) { icon.hardlight( buffColor.get(state) ); }

    @Override
    public float iconFadePercent() { return Math.max(0, (DURATION - visualcooldown()) / DURATION); }

    @Override
    public String iconTextDisplay() { return Integer.toString((int)visualcooldown()); }

    @Override
    public String desc() {
        String result = Messages.get(this, "desc", (int)visualcooldown(), Messages.get(this, state + "_name"));

        result += " " + Messages.get(this, state + "");

        if (!bonusImmune.isEmpty()) {
            result += "\n" + Messages.get(this, "bonus_start");
            for (int i=0; i < bonusImmune.size(); i++) {
                result += " _" + Messages.get(bonusImmune.get(i), "name") + "_";
                if (i != bonusImmune.size()-1) result += ",";
            }

            result += Messages.get(this, "bonus_immune");
        }

        return result;
    }

    @Override
    public String name() { return super.name() + " : " + Messages.get(this, state + "_name"); }
    @Override

    public boolean attachTo(Char target) {
        if (super.attachTo(target)){
            if (state != null)
                stateImmunities();
            return true;
        } else {
            return false;
        }
    }
}
