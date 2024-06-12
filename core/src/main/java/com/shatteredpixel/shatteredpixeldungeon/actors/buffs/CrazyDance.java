package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import static com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.Effects;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TimekeepersHourglass;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfForce;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.Visual;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.BArray;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class CrazyDance extends Buff{

    {

        actPriority = BUFF_PRIO - 1;
        type = buffType.POSITIVE;

    }
    private static final float STEP = 1f;
    private float left;
    private float delay;
    private float evasion;


    @Override
    public boolean attachTo( Char target ) {
        if (super.attachTo( target )) {
            target.paralysed++;

            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean act(){
        if (target.buff(Frost.class) != null
                || target.buff(Paralysis.class) != null
                || target.buff(MagicalSleep.class) != null
                || target.buff(TimekeepersHourglass.timeStasis.class) != null)
            detach();

        spend( STEP );
        left -= STEP;

        if (left <= 0) {
            detach();
        }

        return true;
    }


    public void crazyAttack() {
        ArrayList<Char> targets = new ArrayList<>();

        for (Char ch : Actor.chars()) {
            if (ch.isAlive()
                    && !hero.isCharmedBy(ch)
                    && Dungeon.level.heroFOV[ch.pos]
                    && hero.canAttack(ch)) {
                targets.add(ch);
            }
        }

        int random = Random.index(targets);

        if (targets.isEmpty()) {
             if (!findEnemy()) hero.spendAndNext( TICK );

        } else {
            Char ch = targets.get(random);

            hero.sprite.attack(ch.pos, new Callback() {
                @Override
                public void call() {
                    if (target.attack(ch, 1, 0, 1)) {
                        Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
                   //     Blade.hit(ch);
                    }
                    Invisibility.dispel();
                    hero.spendAndNext(hero.attackDelay());
                }
            });

            targets.clear();

        }
    }

    public boolean findEnemy(){
        final int dist = delay > 4 ? hero.pointsInTalent(Talent.QUICKSTEP) : hero.pointsInTalent(Talent.QUICKSTEP)-1;

        PathFinder.buildDistanceMap(hero.pos,BArray.or(Dungeon.level.passable, Dungeon.level.avoid, null), dist);

        Char closest = null;

        if (dist <= 0) return false;

        for (Char ch : Actor.chars()){
            if (ch.alignment == Char.Alignment.ENEMY
                    && Dungeon.level.heroFOV[ch.pos]
                    && !Dungeon.level.adjacent(target.pos,ch.pos)
                    && PathFinder.distance[target.pos] < Integer.MAX_VALUE){

                if (closest == null || Dungeon.level.trueDistance(hero.pos, closest.pos) > Dungeon.level.trueDistance(hero.pos, ch.pos)){
                    closest = ch;
                }
            }
        }

        if (closest != null && hero.hasTalent(Talent.QUICKSTEP)) {

            int cell = closest.pos;

            int dest = -1;
            for (int i : PathFinder.NEIGHBOURS8) {

                if (Actor.findChar(cell + i) != null) continue;

                if (!Dungeon.level.passable[cell + i] && !(target.flying && Dungeon.level.avoid[cell + i])) {
                    continue;
                }

                if (dest == -1 || PathFinder.distance[dest] > PathFinder.distance[cell + i]) {
                    dest = cell + i;

                } else if (PathFinder.distance[dest] == PathFinder.distance[cell + i]) {
                    if (Dungeon.level.trueDistance(Dungeon.hero.pos, dest) > Dungeon.level.trueDistance(Dungeon.hero.pos, cell + i)) {
                        dest = cell + i;
                    }
                }

            }

            if (dest == -1 || PathFinder.distance[dest] == Integer.MAX_VALUE || Dungeon.hero.rooted) {
                if (Dungeon.hero.rooted) PixelScene.shake( 1, 1f );
                return false;
            }

            int finalDest = dest;

            hero.sprite.emitter().start(Speck.factory(Speck.JET), 0.01f, Math.round(2 + 2*Dungeon.level.trueDistance(hero.pos, dest)));
            Sample.INSTANCE.play(Assets.Sounds.MISS);

            hero.sprite.jump(hero.pos, dest, 0, 0.15f, new Callback() {

                @Override
                public void call() {
                    hero.pos = finalDest;
                    Dungeon.level.occupyCell(hero);
                    Dungeon.observe();

                    hero.spendAndNext(1/hero.speed());
                }
            });

            return true;
        }

        return false;
    }

    public void processDamage(int dmg){
        if (target == null) return;

        if (dmg > 0) this.left--;
    }

    public void set( float duration, float delay, float evasion) {
        left = duration;

        this.delay = delay;
        this.evasion = evasion;
    }

    public float delay(){
        return delay/100f;
    }

    public float evasion(){
        return evasion/10;
    }

    @Override
    public void detach() {
        if (target.paralysed > 0) {
            target.paralysed--;
        }

        BladeDance Bd = target.buff(BladeDance.class);

        if (Bd != null){
            if (hero.hasTalent(Talent.GROWING_STAMINA)
                    && hero.belongings.weapon() instanceof MeleeWeapon
                    && hero.buff(RingOfForce.BrawlersStance.class) == null){

                int tier = ((MeleeWeapon) hero.belongings.weapon()).tier
                    + 3 - hero.pointsInTalent(Talent.GROWING_STAMINA);

                for (int i=0; i< tier; i++) {
                    if (Bd != null) {
                        Bd.remove(0, false);
                    }
                }
                Bd.announce();

            } else {
                Bd.detach();
            }
        }

        super.detach();
    }

    private static final String LEFT	= "left";
    private static final String DELAY	= "delay";
    private static final String EVASION = "evasion";

    @Override
    public void storeInBundle( Bundle bundle ) {
        super.storeInBundle( bundle );
        bundle.put( LEFT, left );
        bundle.put( DELAY, delay );
        bundle.put( EVASION, evasion );
    }

    @Override
    public void restoreFromBundle( Bundle bundle ) {
        super.restoreFromBundle(bundle);
        left = bundle.getFloat( LEFT );
        delay = bundle.getFloat( DELAY );
        evasion = bundle.getFloat( EVASION );
    }

    @Override
    public String name() {
        String name = Messages.get(BladeDance.class, "name");
        name += " : " + Messages.get(Amok.class, "name");
        return name;
    }
    @Override
    public String desc() {
        return Messages.get(BladeDance.class, "dance", delay, evasion());
    }

    @Override
    public int icon() {return BuffIndicator.AMOK;}

    @Override
    public float iconFadePercent() {return Math.max(0, (10 - left) / 10);}

    @Override
    public String iconTextDisplay() {return Integer.toString((int) left);}


    public static class Blade extends Image {

        private static final float TIME_TO_FADE = 1f;

        private float time;

        public Blade() {
           // super( Effects.get( Effects.Type.ILLUSION_BLADE ) );
            hardlight(1f, 1f, 1f);
            origin.set( width / 2, height / 2 );
        }

        public void reset( int p ) {
            revive();

            x = (p % Dungeon.level.width()) * DungeonTilemap.SIZE + (DungeonTilemap.SIZE - width) / 2;
            y = (p / Dungeon.level.width()) * DungeonTilemap.SIZE + (DungeonTilemap.SIZE - height) / 2;

            time = TIME_TO_FADE;
        }

        public void reset(Visual v) {
            revive();

            point(v.center(this));

            time = TIME_TO_FADE;
        }

        @Override
        public void update() {
            super.update();

            if ((time -= Game.elapsed) <= 0) {
                kill();
            } else {
                float p = time / TIME_TO_FADE;
                alpha((float) Math.sqrt(p));
                scale.y = 1 + p;
            }
        }

        public static void hit( Char ch ) {
            hit( ch, 0 );
        }

        public static void hit( Char ch, float angle ) {
            if (ch.sprite.parent != null) {
                Blade w = (Blade) ch.sprite.parent.recycle(Blade.class);
                ch.sprite.parent.bringToFront(w);
                w.reset(ch.sprite);
                w.angle = angle;
            }
        }

        //nothing
        public static void hit( int pos ) {
            hit( pos, 0 );
        }

        public static void hit( int pos, float angle ) {
            Group parent = Dungeon.hero.sprite.parent;
            Blade w = (Blade)parent.recycle( Blade.class );
            parent.bringToFront( w );
            w.reset( pos );
            w.angle = angle;
        }
    }
}
