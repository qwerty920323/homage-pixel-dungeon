package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import static com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
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

public class BladeDance extends Buff implements ActionIndicator.Action {
    private static final float DURATION = 30f;

    {

        actPriority = BUFF_PRIO - 1;
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
            left -= turnDown();
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
            GLog.p(Messages.get(this, "rank", rank(getRankCount())));
            target.sprite.showStatus(CharSprite.WARNING, rank(getRankCount()));
            ActionIndicator.setAction(this);
        }
    }

    public float turnDown() {
        float rankCount = getRankCount() - 1;

        return (float) ((Math.floor(10f * Math.pow(1.125f, rankCount))) / 10f);
        // SSS에서 턴 당 2 감소
    }


    public void rankSet(String s) {
        if (rank.isEmpty()) {
            rankAdd(s);
            this.left = 31f;
        } else if (!s.equals(lastAct()) && getRankCount() < 7) {
            rankAdd(s);
        }
        ActionIndicator.refresh();
    }

    public void rankAdd(String s) {
        if (!crazyDance) {
            rank.add(s);
            left += turnDown();
            announce();
        }
    }

    public String lastAct() {
        if (rank.isEmpty()) return null;
        else                return rank.get(rank.size() - 1);
    }

    public int getRankCount() {
        if (!rank.isEmpty()) return rank.size();
        else return 0;
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
        //delay (공속) = 1.345f // evasion(회피) = 1.17f
        boolean check = hero.buff(Talent.CombinedIncreaseAbilityTracker.class) != null;

        float output = Math.round(10f * (Math.pow(input, getRankCount())));

        float rankcheck = getRankCount() > 4 ? 0.2f : 0.1f;
        float result = check ? output * (1 + rankcheck * hero.pointsInTalent(Talent.COMBINED_AGILITY)) : output;

        return Math.round(result);
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
            desc += " " + Messages.get(this, "dance", bonus(1.345f), bonus(1.17f)/10f);
            desc += "\n" + Messages.get(this, "rank_and_left",
                    turnDown(), Math.ceil(10 * left / turnDown()) / 10, Math.ceil(10 * left) / 10);
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
     //   if (hero.buff(Talent.CombinedIncreaseAbilityTracker.class) != null) return 0x33CC00;

        return 0xff6633;
    }

    @Override
    public void doAction() {
        target.sprite.showStatus(CharSprite.WARNING, Messages.get(this, "action_name"));
        target.sprite.emitter().burst(Speck.factory(Speck.RED_LIGHT), 2);

        Sample.INSTANCE.play(Assets.Sounds.MISS, 2f, 0.8f);
        BuffIndicator.refreshHero();
        ActionIndicator.clearAction(this);

   //     boolean bonus = hero.buff(Talent.CombinedIncreaseAbilityTracker.class) != null;

        hero.busy();
        crazyDance = true;

        hero.sprite.operate(target.pos, new Callback() {
            @Override
            public void call() {
                Buff.affect(target, CrazyDance.class).set(10f, bonus(1.345f), bonus(1.17f));
                hero.next();
            }
        });
    }

    @Override
    public void detach() {
        ActionIndicator.clearAction(this);
        super.detach();
    }
}
