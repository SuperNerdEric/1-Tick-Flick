package com.onetickflick;

import javax.inject.Inject;
import java.awt.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import lombok.Setter;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;

public class OneTickFlickOverlay extends Overlay
{
	private static final int MIN_BAR_HEIGHT = 5;
	private static final int HIDDEN_COMBO_SPACE = 6;
	private static final int COMBO_TEXT_SPACE = 20;
	private static final int HIGHEST_LINE_SPACE = 14;
	private static final int MIN_TEXT_GAP = 8;
	private static final int DEFAULT_BAR_HEIGHT = 12;
	private static final int DEFAULT_TEXT_SPACE = 20;
	private static final int X_SIZE = 4;
	private static final int TICK_LENGTH = 600;
	private static final int MIN_WIDTH = 50;
	private static final int DEFAULT_WIDTH = 150;
	// Default height includes room for a wrapped highest combo line, so wrapping doesn't squash the bar on a fresh install.
	private static final Dimension DEFAULT_SIZE = new Dimension(DEFAULT_WIDTH, DEFAULT_BAR_HEIGHT + DEFAULT_TEXT_SPACE + HIGHEST_LINE_SPACE);

	private final OneTickFlickPlugin plugin;
	private final List<Integer> clickOffsets = new CopyOnWriteArrayList<>();
	private volatile boolean visible = true;
	private boolean showOverlayPrayerBar = true;

	@Setter
	private int greenStart;
	@Setter
	private int greenEnd;
	private boolean showCombo;
	@Setter
	private Color targetZoneColor;
	@Setter
	private Color backgroundColor;
	@Setter
	private Color clickColor;
	@Setter
	private Color swipeLineColor;
	@Setter
	private Color borderColor;
	@Setter
	private Color comboTextColor;
	@Setter
	private String comboCounterText;
	@Setter
	private int swipeLineWidth;
	private boolean showHighestCombo;
	private String highestComboText;

	// Highest combo layout cache. Only recalculated when one of its inputs changes, not every frame.
	private int cachedHighest = -1;
	private int cachedWidth = -1;
	private Font cachedFont;
	private String cachedComboTemplate;
	private String cachedHighestTemplate;
	private String cachedHighestString;
	private int cachedHighestWidth;
	/**
	 * Whether the highest combo is drawn on its own line below the combo counter, because they don't fit side by side.
	 */
	private boolean wrapHighest;


	@Inject
	OneTickFlickOverlay(OneTickFlickPlugin plugin, OneTickFlickConfig config)
	{
		this.plugin = plugin;
		greenStart = config.greenStart();
		greenEnd = config.greenEnd();
		showCombo = config.showCombo();
		showOverlayPrayerBar = config.showOverlayPrayerBar();
		targetZoneColor = config.targetZoneColor();
		backgroundColor = config.backgroundColor();
		clickColor = config.clickColor();
		swipeLineColor = config.swipeLineColor();
		borderColor = config.borderColor();
		comboTextColor = config.comboTextColor();
		comboCounterText = config.comboCounterText();
		swipeLineWidth = config.swipeLineWidth();
		showHighestCombo = config.showHighestCombo();
		highestComboText = config.highestComboText();

		setPosition(OverlayPosition.BOTTOM_LEFT);
		setPreferredSize(DEFAULT_SIZE);
		setMinimumSize(getMinimumHeight());
		setResizable(true);
	}

	void recordClick(int offset)
	{
		clickOffsets.add(offset);
	}

	void newTick()
	{
		clickOffsets.clear();
	}

	void setVisible(boolean v)
	{
		visible = v;
	}

	boolean isVisible()
	{
		return visible;
	}

	public void setShowCombo(boolean showCombo)
	{
		this.showCombo = showCombo;
		setMinimumSize(getMinimumHeight());
	}

	void setShowHighestCombo(boolean showHighestCombo)
	{
		this.showHighestCombo = showHighestCombo;
	}

	void setHighestComboText(String highestComboText)
	{
		this.highestComboText = highestComboText;
	}

	void setShowOverlayPrayerBar(boolean showOverlayPrayerBar)
	{
		this.showOverlayPrayerBar = showOverlayPrayerBar;
		setMinimumSize(getMinimumHeight());
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		if (!visible || (!showOverlayPrayerBar && !showCombo))
		{
			return null;
		}

		Rectangle bounds = getBounds();
		Dimension size = getPreferredSize() == null ? DEFAULT_SIZE : getPreferredSize();

		boolean hasBounds = bounds != null && bounds.width > 0 && bounds.height > 0;
		int width = hasBounds ? Math.max(bounds.width, MIN_WIDTH) : size.width;

		if (showCombo && showHighestCombo)
		{
			updateHighestLayout(g.getFontMetrics(), width);
		}

		int height = hasBounds ? Math.max(bounds.height, getMinimumHeight()) : size.height;

		int barHeight = 0;
		int barY = 0;

		if (showOverlayPrayerBar)
		{
			int reservedSpace = getReservedSpace();
			barHeight = Math.max(MIN_BAR_HEIGHT, height - reservedSpace);
			barY = showCombo ? 0 : (height - barHeight) / 2;

			int greenX1 = width * greenStart / TICK_LENGTH;
			int greenX2 = width * greenEnd / TICK_LENGTH;

			g.setColor(backgroundColor);
			g.fillRect(0, barY, greenX1, barHeight);
			g.fillRect(greenX2, barY, width - greenX2, barHeight);

			g.setColor(targetZoneColor);
			g.fillRect(greenX1, barY, greenX2 - greenX1, barHeight);

			long ms = plugin.millisSinceTick();
			int swipeLineX = (int) (width * ms / (double) TICK_LENGTH);
			swipeLineX = Math.min(swipeLineX, width - swipeLineWidth);

			g.setColor(swipeLineColor);
			g.fillRect(swipeLineX, barY, swipeLineWidth, barHeight);

			g.setColor(borderColor);
			g.drawRect(0, barY, width, barHeight);

			g.setColor(clickColor);
			int xSize = Math.min(X_SIZE, Math.max(1, (barHeight - 1) / 2));
			int y1 = barY + barHeight / 2 - xSize;
			int y2 = barY + barHeight / 2 + xSize;
			for (int offset : clickOffsets)
			{
				int x = width * offset / TICK_LENGTH;
				g.drawLine(x - xSize, y1, x + xSize, y2);
				g.drawLine(x - xSize, y2, x + xSize, y1);
			}
		}

		if (showCombo)
		{
			g.setColor(comboTextColor);
			String text = comboCounterText.replace("{combo}", String.valueOf(plugin.getCombo()));
			int ty;
			if (showOverlayPrayerBar)
			{
				ty = barY + barHeight + g.getFontMetrics().getAscent() + 2;
			}
			else
			{
				ty = (height - HIGHEST_LINE_SPACE + g.getFontMetrics().getAscent()) / 2;
			}
			if (showHighestCombo)
			{
				g.drawString(text, 0, ty);
				if (wrapHighest)
				{
					// Not enough room side by side: highest combo on the line below, both left aligned.
					g.drawString(cachedHighestString, 0, ty + HIGHEST_LINE_SPACE);
				}
				else
				{
					// Combo on the left, highest combo on the right of the same line.
					g.drawString(cachedHighestString, width - cachedHighestWidth, ty);
				}
			}
			else
			{
				int tx = (width - g.getFontMetrics().stringWidth(text)) / 2;
				g.drawString(text, tx, ty);
			}
		}

		return new Dimension(width, height);
	}

	/**
	 * Works out the highest combo text and whether it fits beside the combo counter. The combo counter is measured
	 * showing the highest value, since the current combo can never be larger, so the layout doesn't jump around as
	 * the combo changes. Only recalculated when the highest combo, the texts, the width or the font change.
	 */
	private void updateHighestLayout(FontMetrics fm, int width)
	{
		int highest = plugin.getHighestCombo();
		Font font = fm.getFont();
		if (highest == cachedHighest && width == cachedWidth && font.equals(cachedFont)
				&& comboCounterText == cachedComboTemplate && highestComboText == cachedHighestTemplate)
		{
			return;
		}

		cachedHighest = highest;
		cachedWidth = width;
		cachedFont = font;
		cachedComboTemplate = comboCounterText;
		cachedHighestTemplate = highestComboText;
		cachedHighestString = highestComboText.replace("{highest}", String.valueOf(highest));
		cachedHighestWidth = fm.stringWidth(cachedHighestString);

		int widestCombo = fm.stringWidth(comboCounterText.replace("{combo}", String.valueOf(highest)));
		boolean wrap = widestCombo + MIN_TEXT_GAP + cachedHighestWidth > width;
		wrapHighest = wrap;
	}

	/**
	 * Space below the bar for text. Always includes a second line for a wrapped highest combo, so the bar keeps the
	 * same height whether or not the highest combo wraps.
	 */
	private int getComboTextSpace()
	{
		return COMBO_TEXT_SPACE + HIGHEST_LINE_SPACE;
	}

	private int getMinimumHeight()
	{
		if (showOverlayPrayerBar)
		{
			return MIN_BAR_HEIGHT + getReservedSpace();
		}
		if (showCombo)
		{
			return getComboTextSpace();
		}
		return 0;
	}

	private int getReservedSpace()
	{
		if (!showOverlayPrayerBar)
		{
			return 0;
		}
		return showCombo ? getComboTextSpace() : HIDDEN_COMBO_SPACE;
	}
}