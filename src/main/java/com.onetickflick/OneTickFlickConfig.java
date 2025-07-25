package com.onetickflick;

import net.runelite.client.config.*;

import java.awt.*;

@ConfigGroup("onetickflick")
public interface OneTickFlickConfig extends Config
{
	enum MovementStyle { SINE, LINEAR, EASE_IN_OUT, EASE_OUT_IN, PING_PONG }

	@ConfigItem(
			keyName = "detectPrayerBookClicks",
			name = "Detect prayer book clicks",
			position = 0,
			description = "Whether the overlay should also detect clicks on the prayer book.")
	default boolean detectPrayerBookClicks()
	{
		return true;
	}

	@Units(Units.MILLISECONDS)
	@ConfigItem(
			keyName = "greenStart",
			name = "Target zone start",
			description = "How long into the tick the target zone starts. Used as a visual aid and to determine the combo counter.",
			position = 1
	)
	@Range(min = 0, max = 600)
	default int greenStart()
	{
		return 0;
	}

	@Units(Units.MILLISECONDS)
	@ConfigItem(
			keyName = "greenEnd",
			name = "Target zone end",
			description = "How long into the tick the target zone ends. Used as a visual aid and to determine the combo counter.",
			position = 2
	)
	@Range(min = 0, max = 600)
	default int greenEnd()
	{
		return 500;
	}

	@Units(Units.PIXELS)
	@ConfigItem(
			keyName = "swipeLineWidth",
			name = "Swipe line width",
			position = 3,
			description = "Width in pixels of the vertical line that swipes left to right each tick"
	)
	@Range(min = 1, max = 25)
	default int swipeLineWidth()
	{
		return 1;
	}

	@ConfigItem(
			keyName = "showCombo",
			name = "Show combo counter",
			position = 4,
			description = "Whether to show the combo counter in the overlay. Combo counter is the number of consecutive double clicks within the target zone.")
	default boolean showCombo()
	{
		return true;
	}

	@Units(Units.MILLISECONDS)
	@ConfigItem(
			keyName = "clickDelayMilliseconds",
			name = "Click delay - latency",
			position = 5,
			description = "Add a delay to your clicks to account for latency (ping).")
	@Range(min = 0, max = 500)
	default int clickDelayMilliseconds()
	{
		return 0;
	}

	@ConfigItem(
			keyName = "movementStyle",
			name = "Movement Style",
			description = "The animation style of the tick bar.",
			position = 6
	)
	default MovementStyle movementStyle() { return MovementStyle.SINE; }

	@ConfigItem(
			keyName = "reverseDirection",
			name = "Reverse Direction",
			description = "Reverses the direction of the tick bar.",
			position = 7
	)
	default boolean reverseDirection() { return false; }

	@ConfigSection(
			position = 8,
			name = "Overlay Timeout",
			description = "Configure the overlay timeout settings"
	)
	String timeoutSection = "timeoutSection";

	@ConfigItem(
			keyName = "enableTimeout",
			name = "Enable overlay timeout",
			position = 0,
			section = timeoutSection,
			description = "Whether the overlay should automatically hide after a period without clicking the quick prayer orb.")
	default boolean enableTimeout()
	{
		return false;
	}

	@Units(Units.SECONDS)
	@ConfigItem(
			keyName = "overlayTimeoutSeconds",
			name = "Overlay timeout",
			position = 1,
			section = timeoutSection,
			description = "How long after last clicking the quick prayer orb the overlay should remain visible")
	@Range(min = 1, max = 600)
	default int overlayTimeoutSeconds()
	{
		return 30;
	}

	@ConfigSection(
			position = 9,
			name = "Colors",
			description = "Recolor the various elements of the overlay"
	)
	String colorSection = "colorSection";

	@Alpha
	@ConfigItem(
			keyName = "targetZoneColor",
			name = "Target zone color",
			description = "Color of the target zone in the overlay",
			section = colorSection,
			position = 0
	)
	default Color targetZoneColor()
	{
		return new Color(0, 255, 0, 255);
	}

	@Alpha
	@ConfigItem(
			keyName = "backgroundColor",
			name = "Background color",
			description = "Color of the background bar in the overlay",
			section = colorSection,
			position = 1
	)
	default Color backgroundColor()
	{
		return new Color(255, 0, 0, 255);
	}

	@Alpha
	@ConfigItem(
			keyName = "clickColor",
			name = "X click color",
			description = "Color of the Xs in the overlay that indicate when you clicked",
			section = colorSection,
			position = 2
	)
	default Color clickColor()
	{
		return new Color(0, 0, 0, 255);
	}

	@Alpha
	@ConfigItem(
			keyName = "swipeLineColor",
			name = "Swipe line color",
			description = "Color of the vertical line that swipes left to right each tick",
			section = colorSection,
			position = 3
	)
	default Color swipeLineColor()
	{
		return new Color(0, 0, 0, 255);
	}

	@Alpha
	@ConfigItem(
			keyName = "borderColor",
			name = "Border color",
			description = "Color of the border around the overlay",
			section = colorSection,
			position = 4
	)
	default Color borderColor()
	{
		return new Color(0, 0, 0, 255);
	}

	@Alpha
	@ConfigItem(
			keyName = "comboTextColor",
			name = "Combo text color",
			description = "Color of the combo counter text in the overlay",
			section = colorSection,
			position = 5
	)
	default Color comboTextColor()
	{
		return new Color(255, 255, 255, 255);
	}
}
