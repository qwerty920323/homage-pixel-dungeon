package com.shatteredpixel.shatteredpixeldungeon.items.remains;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LostInventory;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Vial;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfCleansing;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

public class ShatteredVial extends RemainsItem {

    {
        image = ItemSpriteSheet.SHATTERED_VIAL;
    }

    @Override
    protected void doEffect(Hero hero) {
        for (Buff b : hero.buffs()){
            if (b.type == Buff.buffType.NEGATIVE
                    && !(b instanceof AllyBuff)
                    && !(b instanceof LostInventory)){
                b.detach();
            }
        }
        Buff.affect(hero, PotionOfCleansing.Cleanse.class, 5f);
        Sample.INSTANCE.play( Assets.Sounds.DRINK );
    }

}
