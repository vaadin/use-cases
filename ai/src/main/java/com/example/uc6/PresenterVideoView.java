package com.example.uc6;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

import com.example.ai.AiProvider;
import com.example.ai.AiProviders;
import com.example.ai.ProviderNotice;
import com.example.common.UseCaseDescription;
import com.example.uc6.HeyGenClient.VideoStatus;
import com.example.uc6.PresenterVideoService.Presenter;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Video;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC6 — Presenter video.
 * <p>
 * Marketing, HR or support turn a short script into a video with a human-like
 * presenter, for example a weekly update or a how-to clip, without filming.
 * Rendering takes minutes, so it is a background job: the view asks HeyGen to
 * start the video, then polls its status every few seconds while the page is
 * open, using {@code whenAttached} so polling stops when the user navigates
 * away. Each poll writes the status to a signal, and the badge, the elapsed
 * time and finally the video player follow that signal.
 */
@Route(value = "uc6", layout = MainLayout.class)
@UseCaseDescription("Generating a presenter video from a script as a background job that takes minutes")
@Menu(order = 6, title = "UC6 — Presenter video")
@StyleSheet("uc6.css")
public class PresenterVideoView extends VerticalLayout {

    static final String SAMPLE_SCRIPT = "Hi everyone! Here is this week's "
            + "update: the new customer portal is live, support tickets are "
            + "down twelve percent, and Friday's all-hands starts at two. See "
            + "you there!";

    /**
     * A rendering job: the video's id, when it started, when its status was
     * last checked, that status, and the error of the last call, if any.
     */
    record Job(@Nullable String videoId, Instant startedAt, Instant checkedAt,
            @Nullable VideoStatus status, @Nullable String error) {

        boolean isRunning() {
            return videoId != null && error == null
                    && (status == null || !status.isFinished());
        }
    }

    private final ValueSignal<@Nullable Job> job = new ValueSignal<>(null);

    public PresenterVideoView(PresenterVideoService videos,
            AiProviders providers, TaskScheduler scheduler,
            @Qualifier("applicationTaskExecutor") TaskExecutor executor,
            @Value("${app.heygen.poll-interval}") Duration pollInterval) {
        addClassName("uc6-view");
        add(new H1("UC6 — Presenter video"));
        add(new Paragraph("Write a short script and pick a presenter. HeyGen "
                + "renders a video of the presenter speaking it. Rendering "
                + "takes a few minutes: keep this page open, and the status "
                + "below updates by itself until the video is ready."));

        ProviderNotice notice = ProviderNotice.forMissing(providers,
                AiProvider.HEYGEN);
        if (notice != null) {
            add(notice);
        }

        TextArea script = new TextArea("Script");
        script.setValue(SAMPLE_SCRIPT);
        script.setMaxLength(PresenterVideoService.MAX_CHARACTERS);
        script.setHelperText("Up to " + PresenterVideoService.MAX_CHARACTERS
                + " characters, about 20 seconds of video");
        script.setWidthFull();

        Select<Presenter> presenter = new Select<>();
        presenter.setLabel("Presenter");
        presenter.setItems(PresenterVideoService.PRESENTERS);
        presenter.setItemLabelGenerator(Presenter::label);
        presenter.setValue(PresenterVideoService.PRESENTERS.getFirst());
        presenter.setWidth("20em");

        Paragraph quota = new Paragraph();
        quota.addClassName("uc6-quota");

        Button generate = new Button("Generate video");
        generate.addThemeVariants(ButtonVariant.PRIMARY);
        generate.addClickListener(click -> {
            String text = script.getValue();
            Presenter chosen = presenter.getValue();
            Instant now = Instant.now();
            job.set(new Job(null, now, now, null, null));
            executor.execute(() -> {
                try {
                    job.set(new Job(videos.start(chosen, text), now,
                            Instant.now(), null, null));
                } catch (RuntimeException e) {
                    job.set(new Job(null, now, Instant.now(), null,
                            e.getMessage()));
                }
            });
        });

        Span badge = new Span();
        badge.addClassName("uc6-badge");
        Div result = new Div();
        result.addClassName("uc6-result");

        Signal.effect(this, () -> {
            Job current = job.get();
            boolean busy = current != null && current.error() == null
                    && (current.videoId() == null || current.isRunning());
            generate.setEnabled(notice == null && !busy);
            quota.setText(videos.quota().remaining() + " of "
                    + videos.quota().perDay()
                    + " videos left today, for all users of this demo.");
            badge.setVisible(current != null);
            if (current != null) {
                badge.setText(statusText(current));
                badge.getElement().setAttribute("data-status",
                        statusName(current));
            }
        });
        Signal.effect(this, () -> {
            result.removeAll();
            Job current = job.get();
            VideoStatus status = current == null ? null : current.status();
            if (status != null && status.videoUrl() != null) {
                Video video = new Video();
                video.setControlsVisible(true);
                video.addSource(status.videoUrl(), "video/mp4");
                if (status.thumbnailUrl() != null) {
                    video.setPoster(status.thumbnailUrl());
                }
                Anchor download = new Anchor(status.videoUrl(),
                        "Open the video file");
                download.setTarget("_blank");
                result.add(new H2("Your video"), video, download);
            }
        });

        // Poll only while the page is open. The job keeps rendering at HeyGen
        // either way, but the job lives in this view instance: a user who
        // comes back gets a new view that knows nothing about it.
        whenAttached(ui -> {
            ScheduledFuture<?> poll = scheduler
                    .scheduleAtFixedRate(() -> poll(videos), pollInterval);
            return () -> poll.cancel(false);
        });

        add(script, presenter, generate, quota, badge, result);
    }

    private void poll(PresenterVideoService videos) {
        Job current = job.peek();
        String videoId = current == null ? null : current.videoId();
        if (current == null || videoId == null || !current.isRunning()) {
            return;
        }
        try {
            VideoStatus status = videos.status(videoId);
            job.set(new Job(videoId, current.startedAt(), Instant.now(), status,
                    null));
        } catch (RuntimeException e) {
            job.set(new Job(videoId, current.startedAt(), Instant.now(),
                    current.status(),
                    "Could not check the video: " + e.getMessage()));
        }
    }

    static String statusName(Job job) {
        if (job.error() != null) {
            return "failed";
        }
        return job.status() == null ? "pending" : job.status().status();
    }

    static String statusText(Job job) {
        if (job.error() != null) {
            return job.error();
        }
        long seconds = Duration.between(job.startedAt(), job.checkedAt())
                .toSeconds();
        String elapsed = String.format("%d:%02d", seconds / 60, seconds % 60);
        VideoStatus status = job.status();
        if (job.videoId() == null) {
            return "Starting the video…";
        }
        if (status == null || "pending".equals(status.status())) {
            return "Waiting in HeyGen's queue · " + elapsed;
        }
        return switch (status.status()) {
        case "processing" -> "Rendering · " + elapsed;
        case "completed" -> "Ready after " + elapsed;
        case "failed" -> "Rendering failed: "
                + (status.failureMessage() == null ? "no reason given"
                        : status.failureMessage());
        default -> status.status() + " · " + elapsed;
        };
    }
}
