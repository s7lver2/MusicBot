/*
 * Copyright 2018 John Grosh <john.a.grosh@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jagrosh.jmusicbot.audio;

import com.dunctebot.sourcemanagers.DuncteBotSources;
import com.jagrosh.jmusicbot.Bot;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerRegistry;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.getyarn.GetyarnAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.nico.NicoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.clients.Android;
import dev.lavalink.youtube.clients.AndroidVr;
import dev.lavalink.youtube.clients.Ios;
import dev.lavalink.youtube.clients.Music;
import dev.lavalink.youtube.clients.Tv;
import dev.lavalink.youtube.clients.Web;
import net.dv8tion.jda.api.entities.Guild;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class PlayerManager extends DefaultAudioPlayerManager
{
    private final Bot bot;
    
    public PlayerManager(Bot bot)
    {
        this.bot = bot;
    }
    
    public void init()
    {
        TransformativeAudioSourceManager.createTransforms(bot.getConfig().getTransforms()).forEach(t -> registerSourceManager(t));

        String oauthToken = bot.getConfig().getYoutubeOAuthToken();
        boolean useOauth = oauthToken != null && !oauthToken.isEmpty();

        // Register an explicit client set. youtube-source's default web-based clients
        // (WEB/MWEB/TVHTML5) rely on deciphering YouTube's base.js "signature" function,
        // which the library currently cannot parse for YouTube's latest player script
        // ("must find sig function") - this breaks playback on every release/snapshot.
        // The iOS/Android clients return stream URLs that don't require the JS cipher,
        // so they keep playback working; TV is kept for authenticated (OAuth) playback,
        // and Music/Web for search, metadata and playlists.
        YoutubeAudioSourceManager yt = new YoutubeAudioSourceManager(true,
                new Music(), new Web(), new Ios(), new AndroidVr(), new Android(), new Tv());
        yt.setPlaylistPageCount(bot.getConfig().getMaxYTPlaylistPages());
        if (useOauth)
        {
            if (oauthToken.equalsIgnoreCase("GENERATE"))
                // First-time setup: trigger the device-code OAuth flow. The activation
                // URL, and then the refresh token once you authorize, are printed to the
                // logs. Copy that refresh token back into youtubeOAuthToken to persist it.
                yt.useOauth2(null, false);
            else
                // Use an existing refresh token (skip the interactive flow).
                yt.useOauth2(oauthToken, true);
        }
        registerSourceManager(yt);

        registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        registerSourceManager(new BandcampAudioSourceManager());
        registerSourceManager(new VimeoAudioSourceManager());
        registerSourceManager(new TwitchStreamAudioSourceManager());
        registerSourceManager(new BeamAudioSourceManager());
        registerSourceManager(new GetyarnAudioSourceManager());
        registerSourceManager(new NicoAudioSourceManager());
        registerSourceManager(new HttpAudioSourceManager(MediaContainerRegistry.DEFAULT_REGISTRY));

        AudioSourceManagers.registerLocalSource(this);

        DuncteBotSources.registerAll(this, "en-US");
    }
    
    public Bot getBot()
    {
        return bot;
    }
    
    public boolean hasHandler(Guild guild)
    {
        return guild.getAudioManager().getSendingHandler()!=null;
    }
    
    public AudioHandler setUpHandler(Guild guild)
    {
        AudioHandler handler;
        if(guild.getAudioManager().getSendingHandler()==null)
        {
            AudioPlayer player = createPlayer();
            player.setVolume(bot.getSettingsManager().getSettings(guild).getVolume());
            handler = new AudioHandler(this, guild, player);
            player.addListener(handler);
            guild.getAudioManager().setSendingHandler(handler);
        }
        else
            handler = (AudioHandler) guild.getAudioManager().getSendingHandler();
        return handler;
    }
}
