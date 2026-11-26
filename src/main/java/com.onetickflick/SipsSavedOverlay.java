package com.onetickflick;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import lombok.Setter;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.QuantityFormatter;

/**
 * Shows the sips and GP saved by flicking. Kept separate from the tick bar overlay so it can be placed on its own,
 * but shown and hidden together with it.
 * <p>
 * The values only change once per game tick, so the row text is built in {@link #refresh()} (called each tick and on
 * config changes, on the client thread) and {@link #render} only draws the cached rows.
 */
public class SipsSavedOverlay extends Overlay
{
	private static final int ICON_WIDTH = 24;
	private static final int ICON_HEIGHT = 21;
	private static final int ROW_HEIGHT = 24;
	private static final int ICON_TEXT_GAP = 4;

	private final SipsSavedTracker tracker;
	private final ItemManager itemManager;
	/**
	 * Icons pre-scaled to the overlay size once loaded, keyed by item ID, so render never scales images.
	 */
	private final Map<Integer, BufferedImage> scaledIcons = new ConcurrentHashMap<>();
	private final Map<Integer, Boolean> requestedIcons = new ConcurrentHashMap<>();
	private volatile boolean visible = true;
	private volatile List<StatRow> rows = Collections.emptyList();

	@Setter
	private boolean showSessionSips;
	@Setter
	private boolean showSessionGp;
	@Setter
	private boolean showLifetimeSips;
	@Setter
	private boolean showLifetimeGp;
	@Setter
	private RestoreItem restoreItem;
	@Setter
	private Color textColor;

	@Inject
	SipsSavedOverlay(OneTickFlickConfig config, SipsSavedTracker tracker, ItemManager itemManager)
	{
		this.tracker = tracker;
		this.itemManager = itemManager;
		showSessionSips = config.showSessionSipsSaved();
		showSessionGp = config.showSessionGpSaved();
		showLifetimeSips = config.showLifetimeSipsSaved();
		showLifetimeGp = config.showLifetimeGpSaved();
		restoreItem = config.restoreItem();
		textColor = config.comboTextColor();

		setPosition(OverlayPosition.BOTTOM_LEFT);
	}

	/**
	 * Called from the client thread whenever the main overlay is shown or hidden.
	 */
	void setVisible(boolean v)
	{
		boolean becameVisible = v && !visible;
		visible = v;
		if (becameVisible)
		{
			// Rows aren't rebuilt while hidden, so bring them up to date straight away.
			refresh();
		}
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		if (!visible)
		{
			return null;
		}

		List<StatRow> rows = this.rows;
		if (rows.isEmpty())
		{
			return null;
		}

		FontMetrics fm = g.getFontMetrics();
		int width = 0;
		for (StatRow row : rows)
		{
			width = Math.max(width, ICON_WIDTH + ICON_TEXT_GAP + fm.stringWidth(row.text));
		}

		int y = 0;
		for (StatRow row : rows)
		{
			if (row.icon != null)
			{
				g.drawImage(row.icon, 0, y + (ROW_HEIGHT - ICON_HEIGHT) / 2, null);
			}
			g.setColor(textColor);
			g.drawString(row.text, ICON_WIDTH + ICON_TEXT_GAP, y + (ROW_HEIGHT + fm.getAscent()) / 2 - 1);
			y += ROW_HEIGHT;
		}

		return new Dimension(width, y);
	}

	/**
	 * Rebuilds the cached row text. Must run on the client thread.
	 */
	void refresh()
	{
		if (!visible)
		{
			return;
		}
		if (!showSessionSips && !showSessionGp && !showLifetimeSips && !showLifetimeGp)
		{
			rows = Collections.emptyList();
			return;
		}

		List<StatRow> rows = new ArrayList<>(4);
		// The icon shows what is counted (restore item or coins), so the text only needs the value and its scope.
		RestoreItem item = restoreItem;
		if (showSessionSips)
		{
			rows.add(new StatRow(getRestoreIcon(item), String.format("%.2f saved", tracker.getSessionDoses(item))));
		}
		if (showSessionGp)
		{
			rows.add(new StatRow(getCoinsIcon(), formatGp(tracker.getSessionGp(item)) + " saved"));
		}
		if (showLifetimeSips)
		{
			rows.add(new StatRow(getRestoreIcon(item), String.format("%.1f lifetime", tracker.getLifetimeDoses(item))));
		}
		if (showLifetimeGp)
		{
			rows.add(new StatRow(getCoinsIcon(), formatGp(tracker.getLifetimeGp(item)) + " lifetime"));
		}
		this.rows = rows;
	}

	private static String formatGp(double gp)
	{
		return QuantityFormatter.quantityToStackSize(Math.round(gp));
	}

	private BufferedImage getRestoreIcon(RestoreItem item)
	{
		return getScaledIcon(item.getIconItemId(), 1);
	}

	private BufferedImage getCoinsIcon()
	{
		// A small stack of coins, without the quantity number drawn on it.
		return getScaledIcon(SipsSavedTracker.COINS, 5);
	}

	/**
	 * Returns the icon scaled to the overlay size, or null until it has loaded. The first call requests the image;
	 * once it loads it is scaled once and cached, and the next refresh picks it up.
	 */
	private BufferedImage getScaledIcon(int itemId, int quantity)
	{
		BufferedImage scaled = scaledIcons.get(itemId);
		if (scaled == null && requestedIcons.putIfAbsent(itemId, Boolean.TRUE) == null)
		{
			AsyncBufferedImage image = itemManager.getImage(itemId, quantity, false);
			image.onLoaded(() -> scaledIcons.put(itemId, ImageUtil.resizeImage(image, ICON_WIDTH, ICON_HEIGHT)));
		}
		return scaled;
	}

	private static class StatRow
	{
		private final BufferedImage icon;
		private final String text;

		StatRow(BufferedImage icon, String text)
		{
			this.icon = icon;
			this.text = text;
		}
	}
}
