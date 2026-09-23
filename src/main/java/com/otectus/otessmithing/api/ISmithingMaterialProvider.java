package com.otectus.otessmithing.api;

import com.otectus.otessmithing.material.MaterialDefinition;

import java.util.Collection;

/** Supplies material families from code. Resolved against item tags on every reload, like datapack definitions. */
@FunctionalInterface
public interface ISmithingMaterialProvider {
    Collection<MaterialDefinition> materials();
}
