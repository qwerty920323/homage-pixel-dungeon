package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import static com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero;
import static com.shatteredpixel.shatteredpixeldungeon.Dungeon.level;

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
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

public class RangerArrow extends Buff{
    {
        type = Buff.buffType.POSITIVE;
        revivePersists = true;
    }

    public static int arrowCount;
    public static boolean piercingCheck; // true : pierce // false : wide
    public static int maxDistance = 8;

    @Override
    public boolean act(){
        spend(TICK);
        if (arrowCount <= 0) detach();
        return true;
    }

    public void setDuration (int arrowCount) {
        RangerArrow.arrowCount = Math.max(RangerArrow.arrowCount, arrowCount);}

    public SpiritBow bow(){
        SpiritBow bow = Dungeon.hero.belongings.getItem(SpiritBow.class);
        if (bow != null) return bow;
        return null;
    }

    public SpiritBow.SpiritArrow arrow(){
        return bow().knockArrow();
    }

    public int throwPos(Hero hero){
        if (bow().hasEnchant(Projecting.class,hero)) return Ballistica.WONT_STOP;
        else                                         return Ballistica.STOP_SOLID;
    }

    public void rangerArrow(Hero user, int cell, Char enemy, float delay){
        if (piercingCheck) pierceArrow(user, enemy, delay); // 관통
        else               wideArrow (user, cell);          // 확산

    }

    //레인저의 방사 화살
    public void wideArrow(Hero user, int cell) {
        int path = 0;

        //pathfinder.circle quick calculate
        if (user.pos - cell < 0) {
            path = 3;

            if (Math.abs(user.pos - cell) == level.width())
                path = 6;
        }

        loof:// cell 위치의 적 방향을 찾기위한 루프
        for (int num = path; num < 8; num++) {
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

    // 레인저 방사 화살 스프라이트
    public void tripleArrow(Hero user, int cell, int num)  {

        final int end = arrow().throwPos( user, cell + PathFinder.CIRCLE8[(num + 2) % 8] );

        Char ch = Actor.findChar(end);

        user.busy();

        if (ch != null && ch != user){ //양 옆 대상
            ((MissileSprite) user.sprite.parent.recycle(MissileSprite.class)).reset(user.sprite, ch.sprite,
                    arrow(),
                    new Callback() {
                        @Override
                        public void call() {
                            attackArrow(user, ch);
                            user.next();
                        }
                    });
        } else {
            ((MissileSprite) user.sprite.parent.recycle(MissileSprite.class)).reset(user.sprite, end,
                    arrow(),
                    new Callback() {
                        @Override
                        public void call() {
                            Splash.at(end, 0xCC99FFFF, 1);
                            user.next();
                        }
                    });
        }
    }
    //레인저의 관통 화살
    public void pierceArrow(Hero user, Char enemy, final float delay) {
        int cell = QuickSlotButton.autoAim(enemy, arrow());
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
                    arrow(),
                    new Callback() {
                        @Override
                        public void call() {
                            Item i = arrow();
                            if (i != null) i.toThrow(enemy.pos);

                            for (Char ch : chars) {
                                attackArrow(hero, ch);
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
                    arrow(),
                    new Callback() {
                        @Override
                        public void call() {
                            Item i = arrow();
                            if (i != null) i.toThrow(enemy.pos);

                            for (Char ch : chars) {
                                attackArrow(hero, ch);
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
    public void attackArrow(Hero hero, Char enemy){
        float dmgMulti = 0.2f;

        if (hero.hasTalent(Talent.GROWING_ARROW))
            dmgMulti += 0.0667f * hero.pointsInTalent(Talent.GROWING_ARROW);

        hero.belongings.thrownWeapon = arrow();
        boolean hit = hero.attack(enemy, dmgMulti, 0, 1);
        Invisibility.dispel();
        hero.belongings.thrownWeapon = null;

        if (hit && arrowCount > 0) arrowCount--;
    }

    public int icon() {
        if (piercingCheck) return BuffIndicator.PIERCE_ARROW;
        else               return BuffIndicator.WIDE_ARROW;
    }

    @Override
    public String desc() {
        String desc = Messages.get(this, "desc", arrowCount);

        if (bow() == null) return desc;

        if (piercingCheck) desc += " " + Messages.get(this, "piercing");
        else               desc += " " + Messages.get(this, "wide");

        return desc;
    }

    @Override
    public float iconFadePercent() {
        return Math.max(0, (8 - arrowCount) / 8);
    }

    @Override
    public String iconTextDisplay() {
        return Integer.toString((int) arrowCount);
    }

    private static final String ARROW_COUNT = "arrow_count";
    private static final String PIERCINGCHECK = "piercingcheck";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(ARROW_COUNT, arrowCount);
        bundle.put(PIERCINGCHECK, piercingCheck);
    }
    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        arrowCount = bundle.getInt(ARROW_COUNT);
        piercingCheck = bundle.getBoolean(PIERCINGCHECK);
    }
}
