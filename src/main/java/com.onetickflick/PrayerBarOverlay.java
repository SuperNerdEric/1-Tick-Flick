package com.onetickflick;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.Skill;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import lombok.Setter;

public class PrayerBarOverlay extends Overlay
{
	private static final int TICK_LENGTH = 600;
	private static final Dimension PRAYER_BAR_SIZE = new Dimension(30, 5);

	private final Client client;
	private final OneTickFlickConfig config;
	private final OneTickFlickPlugin plugin;

	private boolean showingPrayerBar;
	private volatile boolean visible = true;
	private final List<Integer> clickOffsets = new CopyOnWriteArrayList<>();

	@Setter
	private Color barFillColor;
	@Setter
	private Color barBackgroundColor;
	@Setter
	private Color swipeLineColor;
	@Setter
	private Color clickLineColor;

	@Inject
	PrayerBarOverlay(final Client client, final OneTickFlickConfig config, final OneTickFlickPlugin plugin)
	{
		this.client = client;
		this.config = config;
		this.plugin = plugin;
		barFillColor = config.prayerBarFillColor();
		barBackgroundColor = config.prayerBarBackgroundColor();
		swipeLineColor = config.prayerBarSwipeLineColor();
		clickLineColor = config.prayerBarClickLineColor();

		setPosition(OverlayPosition.DYNAMIC);
		setPriority(PRIORITY_HIGH);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showOverheadPrayerBar() || !showingPrayerBar || !visible)
		{
			return null;
		}

		final int height = client.getLocalPlayer().getLogicalHeight() + 10;
		final LocalPoint localLocation = client.getLocalPlayer().getLocalLocation();
		final Point canvasPoint = Perspective.localToCanvas(client, localLocation, client.getPlane(), height);

		if (canvasPoint == null)
		{
			return null;
		}

		final float ratio = (float) client.getBoostedSkillLevel(Skill.PRAYER) / client.getRealSkillLevel(Skill.PRAYER);

		final int barX = canvasPoint.getX() - 15;
		final int barY = canvasPoint.getY();
		final int barWidth = PRAYER_BAR_SIZE.width;
		final int barHeight = PRAYER_BAR_SIZE.height;

		final int progressFill = (int) Math.ceil(Math.min((barWidth * ratio), barWidth));

		graphics.setColor(barBackgroundColor);
		graphics.fillRect(barX, barY, barWidth, barHeight);
		graphics.setColor(barFillColor);
		graphics.fillRect(barX, barY, progressFill, barHeight);

		long ms = plugin.millisSinceTick();
		int xOffset = (int) (barWidth * ms / (double) TICK_LENGTH);
		xOffset = Math.min(xOffset, barWidth - 1);

		graphics.setColor(swipeLineColor);
		graphics.fillRect(barX + xOffset, barY, 1, barHeight);

		graphics.setColor(clickLineColor);
		for (int offset : clickOffsets)
		{
			int x = barX + barWidth * offset / TICK_LENGTH;
			graphics.fillRect(x, barY, 1, barHeight);
		}

		return null;
	}

	void recordClick(int offset)
	{
		clickOffsets.add(offset);
	}

	void newTick()
	{
		clickOffsets.clear();
	}

	void setVisible(boolean visible)
	{
		this.visible = visible;
	}

	boolean isVisible()
	{
		return visible;
	}

	void onTick()
	{
		final Player localPlayer = client.getLocalPlayer();
		showingPrayerBar = localPlayer != null;
	}
}
