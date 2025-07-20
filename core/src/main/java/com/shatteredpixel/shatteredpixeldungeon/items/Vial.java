package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Regeneration;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.VelvetPouch;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfFrost;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHaste;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfInvisibility;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLevitation;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLiquidFlame;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfMindVision;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfParalyticGas;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfPurity;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.Brew;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfDivineInspiration;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfDragonsBreath;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfMastery;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.HashMap;

public class Vial extends Item {
    public int curCharges, maxCharges = 1;
    public float partialCharge = 0f;

    protected Charger charger;
    protected Potion potion;
    protected int purity = 0; //purity value is positive/negative potion
    protected boolean readyThrow;
    public static final String AC_PURITY = "PURITY";
    public static final String AC_DRINK  = "DRINK";
    public static final String AC_BREW   = "BREW";

    {
        image = ItemSpriteSheet.VIAL;

        defaultAction = defaultAction();
        usesTargeting = targeting();

        unique = true;
        bones = false;
        stackable = true; //vial is only one. this is used because of quickslot logic
    }

    public static final ArrayList<Class> positive = new ArrayList<>();
    public static final ArrayList<Class> negative = new ArrayList<>();
    static {
        positive.add(PotionOfHealing.class);
        positive.add(PotionOfInvisibility.class);
        positive.add(PotionOfMindVision.class);
        positive.add(PotionOfHaste.class);
        positive.add(PotionOfPurity.class);
        positive.add(PotionOfLevitation.class);

        negative.add(PotionOfFrost.class);
        negative.add(PotionOfLiquidFlame.class);
        negative.add(PotionOfToxicGas.class);
        negative.add(PotionOfParalyticGas.class);
    }

    private static final HashMap<Integer, Integer> potionColors = new HashMap<>();
    static {
        potionColors.put(ItemSpriteSheet.ELIXIR_MIGHT,     0xCC0022);
        potionColors.put(ItemSpriteSheet.ELIXIR_DRAGON,    0xFF7F00);
        potionColors.put(ItemSpriteSheet.BREW_SHOCKING,    0xCCBB00);
        potionColors.put(ItemSpriteSheet.ELIXIR_TOXIC,     0x2EE62E);
        potionColors.put(ItemSpriteSheet.ELIXIR_ICY,       0x66B3FF);
        potionColors.put(ItemSpriteSheet.BREW_BLIZZARD,    0x195D80);
        potionColors.put(ItemSpriteSheet.BREW_AQUA,        0xA15CE5);
        potionColors.put(ItemSpriteSheet.BREW_INFERNAL,    0xFF4CD2);
        potionColors.put(ItemSpriteSheet.ELIXIR_FEATHER,   0XD9D9D9);
    }

    @Override
    public ArrayList<String> actions(Hero hero ) {
        ArrayList<String> actions = super.actions( hero );
        actions.add(AC_DRINK);
        if (hero.hasTalent(Talent.PURITY_VIAL)) {
            actions.add(AC_PURITY);
        }
        if (hero.subClass == HeroSubClass.ALCHEMIST) {
            actions.add(AC_BREW);
        }
        return actions;
    }

    public Vial() {
        potion = new PotionOfToxicGas();
        potion.identify();
        curCharges = maxCharges;
        icon = potionsIcon();
    }


    public String defaultAction() {
        if (readyThrow) {
            return AC_THROW;
        } else if (potion != null) {
            if      (potion instanceof Brew)         return AC_THROW;
            else if (potion.mustDrinkPotion(potion)) return AC_DRINK;
        }
        return AC_THROW;
    }

    public boolean targeting () {
        if (readyThrow) {
            return true;
        } else if (potion != null) {
            if      (potion instanceof Brew)         return true;
            else if (potion.mustDrinkPotion(potion)) return false;
        }
        return true;
    }

    @Override
    public void execute(Hero hero, String action) {
        //vial cant do potion.identify if talent point is 0
        potion.canIdentfy(hero.hasTalent(Talent.ALCHEMIST_INTUITION));

        super.execute(hero, action);

        if (action.equals(AC_DRINK)) {
            if (potion == null || curCharges < 1) {
                GLog.w(Messages.get(this, "no_charge"));
                return;
            }
            drink(hero);

        } else if (action.equals(AC_PURITY)) {
            GameScene.show(
                    new WndOptions(new ItemSprite(this),
                            Messages.get(Vial.class, "choose"),
                            Messages.get(Vial.class, "choose_one"),
                            Messages.get(Vial.class, "choose_positive"), Messages.get(Vial.class, "choose_negative"), Messages.get(Vial.class, "choose_reset")) {
                        @Override
                        protected void onSelect(int index) {
                            if (index == 0) {
                                //positive
                                setPurity(hero, 1);
                                GLog.w(Messages.get(Vial.class, "choose_positive") + Messages.get(Vial.class, "often"));
                            } else if (index == 1){
                                //negative
                                setPurity(hero, -1);
                                GLog.w(Messages.get(Vial.class, "choose_negative") + Messages.get(Vial.class, "often"));
                            } else {
                                //reset
                                setPurity(hero, 0);
                                GLog.w(Messages.get(Vial.class, "reset"));
                            }
                        }
                    }
            );

        } else if (action.equals(AC_BREW)) {
            GameScene.selectItem(itemSelector);
        }
    }

    protected WndBag.ItemSelector itemSelector = new WndBag.ItemSelector() {

        @Override
        public String textPrompt() {
            return Messages.get(Vial.class, "prompt");
        }

        @Override
        public Class<?extends Bag> preferredBag(){
            return VelvetPouch.class;
        }

        @Override
        public boolean itemSelectable(Item item) {
            return item instanceof Plant.Seed;
        }

        @Override
        public void onSelect(Item item) {
            if (item != null) {
                float needs = (float) (1f - Math.pow(2f, curUser.pointsInTalent(Talent.SOLVENT_EXTRACTION))/10f);
                if (curUser.hasTalent(Talent.SOLVENT_EXTRACTION)
                        && (curCharges > 0 || partialCharge >= needs)) {

                    Plant.Seed result = null;
                    for (Class <? extends Plant.Seed> seed : Potion.SeedToPotion.types.keySet()) {
                        if (Potion.SeedToPotion.types.get(seed) == potion().getClass()) {
                            result = Reflection.newInstance(seed);
                        }
                    }

                    if (result == null) return;

                    if (partialCharge >= needs) {
                        partialCharge -=- needs;
                    } else if (curCharges > 0) {
                        partialCharge += curCharges - needs;
                        curCharges--;
                    }

                    if (result.doPickUp(Dungeon.hero)) {
                        Dungeon.hero.spend(-TIME_TO_PICK_UP);
                    } else {
                        Dungeon.level.drop( result, curUser.pos ).sprite.drop();
                    }
                }

                Potion p = Reflection.newInstance(Potion.SeedToPotion.types.get(item.getClass()));
                GLog.p( Messages.get(Vial.class, "brew") );

                changePotion(p, curUser);
                curUser.sprite.emitter().start(Speck.factory(Speck.BUBBLE), 0.1f, 12);
                Sample.INSTANCE.play( Assets.Sounds.PUFF );
                curUser.sprite.operate(curUser.pos);

                item.detach(curUser.belongings.backpack);
            }

        }
    };

    public void applyChargeBuff(Char owner){
        if (charger == null) charger = new Charger();
        charger.attachTo(owner);
        potionIdentify((Hero) owner);
    }

    @Override
    public boolean collect( Bag container ) {
        if (super.collect(container)) {
            if (container.owner != null) {
               applyChargeBuff(container.owner);
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onDetach( ) {
        if (charger != null) {
            charger.detach();
            charger = null;
        }
    }

    @Override
    public boolean isUpgradable() {
        return false;
    }

    @Override
    public boolean isIdentified() {
        return true;
    }

    public void changePotion (Potion p, Hero owner) {
        potion = p;
        potionIdentify(owner);
    }

    @Override
    protected void onThrow( int cell ) {
        Vial vial = this;

        if (curCharges < 1) {
            GLog.w(Messages.get(this, "no_charge"));

        } else {
            if (Dungeon.level.pit[cell] && Actor.findChar(cell) == null) {
                //do nothing

            } else {
                potion.isThrowing();
                Dungeon.level.pressCell(cell);
                potion.shatter(cell);

                Talent.onVialUsed(Dungeon.hero,1f + (maxCharges-curCharges)/5f);

                vial.updatePotion(Dungeon.hero);
            }
        }

        if (vial.doPickUp(Dungeon.hero)) {
            Dungeon.hero.spend(-TIME_TO_PICK_UP);
        } else {
            Dungeon.level.drop( vial, Dungeon.hero.pos ).sprite.drop();
        }
    }

    private void drink( Hero hero ) {

        if (potion instanceof PotionOfMastery
                || potion instanceof PotionOfDragonsBreath
                || potion instanceof PotionOfDivineInspiration) {
            potion.vialDrink(hero);

        } else {
            potion.alchemistBuff(potion, hero);

            potion.apply(hero);
            hero.spend( 1f );
            hero.busy();

            Sample.INSTANCE.play( Assets.Sounds.DRINK );

            hero.sprite.operate( hero.pos );

            Talent.onVialUsed(Dungeon.hero,1f + (maxCharges-curCharges)/5f);

            updatePotion(hero);
        }
    }

    @Override
    public void cast( final Hero user, final int dst ) {
        Char ch = Actor.findChar(dst);

        if (ch != null && ch == user) {
            execute(user, AC_DRINK);

        } else {
            super.cast(user, dst);
        }
    }

    public void gainCharge( float amt ){
        if (charger != null){
            charger.gainCharge(amt);
        }
    }

    @Override
    public int energyVal() {
        int val = Math.round(((float) potion.energyVal() * Dungeon.hero.pointsInTalent(Talent.ENERGIZE_VIAL) / 4f));
        return Dungeon.hero.hasTalent(Talent.ENERGIZE_VIAL) && curCharges >= 1 ? val : 0;
    }
    public static void gainEnergy (Item item, int slot) {
        if (item instanceof Vial) {
            item.quantity(1).collect();
            if (slot >= 0) Dungeon.quickslot.setSlot(slot, item);
            ((Vial)item).updatePotion(Dungeon.hero);
        }
    }
    public static int setQuickslot (Item item) {
        int slot = -1;
        if (Dungeon.quickslot.contains(item)){
            slot = Dungeon.quickslot.getSlot(item);
        }
        return slot;
    }

    public Potion potion(){ return potion != null ? potion : null; }

    private void setPurity (Hero hero, int value) {
        purity = value;
        Sample.INSTANCE.play( Assets.Sounds.PUFF );

        hero.busy();
        hero.sprite.operate(hero.pos);
        hero.sprite.emitter().start(Speck.factory(Speck.BUBBLE), 0.1f, 12);
    }

    private int max () {
        //dungeon depth 6, 11, 16, 21
       return Math.min((Dungeon.scalingDepth()-1)/5, 4);
    }
    public void updateMaxCharges() {
        maxCharges = Math.max( 1 + max() , maxCharges );
        curCharges = Math.min( curCharges, maxCharges );
    }

    public void updatePotion () { updatePotion(curUser); }
    private void updatePotion (Hero owner){
        if (potion != null) {
            curCharges--;
        }

        if (!owner.hasTalent(Talent.PURITY_VIAL) && purity != 0) purity = 0;

        Potion p = potion;
        //purity_vial 1 = max 1 / purity_vial 2 = max 3
        int max = purity != 0 ? Math.round(1.4f*owner.pointsInTalent(Talent.PURITY_VIAL)) : 0;

        do {
            //here is low purity
            if (Random.Int(2+max) == 0) {
                //no select purity is negative
                if (purity >= 0) p = ((Potion) Reflection.newInstance(Random.element(negative)));
                if (purity < 0)  p = ((Potion) Reflection.newInstance(Random.element(positive)));

            //here is high purity
            } else {
                //no select purity is positive
                if (purity >= 0) p = ((Potion) Reflection.newInstance(Random.element(positive)));
                if (purity < 0)  p = ((Potion) Reflection.newInstance(Random.element(negative)));

            }
        } while (p.getClass() == potion.getClass());

        if (maxCharges < 1 + max()) {
            GLog.p(Messages.get(Vial.class, "levelup"));
            updateMaxCharges();
        }

       changePotion(p, owner);
    }

    public void potionIdentify (Hero hero) {
        if (hero.pointsInTalent(Talent.ALCHEMIST_INTUITION) == 2) {
            //this is for vial can potion identify
            potion.canIdentfy(true);
            potion.identify();
        }

        icon = potionsIcon();
        updateQuickslot();
    }

    public int potionsIcon () {
        int icon = -1;

        if (potion != null && potion.isKnown()) icon = potion.icon;

        return icon;
    }

    @Override
    public String status() {
        if (Dungeon.hero != null && Dungeon.hero.belongings.getItem(this.getClass()) != null) {
            return Messages.get(this, "charge", curCharges, maxCharges);
        }
        return null;
    }

    @Override
    public String name() {
        if (potion != null && potion.isKnown()) {
            return Messages.get(this, "vial_name", potion.name());
        }
        return super.name();
    }

    @Override
    public String desc() {
        String desc = super.desc();

        if (potion != null){
            if (Dungeon.hero != null && Dungeon.hero.hasTalent(Talent.PURITY_VIAL)) {
                if (purity > 0)   desc += " " + Messages.get(this, "positive");
                if (purity < 0)   desc += " " + Messages.get(this, "negative");
            }

            if (potion.isKnown()) desc += "\n\n" + potion.desc();
            else                  desc += "\n\n" + Messages.get( Potion.class, "unknown_desc");
        }

        return desc;
    }

    @Override
    public ItemSprite.Glowing glowing() {
        return potion != null && potionColors.containsKey(potion.image)? new ItemSprite.Glowing( potionColors.get(potion.image) ) : null;
    }

    private static final String PURITY        = "purity";
    private static final String POTION        = "potion";
    private static final String READY_THROW   = "ready_throw";
    private static final String CUR_CHARGES   = "curCharges";
    private static final String MAX_CHARGES   = "maxCharges";
    private static final String PARTIALCHARGE = "partialCharge";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put( PURITY, purity );
        bundle.put( POTION, potion );
        bundle.put( READY_THROW, readyThrow );
        bundle.put( CUR_CHARGES, curCharges );
        bundle.put( MAX_CHARGES, maxCharges );
        bundle.put( PARTIALCHARGE , partialCharge );
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        purity = bundle.getInt( PURITY );
        potion = (Potion) bundle.get( POTION );
        readyThrow = bundle.getBoolean( READY_THROW );
        curCharges = bundle.getInt( CUR_CHARGES );
        maxCharges = bundle.getInt( MAX_CHARGES );
        partialCharge = bundle.getFloat( PARTIALCHARGE );
    }

    @Override
    public int value() {
        return 0;
    }

    public class Charger extends Buff {
        @Override
        public boolean attachTo( Char target ) {
            if (super.attachTo( target )) {
                //if we're loading in and the hero has partially spent a turn, delay for 1 turn
                if (target instanceof Hero && Dungeon.hero == null && cooldown() == 0 && target.cooldown() > 0) {
                    spend(TICK);
                }
                return true;
            }
            return false;
        }

        @Override
        public boolean act() {
            if (curCharges < maxCharges && target.buff(MagicImmune.class) == null)
                recharge();

            if (partialCharge >= 100f) {
                partialCharge -= 100f;
                curCharges ++;
            }

            readyThrow = ((Hero)target).subClass == HeroSubClass.PLAGUE_DR;
            updateQuickslot();
            spend( TICK );

            return true;
        }
        private void recharge(){
            int missingCharges = maxCharges - 1;
            missingCharges = Math.max(0, missingCharges);

            int square = missingCharges / Math.max(1, curCharges);

            if (Regeneration.regenOn()) {
                float amount = (float) (1f + Math.pow(1.138f, square));
                partialCharge += amount;
            }

        }

        public void gainCharge(float charge){
            if (curCharges < maxCharges) {
                partialCharge += charge;
                while (partialCharge >= 100f) {
                    curCharges++;
                    partialCharge -= 100f;
                }
                if (curCharges >= maxCharges){
                    partialCharge = 0f;
                    curCharges = maxCharges;
                }
                updateQuickslot();
            }
        }
    }
}
