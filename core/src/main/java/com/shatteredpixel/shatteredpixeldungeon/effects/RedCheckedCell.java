package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;

public class RedCheckedCell extends Image {

    private float alpha;
    private float delay;

    public RedCheckedCell( int pos ) {
        super( TextureCache.createSolid( 0xFFFF3A3A ) );

        origin.set( 0.5f );

        point( DungeonTilemap.tileToWorld( pos ).offset(
                DungeonTilemap.SIZE / 2,
                DungeonTilemap.SIZE / 2 ) );

        alpha = 0.8f;
    }

    public RedCheckedCell( int pos, int visSource ) {
        this( pos );
        delay = (Dungeon.level.trueDistance(pos, visSource)-1f);
        //steadily accelerates as distance increases
        if (delay > 0) {
            delay = (float)Math.pow(delay, 0.67f)/10f;
            alpha( 0 );
        }
    }

    @Override
    public void update() {
        if ((delay -= Game.elapsed) > 0){
            alpha( 0 );
        } else if ((alpha -= Game.elapsed) > 0) {
            alpha( alpha );
            scale.set( DungeonTilemap.SIZE * alpha );
        } else {
            killAndErase();
        }
    }
}
