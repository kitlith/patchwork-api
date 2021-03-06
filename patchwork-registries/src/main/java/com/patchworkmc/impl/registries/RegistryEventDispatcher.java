/*
 * Minecraft Forge, Patchwork Project
 * Copyright (c) 2016-2019, 2019
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package com.patchworkmc.impl.registries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.registries.IForgeRegistry;

import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

@SuppressWarnings("rawtypes")
public class RegistryEventDispatcher {
	private static final boolean CHECK_SUPERS = false;
	private static List<IForgeRegistry> registered = new ArrayList<>();

	/**
	 * Used by {@link net.minecraftforge.registries.ForgeRegistries}.
	 *
	 * @param registry the registry to add to the dispatch list
	 */
	public static void register(IForgeRegistry registry) {
		registered.add(registry);
	}

	/**
	 * @return the ordering of registries that Forge expects
	 */
	private static List<Identifier> getExpectedOrdering() {
		List<Identifier> registries = new ArrayList<>(Registry.REGISTRIES.getIds());

		registries.remove(Registry.REGISTRIES.getId(Registry.BLOCK));
		registries.remove(Registry.REGISTRIES.getId(Registry.ITEM));

		registries.sort((o1, o2) -> String.valueOf(o1).compareToIgnoreCase(String.valueOf(o2)));

		registries.add(0, Registry.REGISTRIES.getId(Registry.BLOCK));
		registries.add(1, Registry.REGISTRIES.getId(Registry.ITEM));

		return registries;
	}

	@SuppressWarnings("unchecked")
	public static void dispatchRegistryEvents(Consumer<RegistryEvent.Register> handler) {
		List<Identifier> expectedOrder = getExpectedOrdering();

		if (registered.size() < expectedOrder.size()) {
			throw new IllegalStateException("RegistryEventDispatcher is missing " + (expectedOrder.size() - registered.size()) + " registries!");
		}

		for (IForgeRegistry registry : registered) {
			Identifier identifier = registry.getRegistryName();
			Identifier expected = expectedOrder.remove(0);

			if (!identifier.equals(expected)) {
				throw new IllegalStateException("Bad ordering of registries in RegistryEventDispatcher: expected " + expected + " but got " + identifier);
			}

			if (CHECK_SUPERS) {
				Class superType = registry.getRegistrySuperType();

				for (Map.Entry<Identifier, Object> entry : (Set<Map.Entry<Identifier, Object>>) registry.getEntries()) {
					if (!superType.isAssignableFrom(entry.getValue().getClass())) {
						throw new IllegalStateException("Bad registry type for " + identifier + " (" + entry.getKey() + ")");
					}
				}
			}

			handler.accept(new RegistryEvent.Register(registry));
		}
	}
}
