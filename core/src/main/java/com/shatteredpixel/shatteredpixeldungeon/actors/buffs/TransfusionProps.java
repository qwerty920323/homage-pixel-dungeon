package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfTransfusion;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

import java.util.HashMap;

public class TransfusionProps extends Buff {

    {
        type = buffType.POSITIVE;
    }
    protected static int props;
    protected float left;
    protected float duration;

    private static final HashMap< Integer, Char.Property> property = new HashMap<>();
    static {
        property.put(0 , Char.Property.FIERY);     // 화염
        property.put(1 , Char.Property.ICY);       // 서리
        property.put(2 , Char.Property.ELECTRIC);  // 전기
        property.put(3 , Char.Property.LARGE);     // 대형
        property.put(4 , Char.Property.DEMONIC);   // 악마
        property.put(5 , Char.Property.INORGANIC); // 무기물
    }

    public static final HashMap< Integer, String> propstring = new HashMap<>();
    static {
        propstring.put(0 , "fiery");     // 화염
        propstring.put(1 , "icy");       // 서리
        propstring.put(2 , "electric");  // 전기
        propstring.put(3 , "large");     // 대형
        propstring.put(4 , "demonic");   // 악마
        propstring.put(5 , "inorganic"); // 무기물
    }

    @Override
    public boolean act() {

        spend(TICK);

        left -= TICK;

        if (left <= 0) {
            detach();
        }

        return true;
    }

    public static void add(Buff buff, Char ch) {
        boolean dmg = false;
        if (props == 0 && buff instanceof Frost || buff instanceof Chill) { // fiery
            dmg = true;
        } else if (props == 1 && buff instanceof Burning) {                 // icy
            dmg = true;
        }

        if (dmg && ch.isAlive()) {
            ch.damage( Char.combatRoll( ch.HT/2, ch.HT * 3/5 ), buff );
        }
    }

    public void set(int props, float left){
        this.props = props;
        this.left = this.duration = left;
    }

    public static Char.Property property(){
        return property.get(props);
    }

    @Override
    public int icon() {
        return BuffIndicator.SCHOLAR_BUFF;
    }

    @Override
    public String desc() {
        String desc = Messages.get(this, "desc", Messages.get(this, propstring.get(props)), (int)left);
        switch (props) {
            case 0:
                desc += " " + Messages.get(WandOfTransfusion.class, "fiery");
                break;
            case 1:
                desc += " " + Messages.get(WandOfTransfusion.class, "icy");
                break;
            case 2:
                desc += " " + Messages.get(WandOfTransfusion.class, "electric");
                break;
            case 3:
                desc += " " + Messages.get(WandOfTransfusion.class, "large");
                break;
            case 4:
                desc += " " + Messages.get(WandOfTransfusion.class, "demonic");
                break;
            case 5:
                desc += " " + Messages.get(WandOfTransfusion.class, "inorganic");
                break;
        }
        return desc;
    }

    private static final String LEFT	 =  "left";
    private static final String DURATION =  "duration";
    private static final String PROPERTY =  "property";
    @Override
    public void storeInBundle( Bundle bundle ) {
        super.storeInBundle( bundle );
        bundle.put( LEFT, left );
        bundle.put( DURATION, duration );
        bundle.put( PROPERTY, props );
    }

    @Override
    public void restoreFromBundle( Bundle bundle ) {
        super.restoreFromBundle(bundle);
        left = bundle.getFloat( LEFT );
        duration = bundle.getFloat( DURATION );
        props = bundle.getInt( PROPERTY );
    }


}
