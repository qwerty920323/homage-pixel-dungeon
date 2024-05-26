package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

public class ArrowMark extends FlavourBuff{

    public static final float DURATION	= 10f;

    public int count;
    //화살이 꽂힌 갯수를 세기 위한 장치

    @Override
    public void detach() {
        if (count <= 0) {
            target.remove(this);
            super.detach();
        }
        super.detach();
    }


    public String desc() {
        return Messages.get(this, "desc", dispTurns() , count);}

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
