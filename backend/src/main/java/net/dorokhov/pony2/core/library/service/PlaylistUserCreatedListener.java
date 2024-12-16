package net.dorokhov.pony2.core.library.service;

import net.dorokhov.pony2.api.library.domain.Playlist;
import net.dorokhov.pony2.api.library.service.PlaylistService;
import net.dorokhov.pony2.api.user.domain.User;
import net.dorokhov.pony2.api.user.domain.UserCreatedEvent;
import net.dorokhov.pony2.api.user.service.UserService;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PlaylistUserCreatedListener implements ApplicationListener<UserCreatedEvent> {

    private final UserService userService;
    private final PlaylistService playlistService;

    public PlaylistUserCreatedListener(
            UserService userService,
            PlaylistService playlistService
    ) {
        this.userService = userService;
        this.playlistService = playlistService;
    }

    @Transactional
    @Override
    public void onApplicationEvent(UserCreatedEvent event) {
        User user = userService.getById(event.getUserId()).orElseThrow();
        playlistService.save(new Playlist()
                .setName(Playlist.Type.HISTORY.name())
                .setType(Playlist.Type.HISTORY)
                .setUser(user));
        playlistService.save(new Playlist()
                .setName(Playlist.Type.LIKE.name())
                .setType(Playlist.Type.LIKE)
                .setUser(user));
    }
}
