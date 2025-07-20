package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroAction;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.Feint;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.potionist.Fleeing;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GnollGeomancer;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Ghost;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.RatKing;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Shopkeeper;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Wandmaker;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.DriedRose;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfWarding;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.plants.Earthroot;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.AttackIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.Visual;
import com.watabou.noosa.tweeners.Delayer;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Reflection;

public class DelayAct extends Buff implements ActionIndicator.Action{

    boolean delayInteract = false;

    public int object = -1; // when over 0 that means enemy

    public int select = -1; // when over -1 that means cell

    @Override
    public boolean attachTo( Char target ) {
        if (select > -1) ((Hero)target).immersion++;
        else       ActionIndicator.setAction(this);

        ActionIndicator.refresh();
        return super.attachTo( target );
    }

    @Override
    public boolean act() {
        if (target.paralysed >0) detach();
        
        spend(TICK);
        resetPos();
        return true;
    }

    public void acting () {
        if (select == -1) {
            detach();
            return;
        }

        if (target.fieldOfView == null || target.fieldOfView.length != Dungeon.level.length()){
            target.fieldOfView = new boolean[Dungeon.level.length()];
            Dungeon.level.updateFieldOfView( target, target.fieldOfView );
        }

        Char ch = Actor.findChar( select );
        if (enemy() != null) {
            ch = enemy();
            select = ch.pos;
        }

        if (!Dungeon.level.adjacent(target.pos, select) && object == -1) {
            if (ch != null && (Dungeon.hero.canAttack(ch) || ((Mob) ch).heroShouldInteract())) {
                //do nothing
            } else if (Dungeon.hero.hasTalent(Talent.ENHANCED_DELAY)){
                delayThrow(Dungeon.hero, ch, select);
                return;
            }
        }


        if (ch != null) {
            if (target.fieldOfView[select] && ch instanceof Mob) {
                if (((Mob) ch).heroShouldInteract() || object == -1) { //if do not select character
                    Dungeon.hero.immersion++; //make uncontrollable
                    Dungeon.hero.sprite.turnTo(Dungeon.hero.pos, ch.pos);

                    Char finalCh = ch;
                    target.sprite.parent.add(new Delayer(0.2f){
                        @Override
                        protected void onComplete() {
                            Dungeon.hero.curAction = new HeroAction.Interact(finalCh);
                            getDelayInteract(finalCh);

                            Dungeon.hero.immersion--; //can controll
                            detach();
                        }
                    });
                    return;
                } else {
                    Dungeon.hero.curAction = new HeroAction.Attack(ch);
                }
            }
        } else {
            Dungeon.hero.curAction = new HeroAction.Move( select );
            Dungeon.hero.lastAction = null;
        }

        detach();
    }

    private void delayThrow (Hero hero, Char ch, int cell) {
        Item i = null;
        if (Dungeon.quickslot.getItem(0) != null) {
            i = Dungeon.quickslot.getItem(0);
        }

        if (i == null || i != hero.belongings.getItem(i.getClass())) {
            GLog.w(Messages.get(DelayAct.class, "no_items"));
            detach();
            return;
        }

        if (ch != null)  {
            cell = QuickSlotButton.autoAim(ch, i);
            if (cell < 0){
                GLog.w(Messages.get(Fleeing.class, "no_target"));
                detach();
                return;
            }
        }

        Item item = i;
        int finalCell = cell;
        hero.sprite.operate(hero.pos, new Callback() {
            @Override
            public void call() {
                item.cast(hero, finalCell);
                hero.spend(-item.castDelay(hero, finalCell));
                detach();
            }
        });
    }

    public boolean getDelayInteract (Char ch) {
        boolean result = ch.isAlive() && ch.canInteract(target);
        if (result) {
            if (ch instanceof GnollGeomancer
                    || (ch instanceof Mimic && ch.alignment == Char.Alignment.NEUTRAL)) {
                //interacting with this mob spend tick
                ((Hero) target).spend(-TICK);
                return result;
            }

            if (!(ch instanceof RatKing
                    //excluding npc that create dialogue window
                    || ch instanceof Ghost || ch instanceof Wandmaker
                    || ch instanceof Blacksmith || ch instanceof Imp
                    || ch instanceof Shopkeeper || ch instanceof WandOfWarding.Ward))
                Buff.affect(Dungeon.hero, Talent.DelayInteractTracker.class, 0f);
        }

        return result;
    }

    public void resetPos (){
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
    }

    private static final String SELECT = "select";
    private static final String OBJECT = "object";
    private static final String DELAY_INTERACT = "delay_interact";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(SELECT, select);
        bundle.put(OBJECT, object);
        bundle.put(DELAY_INTERACT, delayInteract);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        select = bundle.getInt(SELECT);
        object = bundle.getInt(OBJECT);
        delayInteract = bundle.getBoolean(DELAY_INTERACT);
    }

    @Override
    public void detach() {
        if (((Hero)target).immersion > 0)
            ((Hero)target).immersion--;

        if (Dungeon.hero.curAction != null)
            Buff.affect(Dungeon.hero, Talent.DelayTimeTracker.class, 0f);

        ActionIndicator.clearAction(this);
        Dungeon.hero.next();
        super.detach();
    }

    public Char enemy (){
        if (object == -1) return null;

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

            } else if ((!Dungeon.level.adjacent(target.pos, cell) && !Dungeon.hero.hasTalent(Talent.ENHANCED_DELAY))
                    || (Dungeon.level.pit[cell] && (!target.flying || target.buff(Levitation.class) == null))
                    || (Dungeon.level.solid[cell] && !Dungeon.level.passable[cell]) //not door
                    || (Dungeon.level.map[cell] == Terrain.ALCHEMY && cell != Dungeon.hero.pos)) {
                GLog.w(Messages.get(Feint.class, "bad_location"));
                return;
            }

            if (target.sprite != null)
                target.sprite.showStatus(CharSprite.POSITIVE, Messages.get(Hero.class, "wait"));

            ((Hero)target).immersion++;
            ((Hero)target).spendAndNextConstant(TICK);
            select = cell;

            Dungeon.hero.busy();
            //act no delay when hero has buff like timeFreeze or TimeBubble
            if (target.cooldown() >= 1)
                spend(TICK);

            ActionIndicator.refresh();
        }

        @Override
        public String prompt() {
            return Messages.get(DelayAct.class, "prompt");
        }
    };

    private CharSprite sprite() {

        Char enemy = enemy();
        CharSprite sprite = enemy.sprite;

        if (enemy != null) {
            sprite = Reflection.newInstance(((Mob) enemy).spriteClass);
            sprite.linkVisuals(enemy);
            sprite.idle();
            sprite.paused = true;

            if (sprite.width() > 20 || sprite.height() > 20) {
                sprite.scale.set(PixelScene.align(20f / Math.max(sprite.width(), sprite.height())));
            }
        }

        return sprite;
    }

    @Override
    public int actionIcon() { return HeroIcon.DELAY_ACT; }

    @Override
    public Visual secondaryVisual() {
        Image ico = null;
        if (object > 0){
            ico = sprite();
        } else if (select > -1){
            ico = Icons.get(Icons.ARROW);
        }
        return ico;
    }
    @Override
    public Visual primaryVisual() {
        Image ico;
        ico = new HeroIcon(this);

        if (select > -1) {
            ico.brightness(0.3f);
        }

        return ico;
    }

    @Override
    public int indicatorColor() {
        if (object > 0){
            return 0xC03838;
        } else if (select > -1){
            return 0xA3A695;
        } else {
            return 0xC9C9C9;
        }
    }

    @Override
    public String actionName() {
        return Messages.get(this, "action_name");
    }
}
