package com.spellbladenext.items;

import com.spellbladenext.Spellblades;
import com.spellbladenext.entity.Archmagus;
import com.spellbladenext.invasions.piglinsummon;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.StreamSupport;

public class PrismaticEffigy extends Item {
    public PrismaticEffigy(Settings properties) {
        super(properties);
    }

    @Override
    public TypedActionResult<ItemStack> use(World level, PlayerEntity player, Hand interactionHand) {

        if(level instanceof ServerWorld level1 && level1.getRegistryKey() == Spellblades.DIMENSIONKEY) {

            if ( level1.getRegistryKey() == Spellblades.DIMENSIONKEY && level1.getEntitiesByType(TypeFilter.instanceOf(Archmagus.class), archmagus -> archmagus.distanceTo(player) < 200).isEmpty()) {
                for (int i = 0; i < 10; i++) {
                    BlockPos vec3 = piglinsummon.getSafePositionAroundPlayer2(level, player.getSteppingPos(), 10);
                    if (vec3 != null &&level.isSkyVisible(vec3.up()) &&  !level.isClient()) {
                        Archmagus magus = new Archmagus(Spellblades.ARCHMAGUS, level);
                        magus.setPosition(vec3.getX(), vec3.getY(), vec3.getZ());
                        if (!player.isCreative()) {
                            player.getStackInHand(interactionHand).decrement(1);
                            if (player.getStackInHand(interactionHand).isEmpty()) {
                                player.getInventory().removeOne(player.getStackInHand(interactionHand));
                            }
                            magus.spawnedfromitem = true;
                        }
                        level.spawnEntity(magus);
                        player.sendMessage(Text.translatable("Magus' full power is unleashed in the Glass Ocean!"));

                        return TypedActionResult.consume(player.getStackInHand(interactionHand));

                    }
                }
                player.sendMessage(Text.translatable("Magus has no room at your location"));
            } else {
                player.sendMessage(Text.translatable("Magus is already present within 200 blocks."));
            }
        }
        return super.use(level, player, interactionHand);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, @Nullable World level, List<Text> list, TooltipContext tooltipFlag) {
        list.add(Text.translatable("Use in the Glass Ocean to summon Magus, if available."));
        super.appendTooltip(itemStack, level, list, tooltipFlag);
    }
}
