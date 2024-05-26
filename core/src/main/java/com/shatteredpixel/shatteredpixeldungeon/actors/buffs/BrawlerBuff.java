package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import static com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;

public class BrawlerBuff extends Buff{

    {

        actPriority = BUFF_PRIO - 1;
        type = buffType.POSITIVE;

    }

    public int pos = -1;

    public int count;

    @Override
    public boolean act() {
        if ( count<=0 ) {
            detach();
        }
        spend(TICK);

        return true;
    }

    public int suplex (Char enemy, int damage){
        if (!target.isCharmedBy(enemy) && Dungeon.level.adjacent( target.pos, enemy.pos )){

            if (enemy.buff(EndSuplexTracker.class) == null){
                int finalpos = enemyPos((Hero) target, enemy);

                int height = finalpos == enemy.pos ? 2 : 0;

                target.sprite.zap(finalpos);

                enemy.sprite.jump(enemy.pos, finalpos, 8+height, 0.3f, new Callback() {
                    @Override
                    public void call() {
                        WandOfBlastWave.BlastWave.blast(finalpos);
                        int dmg = Math.max(hero.drRoll() - enemy.drRoll(), 0);
                    //    dmg = ((Hero)target).attackProc(enemy,dmg);

                        enemy.damage(dmg,this);
                    //    if (enemy.isAlive()) Buff.affect(enemy,EndSuplexTracker.class);

                        enemy.pos = finalpos;
                        Dungeon.level.occupyCell(enemy);
                        Dungeon.observe();
                    }
                });


            }
        }
        hero.busy();
        return damage;
    }

    public int enemyPos(Hero user, Char enemy) {
        int pos = -1;

        loof:
        for (int num = 0; num < 8; num++) {
            int pathPos = user.pos + PathFinder.CIRCLE8[num];

            Char ch = Actor.findChar(pathPos);

            if (enemy == ch) {
                pos = user.pos + PathFinder.CIRCLE8[(num + 4) % 8];
                break loof;
            }
        }

        if (pos == -1
                || (Dungeon.level.solid[pos] && Dungeon.level.map[pos] != Terrain.DOOR)
                || enemy.hasProp(enemy, Char.Property.IMMOVABLE)
                || enemy.hasProp(enemy, Char.Property.LARGE)
                || Actor.findChar(pos) != null){
            return enemy.pos;
        }

        return pos;
    }

    public void setCount(){
        count++;
    }

    public boolean getBooleanPos(){
        if (this.pos != target.pos) {
            return false;
        } else {
            return true;
        }
    }


    @Override
    public String desc() {return Messages.get(this, "desc", count);}

    @Override
    public String iconTextDisplay() {
        return Integer.toString(count);
    }

    @Override
    public int icon() {return BuffIndicator.COMBO;}

    @Override
    public void tintIcon(Image icon) {icon.hardlight(1f, 0.4f, 0.2f);}

    private static final String COUNT = "count";
    private static final String POS   = "pos";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(COUNT, count);
        bundle.put(POS, pos);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        count = bundle.getInt(COUNT);
        pos = bundle.getInt(POS);
    }


    public static class SuplexTracker extends FlavourBuff{}
    public static class EndSuplexTracker extends Buff{}
}
