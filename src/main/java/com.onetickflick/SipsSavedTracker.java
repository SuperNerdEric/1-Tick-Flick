package com.onetickflick;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;

/**
 * Tracks how many prayer points (and so restore sips and GP) the player has saved by 1-tick flicking.
 * <p>
 * Prayer drain in OSRS works with a drain counter: every tick, the drain effects of all active prayers are added
 * to the counter, and each time it exceeds the player's prayer drain resistance (60 + 2 * prayer bonus) a prayer
 * point is lost. While flicking perfectly no drain is applied, so every combo tick saves
 * {@code totalDrainEffect / resistance} prayer points.
 * <p>
 * Equipment-derived values (prayer bonus, holy wrench effect) and the prayer level are cached and only recalculated
 * when the relevant item container or stat changes, so the per-tick cost is just the active prayer lookup.
 */
@Singleton
class SipsSavedTracker
{
	private static final String CONFIG_GROUP = "onetickflick";
	private static final String LIFETIME_POINTS_KEY = "lifetimePrayerPointsSaved";

	static final int COINS = 995;

	static final int INVENTORY_CONTAINER_ID = 93;
	static final int WORN_CONTAINER_ID = 94;

	/**
	 * Minimum ticks between saves when a combo breaks (100 ticks = 1 minute). Saving writes the config and notifies
	 * every plugin, so it is throttled. Logout, world hop and plugin shutdown always save.
	 */
	private static final int SAVE_INTERVAL_TICKS = 100;

	/*
	 * Items with the "holy wrench effect": prayer potions restore 27% of the prayer level instead of 25%.
	 * Matched by exact item name, so every item ID of the same item counts (e.g. ring of the gods (i) has several
	 * IDs depending on where it was imbued, and the max cape has more than one ID). Combat max cape variants
	 * (fire, infernal, god max capes, assembler etc.) lose this perk, and their names don't match.
	 */

	/**
	 * Items that give the effect when carried in the inventory.
	 */
	private static final Set<String> RESTORE_BOOST_INVENTORY_ITEMS = ImmutableSet.of(
			"Holy wrench", "Prayer cape", "Prayer cape(t)", "Max cape");

	/**
	 * Items that give the effect when worn. The holy wrench can't be equipped, and the ring of the gods (i) only works worn.
	 */
	private static final Set<String> RESTORE_BOOST_WORN_ITEMS = ImmutableSet.of(
			"Prayer cape", "Prayer cape(t)", "Max cape", "Ring of the gods (i)");

	/**
	 * Prayer drain effects, from the OSRS wiki. Parallel arrays so the per-tick lookup doesn't allocate.
	 */
	private static final Prayer[] DRAIN_PRAYERS = {
			Prayer.THICK_SKIN, Prayer.BURST_OF_STRENGTH, Prayer.CLARITY_OF_THOUGHT, Prayer.SHARP_EYE, Prayer.MYSTIC_WILL,
			Prayer.ROCK_SKIN, Prayer.SUPERHUMAN_STRENGTH, Prayer.IMPROVED_REFLEXES, Prayer.RAPID_RESTORE, Prayer.RAPID_HEAL,
			Prayer.PROTECT_ITEM, Prayer.HAWK_EYE, Prayer.MYSTIC_LORE, Prayer.STEEL_SKIN, Prayer.ULTIMATE_STRENGTH,
			Prayer.INCREDIBLE_REFLEXES, Prayer.PROTECT_FROM_MAGIC, Prayer.PROTECT_FROM_MISSILES, Prayer.PROTECT_FROM_MELEE,
			Prayer.EAGLE_EYE, Prayer.DEADEYE, Prayer.MYSTIC_MIGHT, Prayer.MYSTIC_VIGOUR, Prayer.RETRIBUTION, Prayer.REDEMPTION,
			Prayer.SMITE, Prayer.PRESERVE, Prayer.CHIVALRY, Prayer.PIETY, Prayer.RIGOUR, Prayer.AUGURY,
	};
	private static final int[] DRAIN_EFFECTS = {
			1, 1, 1, 1, 1,
			6, 6, 6, 1, 2,
			2, 6, 6, 12, 12,
			12, 12, 12, 12,
			12, 12, 12, 12, 3, 6,
			18, 2, 24, 24, 24, 24,
	};
	private static final int[] DRAIN_VARBITS = new int[DRAIN_PRAYERS.length];

	static
	{
		for (int i = 0; i < DRAIN_PRAYERS.length; i++)
		{
			DRAIN_VARBITS[i] = DRAIN_PRAYERS[i].getVarbit();
		}
	}

	private final Client client;
	private final ItemManager itemManager;
	private final ConfigManager configManager;

	/**
	 * Prayer points saved. Stored as points (not sips) so they can be shown as any {@link RestoreItem}.
	 */
	private volatile double sessionPoints;
	private volatile double lifetimePoints;

	// Cached state, updated from events on the client thread.
	private volatile int prayerLevel = 1;
	private int prayerPoints;
	private int prayerBonus;
	private boolean inventoryRestoreBoost;
	private boolean wornRestoreBoost;

	/**
	 * Last non-zero drain effect seen during the current combo. Used when no prayer happens to be active at the
	 * moment the tick is processed. Cleared when the combo ends so it can't carry over into a later combo.
	 */
	private int lastDrainEffect;
	private boolean dirty;
	private int lastSaveTick;

	@Inject
	SipsSavedTracker(Client client, ItemManager itemManager, ConfigManager configManager)
	{
		this.client = client;
		this.itemManager = itemManager;
		this.configManager = configManager;
	}

	/**
	 * Recalculates all cached state. Used when the plugin starts while already logged in. Must run on the client thread.
	 */
	void refreshAll()
	{
		prayerLevel = Math.max(1, client.getRealSkillLevel(Skill.PRAYER));
		prayerPoints = client.getBoostedSkillLevel(Skill.PRAYER);
		onItemContainerChanged(INVENTORY_CONTAINER_ID, client.getItemContainer(INVENTORY_CONTAINER_ID));
		onItemContainerChanged(WORN_CONTAINER_ID, client.getItemContainer(WORN_CONTAINER_ID));
	}

	void onPrayerChanged(int realLevel, int currentPoints)
	{
		prayerLevel = Math.max(1, realLevel);
		prayerPoints = currentPoints;
	}

	void onItemContainerChanged(int containerId, ItemContainer container)
	{
		if (containerId == WORN_CONTAINER_ID)
		{
			prayerBonus = calculatePrayerBonus(container);
			wornRestoreBoost = containsAny(container, RESTORE_BOOST_WORN_ITEMS);
		}
		else if (containerId == INVENTORY_CONTAINER_ID)
		{
			inventoryRestoreBoost = containsAny(container, RESTORE_BOOST_INVENTORY_ITEMS);
		}
	}

	/**
	 * Called once for every tick that continued the combo.
	 */
	void recordFlickTick()
	{
		// With no prayer points, prayers can't be turned on, so nothing is being saved.
		if (prayerPoints <= 0)
		{
			return;
		}

		int drainEffect = getActiveDrainEffect();
		if (drainEffect > 0)
		{
			lastDrainEffect = drainEffect;
		}
		else
		{
			drainEffect = lastDrainEffect;
		}

		if (drainEffect <= 0)
		{
			return;
		}

		int resistance = Math.max(1, 60 + 2 * prayerBonus);
		double pointsSaved = drainEffect / (double) resistance;

		sessionPoints += pointsSaved;
		lifetimePoints += pointsSaved;
		dirty = true;
	}

	/**
	 * Called when the combo breaks. Resets the current session, and saves the lifetime total if it's been a while.
	 */
	void endSession()
	{
		sessionPoints = 0;
		lastDrainEffect = 0;
		if (dirty && client.getTickCount() - lastSaveTick >= SAVE_INTERVAL_TICKS)
		{
			save();
		}
	}

	void load()
	{
		lifetimePoints = readDouble(LIFETIME_POINTS_KEY);
		dirty = false;
	}

	/**
	 * Saves the lifetime total if it changed since the last save.
	 */
	void save()
	{
		if (!dirty)
		{
			return;
		}
		configManager.setRSProfileConfiguration(CONFIG_GROUP, LIFETIME_POINTS_KEY, String.valueOf(lifetimePoints));
		dirty = false;
		lastSaveTick = client.getTickCount();
	}

	/**
	 * Clears the lifetime totals for the current account.
	 */
	void resetLifetime()
	{
		lifetimePoints = 0;
		dirty = false;
		configManager.unsetRSProfileConfiguration(CONFIG_GROUP, LIFETIME_POINTS_KEY);
	}

	double getSessionDoses(RestoreItem item)
	{
		return toDoses(sessionPoints, item);
	}

	double getSessionGp(RestoreItem item)
	{
		return getGeValue(getSessionDoses(item), item);
	}

	double getLifetimeDoses(RestoreItem item)
	{
		return toDoses(lifetimePoints, item);
	}

	/**
	 * Lifetime GP saved, valued at current GE prices.
	 */
	double getLifetimeGp(RestoreItem item)
	{
		return getGeValue(getLifetimeDoses(item), item);
	}

	private double toDoses(double points, RestoreItem item)
	{
		boolean restoreBoost = inventoryRestoreBoost || wornRestoreBoost;
		return points / item.getRestoreAmount(prayerLevel, restoreBoost);
	}

	private int getActiveDrainEffect()
	{
		int total = 0;
		for (int i = 0; i < DRAIN_VARBITS.length; i++)
		{
			if (client.getVarbitValue(DRAIN_VARBITS[i]) == 1)
			{
				total += DRAIN_EFFECTS[i];
			}
		}
		return total;
	}

	private int calculatePrayerBonus(ItemContainer equipment)
	{
		if (equipment == null)
		{
			return 0;
		}

		int bonus = 0;
		for (Item item : equipment.getItems())
		{
			if (item == null || item.getId() <= 0)
			{
				continue;
			}
			ItemStats stats = itemManager.getItemStats(item.getId());
			if (stats != null && stats.getEquipment() != null)
			{
				bonus += stats.getEquipment().getPrayer();
			}
		}
		return bonus;
	}

	private boolean containsAny(ItemContainer container, Set<String> itemNames)
	{
		if (container == null)
		{
			return false;
		}
		for (Item item : container.getItems())
		{
			if (item == null || item.getId() <= 0)
			{
				continue;
			}
			String name = itemManager.getItemComposition(item.getId()).getName();
			if (itemNames.contains(name))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Values a number of doses at GE prices the way they would actually be bought. Decanting inflates the price of
	 * full potions, so doses are counted as many 4-dose potions as possible, then a 3-dose for the remainder,
	 * then a 2-dose, and any remaining doses (including fractions) at the 1-dose price. Single-use items such as
	 * moonlight moths are simply doses * price.
	 */
	double getGeValue(double doses, RestoreItem item)
	{
		int[] ids = item.getItemIdsByDose();
		double remaining = doses;
		double value = 0;

		for (int doseCount = ids.length; doseCount > 1; doseCount--)
		{
			long count = (long) Math.floor(remaining / doseCount);
			value += count * (double) itemManager.getItemPrice(ids[doseCount - 1]);
			remaining -= count * doseCount;
		}

		value += remaining * itemManager.getItemPrice(ids[0]);
		return value;
	}

	private double readDouble(String key)
	{
		String value = configManager.getRSProfileConfiguration(CONFIG_GROUP, key);
		if (value == null)
		{
			return 0;
		}
		try
		{
			return Double.parseDouble(value);
		}
		catch (NumberFormatException e)
		{
			return 0;
		}
	}
}
