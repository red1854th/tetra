package se.mickelus.tetra.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import se.mickelus.tetra.effect.potion.BleedingPotionEffect;
import se.mickelus.tetra.effect.potion.PuncturedPotionEffect;
import se.mickelus.tetra.items.modular.ItemModularHandheld;

public class PunctureEffect extends ChargedAbilityEffect {

    public static final PunctureEffect instance = new PunctureEffect();

    PunctureEffect() {
        super(20, 0.5f, 40, 8, ItemEffect.puncture, TargetRequirement.entity, UseAction.SPEAR, "raised");
    }

    @Override
    public void perform(PlayerEntity attacker, Hand hand, ItemModularHandheld item, ItemStack itemStack, LivingEntity target, Vector3d hitVec, int chargedTicks) {

        AbilityUseResult result;
        if (isDefensive(item, itemStack, hand)) {
            result = performDefensive(attacker, hand, item, itemStack, target);
        } else {
            result = performRegular(attacker, hand, item, itemStack, target, chargedTicks);
        }

        attacker.addExhaustion(0.05f);
        attacker.swing(hand, false);
        attacker.getCooldownTracker().setCooldown(item, getCooldown(item, itemStack) + target.getTotalArmorValue() * 10);

        item.tickProgression(attacker, itemStack, result == AbilityUseResult.fail ? 1 : 2);
        item.applyDamage(2, itemStack, attacker);
    }

    public AbilityUseResult performRegular(PlayerEntity attacker, Hand hand, ItemModularHandheld item, ItemStack itemStack, LivingEntity target, int chargedTicks) {
        int armor = target.getTotalArmorValue();

        AbilityUseResult result = item.hitEntity(itemStack, attacker, target, 1, 0.2f, 0.2f);

        if (result != AbilityUseResult.fail) {
            int overchargeBonus = canOvercharge(item, itemStack) ? getOverchargeBonus(item, itemStack, chargedTicks) : 0;
            boolean isPunctured = target.getActivePotionEffect(PuncturedPotionEffect.instance) != null;
            if (armor < 6 || isPunctured) {
                int duration = 80;

                if (overchargeBonus > 0) {
                    duration += (int) (overchargeBonus * item.getEffectEfficiency(itemStack, ItemEffect.abilityOvercharge) * 10);
                }

                target.addPotionEffect(new EffectInstance(BleedingPotionEffect.instance, duration, 1, false, false));
            } else {
                int amplifier = item.getEffectLevel(itemStack, ItemEffect.puncture) - 1;
                int duration = (int) (item.getEffectEfficiency(itemStack, ItemEffect.puncture) * 20);


                if (overchargeBonus > 0) {
                    amplifier += overchargeBonus * item.getEffectLevel(itemStack, ItemEffect.abilityOvercharge);
                }

                target.addPotionEffect(new EffectInstance(PuncturedPotionEffect.instance, duration, amplifier, false, false));
            }

            target.getEntityWorld().playSound(null, target.getPosition(), SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1, 0.8f);
        } else {
            target.getEntityWorld().playSound(attacker, target.getPosition(), SoundEvents.ENTITY_PLAYER_ATTACK_WEAK, SoundCategory.PLAYERS, 1, 0.8f);
        }

        return result;
    }

    public AbilityUseResult performDefensive(PlayerEntity attacker, Hand hand, ItemModularHandheld item, ItemStack itemStack, LivingEntity target) {
        int armor = target.getTotalArmorValue();

        float knockbackMultiplier = 0.3f;

        boolean isPunctured = target.getActivePotionEffect(PuncturedPotionEffect.instance) != null;
        if (armor < 6 || isPunctured) {
            knockbackMultiplier += 0.6f;
        }

        AbilityUseResult result = item.hitEntity(itemStack, attacker, target, 0.3, 0.8f, knockbackMultiplier);

        if (result != AbilityUseResult.fail) {
            target.addPotionEffect(new EffectInstance(Effects.SLOWNESS, (int) (item.getEffectEfficiency(itemStack, ItemEffect.abilityDefensive) * 20),
                    item.getEffectLevel(itemStack, ItemEffect.abilityDefensive), false, true));

            target.getEntityWorld().playSound(null, target.getPosition(), SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.PLAYERS, 1, 0.8f);
        } else {
            target.getEntityWorld().playSound(attacker, target.getPosition(), SoundEvents.ENTITY_PLAYER_ATTACK_WEAK, SoundCategory.PLAYERS, 1, 0.8f);
        }

        return result;
    }
}
