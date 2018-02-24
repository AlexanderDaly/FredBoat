/*
 * MIT License
 *
 * Copyright (c) 2017 Frederik Ar. Mikkelsen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package fredboat.audio.player;

import fredboat.main.Launcher;
import net.dv8tion.jda.core.entities.Guild;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PlayerRegistry {

    public static final float DEFAULT_VOLUME = 1f;

    private final Map<Long, GuildPlayer> REGISTRY = new ConcurrentHashMap<>();

    //internal holder pattern
    private static PlayerRegistry instance() {
        return RegistryHolder.INSTANCE;
    }

    private static class RegistryHolder {
        private static final PlayerRegistry INSTANCE = new PlayerRegistry();
    }

    @Nonnull
    public static GuildPlayer getOrCreate(@Nonnull Guild guild) {
        GuildPlayer player = instance().REGISTRY.computeIfAbsent(
                guild.getIdLong(), guildId -> {
                    GuildPlayer p = new GuildPlayer(guild);
                    p.setVolume(DEFAULT_VOLUME);
                    return p;
                });

        // Attempt to set the player as a sending handler. Important after a shard revive
        if (!Launcher.getBotController().getLavalinkManager().isEnabled()) {
            guild.getAudioManager().setSendingHandler(player);
        }

        return player;
    }

    @Nullable
    public static GuildPlayer getExisting(@Nonnull Guild guild) {
        return getExisting(guild.getIdLong());
    }

    @Nullable
    public static GuildPlayer getExisting(long guildId) {
        return instance().REGISTRY.get(guildId);
    }

    public static Map<Long, GuildPlayer> getRegistry() {
        return instance().REGISTRY;

    }

    public static List<GuildPlayer> getPlayingPlayers() {
        return instance().REGISTRY.values().stream()
                .filter(GuildPlayer::isPlaying)
                .collect(Collectors.toList());
    }

    public static void destroyPlayer(Guild g) {
        destroyPlayer(g.getIdLong());
    }

    public static void destroyPlayer(long guildId) {
        GuildPlayer player = getExisting(guildId);
        if (player != null) {
            player.destroy();
            instance().REGISTRY.remove(guildId);
        }
    }

    public static long playingCount() {
        return instance().REGISTRY.values().stream()
                .filter(GuildPlayer::isPlaying)
                .count();
    }
}
