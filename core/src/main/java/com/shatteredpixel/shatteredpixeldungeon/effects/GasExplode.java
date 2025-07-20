package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blizzard;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ConfusionGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.CorrosiveGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Inferno;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ParalyticGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SmokeScreen;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.StenchGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.StormCloud;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GasRelease;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BlastParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SmokeParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.HashSet;

public class GasExplode extends Actor {
    {
        actPriority = VFX_PRIO;
    }

    int pos;
    Char ch;

    public GasExplode (int pos, Char ch) {
        this.pos = pos;
        this.ch = ch;
    }
    @Override
    protected boolean act() {
        gasExplode(pos, ch);
        Actor.remove( GasExplode.this );
        return true;
    }

    ArrayList<Class> affectedBlobs = new ArrayList<>();

    public void searchGas (int cell, HashSet<Integer> set) {
        for (int p : PathFinder.NEIGHBOURS4) {
            int path = cell + p;
            int amount = 0;

            if (set.contains(path)) continue;

            for (Class c : affectedBlobs) {
                Blob b = Dungeon.level.blobs.get(c);

                if (b != null && b.volume > 0 && b.cur[path] > 0) {
                    amount += clearBlobs(b, path);
                }
            }

            if (amount > 0) {
                damage(path, amount);

                set.add(path);
                searchGas(path, set);
            }
        }
    }

    public boolean explode(HashSet <Integer> set) {
        if (set.isEmpty()) return false;

        boolean terrainAffected = false;
        for (int pos : set) {
            if (Dungeon.level.heroFOV[pos]) {
                CellEmitter.center(pos).burst(BlastParticle.FACTORY, 30);
                CellEmitter.get(pos).burst(SmokeParticle.FACTORY, 4);
            }

            if (Dungeon.level.flamable[pos]) {
                Dungeon.level.destroy(pos);
                GameScene.updateMap(pos);
                terrainAffected = true;
            }
        }

        if (terrainAffected) {
            Dungeon.observe();
        }

        Sample.INSTANCE.play( Assets.Sounds.BLAST );
        set.clear();
        return true;
    }

    public ArrayList<Class> effectGas (Boolean isHero) {
        ArrayList<Class> result = new ArrayList<>();
        result.add(GasRelease.HeatGas.class);

        if (Dungeon.hero == null || !isHero) return result;

        int point = Dungeon.hero.pointsInTalent(Talent.VARIOUS_CATALYSTS);
        if (point >= 1) {
            result.add(ToxicGas.class);
            result.add(ConfusionGas.class);
            result.add(StenchGas.class);
        }

        if (point >= 2) {
            result.add(CorrosiveGas.class);
            result.add(ParalyticGas.class);
            result.add(StormCloud.class);
        }

        if (point >= 3) {
            result.add(SmokeScreen.class);
            result.add(Inferno.class);
            result.add(Blizzard.class);
        }

        return result;
    }

    public void damage (int pos, int amount) {
        Char ch = Actor.findChar(pos);
        if (ch != null
                && (ch.alignment == Char.Alignment.ENEMY
                || (ch instanceof Mimic && ch.alignment == Char.Alignment.NEUTRAL))) {

            if (!ch.isAlive()){
                return;
            }

            int dmg = Random.NormalIntRange(2+Dungeon.scalingDepth(), Dungeon.scalingDepth() + amount);

            dmg = Math.min(dmg, 12 + 3*Dungeon.scalingDepth());
            dmg -= ch.drRoll();

            if (dmg > 0)
                ch.damage(dmg, new Bomb.ConjuredBomb());
        }

    }
    public int clearBlobs (Blob b, int cell) {
        int amount = b.cur[cell];
        if (Dungeon.hero.hasTalent(Talent.HIGH_EFFICIENCY)) {
            int remain = Dungeon.hero.pointsInTalent(Talent.HIGH_EFFICIENCY) * b.cur[cell] / 9;
            b.cur[cell] = remain;
            return amount - remain;
        } else {
            b.clear(cell);
        }
        return amount;
    }

    public void gasExplode(int pos, Char attacker) {
        int amount = 0;
        boolean hero = attacker != null && attacker == Dungeon.hero;
        HashSet<Integer> cells = new HashSet<>();

        affectedBlobs = effectGas(hero);
        for (Class c : affectedBlobs) {
            Blob b = Dungeon.level.blobs.get(c);

            if (b != null && b.volume > 0 && b.cur[pos] > 0) {
                amount += clearBlobs(b, pos);
            }
        }

        if (amount > 0) {
            cells.add(pos);
            searchGas(pos, cells);

            damage(pos, amount);
            explode(cells);
        }
    }
}
