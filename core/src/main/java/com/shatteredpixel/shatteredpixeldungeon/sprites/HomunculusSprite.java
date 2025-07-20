package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AlchemistBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.potionist.Homunculus;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.TorchHalo;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BloodParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ChallengeParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ElmoParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.EnergyParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.PurpleParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SacrificialParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.particles.Emitter;

public abstract class HomunculusSprite extends MobSprite {
    private Emitter emitter;
    protected boolean darkmode;
    protected abstract int texOffset();
    protected abstract Emitter createEmitter();
    public HomunculusSprite() {
        super();

        int c = texOffset();
        texture( Assets.Sprites.HOMUNCULUS );

        TextureFilm film = new TextureFilm(texture, 12, 15);

        idle = new Animation(1, true);
        idle.frames(film, c+0, c+0, c+0, c+1, c+0, c+0, c+1, c+1);

        run = new Animation(20, true);
        run.frames(film, c+2, c+3, c+4, c+5, c+6, c+7);

        die = new Animation(30, false);
        die.frames(film, c+0);

        attack = new Animation(15, false);
        attack.frames(film, c+8, c+9, c+10, c+0);

        idle();
    }

    public void setEmitter () {
        if (emitter != null) emitter.remove();
        emitter = createEmitter();
    }

    @Override
    public void resetColor() {
        super.resetColor();
        if (darkmode) {
            brightness(0.65f);
        }
    }

    @Override
    public void link(Char ch) {
        super.link(ch);
        if (emitter == null) {
            emitter = createEmitter();
        }
    }

    @Override
    public void update() {
        super.update();

        if (emitter != null) {
            emitter.visible = visible;
        }

        if (darkmode) {
            brightness(0.65f);
        }
    }

    @Override
    public void die() {
        super.die();
        if (emitter != null){
            emitter.on = false;
        }
    }

    @Override
    public void kill() {
        super.kill();

        if (emitter != null) {
            emitter.on = false;
        }
    }

    public static class Red extends HomunculusSprite {
        @Override
        protected int texOffset() {
            return 0;
        }

        @Override
        protected Emitter createEmitter() {
            Emitter emitter = emitter();
            darkmode = false;
            if (((Homunculus.HomunculusAlly)ch).state() == AlchemistBuff.State.FLAME)
                emitter.pour( FlameParticle.FACTORY, 0.06f );
            else {
                emitter.pour(ChallengeParticle.FACTORY, 0.10f);
                darkmode = true;
            }
            return emitter;
        }

        @Override
        public int blood() {
            return 0xFFFFBB33;
        }
    }

    public static class Yellow extends HomunculusSprite {
        @Override
        protected int texOffset() {
            return 11;
        }

        @Override
        protected Emitter createEmitter() {
            Emitter emitter = emitter();
            darkmode = false;
            if (((Homunculus.HomunculusAlly)ch).state() == AlchemistBuff.State.HASTE)
                emitter.pour( EnergyParticle.FACTORY, 0.04f );
            else {
                emitter.pour(SparkParticle.STATIC, 0.08f);
                darkmode = true;
            }

            return emitter;
        }

        @Override
        public int blood() {
            return 0xFFFFFF85;
        }
    }

    public static class Green extends HomunculusSprite {
        @Override
        protected int texOffset() {
            return 22;
        }

        @Override
        protected Emitter createEmitter() {
            Emitter emitter = emitter();
            darkmode = false;
            if (((Homunculus.HomunculusAlly)ch).state() == AlchemistBuff.State.TOXIC_GAS) {
                emitter.pour(ElmoParticle.FACTORY, 0.06f);
                darkmode = true;
            } else
                emitter.pour( BloodParticle.BURST, 0.06f );

            return emitter;
        }

        @Override
        public int blood() {
            return 0xFF85FFC8;
        }
    }

    public static class Blue extends HomunculusSprite {
        @Override
        protected int texOffset() {
            return 33;
        }

        @Override
        protected Emitter createEmitter() {
            Emitter emitter = emitter();
            darkmode = false;
            if (((Homunculus.HomunculusAlly)ch).state() == AlchemistBuff.State.FROST)
                emitter.pour( MagicMissile.MagicParticle.FACTORY, 0.06f );
            else {
                emitter.pour( ShadowParticle.UP, 0.06f );
                darkmode = true;
            }

            return emitter;
        }

        @Override
        public int blood() {
            return 0xFF8EE3FF;
        }
    }

    public static class Purple extends HomunculusSprite {
        @Override
        protected int texOffset() {
            return 44;
        }

        @Override
        public void setEmitter () {
            if (light != null){
                light.killAndErase();
            }
            super.setEmitter();
        }

        @Override
        public void update() {
            super.update();
            if (light != null){
                light.visible = visible;
                light.point(center());
            }
        }
        @Override
        protected Emitter createEmitter() {
            Emitter emitter = emitter();
            darkmode = false;
            if (((Homunculus.HomunculusAlly)ch).state() == AlchemistBuff.State.PURITY) {
                emitter.pour(PurpleParticle.MISSILE, 0.06f);
                darkmode = true;
            } else {
                GameScene.effect(light = new TorchHalo(this));
                light.hardlight(0x00FF3FFA);
                light.alpha(0.3f);
                light.radius(15);
            }
            return emitter;
        }

        @Override
        public void kill() {
            super.kill();
            if (light != null){
                light.killAndErase();
            }
        }

        @Override
        public int blood() {return 0xFFFF33FF;}
    }

    public static class White extends HomunculusSprite {
        @Override
        protected int texOffset() {
            return 55;
        }

        @Override
        protected Emitter createEmitter() {
            Emitter emitter = emitter();
            darkmode = false;
            if (((Homunculus.HomunculusAlly)ch).state() == AlchemistBuff.State.LEVITATE)
                emitter.pour( Speck.factory( Speck.JET ), 0.06f );
            else {
                emitter.pour( Speck.factory( Speck.LIGHT ), 0.06f);
                darkmode = true;
            }
            return emitter;
        }

        @Override
        public int blood() {return 0xFFFFFFFF;}
    }
}
