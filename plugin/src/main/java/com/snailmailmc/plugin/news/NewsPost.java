package com.snailmailmc.plugin.news;

import java.time.Instant;

/** One post on the town news board. */
public record NewsPost(long id, String title, String body, String author, Instant postedAt) {
}
