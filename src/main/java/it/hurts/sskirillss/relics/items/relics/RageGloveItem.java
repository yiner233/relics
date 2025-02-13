package it.hurts.sskirillss.relics.items.relics;

import com.google.common.collect.Lists;
import it.hurts.sskirillss.relics.configs.variables.stats.RelicStats;
import it.hurts.sskirillss.relics.init.ItemRegistry;
import it.hurts.sskirillss.relics.items.RelicItem;
import it.hurts.sskirillss.relics.utils.NBTUtils;
import it.hurts.sskirillss.relics.utils.Reference;
import it.hurts.sskirillss.relics.utils.RelicUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.UUID;

public class RageGloveItem extends RelicItem<RageGloveItem.Stats> implements ICurioItem {
    public static final String TAG_UPDATE_TIME = "time";
    public static final String TAG_STACKS_AMOUNT = "stacks";
    public static final String TAG_TARGETED_ENTITY = "target";

    public static RageGloveItem INSTANCE;

    public RageGloveItem() {
        super(Rarity.RARE);

        INSTANCE = this;
    }

    @Override
    public List<ITextComponent> getShiftTooltip(ItemStack stack) {
        List<ITextComponent> tooltip = Lists.newArrayList();
        tooltip.add(new TranslationTextComponent("tooltip.relics.rage_glove.shift_1"));
        return tooltip;
    }

    @Override
    public void curioTick(String identifier, int index, LivingEntity livingEntity, ItemStack stack) {
        if (livingEntity.tickCount % 20 == 0) {
            int time = NBTUtils.getInt(stack, TAG_UPDATE_TIME, 0);
            int stacks = NBTUtils.getInt(stack, TAG_STACKS_AMOUNT, 0);
            if (stacks > 0) {
                NBTUtils.setInt(stack, TAG_UPDATE_TIME, time + 1);
                if (time >= config.stackDuration) {
                    NBTUtils.setInt(stack, TAG_STACKS_AMOUNT, stacks - 1);
                    NBTUtils.setInt(stack, TAG_UPDATE_TIME, 0);
                }
            }
        }
    }

    @Override
    public List<ResourceLocation> getLootChests() {
        return RelicUtils.Worldgen.NETHER;
    }

    @Override
    public Class<Stats> getConfigClass() {
        return Stats.class;
    }

    @Mod.EventBusSubscriber(modid = Reference.MODID)
    public static class RageGloveEvents {
        @SubscribeEvent
        public static void onLivingHurt(LivingHurtEvent event) {
            Stats config = INSTANCE.config;
            if (event.getSource().getEntity() instanceof PlayerEntity) {
                PlayerEntity player = (PlayerEntity) event.getSource().getEntity();
                if (event.getAmount() > config.minDamage
                        && CuriosApi.getCuriosHelper().findEquippedCurio(ItemRegistry.RAGE_GLOVE.get(), player).isPresent()) {
                    LivingEntity entity = event.getEntityLiving();
                    ItemStack stack = CuriosApi.getCuriosHelper().findEquippedCurio(ItemRegistry.RAGE_GLOVE.get(), player).get().getRight();
                    if (!NBTUtils.getString(stack, TAG_TARGETED_ENTITY, "").equals("")
                            && UUID.fromString(NBTUtils.getString(stack, TAG_TARGETED_ENTITY, "")).equals(entity.getUUID())) {
                        NBTUtils.setInt(stack, TAG_STACKS_AMOUNT, NBTUtils.getInt(stack, TAG_STACKS_AMOUNT, 0) + 1);
                        NBTUtils.setInt(stack, TAG_UPDATE_TIME, 0);
                        event.setAmount(event.getAmount() + (event.getAmount() * config.dealtDamageMultiplier * NBTUtils.getInt(stack, TAG_STACKS_AMOUNT, 0)));
                    } else {
                        NBTUtils.setInt(stack, TAG_UPDATE_TIME, 0);
                        NBTUtils.setInt(stack, TAG_STACKS_AMOUNT, 1);
                        NBTUtils.setString(stack, TAG_TARGETED_ENTITY, entity.getUUID().toString());
                    }
                }
            }

            if (event.getSource().getEntity() instanceof LivingEntity
                    && event.getEntityLiving() instanceof PlayerEntity) {
                PlayerEntity player = (PlayerEntity) event.getEntityLiving();
                if (CuriosApi.getCuriosHelper().findEquippedCurio(ItemRegistry.RAGE_GLOVE.get(), player).isPresent()) {
                    ItemStack stack = CuriosApi.getCuriosHelper().findEquippedCurio(ItemRegistry.RAGE_GLOVE.get(), player).get().getRight();
                    if (!NBTUtils.getString(stack, TAG_TARGETED_ENTITY, "").equals("")
                            && event.getSource().getEntity() == ((ServerWorld) player.getCommandSenderWorld()).getEntity(UUID.fromString(NBTUtils.getString(stack, TAG_TARGETED_ENTITY, "")))) {
                        event.setAmount(event.getAmount() + (event.getAmount() * config.incomingDamageMultiplier * NBTUtils.getInt(stack, TAG_STACKS_AMOUNT, 0)));
                    }
                }
            }
        }
    }

    public static class Stats extends RelicStats {
        public int stackDuration = 5;
        public float minDamage = 3.0F;
        public float dealtDamageMultiplier = 0.1F;
        public float incomingDamageMultiplier = 0.05F;
    }
}