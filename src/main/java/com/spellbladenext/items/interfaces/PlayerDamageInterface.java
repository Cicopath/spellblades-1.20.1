package com.spellbladenext.items.interfaces;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.spell_engine.entity.SpellProjectile;

import java.util.List;

public interface PlayerDamageInterface {
    void setDamageMultiplier(float f);
    void repeat();
    int getRepeats();
    void resetRepeats();

    void override(boolean bool);
    void setLastAttacked(Entity entity);
    Entity getLastAttacked();

    void shouldUnfortify(boolean bool);
    void listAdd(LivingEntity entity);
    void listRefresh();
    List<LivingEntity> getList();
    boolean listContains(LivingEntity entity);

}
