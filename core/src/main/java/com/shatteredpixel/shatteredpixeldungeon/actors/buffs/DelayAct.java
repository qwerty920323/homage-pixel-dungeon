package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import static com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.QuickSlot;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.Feint;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GnollGeomancer;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.plants.Earthroot;
import com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.AttackIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.Visual;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;

import sun.security.provider.Sun;

public class DelayAct extends Buff implements ActionIndicator.Action{
    {
        //before the hero acts
        actPriority = HERO_PRIO+1;
    }

    public int pos = -1;

    public int object = 0;

    int select = -1;

    @Override
    public boolean act() {
        if (target.paralysed > 0) {
            return resetBuff();
        }

        if (select > 0) {
            Char enemy = enemy();
            Hero hero = (Hero) target;

            if (enemy != null) {

               return AttackOrInteract(hero, enemy);

            } else {
                Char ch = Actor.findChar(select);

                if (ch != null && Dungeon.level.heroFOV[ch.pos]){

                    if (((Mob) ch).heroShouldInteract()) {
                        return AttackOrInteract(hero, ch);

                    } else if (!target.rooted
                            && fling(hero, ch)
                            && Dungeon.level.adjacent(hero.pos, ch.pos)){
                        //fling ch
                    } else {
                        GLog.w(Messages.get(Feint.class, "bad_location"));
                    }

                } else if (!target.rooted && Dungeon.level.adjacent(hero.pos, select)){

                    hero.sprite.move(hero.pos, select);
                    hero.move(select);

                    hero.justMoved = true;

                    hero.search(false);
                } else {
                    if (target.rooted) {
                        PixelScene.shake( 1, 1f );
                    }

                    GLog.w(Messages.get(Feint.class, "bad_location"));
                }
            }
        }

        return resetBuff();
    }
    public boolean AttackOrInteract (Hero hero, Char enemy){
        if (hero == enemy)
            return resetBuff();

        if (((Mob) enemy).heroShouldInteract()) {
            //interact
            if (enemy.canInteract(hero)) {

                hero.sprite.turnTo( hero.pos, enemy.pos );

                if (enemy instanceof GnollGeomancer
                        || (enemy instanceof Mimic && enemy.alignment == Char.Alignment.NEUTRAL)) {
                    //enemy interact : hero.spend(TICK)
                    hero.spend(-TICK);
                }

                enemy.interact(hero);

            } else if (!target.rooted && Dungeon.level.adjacent(hero.pos, select)){
                //move to select when can't interact with enemy
                Char ch = Actor.findChar(select);
                if (ch != null) {
                    if (fling(hero, ch)){
                        //fling ch
                    } else {
                        GLog.w(Messages.get(Feint.class, "bad_location"));
                    }

                } else {
                    hero.sprite.move(hero.pos, select);
                    hero.move(select);

                    hero.justMoved = true;

                    hero.search(false);
                }
            } else {
                if (target.rooted) {
                    PixelScene.shake( 1, 1f );
                }
                GLog.w(Messages.get(Feint.class, "bad_location"));
            }
        } else {
            //Attack
            if (target.isCharmedBy(enemy)) {
                GLog.w( Messages.get(Charm.class, "cant_attack"));
                return resetBuff();
            }

            if (hero.canAttack( enemy ) && enemy.invisible == 0) {
                if (hero.hasTalent(Talent.AGGRESSIVE_BARRIER)
                        && hero.buff(Talent.AggressiveBarrierCooldown.class) == null
                        && (hero.HP / (float)hero.HT) < 0.20f*(1+hero.pointsInTalent(Talent.AGGRESSIVE_BARRIER))){
                    Buff.affect(hero, Barrier.class).setShield(3);
                    hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, "3", FloatingText.SHIELDING);
                    Buff.affect(hero, Talent.AggressiveBarrierCooldown.class, 50f);
                }

                target.sprite.attack(enemy.pos, new Callback() {
                    @Override
                    public void call() {
                        AttackIndicator.target(enemy);

                        float dmgBoost = hero.attackDelay() > 0 ? Math.max(0f, 1f / hero.attackDelay()) : 1f;

                        if (hero.attack(enemy, dmgBoost, 0, 1f)) {
                            if (hero.hasTalent(Talent.RUSH_ATTACK))
                                Buff.affect( hero, Talent.RushAttackTracker.class, 10f);
                        }

                        Invisibility.dispel();
                        resetBuff();
                    }
                });
            } else if (hero.pointsInTalent(Talent.ENHANCED_DELAY) >= 3 && delayThrow(hero, enemy)) {
                //throw missileweapon
            } else {
                GLog.w(Messages.get(Preparation.class, "out_of_reach"));
            }
        }

        return resetBuff();
    }

    private boolean fling (Hero hero, Char ch){
        if (ch.rooted
                || hero.buff(Vertigo.class) != null
                || hero.pointsInTalent(Talent.ENHANCED_DELAY) < 2
                || ch.properties().contains(Char.Property.IMMOVABLE)
                || (ch.properties().contains(Char.Property.LARGE) && !Dungeon.level.openSpace[hero.pos])){
            return false;
        }

        int oldPos = target.pos;
        int newPos = ch.pos;

        target.sprite.jump(target.pos, ch.pos, new Callback() {
            @Override
            public void call() {
                target.pos = newPos;
                target.move( newPos );

                ch.sprite.jump( newPos, oldPos , 8f, 0.23f, new Callback() {
                    @Override
                    public void call() {
                        ch.pos = oldPos;
                        ch.move(oldPos);
                        resetBuff();
                    }});
            }
        });

        return true;
    }

    private boolean delayThrow (Hero hero, Char ch) {
        Item i = null;
        if (Dungeon.quickslot.getItem(0) != null) {
            i = Dungeon.quickslot.getItem(0);
        }

        if (ch == null || i == null || i != hero.belongings.getItem(i.getClass())) return false;

        int cell = QuickSlotButton.autoAim(ch, i);
        if (cell == -1) return false;

        Item item = i;
        hero.sprite.operate(target.pos, new Callback() {
            @Override
            public void call() {
                item.cast(hero, cell);
                hero.spend(-item.castDelay(hero, cell));
                resetBuff();
            }
        });
        return true;
    }

    public void delaySpend (){
        //use interact move
        if (select > 0) Dungeon.hero.spend(-1 / Dungeon.hero.speed());
    }

    private boolean resetBuff (){
        //check target.pos
        if (target.buff(HoldFast.class) != null)
            target.buff(HoldFast.class).armorBonus();

        if (target.buff(Earthroot.Armor.class) != null)
            target.buff(Earthroot.Armor.class).absorb(0);

        Talent.PatientStrikeTracker strike = target.buff(Talent.PatientStrikeTracker.class);
        if (strike != null && strike.pos != target.pos)
            strike.detach();

        Dungeon.observe();
        GameScene.updateFog();
        detach();
        return true;
    }

    private static final String SELECT = "select";
    private static final String OBJECT = "object";
    private static final String POS = "pos";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(SELECT, select);
        bundle.put(OBJECT, object);
        bundle.put(POS, pos);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        select = bundle.getInt(SELECT);
        object = bundle.getInt(OBJECT);
        pos = bundle.getInt(POS);
    }

    @Override
    public boolean attachTo(Char target) {
        if (super.attachTo( target )) {
            ActionIndicator.setAction(this);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void detach() {
        super.detach();
        ActionIndicator.clearAction(this);
    }

    public Char enemy (){
        if (object == 0) return null;

        Char enemy = (Char) Actor.findById(object);

        if (!enemy.isAlive() || !Dungeon.level.heroFOV[enemy.pos]){
            enemy = null;
        }

        return enemy;
    }
    @Override
    public void doAction() {GameScene.selectCell(handle);}

    private CellSelector.Listener handle = new CellSelector.Listener() {

        @Override
        public void onSelect(Integer cell) {
            if (cell == null) return;

            final Char enemy = Actor.findChar(cell);

            if (enemy != null && Dungeon.hero.fieldOfView[enemy.pos]) {
                if (enemy instanceof Hero) {
                    return;
                }

                object = enemy.id();

            } else if (!Dungeon.level.adjacent(target.pos, cell)
                    || (Dungeon.level.pit[cell] && (!target.flying || target.buff(Levitation.class) == null))
                    || (Dungeon.level.solid[cell] && !Dungeon.level.passable[cell])
                    || (Dungeon.level.map[cell] == Terrain.ALCHEMY && cell != Dungeon.hero.pos)) {
                GLog.w(Messages.get(Feint.class, "bad_location"));
                return;
            }

            // like rest
            if (Dungeon.hero.hasTalent(Talent.ENHANCED_DELAY)) {
                if (Dungeon.hero.hasTalent(Talent.HOLD_FAST)) {
                    Buff.affect(Dungeon.hero, HoldFast.class).pos = Dungeon.hero.pos;
                }
                if (Dungeon.hero.hasTalent(Talent.PATIENT_STRIKE)) {
                    Buff.affect(Dungeon.hero, Talent.PatientStrikeTracker.class).pos = Dungeon.hero.pos;
                }
            }

            /** delay act */
            if (target.sprite != null)
                target.sprite.showStatus(CharSprite.POSITIVE, Messages.get(Hero.class, "wait"));

            Dungeon.hero.spendAndNextConstant(TICK);
            select = cell;
           // GLog.w(Messages.get(DelayAct.class, "time", select));

            if (target.cooldown() >= 1)
                spend(TICK);

        }

        @Override
        public String prompt() {
            return Messages.get(DelayAct.class, "prompt");
        }
    };

    @Override
    public int actionIcon() { return HeroIcon.DELAY_ACT; }

    @Override
    public Visual primaryVisual() {
        Image ico;
        ico = new HeroIcon(this);
        return ico;
    }

    @Override
    public int indicatorColor() {
        return 0x00FFFF;
    }

    @Override
    public String actionName() {
        return Messages.get(this, "action_name");
    }
}
