package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import static com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SacrificialParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class ArrowBlast extends FlavourBuff implements ActionIndicator.Action {

    public static final float DURATION = 10f;

    {
        type = buffType.POSITIVE;
        actPriority = VFX_PRIO-1;
    }


    public static SpiritBow bow(){
        SpiritBow bow = Dungeon.hero.belongings.getItem(SpiritBow.class);
        if (bow != null) return bow;
        else return null;
    }

    @Override
    public boolean attachTo(Char target) {
        ActionIndicator.setAction(this);
        return super.attachTo(target);
    }

    @Override
    public void detach() {
        super.detach();
        ActionIndicator.clearAction(this);
    }

    @Override
    public int actionIcon() {
        return HeroIcon.ARROW_BLAST;
    }

    @Override
    public int indicatorColor() {
        return 0x75FFED;
    }

    @Override
    public float iconFadePercent() {
        return Math.max(0, (DURATION - visualcooldown()) / DURATION);
    }

    @Override
    public String desc() {
        return Messages.get(this, "desc");
    }

    @Override
    public String actionName() {
        SpiritBow bow = Dungeon.hero.belongings.getItem(SpiritBow.class);

        if (bow == null) return null;

        return null;
    }

    public float damage (Char mob, float bonus){
        float endDamage=0;

        PinCushion pinCushion = mob.buff(PinCushion.class);

        int arrowCounter = 0;  // 부착된 마법 화살

        if (mob.buff(ArrowMark.class) != null) {

            if (pinCushion != null && hero.hasTalent(Talent.PROJECTILES_SHARE)){
                int pinCount = pinCushion.pinCount();  // 부착된 투척 무기

                if (pinCount >= hero.pointsInTalent(Talent.PROJECTILES_SHARE)){
                    pinCount = hero.pointsInTalent(Talent.PROJECTILES_SHARE);
                }
                arrowCounter += pinCount;  // 투척 무기 개수 추가
            }

            arrowCounter += mob.buff(ArrowMark.class).count;  // 화살 개수 체크

            for (int i = arrowCounter; i >0; i--){
                // (활레벨*증강)/2 ~ (활레벨 *증강)
                endDamage += bonus * (Char.combatRoll( Math.round(bow().level()/2f) , Math.round(bow().level())));
                arrowCounter--;
            }
        }

        Invisibility.dispel(Dungeon.hero);
        return endDamage;
    }


    public void doAttack (float weaponlvl , Char mob){

        if (mob.buff(ArrowMark.class) != null) {

            float bonus;

            Sample.INSTANCE.play(Assets.Sounds.BLAST);
            CellEmitter.get(mob.pos).burst(SacrificialParticle.FACTORY, 20);
            Sample.INSTANCE.play(Assets.Sounds.BURNING);

            // 증강상태 * (0~활 레벨 * 화살 개수) // * 무기 레벨
            switch (bow().augment) {
                case NONE:
                default:
                    bonus = 1;
                    break;
                case SPEED:
                    bonus = 0.7f;
                    break;
                case DAMAGE:
                    bonus = 1.5f;
                    break;
            }

            // (활레벨*증강)/2 ~ (활레벨+1 *증강) * 화살 개수 // * 무기 레벨
            mob.damage((int) Math.ceil(weaponlvl * damage(mob,bonus)) , this);

            if (!mob.isAlive()) {
                //순찰자 화살 버프
                Buff.affect(hero, RangerArrow.class).duration = 6 + bow().level();
                Sample.INSTANCE.play( Assets.Sounds.CHARGEUP );
            }
            else if(mob.buff(ArrowMark.class) != null) {
                mob.buff(ArrowMark.class).detach();
            }
        }
    }

    @Override
    public void doAction() {
        Hero hero = Dungeon.hero;
        if (hero == null) return;

        Dungeon.hero.busy();
        hero.sprite.operate(hero.pos);
        ArrayList<Char> mobs = new ArrayList<>();

        for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
            if (mob.alignment != Char.Alignment.ALLY
                    && mob.buff(ArrowMark.class) != null
                    && Dungeon.level.heroFOV[mob.pos]) {

                mobs.add(mob);
                doAttack(1, mob);

            }
        }

        if (mobs.isEmpty()) GLog.w( Messages.get(this, "no_enemy"));

        ActionIndicator.clearAction(this);
        hero.spend(TICK);
        hero.next();
        detach();
    }
}
