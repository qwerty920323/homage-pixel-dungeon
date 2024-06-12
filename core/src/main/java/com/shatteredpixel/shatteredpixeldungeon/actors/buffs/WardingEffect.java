package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.ElementalBlast;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC;
import com.shatteredpixel.shatteredpixeldungeon.effects.Beam;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfCorrosion;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfCorruption;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfDisintegration;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFireblast;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFrost;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLightning;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfPrismaticLight;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfRegrowth;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfTransfusion;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfWarding;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.HashMap;

public class WardingEffect extends Buff {

    {
        revivePersists = true;
    }

    public Class lastWand = null;
    private static final HashMap< Class<? extends Wand>, Integer> effectBonus = new HashMap<>();
    static {
        effectBonus.put(WandOfFireblast.class,      0);  // 화염
        effectBonus.put(WandOfFrost.class,          1);  // 서리
        effectBonus.put(WandOfLightning.class,      2);  // 번개
        effectBonus.put(WandOfRegrowth.class,       3);  // 재성장
        effectBonus.put(WandOfLivingEarth.class,    4);  // 대지
        effectBonus.put(WandOfPrismaticLight.class, 5);  // 굴절광
        effectBonus.put(WandOfBlastWave.class,      6);  // 충격파
        effectBonus.put(WandOfCorrosion.class,      7);  // 부식
        effectBonus.put(WandOfCorruption.class,     8);  // 타락
        effectBonus.put(WandOfDisintegration.class, 9);  // 붕괴
        effectBonus.put(WandOfTransfusion.class,   10);  // 이식
        effectBonus.put(WandOfWarding.class,       11);  // 수호
        effectBonus.put(WandOfMagicMissile.class,  12);  // 마탄
    }

    public void setEffectBonus (NPC npc, Char ch, int dmg){
        int e = effectBonus.get(lastWand);

        switch (e) {
            case 0: // WandOfFireblast
                Buff.affect(ch, Burning.class).reignite(ch);
                break;
            case 1: // WandOfFrost
                if (ch.buff(Frost.class) != null){
                    return;
                }
                if (Dungeon.level.water[ch.pos])
                    Buff.affect(ch, Chill.class, 4);
                else
                    Buff.affect(ch, Chill.class, 2);
                break;
            case 2: // WandOfLightning
                Buff.affect( ch, Paralysis.class, Paralysis.DURATION/5 );
                break;
            case 3: // WandOfRegrowth
                Buff.prolong( ch, Roots.class, 2f );
                break;
            case 4: // WandOfLivingEarth
                for (Mob m : Dungeon.level.mobs){
                    if (m instanceof WandOfLivingEarth.EarthGuardian){
                        ((WandOfLivingEarth.EarthGuardian) m).setInfo(Dungeon.hero, 0, 5);
                        break;
                    }
                }

                WandOfLivingEarth.RockArmor buff = Dungeon.hero.buff(WandOfLivingEarth.RockArmor.class);
                if (buff == null) {
                    Buff.affect(Dungeon.hero, WandOfLivingEarth.RockArmor.class);
                }
                break;
            case 5: // WandOfPrismaticLight
                Buff.prolong(ch, Blindness.class, 2f);
                break;
            case 6: // WandOfBlastWave
                Ballistica aim = new Ballistica(npc.pos, ch.pos, Ballistica.WONT_STOP);
                WandOfBlastWave.throwChar(ch,
                        new Ballistica(ch.pos, aim.collisionPos, Ballistica.MAGIC_BOLT),
                        2,
                        true,
                        true,
                        WandOfWarding.class);
                break;
            case 7: // WandOfCorrosion
                if (!ch.isImmune(Corrosion.class))
                    Buff.affect(ch, Corrosion.class).set(2f, 2, WandOfWarding.class);
                break;
            case 8: // WandOfCorruption
                Buff.prolong(ch, Amok.class, 2f);
                break;
            case 9: // WandOfDisintegration
                Ballistica beam = new Ballistica(npc.pos, ch.pos, Ballistica.STOP_SOLID);
                ArrayList<Char> chars = new ArrayList<>();
                npc.sprite.parent.add(new Beam.DeathRay(npc.sprite.center(), DungeonTilemap.raisedTileCenterToWorld( ch.pos )));

                for (int c : beam.path) {
                    Char mob = Actor.findChar( c );
                    if (mob != null && mob != ch) {
                        chars.add(mob);
                    }
                }
                if (!chars.isEmpty()) {
                    for (Char enemy : chars)
                        enemy.damage( dmg/2, this );
                }
                break;
            case 10: // WandOfTransfusion
                Charm charm = Buff.affect(ch, Charm.class, Charm.DURATION/5f);
                charm.object = npc.id();
                charm.ignoreHeroAllies = true;
                break;
            case 11: // WandOfWarding

                break;
            case 12: // WandOfMagicMissile

                break;
        }
    }


    private static final String LASTWAND = "lastwand";
    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(LASTWAND, lastWand);
    }
    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        lastWand = bundle.getClass(LASTWAND);
    }

    @Override
    public boolean act() {

        spend(TICK);
        return true;
    }

}
