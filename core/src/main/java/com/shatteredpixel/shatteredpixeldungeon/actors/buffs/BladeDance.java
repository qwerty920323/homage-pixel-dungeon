package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import static com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
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

import java.util.ArrayList;

import javax.naming.NameParser;

public class BladeDance extends Buff implements ActionIndicator.Action {
    private static final float DURATION = 30f;

    {

        actPriority = BUFF_PRIO + 1;
        type = buffType.POSITIVE;
        revivePersists = true;

    }

    public static String attack  = "attack";
    public static String ability = "ability";
    public static String shoot   = "shoot";

    public boolean crazyDance;
    int icon;

    protected float left;

    protected ArrayList<String> rank = new ArrayList<>();

    @Override
    public boolean act() {

        if (!crazyDance) {
            ActionIndicator.refresh();
            left -= TICK;
        }

        if (left <= 0) {
            remove(0,true);
        }

        spend(TICK);

        return true;
    }

    @Override
    public boolean attachTo(Char target) {
        ActionIndicator.setAction(this);
        return super.attachTo(target);
    }

    public void remove(int i, boolean announce) {
        if (!rank.isEmpty()) rank.remove(i);
        if (crazyDance) crazyDance = false;

        if (rank.isEmpty()) {
            detach();
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

    public void setShiled() {
        Buff.affect(target, InfiniteEvasionDance.class, 10f);

        Barrier barrier = Buff.affect(target, Barrier.class);
        barrier.incShield(getRankCount() * ((Hero)target).pointsInTalent(Talent.QUICKSTEP));
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

    public float bonus (float input){
        //delay (공속) = 1.345f // evasion(회피) = 1.22f
        float output = Math.round(10f * (Math.pow(input, getRankCount())));

        return output;
    }

    public boolean weaponChargeCheck (){
        if (crazyDance
                && lastAct() == ability
                && Dungeon.hero.hasTalent(Talent.COMBINED_AGILITY)){
            return true;
        }
        return false;
    }


    @Override
    public String name() {
        String name = Messages.get(this, "name");
        name += " " + Messages.get(this, "ready");
        name += " : " + Messages.get(this, lastAct());
        return name;
    }

    @Override
    public String desc() {
        String desc = Messages.get(this, "desc");
        if (!rank.isEmpty()) {
            desc += " " + Messages.get(this, "dance", bonus(1.345f), bonus(1.22f)/10f);
            desc += "\n" + Messages.get(this, "rank_and_left", left);
            desc += "\n\n" + Messages.get(this, "acts", Messages.get(this, lastAct()));
        }
        return desc;
    }


    @Override
    public int icon() {
        int result;
        if (crazyDance){
            result = BuffIndicator.NONE;
        } else if (lastAct() == attack) {
            result = BuffIndicator.COMBO;
        } else if (lastAct() == ability) {
            result = BuffIndicator.DANCER_ABIL;
        } else if (lastAct() == shoot) {
            result = BuffIndicator.DANCER_SHOT;
        } else {
            result = icon; //재시작 시 계산
        }
        return result;
    }

    @Override
    public float iconFadePercent() {
        return Math.max(0, (DURATION - left) / DURATION);
    }

    @Override
    public String iconTextDisplay() {
        return Integer.toString((int) left);
    }

    @Override
    public void tintIcon(Image icon) {icon.hardlight(1f, 0.4f, 0.2f);}

    private static final String RANK = "rank";
    private static final String LEFT = "left";
    private static final String CRAZYDANCE = "crazydance";
    private static final String ICON = "icon";

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
        bundle.put(ICON, icon());
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
        return HeroIcon.BLADE_DANCE;
    }

    @Override
    public int indicatorColor() {
        return 0xff6633;
    }

    @Override
    public void doAction() {
        boolean canAttack = false;
        for (Mob mob : Dungeon.level.mobs.toArray( new Mob[0] )) {
            if (Dungeon.level.heroFOV[mob.pos] && !(mob instanceof NPC)){
                canAttack = true;
                break;
            }
        }

        if (!canAttack) {
            GLog.w(Messages.get(Preparation.class, "no_target"));
            return;
        }

        target.sprite.showStatus(CharSprite.WARNING, Messages.get(this, "action_name"));
        target.sprite.emitter().burst(Speck.factory(Speck.JET), getRankCount());

        Sample.INSTANCE.play(Assets.Sounds.MISS, 2f, 0.8f);
        BuffIndicator.refreshHero();
        ActionIndicator.clearAction(this);

        hero.busy();
        crazyDance = true;

        hero.sprite.operate(target.pos, new Callback() {
            @Override
            public void call() {
                Buff.affect(target, CrazyDance.class).set(bonus(1.345f), bonus(1.22f));
                hero.next();
            }
        });
    }

    @Override
    public void detach() {
        ActionIndicator.clearAction(this);
        super.detach();
    }

    public static class InfiniteEvasionDance extends FlavourBuff {}
}
