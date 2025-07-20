package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Gas;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.GasExplode;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Image;
import com.watabou.noosa.Visual;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
public class GasRelease extends Buff implements ActionIndicator.Action{

    @Override
    public boolean attachTo( Char target ) {
        if (super.attachTo( target )) {
            ActionIndicator.setAction(this);
            return true;
        }
        return false;
    }
    boolean release;
    float amount;
    int keeping;
    private static final String RELEASE = "release";
    private static final String KEEPING  = "keeping";
    private static final String AMOUNT  = "amount";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(RELEASE, release);
        bundle.put(KEEPING, keeping);
        bundle.put(AMOUNT, amount);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        release = bundle.getBoolean(RELEASE);
        keeping = bundle.getInt(KEEPING);
        amount = bundle.getFloat(AMOUNT);
    }

    @Override
    public boolean act() {
        if (release) {
            //spreads 45 units of gas total
            int centerVolume = 5;
            for (int i : PathFinder.NEIGHBOURS8) {
                if (!Dungeon.level.solid[target.pos + i]) {
                    GameScene.add(Blob.seed(target.pos + i, 5, HeatGas.class));
                } else {
                    centerVolume += 5;
                }
            }
            GameScene.add(Blob.seed(target.pos, centerVolume, HeatGas.class));

            amount -= 3.4f + keeping++;
            ActionIndicator.refresh();
        }

        if (Dungeon.hero.hasTalent(Talent.VIOLENT_REACT)
                && keeping > 5-Dungeon.hero.pointsInTalent(Talent.VIOLENT_REACT)){
            Actor.add(new GasExplode(target.pos, null));
            keeping -= keeping;
        }

        spend(TICK);

        if (amount < 1)
            detach();

        return true;
    }

    @Override
    public void detach() {
        super.detach();
        ActionIndicator.clearAction(this);
    }

    public void setAmount (float set) {
        amount += set;
        amount = Math.min(100, amount);
        ActionIndicator.refresh();
    }
    @Override
    public String actionName() {return Messages.get(GasRelease.class, "action_name");}

    @Override
    public int actionIcon() {return HeroIcon.GAS_EXPLODE;}
    @Override
    public Visual primaryVisual() {
        Image ico;
        ico = new HeroIcon(this);

        if (!release) {
            ico.brightness(0.65f);
        }

        return ico;
    }
    @Override
    public Visual secondaryVisual() {
        BitmapText txt = new BitmapText(PixelScene.pixelFont);
        txt.text( Messages.get(this, "amount", (int) amount) );
        txt.hardlight(CharSprite.POSITIVE);
        txt.measure();
        return txt;
    }

    @Override
    public int indicatorColor() {
        if (release) return 0xDADADA;
        else         return 0x616161;
    }

    @Override
    public void doAction() {

        target.sprite.operate(target.pos);
        release = !release;
        ActionIndicator.refresh();

        if (release) {
            GLog.w(Messages.get(this, "release"));
            Sample.INSTANCE.play( Assets.Sounds.GAS );

            target.sprite.emitter().burst(Speck.factory(Speck.HEAT), 5 );
        } else {
            GLog.w(Messages.get(this, "sealed"));
            Sample.INSTANCE.play( Assets.Sounds.MISS);
        }

        keeping -= keeping;
    }

    public static class HeatGas extends Gas {

        @Override
        public void use( BlobEmitter emitter ) {
            super.use( emitter );

            emitter.pour( Speck.factory( Speck.HEAT ), 0.15f );
        }

        @Override
        public String tileDesc() {
            return Messages.get(this, "desc");
        }
    }
}
