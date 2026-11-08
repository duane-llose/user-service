package com.vinsguru.netflux.user.service;

import com.vinsguru.netflux.user.entity.Watchlist;
import com.vinsguru.netflux.user.repository.WatchlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;

    public WatchlistService(WatchlistRepository watchlistRepository) {
        this.watchlistRepository = watchlistRepository;
    }

    public List<Long> getWatchlist(Long userId) {
        return watchlistRepository.findByUserIdOrderByIdAsc(userId)
                .stream()
                .map(Watchlist::getMovieId)
                .toList();
    }

    @Transactional
    public void add(Long userId, Long movieId) {
        watchlistRepository.addIfAbsent(userId, movieId);
    }

    @Transactional
    public void remove(Long userId, Long movieId) {
        watchlistRepository.deleteByUserIdAndMovieId(userId, movieId);
    }
}
