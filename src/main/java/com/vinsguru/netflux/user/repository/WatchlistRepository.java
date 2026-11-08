package com.vinsguru.netflux.user.repository;

import com.vinsguru.netflux.user.entity.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {

    List<Watchlist> findByUserIdOrderByIdAsc(Long userId);

    @Modifying
    @Query(value = """
            INSERT INTO watchlist (user_id, movie_id) VALUES (:userId, :movieId)
            ON CONFLICT (user_id, movie_id) DO NOTHING
            """, nativeQuery = true)
    void addIfAbsent(Long userId, Long movieId);

    void deleteByUserIdAndMovieId(Long userId, Long movieId);
}
