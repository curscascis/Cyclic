package com.lothrazar.cyclicmagic.item;
import com.lothrazar.cyclicmagic.entity.projectile.EntityDynamite;
import com.lothrazar.cyclicmagic.entity.projectile.EntityDynamiteBlockSafe;
import com.lothrazar.cyclicmagic.entity.projectile.EntityDynamiteMining;
import com.lothrazar.cyclicmagic.entity.projectile.EntityThrowableDispensable;
import com.lothrazar.cyclicmagic.item.base.BaseItemProjectile;
import com.lothrazar.cyclicmagic.util.UtilPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;

public class ItemProjectileTNT extends BaseItemProjectile {
  public static enum ExplosionType {
    NORMAL, BLOCKSAFE, MINING;
  }
  private ExplosionType type;
  private int strength;
  public ItemProjectileTNT(int str, ExplosionType t) {
    super();
    this.strength = str;
    this.type = t;
  }
  public EntityThrowableDispensable getThrownEntity(World world, double x, double y, double z) {
    switch (type) {
      case NORMAL:
        return new EntityDynamite(world, this.strength, x, y, z);
      case MINING:
        return new EntityDynamiteMining(world, this.strength, x, y, z);
      case BLOCKSAFE:
        return new EntityDynamiteBlockSafe(world, this.strength, x, y, z);
    }
    return null;
  }
  @Override
  public void onItemThrow(ItemStack held, World world, EntityPlayer player, EnumHand hand) {
    EntityThrowableDispensable d = null;
    switch (type) {
      case NORMAL:
        d = new EntityDynamite(world, player, this.strength);
      break;
      case MINING:
        d = new EntityDynamiteMining(world, player, this.strength);
      break;
      case BLOCKSAFE:
        d = new EntityDynamiteBlockSafe(world, player, this.strength);
      break;
      default:
      break;
    }
    this.doThrow(world, player, hand, d);
    UtilPlayer.decrStackSize(player, hand);
  }
  @Override
  public SoundEvent getSound() {
    // TODO Auto-generated method stub
    return SoundEvents.ENTITY_EGG_THROW;
  }
}
