package com.shatteredpixel.shatteredpixeldungeon.levels.features;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Splash;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SnowParticle;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;

public class Ice {
    public static void trample (Level level, int pos ) {
        if (Dungeon.level.heroFOV[pos]) {
            CellEmitter.get( pos ).start( SnowParticle.FACTORY, 0.2f, 6 );
        }

        if (ShatteredPixelDungeon.scene() instanceof GameScene) {
            GameScene.updateMap(pos);

            Splash.at(pos, 0xCCBFFFFE, 2);
            if (Dungeon.level.heroFOV[pos]) Dungeon.observe();
        }
    }
}
