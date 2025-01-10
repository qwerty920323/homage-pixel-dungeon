package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import static com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

public class PinArrow extends FlavourBuff{

    public static final float DURATION	= 10f;

    public int count;
    //pined arrow count

    public int infoDmg (){
        SpiritBow bow = Dungeon.hero.belongings.getItem(SpiritBow.class);

        if (bow == null) return 0;

        int lvl = bow.buffedLvl();
        int arrowCount = count;
        float augment;

        switch (bow.augment) {
            case NONE:
            default:
                augment = 1;
                break;
            case SPEED:
                augment = 0.7f;
                break;
            case DAMAGE:
                augment = 1.5f;
                break;
        }

        PinCushion pinCushion = target.buff(PinCushion.class);

        if (pinCushion != null && Dungeon.hero.hasTalent(Talent.PROJECTILES_SHARE)){
            // 부착된 투척 무기
            int pinCount = Math.round(0.334f * pinCushion.pinCount() * hero.pointsInTalent(Talent.PROJECTILES_SHARE));
            arrowCount += pinCount;
        }

        return Math.round(augment * lvl * arrowCount);
    }

    public String desc() {
        String info = Messages.get(this, "desc", dispTurns(), count);
        SpiritBow bow = Dungeon.hero.belongings.getItem(SpiritBow.class);

        if (Dungeon.hero != null && bow != null)
            info += "\n " + Messages.get(this, "info_dmg", Math.round(infoDmg()/2f), infoDmg());

        return info;
    }

    @Override
    public int icon() {
        return BuffIndicator.PINNED_ARROW;
    }

    @Override
    public float iconFadePercent() {
        return Math.max(0, (DURATION - visualcooldown()) / DURATION);
    }

    @Override
    public void fx(boolean on) {
        if (on) target.sprite.add(CharSprite.State.ARROW);
        else target.sprite.remove(CharSprite.State.ARROW);
    }
    private static final String COUNT = "count";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(COUNT, count);
    }

    @Override
    public void restoreFromBundle( Bundle bundle ) {
        super.restoreFromBundle( bundle );
        count = bundle.getInt( COUNT );
    }
}
