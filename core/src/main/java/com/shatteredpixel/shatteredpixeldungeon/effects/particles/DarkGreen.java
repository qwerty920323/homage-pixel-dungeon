package com.shatteredpixel.shatteredpixeldungeon.effects.particles;

import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Gizmo;

public class DarkGreen extends Gizmo {

    private CharSprite target;

    public DarkGreen( CharSprite target ) {
        super();

        this.target = target;
    }

    @Override
    public void update() {
        super.update();

        target.rm = 0.4f;
        target.gm = 0.85f;
        target.bm = 0f;

    }

    public void lighten() {

        target.resetColor();
        killAndErase();

    }

    public static DarkGreen greenig(CharSprite sprite ) {

        DarkGreen darkGreen = new DarkGreen( sprite );
        if (sprite.parent != null) {
            sprite.parent.add(darkGreen);
        }

        return darkGreen;
    }
}
