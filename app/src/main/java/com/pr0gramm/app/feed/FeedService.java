package com.pr0gramm.app.feed;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.inject.Singleton;
import com.pr0gramm.app.api.pr0gramm.Api;
import com.pr0gramm.app.api.pr0gramm.response.Feed;
import com.pr0gramm.app.api.pr0gramm.response.Post;

import java.util.Set;

import javax.inject.Inject;

import rx.Observable;

/**
 * Performs the actual request to get the items for a feed.
 */
@Singleton
public class FeedService {
    private final Api api;

    @Inject
    public FeedService(Api api) {
        this.api = api;
    }

    public Observable<Feed> getFeedItems(FeedFilter feedFilter, Set<ContentType> contentTypes, Optional<Long> start, Optional<Long> around) {
        return performRequest(feedFilter, start, contentTypes, Optional.<Long>absent(), around);
    }

    public Observable<Feed> getFeedItemsNewer(FeedFilter feedFilter, Set<ContentType> contentTypes, long start) {
        return performRequest(feedFilter, Optional.<Long>absent(), contentTypes, Optional.of(start), Optional.<Long>absent());
    }

    private Observable<Feed> performRequest(FeedFilter feedFilter,
                                            Optional<Long> older,
                                            Set<ContentType> contentTypes,
                                            Optional<Long> newer,
                                            Optional<Long> around) {

        // filter by feed-type
        Integer promoted = (feedFilter.getFeedType() == FeedType.PROMOTED) ? 1 : null;
        Integer following = (feedFilter.getFeedType() == FeedType.PREMIUM) ? 1 : null;

        int flags = ContentType.combine(contentTypes);
        String tags = feedFilter.getTags().orNull();
        String user = feedFilter.getUsername().orNull();

        // FIXME this is quite hacky right now.
        String likes = feedFilter.getLikes().orNull();
        Boolean self = Strings.isNullOrEmpty(likes) ? null : true;

        return api.itemsGet(promoted, following, older.orNull(),
                newer.orNull(), around.orNull(),
                flags, tags, likes, self, user);
    }

    public Observable<Post> loadPostDetails(long id) {
        return api.info(id);
    }
}
