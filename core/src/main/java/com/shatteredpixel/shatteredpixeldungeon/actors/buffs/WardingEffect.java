package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.Bundle;

public class WardingEffect extends Buff {

    {
        revivePersists = true;
    }

    public int left, maxLeft, atkCount;

    public static String setBonusEffect(NPC npc, Char ch, int wandlvl){
        String result = "cripple";

        int left = 2;
        boolean fov = Dungeon.level.heroFOV[npc.pos];
        boolean nonChar = ch == null;

        switch (Dungeon.level.map[npc.pos]) {
            default:                    // Cripple
                if (nonChar){
                    result = "cripple";
                    break;
                }
                Buff.affect(ch, Cripple.class, left);
                break;

            case Terrain.EMBERS:        // Burning
                if (nonChar){
                    result = "burning";
                    break;
                }
                Buff.affect(ch, Burning.class).reignite(ch,left);
                break;

            case Terrain.ICE:
            case Terrain.WATER:         // Chill
                if (nonChar){
                    result = "chill";
                    break;
                }
                if (Dungeon.level.water[ch.pos])
                    Buff.affect(ch, Chill.class, left + 1);
                else
                    Buff.affect(ch, Chill.class, left);

                break;

            case Terrain.INACTIVE_TRAP: // Paralysis
                if (nonChar){
                    result = "paralysis";
                    break;
                }

                Buff.affect( ch, Paralysis.class, left );

                if (fov) {
                    ch.sprite.centerEmitter().burst(SparkParticle.FACTORY, 3);
                    ch.sprite.flash();
                }
                break;

            case Terrain.GRASS:         // Roots
            case Terrain.HIGH_GRASS:
            case Terrain.FURROWED_GRASS:
                if (nonChar){
                    result = "roots";
                    break;
                }
                Buff.prolong(ch, Roots.class, left);

                if (fov)
                    CellEmitter.get(ch.pos).burst(LeafParticle.GENERAL, 8);
                break;

            case Terrain.PEDESTAL:
                //power up?
                if (nonChar) {
                    result = "power";
                    break;
                }

                break;
        }
        return result;
    }

    public void setWarding(int left, int count){
        this.left = maxLeft = left;  // 턴마다 줄어드는 left, 초기화 할때 필요한 maxLeft
        this.atkCount = count;       // 공격마다 줄어드는 atkCount
    }

    private static final String LEFT = "left";
    private static final String MAX_LEFT = "max_left";
    private static final String ATK_COUNT = "atk_count";
    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(LEFT, left);
        bundle.put(MAX_LEFT, maxLeft);
        bundle.put(ATK_COUNT, atkCount);
    }
    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        left = bundle.getInt(LEFT);
        maxLeft = bundle.getInt(MAX_LEFT);
        atkCount = bundle.getInt(ATK_COUNT);
    }

    public void afterAttack () {
        atkCount--;
        if (atkCount <= 0)
            detach();
    }

    @Override
    public boolean act() {
        if (left > 0)
            left--;

        if (left <= 0) {
            left = maxLeft;
            afterAttack();
        }

        spend(TICK);
        return true;
    }

}
