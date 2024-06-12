package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import static com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Splash;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Projecting;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class RangerArrow extends Buff{
    {
        type = Buff.buffType.POSITIVE;
        revivePersists = true;
    }

    public static int duration; //화살 갯수
    public static boolean piercingCheck; // true : 관통 // false : 확산
    public static int maxDistance = 8;

    @Override
    public boolean act(){
        spend(TICK);
        if (duration <= 0) detach();
        return true;
    }

    public static SpiritBow bow(){
        SpiritBow bow = Dungeon.hero.belongings.getItem(SpiritBow.class);
        if (bow != null) return bow;
        return null;
    }

    public static int throwPos(Hero hero){
        if (bow().hasEnchant(Projecting.class,hero)) return Ballistica.WONT_STOP;
        else                                         return Ballistica.STOP_SOLID;
    }

    public static void rangerArrow(Hero user, int cell, Char enemy, float delay){
        if (piercingCheck) pierceArrow(user, enemy, delay); // 관통
        else               wideArrow (user, cell);          // 확산

    }

    //순찰자의 방사 화살
    public static void wideArrow(Hero user, int cell) {
        loof:// cell 위치의 적 방향을 찾기위한 루프
        for (int num = 0; num < 8; num++) {
            int pathPos = user.pos + PathFinder.CIRCLE8[num];

            ConeAOE cone;

            cone = new ConeAOE(new Ballistica(user.pos, pathPos, Ballistica.WONT_STOP), maxDistance, 40, throwPos(user));

            if (Dungeon.level.distance(user.pos, cell) > 1) {
                if (cone.cells.contains(cell)) {
                    tripleArrow(user, cell, num);           //첫번째 대상
                    tripleArrow(user, cell, num + 4);  //맞은편 대상
                    break loof;
                }
            } else {
                if (pathPos == cell) {
                    tripleArrow(user, cell, num);           //첫번째 대상
                    tripleArrow(user, cell, num + 4);  //맞은편 대상
                    break loof;
                }
            }
        }
    }

    // 순찰자 방사 화살 스프라이트
    public static void tripleArrow(Hero user, int cell, int num)  {

        final int end = bow().throwPos( user, cell + PathFinder.CIRCLE8[(num + 2) % 8] );

        Char ch = Actor.findChar(end);

        if (ch != null && ch != user){ //양 옆 대상
            ((MissileSprite) user.sprite.parent.recycle(MissileSprite.class)).reset(user.sprite, ch.sprite,
                    bow().knockArrow(),
                    new Callback() {
                        @Override
                        public void call() {
                            attackArrow(hero,ch,0.2f); //피해는 한번만
                            user.next();
                        }
                    });
        } else {
            ((MissileSprite) user.sprite.parent.recycle(MissileSprite.class)).reset(user.sprite, end,
                    bow().knockArrow(),
                    new Callback() {
                        @Override
                        public void call() {
                            Splash.at(end, 0xCC99FFFF, 1);
                            user.next();
                        }
                    });
        }
    }
    //순찰자의 관통 화살
    public static void pierceArrow(Hero user, Char enemy, final float delay) {
        int cell = QuickSlotButton.autoAim(enemy, bow().knockArrow());
        if (cell == -1) cell = enemy.pos;

        final Ballistica b = new Ballistica(user.pos, cell, throwPos(user));

        ArrayList<Char> chars = new ArrayList<>();

        int target = Math.min(b.dist , maxDistance);

        for (int c : b.subPath(1, target)) {    // 적 찾기

            Char ch;
            if ((ch = Actor.findChar(c)) != null) {

                if (ch instanceof Mob && ((Mob) ch).state == ((Mob) ch).PASSIVE
                        && !(Dungeon.level.mapped[c] || Dungeon.level.visited[c])) {
                    //avoid harming undiscovered passive chars
                } else {
                    if (ch != enemy && chars.size() <= 2) chars.add(ch); //첫 대상 제외
                }
            }
        }

        Char lastEnemy = null;

        if (!chars.isEmpty()) {
            lastEnemy = chars.get(chars.size() - 1);
        }

        int lastCell = b.path.get(target);

        if (lastEnemy != null && chars.size() >= 2) { //적 최대 갯수
            ((MissileSprite) user.sprite.parent.recycle(MissileSprite.class)).reset(user.sprite, lastEnemy.sprite,
                    bow().knockArrow(),
                    new Callback() {
                        @Override
                        public void call() {
                            attackArrow(hero, enemy,1f);

                            for (Char ch : chars) {
                                attackArrow(hero, ch,0.2f);
                            }

                            if (user.buff(Talent.LethalMomentumTracker.class) != null){
                                user.buff(Talent.LethalMomentumTracker.class).detach();
                                user.next();
                            } else {
                                user.spendAndNext(delay);
                            }

                        }
                    });
        } else {
            ((MissileSprite) user.sprite.parent.recycle(MissileSprite.class)).reset(user.sprite, lastCell,
                    bow().knockArrow(),
                    new Callback() {
                        @Override
                        public void call() {
                            hero.shoot(enemy,bow().knockArrow());

                            for (Char ch : chars) {
                                attackArrow(hero, ch, 0.2f);
                            }

                            if (user.buff(Talent.LethalMomentumTracker.class) != null){
                                user.buff(Talent.LethalMomentumTracker.class).detach();
                                user.next();
                            } else {
                                user.spendAndNext(delay);
                            }
                            Splash.at(lastCell, 0xCC99FFFF, 1);
                        }
                    });
        }
    }

    // 최종 피해 계산식
    public static void attackArrow(Hero hero, Char enemy, float dmgMulti){
        if (Random.Int(10) < 2 * hero.pointsInTalent(Talent.GROWING_ARROW)
                && hero.hasTalent(Talent.GROWING_ARROW)){
            dmgMulti = bow().proc( hero, enemy, Math.round(dmgMulti));
        }

        hero.belongings.thrownWeapon = bow().knockArrow();
        boolean hit = hero.attack(enemy, dmgMulti, 0, 1);
        Invisibility.dispel();
        hero.belongings.thrownWeapon = null;

        if (hit && duration > 0) duration--;
    }

    public int icon() {
        if (piercingCheck) return BuffIndicator.PIERCE_ARROW;
        else               return BuffIndicator.WIDE_ARROW;
    }

    @Override
    public String desc() {
        String desc = Messages.get(this, "desc", duration);

        if (bow() == null) return desc;

        if (piercingCheck) desc += " " + Messages.get(this, "piercing");
        else               desc += " " + Messages.get(this, "wide");

        return desc;
    }

    private static final String DURATION = "duration";
    private static final String PIERCINGCHECK = "piercingcheck";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(DURATION, duration);
        bundle.put(PIERCINGCHECK, piercingCheck);
    }
    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        duration = bundle.getInt(DURATION);
        piercingCheck = bundle.getBoolean(PIERCINGCHECK);
    }
}
