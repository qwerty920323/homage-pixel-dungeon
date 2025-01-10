package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.ColorMath;
import com.watabou.utils.Random;


public class FlashDots extends PixelParticle {

    public static final Emitter.Factory SKYBLUE = new Emitter.Factory() {
        @Override
        public void emit( Emitter emitter, int index, float x, float y ) {
            ((FlashDots)emitter.recycle( FlashDots.class )).reset( x, y , 3.5f, 0x75FFED,0x75FFED);
        }
    };

    public static final Emitter.Factory GREEN = new Emitter.Factory() {
        @Override
        public void emit( Emitter emitter, int index, float x, float y ) {
            ((FlashDots)emitter.recycle( FlashDots.class )).reset( x, y ,8f,0x9AFF80, 0xFFFFFF);
        }
    };

    float sizes;
    int startColor;
    int endColor;

    public FlashDots() {
        super();

        lifespan = 1.2f;

        speed.polar( 0, 0 );
    }

    public void reset( float x, float y, float size, int colorStart, int colorEnd ) {
        revive();

        startColor = colorStart;
        endColor = colorEnd;

        this.x = x - speed.x * lifespan;
        this.y = y - speed.y * lifespan;

        left = lifespan;
        sizes = size;

    }

    @Override
    public void update() {
        super.update();
        float p = left / lifespan;
        am = (p < 0.5f ? p : 1 - p) * 3f;
        color( ColorMath.interpolate( endColor, startColor, (left / lifespan) ));
        size( Random.Float( sizes * left / lifespan ) );

    }
}
