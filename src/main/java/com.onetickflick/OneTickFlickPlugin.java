package com.onetickflick;

import com.google.inject.Provides;

import javax.inject.Inject;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@PluginDescriptor(
		name = "1 Tick Flick",
		description = "Visual tick bar and combo counter for 1-tick prayer flicking with the quick prayer orb",
		tags = {"1", "one", "tick", "flick", "prayer", "metronome", "visual", "quick", "orb"}
)
public class OneTickFlickPlugin extends Plugin
{
	private static final int TICK_LENGTH = 600;

	@Inject
	private OverlayManager overlayManager;
	@Inject
	private OneTickFlickOverlay overlay;
	@Inject
	private PrayerBarOverlay prayerBarOverlay;
	@Inject
	private SipsSavedOverlay sipsSavedOverlay;
	@Inject
	private OneTickFlickConfig config;
	@Inject
	private Client client;
	@Inject
	private SipsSavedTracker sipsSavedTracker;
	@Inject
	private ConfigManager configManager;
	@Inject
	private ClientThread clientThread;

	private long lastTickTime;
	private long lastInteraction;
	private final List<Integer> currentTickClicks = new CopyOnWriteArrayList<>(); // A list of times the quick prayer orb was clicked, in milliseconds since the last onGameTick.
	private final List<Integer> nextTickClicks = new CopyOnWriteArrayList<>(); // Only used if the click delay config option causes the click to fall into the next tick.

	@Getter(AccessLevel.PACKAGE)
	private int combo;

	@Override
	protected void startUp()
	{
		lastTickTime = System.currentTimeMillis();
		lastInteraction = lastTickTime;
		combo = 0;
		sipsSavedTracker.load();
		clientThread.invoke(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				sipsSavedTracker.refreshAll();
			}
			sipsSavedOverlay.refresh();
		});

		overlay.setVisible(true);
		sipsSavedOverlay.setVisible(true);
		prayerBarOverlay.setVisible(true);
		overlay.setShowOverlayPrayerBar(config.showOverlayPrayerBar());
		overlayManager.add(overlay);
		overlayManager.add(prayerBarOverlay);
		overlayManager.add(sipsSavedOverlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		overlayManager.remove(prayerBarOverlay);
		overlayManager.remove(sipsSavedOverlay);
		sipsSavedTracker.endSession();
		sipsSavedTracker.save();
		currentTickClicks.clear();
		nextTickClicks.clear();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals("onetickflick"))
		{
			return;
		}

		switch (event.getKey())
		{
			case "showOverlayPrayerBar":
				overlay.setShowOverlayPrayerBar(config.showOverlayPrayerBar());
				break;
			case "greenStart":
				overlay.setGreenStart(config.greenStart());
				break;
			case "greenEnd":
				overlay.setGreenEnd(config.greenEnd());
				break;
			case "showCombo":
				overlay.setShowCombo(config.showCombo());
				break;
			case "comboCounterText":
				overlay.setComboCounterText(config.comboCounterText());
				break;
			case "targetZoneColor":
				overlay.setTargetZoneColor(config.targetZoneColor());
				break;
			case "backgroundColor":
				overlay.setBackgroundColor(config.backgroundColor());
				break;
			case "clickColor":
				overlay.setClickColor(config.clickColor());
				break;
			case "swipeLineColor":
				overlay.setSwipeLineColor(config.swipeLineColor());
				break;
			case "borderColor":
				overlay.setBorderColor(config.borderColor());
				break;
			case "comboTextColor":
				overlay.setComboTextColor(config.comboTextColor());
				sipsSavedOverlay.setTextColor(config.comboTextColor());
				break;
			case "swipeLineWidth":
				overlay.setSwipeLineWidth(config.swipeLineWidth());
				break;
			case "showSessionSipsSaved":
				sipsSavedOverlay.setShowSessionSips(config.showSessionSipsSaved());
				clientThread.invoke(sipsSavedOverlay::refresh);
				break;
			case "showSessionGpSaved":
				sipsSavedOverlay.setShowSessionGp(config.showSessionGpSaved());
				clientThread.invoke(sipsSavedOverlay::refresh);
				break;
			case "showLifetimeSipsSaved":
				sipsSavedOverlay.setShowLifetimeSips(config.showLifetimeSipsSaved());
				clientThread.invoke(sipsSavedOverlay::refresh);
				break;
			case "showLifetimeGpSaved":
				sipsSavedOverlay.setShowLifetimeGp(config.showLifetimeGpSaved());
				clientThread.invoke(sipsSavedOverlay::refresh);
				break;
			case "restoreItem":
				sipsSavedOverlay.setRestoreItem(config.restoreItem());
				clientThread.invoke(sipsSavedOverlay::refresh);
				break;
			case "resetLifetimeSipsSaved":
				if (config.resetLifetimeSipsSaved())
				{
					SwingUtilities.invokeLater(this::confirmResetLifetime);
				}
				break;
			case "prayerBarFillColor":
				prayerBarOverlay.setBarFillColor(config.prayerBarFillColor());
				break;
			case "prayerBarBackgroundColor":
				prayerBarOverlay.setBarBackgroundColor(config.prayerBarBackgroundColor());
				break;
			case "prayerBarSwipeLineColor":
				prayerBarOverlay.setSwipeLineColor(config.prayerBarSwipeLineColor());
				break;
			case "prayerBarClickLineColor":
				prayerBarOverlay.setClickLineColor(config.prayerBarClickLineColor());
				break;
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if (currentTickClicks.size() == 2 && currentTickClicks.stream().allMatch(this::inGreenZone))
		{
			combo++;
			sipsSavedTracker.recordFlickTick();
		}
		else
		{
			if (combo > 0)
			{
				sipsSavedTracker.endSession();
			}
			combo = 0;
		}

		currentTickClicks.clear();
		overlay.newTick();
		prayerBarOverlay.newTick();
		prayerBarOverlay.onTick();
		lastTickTime = System.currentTimeMillis();

		if (!nextTickClicks.isEmpty())
		{
			for (Integer offset : nextTickClicks)
			{
				currentTickClicks.add(offset);
				overlay.recordClick(offset);
				prayerBarOverlay.recordClick(offset);
			}
			nextTickClicks.clear();
		}

		applyTimeoutVisibility();
		sipsSavedOverlay.refresh();
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		int id = event.getContainerId();
		if (id == SipsSavedTracker.INVENTORY_CONTAINER_ID || id == SipsSavedTracker.WORN_CONTAINER_ID)
		{
			sipsSavedTracker.onItemContainerChanged(id, event.getItemContainer());
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (event.getSkill() == Skill.PRAYER)
		{
			sipsSavedTracker.onPrayerChanged(event.getLevel(), event.getBoostedLevel());
		}
	}

	private void confirmResetLifetime()
	{
		// Untick the box straight away, so it acts like a button.
		configManager.setConfiguration("onetickflick", "resetLifetimeSipsSaved", false);

		int result = JOptionPane.showConfirmDialog(
				client.getCanvas(),
				"Reset lifetime sips and GP saved for this account?\nThis cannot be undone.",
				"1 Tick Flick",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE);

		if (result == JOptionPane.YES_OPTION)
		{
			clientThread.invoke(() ->
			{
				sipsSavedTracker.resetLifetime();
				sipsSavedOverlay.refresh();
			});
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING)
		{
			combo = 0;
			sipsSavedTracker.endSession();
			sipsSavedTracker.save();
		}
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		// Lifetime totals are stored per account, so reload them when the account changes.
		sipsSavedTracker.load();
		sipsSavedOverlay.refresh();
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked e)
	{
		Widget widget = e.getWidget();

		if (widget == null
				|| (widget.getId() != InterfaceID.Orbs.PRAYERBUTTON
				&& widget.getId() != InterfaceID.OrbsNomap.PRAYERBUTTON
				&& !(config.detectPrayerBookClicks() && isPrayerBookWidget(widget.getId()))))
		{
			return;
		}

		if (e.getMenuAction() != MenuAction.CC_OP && e.getMenuAction() != MenuAction.CC_OP_LOW_PRIORITY)
		{
			return;
		}

		int offset = (int) (System.currentTimeMillis() - lastTickTime);
		offset += config.clickDelayMilliseconds();

		if (offset >= TICK_LENGTH)
		{
			// If the click is past the current tick, we add it for the next tick instead. It will be added to the overlay on the next onGameTick.
			nextTickClicks.add(offset - TICK_LENGTH);
		}
		else
		{
			currentTickClicks.add(offset);
			overlay.recordClick(offset);
			prayerBarOverlay.recordClick(offset);
		}

		lastInteraction = System.currentTimeMillis();
		if (!overlay.isVisible() || !prayerBarOverlay.isVisible())
		{
			overlay.setVisible(true);
			sipsSavedOverlay.setVisible(true);
			prayerBarOverlay.setVisible(true);
		}
	}

	private void applyTimeoutVisibility()
	{
		if (config.enableTimeout())
		{
			if (!overlay.isVisible() && !prayerBarOverlay.isVisible())
			{
				return;
			}

			long elapsed = System.currentTimeMillis() - lastInteraction;
			boolean timedOut = elapsed > config.overlayTimeoutSeconds() * 1000L;
			boolean prayerActive = client.getServerVarbitValue(VarbitID.PRAYER_ALLACTIVE) != 0;

			if (timedOut && (config.timeoutWhilePrayerActive() || !prayerActive))
			{
				overlay.setVisible(false);
				sipsSavedOverlay.setVisible(false);
				prayerBarOverlay.setVisible(false);
			}
		}
		else
		{
			if (!overlay.isVisible())
			{
				overlay.setVisible(true);
				sipsSavedOverlay.setVisible(true);
			}
			if (!prayerBarOverlay.isVisible())
			{
				prayerBarOverlay.setVisible(true);
			}
		}
	}

	/**
	 * Returns the number of milliseconds since the last tick.
	 */
	long millisSinceTick()
	{
		return System.currentTimeMillis() - lastTickTime;
	}

	/**
	 * Checks if the given milliseconds are within the green zone defined by the config.
	 *
	 * @param ms
	 * @return
	 */
	private boolean inGreenZone(int ms)
	{
		return ms >= config.greenStart() && ms <= config.greenEnd();
	}

	@Provides
	OneTickFlickConfig provideConfig(ConfigManager cm)
	{
		return cm.getConfig(OneTickFlickConfig.class);
	}

	/**
	 * Checks if the given widget ID is a prayer book widget.
	 *
	 * @param widgetId
	 * @return
	 */
	private static boolean isPrayerBookWidget(int widgetId)
	{
		return widgetId >= InterfaceID.Prayerbook.PRAYER1 && widgetId <= InterfaceID.Prayerbook.PRAYER30;
	}
}
