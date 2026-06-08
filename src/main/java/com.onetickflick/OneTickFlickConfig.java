package com.onetickflick;

import net.runelite.client.config.*;

import java.awt.*;

@ConfigGroup("onetickflick")
public interface OneTickFlickConfig extends Config
{
	@ConfigItem(
			keyName = "showOverheadPrayerBar",
			name = "Show overhead prayer bar",
			position = 0,
			description = "Whether to show the prayer bar above your character's head, similar to the RuneLite \"Prayer\" plugin prayer bar")
	default boolean showOverheadPrayerBar()
	{
		return false;
	}

	@ConfigItem(
			keyName = "showOverlayPrayerBar",
			name = "Show overlay prayer bar",
			position = 1,
			description = "Whether to show the overlay prayer bar")
	default boolean showOverlayPrayerBar()
	{
		return true;
	}

	@ConfigItem(
			keyName = "showCombo",
			name = "Show combo counter",
			position = 2,
			description = "Whether to show the combo counter in the overlay. Combo counter is the number of consecutive double clicks within the target zone.")
	default boolean showCombo()
	{
		return true;
	}

	@ConfigItem(
			keyName = "detectPrayerBookClicks",
			name = "Detect prayer book clicks",
			position = 3,
			description = "Whether the overlay should also detect clicks on the prayer book.")
	default boolean detectPrayerBookClicks()
	{
		return true;
	}

	@Units(Units.MILLISECONDS)
	@ConfigItem(
			keyName = "clickDelayMilliseconds",
			name = "Click delay - latency",
			position = 4,
			description = "Add a delay to your clicks to account for latency (ping).")
	@Range(min = 0, max = 500)
	default int clickDelayMilliseconds()
	{
		return 0;
	}

	@ConfigSection(
			position = 5,
			name = "Overlay",
			description = "Configure the overlay settings"
	)
	String overlaySection = "overlaySection";

	@Units(Units.MILLISECONDS)
	@ConfigItem(
			keyName = "greenStart",
			name = "Target zone start",
			description = "How long into the tick the target zone starts. Used as a visual aid and to determine the combo counter.",
			section = overlaySection,
			position = 0
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
			section = overlaySection,
			position = 1
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
			section = overlaySection,
			position = 2,
			description = "Width in pixels of the vertical line that swipes left to right each tick"
	)
	@Range(min = 1, max = 25)
	default int swipeLineWidth()
	{
		return 1;
	}

	@ConfigItem(
			keyName = "comboCounterText",
			name = "Combo counter text",
			section = overlaySection,
			position = 3,
			description = "Text displayed for the combo counter. Use {combo} to insert the current combo count.")
	default String comboCounterText()
	{
		return "Combo: {combo}";
	}

	@ConfigSection(
			position = 6,
			name = "Timeout",
			description = "Configure the timeout settings"
	)
	String timeoutSection = "timeoutSection";

	@ConfigItem(
			keyName = "enableTimeout",
			name = "Enable timeout",
			position = 0,
			section = timeoutSection,
			description = "Whether the overhead bar / overlay should automatically hide after a period without clicking the quick prayer orb.")
	default boolean enableTimeout()
	{
		return false;
	}

	@ConfigItem(
			keyName = "timeoutWhilePrayerActive",
			name = "Timeout while prayer active",
			position = 1,
			section = timeoutSection,
			description = "Whether the overhead bar / overlay should timeout even while you are actively praying.")
	default boolean timeoutWhilePrayerActive()
	{
		return false;
	}

	@Units(Units.SECONDS)
	@ConfigItem(
			keyName = "overlayTimeoutSeconds",
			name = "Timeout",
			position = 2,
			section = timeoutSection,
			description = "How long after last clicking the quick prayer orb the overhead bar / overlay should remain visible")
	@Range(min = 1, max = 600)
	default int overlayTimeoutSeconds()
	{
		return 30;
	}

	@ConfigSection(
			position = 7,
			name = "Overhead Prayer Bar Colors",
			description = "Recolor the various elements of the overhead prayer bar"
	)
	String overheadPrayerBarColorSection = "overheadPrayerBarColorSection";

	@Alpha
	@ConfigItem(
			keyName = "prayerBarFillColor",
			name = "Bar fill color",
			description = "Color of the prayer points fill in the overhead prayer bar",
			section = overheadPrayerBarColorSection,
			position = 0
	)
	default Color prayerBarFillColor()
	{
		return new Color(0, 149, 151, 255);
	}

	@Alpha
	@ConfigItem(
			keyName = "prayerBarBackgroundColor",
			name = "Bar background color",
			description = "Color of the background in the overhead prayer bar",
			section = overheadPrayerBarColorSection,
			position = 1
	)
	default Color prayerBarBackgroundColor()
	{
		return new Color(0, 0, 0, 255);
	}

	@Alpha
	@ConfigItem(
			keyName = "prayerBarSwipeLineColor",
			name = "Swipe line color",
			description = "Color of the vertical line that swipes left to right each tick on the overhead prayer bar",
			section = overheadPrayerBarColorSection,
			position = 2
	)
	default Color prayerBarSwipeLineColor()
	{
		return new Color(0, 255, 255, 255);
	}

	@Alpha
	@ConfigItem(
			keyName = "prayerBarClickLineColor",
			name = "Click line color",
			description = "Color of the vertical lines on the overhead prayer bar that indicate when you clicked",
			section = overheadPrayerBarColorSection,
			position = 3
	)
	default Color prayerBarClickLineColor()
	{
		return new Color(255, 255, 255, 255);
	}

	@ConfigSection(
			position = 8,
			name = "Overlay Colors",
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
