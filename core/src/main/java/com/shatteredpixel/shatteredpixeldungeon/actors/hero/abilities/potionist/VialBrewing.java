package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.potionist;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Vial;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.PotionistArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfFrost;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLevitation;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLiquidFlame;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfParalyticGas;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.AquaBrew;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.BlizzardBrew;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.Brew;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.InfernalBrew;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.ShockingBrew;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.Elixir;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfDragonsBlood;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfFeatherFall;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfIcyTouch;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfMight;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfToxicEssence;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.ExoticPotion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfCorrosiveGas;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfDragonsBreath;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfSnapFreeze;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfStormClouds;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.Reflection;

import java.util.LinkedHashMap;

public class VialBrewing extends ArmorAbility {

    {
        baseChargeUse = 35f;
    }

    public static final LinkedHashMap<Class<?extends Potion>, Class<?extends Potion>> brews = new LinkedHashMap<>();
    static {
        //potion to brew
        brews.put(PotionOfFrost.class,           BlizzardBrew.class);
        brews.put(PotionOfLiquidFlame.class,     InfernalBrew.class);
        brews.put(PotionOfParalyticGas.class,    ShockingBrew.class);
        //potion to elixir
        brews.put(PotionOfLevitation.class,      ElixirOfFeatherFall.class);
        brews.put(PotionOfStrength.class,        ElixirOfMight.class);

        //exotic to brew
        brews.put(PotionOfStormClouds.class,     AquaBrew.class);
        //exotic to elixir
        brews.put(PotionOfSnapFreeze.class,      ElixirOfIcyTouch.class);
        brews.put(PotionOfCorrosiveGas.class,    ElixirOfToxicEssence.class);
        brews.put(PotionOfDragonsBreath.class,   ElixirOfDragonsBlood.class);
    }
    boolean overChargeUse = false;

    @Override
    public float chargeUse( Hero hero ) {
        float chargeUse = super.chargeUse(hero);
        if (hero.buff(DoubleBrewTracker.class) != null){
            //reduced charge use by 16%/30%/41%/50%
            chargeUse *= Math.pow(0.84, hero.pointsInTalent(Talent.RESIDUAL_HEAT));
        }
        return chargeUse;
    }

    @Override
    public void activate(ClassArmor armor, Hero hero, Integer target ) {
        Potion potion = potionInVial(hero);
        if (potion == null) {
            GLog.w(Messages.get(this, "no_vial"));
            return;
        }

        if (target != null) {
            if (potion instanceof Brew
                    || potion instanceof Elixir
                    || (potion instanceof ExoticPotion && !cantBrewing(hero,potion))) {
                if (hero.hasTalent(Talent.RESIDUAL_HEAT))
                    Buff.prolong(hero, DoubleBrewTracker.class, 5f);

                float bonus = 50 * hero.pointsInTalent(Talent.HIGH_QUALITY)/2f;
                Vial vial = hero.belongings.getItem(Vial.class);
                vial.gainCharge(50f + bonus);

                GLog.w(Messages.get(this, "no_more"));
                ScrollOfRecharging.charge(hero);

                Sample.INSTANCE.play(Assets.Sounds.PUFF);
                armor.charge -= chargeUse(hero);
                armor.updateQuickslot();
                return;
            }

            float chargeUse = chargeUse(hero) * overNeeded(hero);
            overChargeUse = armor.charge >= chargeUse;

            Potion exotic = null;
            Potion brewing = null;

            if (!(potion instanceof ExoticPotion)) {
                exotic = Reflection.newInstance(ExoticPotion.regToExo.get(potion.getClass()));
            }

            if (cantBrewing(hero, potion)) {
                brewing = Reflection.newInstance(brews.get(potion.getClass()));
            }

            if (exotic != null && brewing != null) {
                choosePotions(armor, hero, exotic, brewing);
                return;
            }

            if (exotic == null) exotic = brewing;

            brewingWnd(armor, hero, exotic);
        }
    }

    public boolean cantBrewing(Hero hero, Potion potion) {
        return potion.isKnown() && brews.containsKey(potion.getClass()) && hero.hasTalent(Talent.SMART_BREWING);
    }

    protected Potion potionInVial(Hero hero) {
        if (hero.belongings.getItem(Vial.class) != null) {
            return hero.belongings.getItem(Vial.class).potion();
        }

        return null;
    }

    protected void updateArmorAndVial (ClassArmor armor, Hero hero, Potion p) {
        Vial vial = hero.belongings.getItem(Vial.class);
        float chargeUse = chargeUse(hero);

        if (vial != null) {
            vial.changePotion(p, hero);
            hero.sprite.operate(hero.pos);
            hero.sprite.emitter().start(Speck.factory(Speck.BUBBLE), 0.1f, 12);
            hero.spendAndNext(1f);

            float over = 1f;
            if (p instanceof Elixir || p instanceof Brew)
                over = overNeeded(hero);

            if (Dungeon.level.water[hero.pos]
                    && hero.hasTalent(Talent.RESIDUAL_HEAT)) {
                vial.gainCharge(35f * hero.pointsInTalent(Talent.RESIDUAL_HEAT));
            }

            if (hero.hasTalent(Talent.RESIDUAL_HEAT))
                Buff.prolong(hero, DoubleBrewTracker.class, 5f);

            Sample.INSTANCE.play(Assets.Sounds.PUFF);
            armor.charge -= chargeUse * over;
            armor.updateQuickslot();
            GLog.p(Messages.get(this, "change", p.name()));
        }
    }

    protected float overNeeded(Hero hero) {
        return 2.064f - 0.266f * hero.pointsInTalent(Talent.SMART_BREWING);
    }

    public void brewingWnd(ClassArmor armor, Hero hero, Potion result){
        GameScene.show(
                new WndOptions(new ItemSprite(result),
                        Messages.titleCase(result.name()),
                        Messages.get(result.getClass(), "desc") + "\n\n" + Messages.get(VialBrewing.class, "can_do", result.name()),
                        Messages.get(VialBrewing.class, "yes"), Messages.get(VialBrewing.class, "no")) {
                    @Override
                    protected void onSelect(int index) {
                        if (index == 0) {
                            if (result instanceof ExoticPotion)
                                updateArmorAndVial(armor, hero, result);
                            else if (overChargeUse) {
                                updateArmorAndVial(armor, hero, result);
                            } else {
                                brewingWnd(armor, hero, result);
                                GLog.w( Messages.get(armor, "low_charge") );
                                PixelScene.shake( 1, 0.1f );
                            }
                        } else {
                            //close window
                        }
                    }
                });
    }
    public void choosePotions (ClassArmor armor, Hero hero, Potion basic, Potion brew) {
        Game.runOnRenderThread(new Callback() {
            @Override
            public void call() {
                String[] options = new String[2];
                int i = 0;

                options[i++] = Messages.get(VialBrewing.class, "basic", Messages.titleCase(basic.title()));
                options[i] = Messages.get(VialBrewing.class, "brew", Messages.titleCase(brew.title()));

                GameScene.show(new WndOptions(new ItemSprite(new PotionistArmor()),
                        Messages.titleCase(name()),
                        Messages.get(VialBrewing.class, "choose", (int) chargeUse(hero), Math.round(chargeUse(hero)*overNeeded(hero))) , options){
                    @Override
                    protected void onSelect(int index) {
                        super.onSelect(index);
                        if (index == 0){
                            brewingWnd(armor, hero, basic);
                        } else {
                            brewingWnd(armor, hero, brew);
                        }
                    }

                    @Override
                    protected boolean hasIcon(int index) {
                        return index < 2;
                    }

                    @Override
                    protected Image getIcon(int index) {
                        if (index == 0){
                            return new ItemSprite(basic);
                        } else {
                            return new ItemSprite(brew);
                        }
                    }
                });
            }
        });
    }

    public static class DoubleBrewTracker extends FlavourBuff {};

    @Override
    public String desc() {
        String desc = Messages.get(this, "desc");
        if (Dungeon.hero != null) {
            Potion p = Reflection.newInstance(ExoticPotion.regToExo.get(potionInVial(Dungeon.hero).getClass()));
            if (p != null) {
                return desc +"\n\n"+ Messages.get(this, "vial", p.name()) + "\n\n" + Messages.get(this, "cost", (int)baseChargeUse);
            }
        }
        return desc + "\n\n" + Messages.get(this, "cost", (int)baseChargeUse);
    }

    @Override
    public int icon() { return HeroIcon.VIAL_BREWING; }

    @Override
    public Talent[] talents() {
        return new Talent[]{Talent.SMART_BREWING, Talent.HIGH_QUALITY, Talent.RESIDUAL_HEAT, Talent.HEROIC_ENERGY};
    }
}
