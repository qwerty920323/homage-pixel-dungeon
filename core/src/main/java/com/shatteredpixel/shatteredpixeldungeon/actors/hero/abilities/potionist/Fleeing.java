package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.potionist;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blizzard;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ConfusionGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.CorrosiveGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Gas;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Inferno;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ParalyticGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Regrowth;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SmokeScreen;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.StenchGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.StormCloud;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.BlobImmunity;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.tweeners.Delayer;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

public class Fleeing extends ArmorAbility {

    {
        baseChargeUse = 45f;
    }

    @Override
    public String targetingPrompt() {
        return Messages.get(this, "prompt");
    }

    @Override
    public int targetedPos(Char user, int dst) {
        return dst;
    }

    @Override
    public float chargeUse( Hero hero ) {
        float chargeUse = super.chargeUse(hero);
        if (hero.buff(DistancingTracker.class) != null){
            //reduced charge use by 16%/30%/41%/50%
            chargeUse *= Math.pow(0.84, hero.pointsInTalent(Talent.DISTANCING));
            hero.buff(DistancingTracker.class).detach();
        }
        return chargeUse;
    }

    @Override
    public void activate(ClassArmor armor, Hero hero, Integer target ) {
        if (target == null){
            return;
        }

        Char ch = Actor.findChar(target);

        if (ch == null || !Dungeon.level.heroFOV[target]){
            GLog.w(Messages.get(this, "no_target"));
            return;
        } else if (ch.alignment != Char.Alignment.ENEMY){
            GLog.w(Messages.get(this, "ally_target"));
            return;
        }

        Buff.affect(hero, FleeingTracker.class).set(ch);

        hero.sprite.move(ch.pos, hero.pos);
        hero.sprite.parent.add(new Delayer(0.5f){
            @Override
            protected void onComplete() {
                if (((Mob)ch).isTargeting(hero) && ((Mob)ch).state == ((Mob)ch).HUNTING) {
                    Buff.prolong(ch, EscapeTracker.class, 5f);
                }

                if (hero.buff(Roots.class) != null) hero.buff(Roots.class).detach();
                if (hero.buff(Cripple.class) != null) hero.buff(Cripple.class).detach();
                if (hero.buff(Slow.class) != null) hero.buff(Slow.class).detach();
                if (hero.buff(Chill.class) != null) hero.buff(Chill.class).detach();

                armor.charge -= chargeUse( hero );
                armor.updateQuickslot();
                hero.next();
            }
        });
    }
    public static class EscapeTracker extends FlavourBuff {}
    public static class DistancingTracker extends Buff {{revivePersists = true;}}

    @Override
    public int icon() { return HeroIcon.FLEEING; }

    @Override
    public Talent[] talents() {
        return new Talent[]{Talent.MENTAL_SAFETY, Talent.GAS_SPRAY, Talent.DISTANCING, Talent.HEROIC_ENERGY};
    }

    public static class FleeingTracker extends Buff {

        public static float DURATION = 5f;
        private float left;
        public int object;
        int dist;
        Gas gas;
        public Mob mob;

        {
            announced = true;
            gas = setGas();
            mob = setMob();
            immunities.addAll(new BlobImmunity().immunities());
        }
        private static Class<?extends Gas>[] randomGases = new Class[]{
                ToxicGas.class,
                ParalyticGas.class,
                ConfusionGas.class,
                CorrosiveGas.class,
                StenchGas.class,
                SmokeScreen.class,
                StormCloud.class,
                Inferno.class,
                Blizzard.class,
                Regrowth.class
        };
        @Override
        public boolean act() {
            if (Dungeon.hero.pointsInTalent(Talent.GAS_SPRAY) > DURATION - left) {
                //spreads 36 units of gas total
                int centerVolume = 4;
                for (int i : PathFinder.NEIGHBOURS8) {
                    if (!Dungeon.level.solid[target.pos + i]) {
                        GameScene.add(Blob.seed(target.pos + i, 4, gas.getClass()));
                    } else {
                        centerVolume += 4;
                    }
                }
                GameScene.add(Blob.seed(target.pos, centerVolume, gas.getClass()));
            }

            if (Dungeon.hero.hasTalent(Talent.DISTANCING)) {
                if (mob != null) {
                    if (mob.buff(EscapeTracker.class) != null && mob.state == mob.WANDERING) {
                        Buff.affect(target, DistancingTracker.class);
                    }
                }
            }

            spend( TICK );
            left -= TICK;

            if (left <= 0)
                detach();

            return true;
        }
        @Override
        public int icon() {
            return BuffIndicator.MOMENTUM;
        }

        @Override
        public void tintIcon(Image icon) {
            icon.hardlight(0.2f, 1f, 0.2f);
        }

        @Override
        public float iconFadePercent() {
            return Math.max(0, (DURATION - left) / DURATION);
        }
        private Gas setGas () {return ((Gas) Reflection.newInstance(Random.oneOf(randomGases)));}
        private Mob setMob () {return (Mob) Actor.findById(object);}
        private void set(Char enemy) {
            object = enemy.id();
            mob = setMob();
            left = 5f;
        }

        public void setDist (int dist) {this.dist += dist;}

        @Override
        public boolean attachTo(Char target) {
            if (super.attachTo(target)){
                ((Hero)target).immersion++;

                return true;
            } else {
                return false;
            }
        }

        @Override
        public void detach() {
            if (((Hero)target).immersion > 0) {
                ((Hero)target).immersion--;
            }

            if (dist > 0) {
                int shield = dist + Math.round(dist * 2f * (Dungeon.hero.pointsInTalent(Talent.MENTAL_SAFETY)-1f) / 3f);
                target.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shield), FloatingText.SHIELDING);
                Buff.affect(target, Barrier.class).setShield(shield);
            }

            if (Dungeon.hero.hasTalent(Talent.GAS_SPRAY)) {
                float duration = Math.min (DURATION-left, Dungeon.hero.pointsInTalent(Talent.GAS_SPRAY));
                Buff.prolong(target, BlobImmunity.class, duration);
            }
            super.detach();
        }

        public void destroy () {
            if (Dungeon.hero.hasTalent(Talent.DISTANCING))
                Buff.affect(target, DistancingTracker.class);

            detach();
        }

        private static String LEFT = "left";
        private static String ID = "id";
        private static String DIST = "dist";

        @Override
        public void storeInBundle(Bundle bundle) {
            super.storeInBundle(bundle);
            bundle.put(LEFT, left);
            bundle.put(DIST, dist);
            bundle.put(ID, object);
        }

        @Override
        public void restoreFromBundle(Bundle bundle) {
            super.restoreFromBundle(bundle);
            left = bundle.getFloat(LEFT);
            dist = bundle.getInt(DIST);
            object = bundle.getInt(ID);
        }
    }
}
