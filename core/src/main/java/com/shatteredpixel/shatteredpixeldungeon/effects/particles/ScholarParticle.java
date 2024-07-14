package com.shatteredpixel.shatteredpixeldungeon.effects.particles;

import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Random;

public class ScholarParticle extends PixelParticle.Shrinking {

    public static final Emitter.Factory YELLOW = new Emitter.Factory() {
        @Override
        public void emit( Emitter emitter, int index, float x, float y ) {
            ((ScholarParticle)emitter.recycle( ScholarParticle.class )).reset( x, y , 0xFFAA30);
        }
        @Override
        public boolean lightMode() {
            return false;
        }
    };

    public static final Emitter.Factory WHITE = new Emitter.Factory() {
        @Override
        public void emit( Emitter emitter, int index, float x, float y ) {
            ((ScholarParticle)emitter.recycle( ScholarParticle.class )).reset( x, y , 0xFFFFFF);
        }
        @Override
        public boolean lightMode() {
            return false;
        }
    };

    public ScholarParticle() {
        super();

        lifespan = 0.6f;
    }

    public void reset( float x, float y , int color){
        revive();

        this.x = x;
        this.y = y;

        left = lifespan;
        size = 8;
        this.color( color );

        speed.set( Random.Float( -8, +8 ), Random.Float( -16, -32 ) );
    }

    @Override
    public void update() {
        super.update();

        am = 1 - left / lifespan;
    }

}
