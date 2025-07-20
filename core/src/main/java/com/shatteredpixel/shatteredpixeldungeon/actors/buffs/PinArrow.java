package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import static com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SacrificialParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfSharpshooting;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

public class PinArrow extends FlavourBuff{

    public static final float DURATION	= 10f;

    public int count;

    public void doAttack(float dmgPercent){
        Sample.INSTANCE.play(Assets.Sounds.BLAST);
        Sample.INSTANCE.play(Assets.Sounds.BURNING);
        CellEmitter.get(target.pos).burst(SacrificialParticle.FACTORY, 20);

        int dmg = damage(false);
        target.damage(Math.round(dmgPercent * Hero.heroDamageIntRange( Math.round(dmg/2f) , dmg)) , this);

        if (!target.isAlive()) {
            SpiritBow.PiercingArrow pierce = hero.buff(SpiritBow.PiercingArrow.class);
            SpiritBow.WideArrow wide = hero.buff(SpiritBow.WideArrow.class);

            if (wide != null) wide.countUp(10f - wide.count());
            else if (pierce != null) pierce.countUp(10f - pierce.count());
            else Buff.affect(hero, SpiritBow.WideArrow.class).countUp(10f);

            Sample.INSTANCE.play( Assets.Sounds.CHARGEUP );
        } else {
            detach();
        }
    }

    public int damage (boolean desc){
        int result = 0;
        int arrowCounter = count;

        PinCushion pinCushion = target.buff(PinCushion.class);
        if (pinCushion != null && hero.hasTalent(Talent.PROJECTILES_SHARE)){
            int pinCount = Math.round(0.334f * pinCushion.pinCount() * hero.pointsInTalent(Talent.PROJECTILES_SHARE));
            arrowCounter += pinCount;
        }

        int damage = 1;
        SpiritBow bow = Dungeon.hero.belongings.getItem(SpiritBow.class);
        if (bow != null) {
            //RingOfSharpshooting bonus is after damage calculation
            damage = Math.round(bow.augment.damageFactor(bow.min() - RingOfSharpshooting.levelDamageBonus(Dungeon.hero)));

            //when attack, buff affect hero
            if (!desc && hero.hasTalent(Talent.ENCHANT_BLAST)) {
                Talent.EnchantBlastTracker tracker = Buff.prolong(hero, Talent.EnchantBlastTracker.class, 0f);
                tracker.count = arrowCounter;
                result = bow.proc(hero, target, result);
            }
        }

        result += damage * arrowCounter;

        Invisibility.dispel(Dungeon.hero);
        //RingOfSharpshooting bonus is here
        return result + RingOfSharpshooting.levelDamageBonus(Dungeon.hero);
    }

    public String desc() {
        String info = Messages.get(this, "desc", dispTurns());
        info += "\n" + Messages.get(this, "info_dmg", Math.round(damage(true)/2f), damage(true));
        return info;
    }

    @Override
    public void detach() {
        super.detach();

        boolean checkPind = false;
        for (Mob mob : Dungeon.level.mobs.toArray( new Mob[0] )) {
            if (mob.buff(PinArrow.class) != null) {
                checkPind = true;
                break;
            }
        }

        if (!checkPind && Dungeon.hero.buff(ArrowBlast.class) != null)
            Dungeon.hero.buff(ArrowBlast.class).detach();
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
