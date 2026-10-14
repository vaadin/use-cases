package com.example.common;

/**
 * A {@link BaseMainLayout} for the views in {@code com.example.common.layout}
 * that sends feedback to whichever {@link SlackFeedback} a test sets.
 */
public class TestMainLayout extends BaseMainLayout {

    static SlackFeedback slackFeedback = new SlackFeedback(null);

    public TestMainLayout() {
        super("signals", "Signal API", slackFeedback);
    }
}
