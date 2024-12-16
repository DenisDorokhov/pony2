package net.dorokhov.pony2.core.library.service;

import net.dorokhov.pony2.api.library.service.PlaylistService;
import net.dorokhov.pony2.api.user.domain.UserDeletingEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PlaylistUserDeletedListener implements ApplicationListener<UserDeletingEvent> {

    private final PlaylistService playlistService;

    public PlaylistUserDeletedListener(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    @Transactional
    @Override
    public void onApplicationEvent(UserDeletingEvent event) {
        playlistService.deleteByUserId(event.getUserId());
    }
}
