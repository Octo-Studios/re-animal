package it.hurts.shatterbyte.reanimal.client.renderer.quill_arrow;

import it.hurts.shatterbyte.reanimal.ReAnimal;
import it.hurts.shatterbyte.reanimal.common.entity.hedgehog.QuillArrowEntity;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.resources.Identifier;

public class QuillArrowRenderer extends ArrowRenderer<QuillArrowEntity, ArrowRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(ReAnimal.MODID, "textures/entity/quill_arrow.png");

    public QuillArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected Identifier getTextureLocation(ArrowRenderState state) {
        return TEXTURE;
    }

    @Override
    public ArrowRenderState createRenderState() {
        return new ArrowRenderState();
    }
}
