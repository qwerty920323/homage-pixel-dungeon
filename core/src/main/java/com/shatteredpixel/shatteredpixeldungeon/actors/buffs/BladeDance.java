package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import static com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroAction;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Shopkeeper;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfForce;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Image;
import com.watabou.noosa.Visual;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;



public class BladeDance extends Buff implements ActionIndicator.Action {
    private static final float DURATION = 30f;

    {
        type = buffType.POSITIVE;
        revivePersists = true;
    }

    public static String attack  = "attack";
    public static String ability = "ability";
    public static String shoot   = "shoot";


    public boolean crazyDance;
    int icon = -1;

    protected float left;

    @Override
    public boolean attachTo( Char target ) {
        if (crazyDance) ((Hero)target).immersion++;
        else       ActionIndicator.setAction(this);

        BuffIndicator.refreshHero();
        return super.attachTo( target );
    }

    protected ArrayList<String> rank = new ArrayList<>();

    @Override
    public boolean act() {
        left -= TICK;
        if (left <= 0) {
            remove(0,true);
        }

        spend(TICK);

        return true;
    }

    public void remove(int i, boolean announce) {
        if (crazyDance) {
            crazyDance = false;
            detach();
            return;
        }

        if (!rank.isEmpty()) rank.remove(i);

        if (rank.isEmpty()) {
            ActionIndicator.clearAction(this);
            super.detach();
        } else {
            this.left = 30f;
            if (announce) announce();
        }
    }

    public void announce(){
        if (!rank.isEmpty()) {
            target.sprite.showStatus(CharSprite.WARNING, rank(getRankCount()));
            ActionIndicator.setAction(this);
        }
    }

    public void rankSet(String s) {
        if (crazyDance) return;

        if (rank.isEmpty()) {
            rank.add(s);
            this.left = 30f;
            announce();

        } else if (!s.equals(lastAct()) && getRankCount() < 7) {
            rank.add(s);
            this.left = 30f;
            left += TICK;
            announce();
        }

        if (attack.equals(lastAct())) {
            icon = BuffIndicator.COMBO;
        } else if (ability.equals(lastAct())) {
            icon = BuffIndicator.DANCER_ABIL;
        } else if (shoot.equals(lastAct())) {
            icon = BuffIndicator.DANCER_SHOT;
        }

        ActionIndicator.refresh();
    }

    public String lastAct() {
        if (rank.isEmpty()) return null;
        else                return rank.get(rank.size() - 1);
    }

    public int getRankCount() {
        if (!rank.isEmpty()) return rank.size();
        return 0;
    }

    public String rank(int rank) {
        String result;
        if (rank > 6) rank = 7;
        switch (rank) {
            case 1:
            default:
                result = "D";
                break;
            case 2:
                result = "C";
                break;
            case 3:
                result = "B";
                break;
            case 4:
                result = "A";
                break;
            case 5:
                result = "S";
                break;
            case 6:
                result = "SS";
                break;
            case 7:
                result = "SSS";
                break;
        }
        return result;
    }

    public boolean attack() {
        ArrayList<Char> targets = new ArrayList<>();

        for (Char ch : Actor.chars()) {
            if (ch.isAlive()
                    && !(ch instanceof Shopkeeper)
                    && !hero.isCharmedBy(ch)
                    && Dungeon.level.heroFOV[ch.pos]
                    && hero.canAttack(ch)) {
                targets.add(ch);
            }
        }
        //this look like dnace!
        int random = Random.index(targets);

        if (targets.isEmpty()) {
            return false;

        } else {
            Char ch = targets.get(random);

            Dungeon.hero.curAction = new HeroAction.Attack( ch );
            Dungeon.hero.next();
            return true;
        }
    }

    public int chaseEnemyPos(){
        Char closest = null;
        int pos = -1;

        for (Char ch : Actor.chars()){
            if (ch.alignment == Char.Alignment.ENEMY
                    && Dungeon.level.heroFOV[ch.pos]
                    && !Dungeon.level.adjacent(target.pos,ch.pos)){

                if (closest == null || Dungeon.level.trueDistance(hero.pos, closest.pos) > Dungeon.level.trueDistance(hero.pos, ch.pos)){
                    closest = ch;
                }
            }
        }

        if (closest != null)
            pos = closest.pos;

        //when no enemy to attack
        if (pos < 0) left--;

        return pos;
    }

    public void processDamage(int dmg){
        if (target == null || !target.isAlive() || !crazyDance)
            return;

        if (dmg > 0 && target.HP <= target.HT / 4) {
            left /= 2f;
            //device to prevent damage
            if (target.HP <= 0.15f * target.HT) {
                detach();
                Buff.prolong(hero, Talent.EvasionTracker.class, 0f);
            }
        }
    }

    //description
    public float descDelay() {return 11f + Math.round(10f * (Math.pow(1.367f, getRankCount())));}
    //attack speed
    public float delay() {return descDelay()/100f;}

    public float evasion () { return Math.round(100f*(Math.pow(quickstep(), getRankCount())))/100f;}
    public float quickstep () { return Math.min(1.22f, 1.05f + 0.06f * hero.pointsInTalent(Talent.QUICKSTEP));}
    public void danceOfDeath (Hero hero, Mob mob) {
        // +1 SSS over, +2 SS over, +3 S over
        if (crazyDance && getRankCount() > 7-Dungeon.hero.pointsInTalent(Talent.DANCE_OF_DEATH)) {

            if (hero.hasTalent(Talent.AGGRESSIVE_BARRIER) && (hero.HP / (float)hero.HT) <= 0.5f){
                int shieldAmt = 1 + 2*hero.pointsInTalent(Talent.AGGRESSIVE_BARRIER);
                Buff.affect(hero, Barrier.class).setShield(shieldAmt);
                hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shieldAmt), FloatingText.SHIELDING);
            }

            if (hero.buff(Talent.CounterAbilityTacker.class) != null){
                MeleeWeapon.Charger charger = Buff.affect(hero, MeleeWeapon.Charger.class);
                charger.gainCharge(hero.pointsInTalent(Talent.COUNTER_ABILITY)*0.375f);
                hero.buff(Talent.CounterAbilityTacker.class).detach();
            }

            if (hero.hasTalent(Talent.PRECISE_ASSAULT)){
                Buff.prolong(hero, Talent.PreciseAssaultTracker.class, hero.cooldown()+4f);
            }

            Buff.affect( hero, MeleeWeapon.Charger.class ).gainCharge(0.5f);
            ScrollOfRecharging.charge( hero );

            MeleeWeapon.onAbilityKill(hero, mob);
        }
    }

    @Override
    public String name() {
        String name = Messages.get(this, "name");
        if (!crazyDance) {
            name += " " + Messages.get(this, "ready");
            name += " : " + Messages.get(this, lastAct());
        }
        return name;
    }

    @Override
    public String desc() {
        String desc = Messages.get(this, "desc");
        if (!rank.isEmpty()) {
            desc += " " + Messages.get(this, "dance", descDelay());

            if (hero.hasTalent(Talent.QUICKSTEP))
                desc += "\n" + Messages.get(this, "evasion", evasion());

            if (!crazyDance) {
                desc += "\n" + Messages.get(this, "rank_and_left", left);
                desc += "\n\n" + Messages.get(this, "acts", Messages.get(this, lastAct()));
            }
        }
        return desc;
    }


    @Override
    public int icon() {
        if (crazyDance) return BuffIndicator.AMOK;
        else            return icon;
    }

    public float duration () {return crazyDance ? 10f : DURATION;}

    @Override
    public float iconFadePercent() {return Math.max(0, (duration() - left) / duration());}

    @Override
    public String iconTextDisplay() {return Integer.toString((int) left);}

    @Override
    public void tintIcon(Image icon) {icon.hardlight(1f, 0.4f, 0.2f);}

    private static final String ICON = "icon";
    private static final String RANK = "rank";
    private static final String LEFT = "left";
    private static final String CRAZYDANCE = "crazydance";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        String[] values = new String[rank.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = rank.get(i);
            bundle.put(RANK, values);
        }
        bundle.put(LEFT, left);
        bundle.put(CRAZYDANCE, crazyDance);
        bundle.put(ICON, icon);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        String[] values = bundle.getStringArray(RANK);
        for (String value : values) {
            rank.add(value);
        }
        left = bundle.getFloat(LEFT);
        crazyDance = bundle.getBoolean(CRAZYDANCE);
        icon = bundle.getInt(ICON);
    }

    @Override
    public Visual primaryVisual() {return new HeroIcon(this);}

    @Override
    public Visual secondaryVisual() {
        BitmapText txt = new BitmapText(PixelScene.pixelFont);
        txt.text(rank(getRankCount()));
        txt.hardlight(CharSprite.POSITIVE);
        if (getRankCount()>4) txt.hardlight(CharSprite.NEGATIVE);
        txt.measure();
        return txt;
    }

    @Override
    public String actionName() {
        return Messages.get(this, "action_name");
    }

    @Override
    public int actionIcon() {
        if (icon == BuffIndicator.COMBO) {
            return HeroIcon.COMBO;
        }
        if (icon == BuffIndicator.DANCER_ABIL) {
            return HeroIcon.WEAPON_SWAP;
        }
        if (icon == BuffIndicator.DANCER_SHOT) {
            return HeroIcon.MISSILE_MARK;
        }
        return HeroIcon.NONE;
    }

    @Override
    public int indicatorColor() {return 0xff6633;}

    @Override
    public void doAction() {
        boolean canAttact = false;
        for (Mob mob : Dungeon.level.mobs.toArray( new Mob[0] )) {
            if (Dungeon.level.heroFOV[mob.pos] && !(mob instanceof NPC)){
                canAttact = true;
                break;
            }
        }

        if (!canAttact) {
            GLog.w(Messages.get(Preparation.class, "no_target"));
            return;
        }

        hero.busy();
        crazyDance = true;
        left = 10f;

        target.sprite.showStatus(CharSprite.WARNING, Messages.get(this, "action_name"));
        target.sprite.emitter().burst(Speck.factory(Speck.JET), getRankCount());

        Sample.INSTANCE.play(Assets.Sounds.MISS, 2f, 0.8f);
        BuffIndicator.refreshHero();
        ActionIndicator.clearAction(this);

        hero.sprite.operate(target.pos, new Callback() {
            @Override
            public void call() {
                ((Hero)target).immersion++;
                hero.next();
            }
        });
    }


    @Override
    public void detach() {
        ActionIndicator.clearAction(this);

        Hero hero = ((Hero)target);
        if (hero.immersion > 0)
            hero.immersion--;

        if (lastAct() == ability
                && hero.hasTalent(Talent.COMBINED_STAMINA)
                && hero.belongings.weapon() instanceof MeleeWeapon
                && hero.buff(RingOfForce.BrawlersStance.class) == null){

            int tier = ((MeleeWeapon) hero.belongings.weapon()).tier
                    + 2 - hero.pointsInTalent(Talent.COMBINED_STAMINA);

            for (int i=0; i< tier; i++) {
                remove(0, false);
            }
            
            announce();
        } else {
            super.detach();
        }
    }
}
