package com.grinderwolf.swm.api.events;

import com.grinderwolf.swm.api.loaders.SlimeLoader;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class AsyncPreGetWorldEvent extends Event implements Cancellable {

  private static final HandlerList handlers = new HandlerList();
  private boolean isCancelled;
  private SlimeLoader slimeLoader;
  private String worldName;

  public AsyncPreGetWorldEvent(SlimeLoader slimeLoader, String worldName) {
    super(true);
    this.slimeLoader = Objects.requireNonNull(slimeLoader, "slimeLoader cannot be null");
    this.worldName = Objects.requireNonNull(worldName, "worldName cannot be null");
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return handlers;
  }

  @Override
  public boolean isCancelled() {
    return this.isCancelled;
  }

  @Override
  public void setCancelled(boolean cancelled) {
    this.isCancelled = cancelled;
  }

  public SlimeLoader getSlimeLoader() {
    return this.slimeLoader;
  }

  public void setSlimeLoader(SlimeLoader slimeLoader) {
    this.slimeLoader = Objects.requireNonNull(slimeLoader, "slimeLoader cannot be null");
  }

  public String getWorldName() {
    return this.worldName;
  }

  public void setWorldName(String worldName) {
    this.worldName = Objects.requireNonNull(worldName, "worldName cannot be null");
  }
}
