package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Splash;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class Plague extends Buff {

    public static final float DURATION = 10f;

    {
        type = buffType.NEUTRAL; //when incubation period over type change negative
        announced = true;
    }
    private int dmg, bonusLeft;
    private float left;
    private boolean incubation = true; //whether the debuff has done any damage at all yet

    private static final String DMG 	   = "dmg";
    private static final String LEFT	   = "left";
    private static final String BONUS_LEFT = "bonus_left";
    private static final String INCUBATION = "incubation";

    @Override
    public void storeInBundle( Bundle bundle ) {
        super.storeInBundle( bundle );
        bundle.put( DMG, dmg );
        bundle.put( LEFT, left );
        bundle.put( BONUS_LEFT, bonusLeft );
        bundle.put( INCUBATION, incubation );
    }

    @Override
    public void restoreFromBundle( Bundle bundle ) {
        super.restoreFromBundle(bundle);
        dmg = bundle.getInt(DMG);
        left = bundle.getFloat(LEFT);
        bonusLeft = bundle.getInt(BONUS_LEFT);
        incubation = bundle.getBoolean(INCUBATION);
    }

    @Override
    public int icon() {
        return BuffIndicator.PLAGUE;
    }

    @Override
    public float iconFadePercent() {
        return Math.max(0, (DURATION - left) / DURATION);
    }

    @Override
    public String iconTextDisplay() {
        return Integer.toString((int)left);
    }

    @Override
    public String desc() {
        String info = Messages.get(this, "desc", dispTurns(left));

        if (isIncubation())
            info += "\n\n" + Messages.get(this, "incubation");
        else if (target == Dungeon.hero || Char.hasProp(target, Char.Property.INORGANIC))
            info += "\n\n" + Messages.get(this, "immunity");
        else
            info += "\n\n" + Messages.get(this, "acted", Math.max(1,dmg));

        return info;
    }

    @Override
    public void tintIcon(Image icon) {
        if (incubation){
            icon.hardlight(0.9f, 0.9f, 0.1f);
        } else {
            icon.hardlight(1f, 0.33f, 0.33f );
        }
    }

    @Override
    public void fx(boolean on) {
        if (on) target.sprite.add(CharSprite.State.PLAGUE);
        else target.sprite.remove(CharSprite.State.PLAGUE);
    }

    public void set(float left){
        this.left = left;
        incubation = true;
    }

    public void doAct (int dmg, Object src) {
        if (incubation) {
            type = buffType.NEGATIVE;

            if (Dungeon.hero.hasTalent(Talent.ASYMPTOMATIC_INFECT)
                    && Char.hasProp(target, Char.Property.INORGANIC)) {
                //do nothing
            } else {
                incubation = false;
            }
        }

        if (dmg > 0 && src != Plague.class) {
            //plague dmg is target.damage 15%
            this.dmg = Math.max(Math.round(0.15f * dmg), this.dmg);
        }
    }

    public boolean isIncubation () {
        return incubation;
    }

    protected float chance (Char ch) {
        float chance = 0;

        if (ch.buff(PlagueChanceTracker.class) != null) {
            float count = ch.buff(PlagueChanceTracker.class).count();

            chance = (0.04f * count);
        }

        if (Dungeon.hero.hasTalent(Talent.ASYMPTOMATIC_INFECT)
                && Char.hasProp(target, Char.Property.INORGANIC)) {
            chance += 0.05f * Dungeon.hero.pointsInTalent(Talent.ASYMPTOMATIC_INFECT);
        }

        return chance;
    }

    @Override
    public void detach() {
        if (Dungeon.level.heroFOV[target.pos]) {
            CellEmitter.get(target.pos).start(PlagueParticle.FACTORY, 0.05f, 12);
            Splash.at(target.pos, 0x4ACC4A, 5);
        }

        if (target.isAlive()) {
            //infected char resist plague while 5 turn after detach plague
            Buff.affect(target, resistPlague.class, 5f);
        }

        super.detach();
    }

        @Override
    public boolean act() {
        spend(TICK);
        //contagion
        for (int path : PathFinder.NEIGHBOURS8) {
            Char ch = Actor.findChar(target.pos + path);

            if (ch != null && ch != Dungeon.hero && ch.buff(Plague.class) == null) {
                if (ch.buff(resistPlague.class) != null)
                    continue;

                //15% chance
                if (Random.Float() <= 0.15f + chance(ch))
                    Buff.affect(ch, Plague.class).set(Plague.DURATION);

                else {
                    PlagueChanceTracker tracker = Buff.affect(ch, PlagueChanceTracker.class);
                    tracker.countUp(1);
                }
            }
        }

        if (Dungeon.hero.hasTalent(Talent.CHRONIC_DISEASE)) {
            bonusLeft++;
            int max = 10 * (4 - Dungeon.hero.pointsInTalent(Talent.CHRONIC_DISEASE));
            if (bonusLeft >= max && left < 30) {
                left++;
                bonusLeft = 0;
            }
        }

        if (target.isAlive() && !incubation) {
            if (target == Dungeon.hero || Char.hasProp(target, Char.Property.INORGANIC)) {
                //do nothing
            } else {
                target.damage(Math.max(1, dmg), this);
            }

            left -= TICK;

            if (left <= 0) {
                detach();
            }
        }

        return true;
    }

    public static class resistPlague extends FlavourBuff {}
    public static class PlagueChanceTracker extends CounterBuff{};
    public static class PlagueParticle extends PixelParticle {

        public static final Emitter.Factory FACTORY = new Emitter.Factory() {
            @Override
            public void emit( Emitter emitter, int index, float x, float y ) {
                ((PlagueParticle)emitter.recycle( PlagueParticle.class )).reset( x, y );
            }
        };

        public PlagueParticle() {
            super();
            color( 0x4ACC4A );
            speed.set( 0, Random.Float( 4,9 ) );
            lifespan = 1.2f;
        }

        public void reset( float x, float y ) {
            revive();

            this.x = x;
            this.y = y - speed.y * lifespan;

            left = lifespan;
        }

        @Override
        public void update() {
            super.update();
            float p = left / lifespan;
            am = (p < 0.5f ? p : 1 - p) * 1.5f;
        }
    }
}
