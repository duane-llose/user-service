package com.vinsguru.netflux.user.controller;

import com.vinsguru.netflux.user.dto.AddToWatchlistRequest;
import com.vinsguru.netflux.user.service.WatchlistService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users/watchlist")
public class WatchlistController {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @GetMapping
    public List<Long> getWatchlist(@RequestHeader(USER_ID_HEADER) Long userId) {
        return watchlistService.getWatchlist(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void add(@RequestHeader(USER_ID_HEADER) Long userId,
                    @Valid @RequestBody AddToWatchlistRequest request) {
        watchlistService.add(userId, request.movieId());
    }

    @DeleteMapping("/{movieId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@RequestHeader(USER_ID_HEADER) Long userId,
                       @PathVariable @Positive Long movieId) {
        watchlistService.remove(userId, movieId);
    }
}
