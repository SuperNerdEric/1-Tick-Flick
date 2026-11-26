package com.onetickflick;

import lombok.Getter;

/**
 * Items that restore prayer, used to convert saved prayer points into sips and GP.
 */
@Getter
public enum RestoreItem
{
	/**
	 * Restores 7 + floor(25% of prayer level), or 27% with the holy wrench effect.
	 */
	PRAYER_POTION("Prayer potion", 7, true, new int[]{143, 141, 139, 2434}),
	/**
	 * Restores 8 + floor(25% of prayer level), or 27% with the holy wrench effect.
	 */
	SUPER_RESTORE("Super restore", 8, true, new int[]{3030, 3028, 3026, 3024}),
	/**
	 * Restores a flat 22 prayer points. Not affected by the holy wrench effect.
	 */
	MOONLIGHT_MOTH("Moonlight moth", 22, false, new int[]{28893});

	private final String displayName;
	private final int baseRestore;
	private final boolean scalesWithLevel;
	/**
	 * Item IDs indexed by dose count - 1, i.e. [1-dose, 2-dose, 3-dose, 4-dose].
	 */
	private final int[] itemIdsByDose;

	RestoreItem(String displayName, int baseRestore, boolean scalesWithLevel, int[] itemIdsByDose)
	{
		this.displayName = displayName;
		this.baseRestore = baseRestore;
		this.scalesWithLevel = scalesWithLevel;
		this.itemIdsByDose = itemIdsByDose;
	}

	/**
	 * Prayer points restored by one dose.
	 *
	 * @param prayerLevel  the player's base prayer level
	 * @param restoreBoost whether the player has the holy wrench effect
	 */
	int getRestoreAmount(int prayerLevel, boolean restoreBoost)
	{
		if (!scalesWithLevel)
		{
			return baseRestore;
		}
		return baseRestore + prayerLevel * (restoreBoost ? 27 : 25) / 100;
	}

	/**
	 * The item shown as the overlay icon: the full potion, or the moth.
	 */
	int getIconItemId()
	{
		return itemIdsByDose[itemIdsByDose.length - 1];
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
