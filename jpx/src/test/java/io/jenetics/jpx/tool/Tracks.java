package io.jenetics.jpx.tool;

import static java.lang.String.format;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import io.jenetics.jpx.Track;
import io.jenetics.jpx.TrackSegment;
import io.jenetics.jpx.WayPoint;

final class Tracks {
	private Tracks() {
	}

	public static Collector<WayPoint, ?, Optional<Track>>
	toTrack(final Duration gap, final int minSegmentSize) {
		return Collectors.collectingAndThen(
			Collectors.toList(),
			points -> toTrack(points, gap, minSegmentSize)
		);
	}

	private static Optional<Track> toTrack(
		final List<WayPoint> points,
		final Duration gap,
		final int minSegmentSize
	) {
		if (points.size() < minSegmentSize) {
			return Optional.empty();
		}

		final Track.Builder track = Track.builder()
			.number(1)
			.name("Track 1")
			.cmt(trackCmt(points));

		ZonedDateTime last = zonedDateTime(points.get(0));
		TrackSegment.Builder segment = TrackSegment.builder();

		for (final WayPoint point : points) {
			final ZonedDateTime zdt = zonedDateTime(point);

			if (last.plusNanos(gap.toNanos()).isAfter(zdt)) {
				segment.addPoint(point);
			} else {
				if (segment.points().size() >= minSegmentSize) {
					track.addSegment(segment.build());
				}
				segment = TrackSegment.builder();
			}

			last = zdt;
		}

		if (segment.points().size() >= minSegmentSize) {
			track.addSegment(segment.build());
		}

		track.desc(format(
			"%d segments; %d track points",
			track.segments().size(),
			track.segments().stream()
				.flatMap(TrackSegment::points)
				.count()
		));

		return track.segments().isEmpty()
			? Optional.empty()
			: Optional.of(track.build());
	}

	private static ZonedDateTime zonedDateTime(final WayPoint wp) {
		return wp
			.getTime()
			.orElse(ZonedDateTime.of(LocalDateTime.MAX, ZoneId.systemDefault()));
	}

	private static String trackCmt(final List<WayPoint> points) {
		final OffsetDateTime start = points.get(0).getTime()
			.map(ZonedDateTime::toOffsetDateTime)
			.orElse(OffsetDateTime.now());

		final OffsetDateTime end = points.get(points.size() - 1).getTime()
			.map(ZonedDateTime::toOffsetDateTime)
			.orElse(OffsetDateTime.now());

		return format(
			"Track[start=%s, end=%s, duration=%s]",
			start, end, Duration.between(start, end)
		);
	}

}