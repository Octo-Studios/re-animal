package it.hurts.shatterbyte.reanimal.client.renderer.hedgehog;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;

public final class HeadHedgehogAnimatable implements GeoAnimatable {
    public static final HeadHedgehogAnimatable INSTANCE = new HeadHedgehogAnimatable();

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private HeadHedgehogAnimatable() {
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
