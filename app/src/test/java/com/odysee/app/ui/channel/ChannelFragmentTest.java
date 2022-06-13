package com.odysee.app.ui.channel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ChannelFragmentTest {
    @Test
    public void tabFromPosition() {
        List<Integer> positions = Arrays.asList(0, 1, 2, 3);
        List<Integer> expected = Arrays.asList(ChannelFragment.CONTENT,
                ChannelFragment.PLAYLISTS,
                ChannelFragment.ABOUT,
                ChannelFragment.COMMENTS);
        List<Integer> obtained = positions
                .stream()
                .map(pos -> ChannelFragment.tabFromPosition(pos, false))
                .collect(Collectors.toList());
        assertEquals(expected, obtained);
    }

    @Test
    public void tabFromPositionWithLivestreams() {
        List<Integer> positions = Arrays.asList(0, 1, 2, 3, 4);
        List<Integer> expected = Arrays.asList(ChannelFragment.CONTENT,
                ChannelFragment.SCHEDULED_LIVESTREAMS,
                ChannelFragment.PLAYLISTS,
                ChannelFragment.ABOUT,
                ChannelFragment.COMMENTS);
        List<Integer> obtained = positions
                .stream()
                .map(pos -> ChannelFragment.tabFromPosition(pos, true))
                .collect(Collectors.toList());
        assertEquals(expected, obtained);
    }

    @Test
    public void tabFromPositionInvalidValue() {
        assertEquals(-1, ChannelFragment.tabFromPosition(4, false));
    }

    @Test
    public void tabFromPositionInvalidValueWithLivestreams() {
        assertEquals(-1, ChannelFragment.tabFromPosition(5, true));
    }

    @Test
    public void positionFromTab() {
        List<Integer> tabs = Arrays.asList(ChannelFragment.CONTENT,
                ChannelFragment.PLAYLISTS,
                ChannelFragment.ABOUT,
                ChannelFragment.COMMENTS);
        List<Integer> expected = Arrays.asList(0, 1, 2, 3);
        List<Integer> obtained = tabs.stream()
                .map((tab) -> ChannelFragment.positionFromTab(tab, false))
                .collect(Collectors.toList());
        assertEquals(expected, obtained);
    }

    @Test
    public void positionFromTabWithLivestreams() {
        List<Integer> tabs = Arrays.asList(ChannelFragment.CONTENT,
                ChannelFragment.SCHEDULED_LIVESTREAMS,
                ChannelFragment.PLAYLISTS,
                ChannelFragment.ABOUT,
                ChannelFragment.COMMENTS);
        List<Integer> expected = Arrays.asList(0, 1, 2, 3, 4);
        List<Integer> obtained = tabs.stream()
                .map((tab) -> ChannelFragment.positionFromTab(tab, true))
                .collect(Collectors.toList());
        assertEquals(expected, obtained);
    }

    @Test
    public void positionFromTabInvalidValue() {
        assertEquals(-1, ChannelFragment.positionFromTab(ChannelFragment.SCHEDULED_LIVESTREAMS, false));
    }
}
