package com.pr0gramm.app.ui.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.pr0gramm.app.AndroidUtility;
import com.pr0gramm.app.R;
import com.pr0gramm.app.Uris;
import com.pr0gramm.app.feed.FeedType;
import com.pr0gramm.app.services.InboxService;
import com.pr0gramm.app.ui.InboxType;
import com.pr0gramm.app.ui.MainActivity;
import com.pr0gramm.app.ui.MessageActionListener;
import com.pr0gramm.app.ui.dialogs.ReplyCommentDialogFragment;
import com.pr0gramm.app.ui.dialogs.WritePrivateMessageDialog;
import com.squareup.picasso.Picasso;

import org.joda.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import javax.inject.Inject;

import roboguice.fragment.RoboFragment;
import roboguice.inject.InjectView;

import static com.pr0gramm.app.ui.dialogs.ErrorDialogFragment.defaultOnError;
import static org.joda.time.Duration.standardMinutes;

/**
 */
public abstract class InboxFragment<T> extends RoboFragment {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected static final String ARG_INBOX_TYPE = "InboxFragment.inboxType";

    @Inject
    private InboxService inboxService;

    @Inject
    private Picasso picasso;

    @InjectView(R.id.messages)
    private RecyclerView messagesView;

    @InjectView(android.R.id.empty)
    private View viewNothingHere;

    @InjectView(R.id.busy_indicator)
    private View viewBusyIndicator;

    @InjectView(R.id.refresh)
    private SwipeRefreshLayout swipeRefreshLayout;

    private LoaderHelper<List<T>> loader;
    private Instant loadStartedTimestamp;

    public InboxFragment() {
        setRetainInstance(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.loader = newLoaderHelper();
        this.loader.reload();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_inbox, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        messagesView.setItemAnimator(null);
        messagesView.setLayoutManager(new LinearLayoutManager(getActivity()));

        swipeRefreshLayout.setOnRefreshListener(this::reloadInboxContent);
        swipeRefreshLayout.setColorSchemeResources(R.color.primary);

        showBusyIndicator();

        // load the messages
        loadStartedTimestamp = Instant.now();
        loader.load(this::onMessagesLoaded, defaultOnError(), this::hideBusyIndicator);
    }

    @Override
    public void onResume() {
        super.onResume();

        // reload if re-started after one minute
        if (loadStartedTimestamp.plus(standardMinutes(1)).isBeforeNow()) {
            loadStartedTimestamp = Instant.now();
            reloadInboxContent();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        loader.detach();
        AndroidUtility.uninjectViews(this);
    }

    private void reloadInboxContent() {
        loader.reload();
    }

    private void hideNothingHereIndicator() {
        if (hasView()) {
            viewNothingHere.setVisibility(View.GONE);
        }
    }

    private void showNothingHereIndicator() {
        if (hasView()) {
            viewNothingHere.setVisibility(View.VISIBLE);
            viewNothingHere.setAlpha(0);
            viewNothingHere.animate().alpha(1).start();
        }
    }

    private void showBusyIndicator() {
        if (hasView() && viewBusyIndicator != null) {
            viewBusyIndicator.setVisibility(View.VISIBLE);
        }
    }

    private void hideBusyIndicator() {
        if (hasView()) {
            if (viewBusyIndicator != null) {
                viewBusyIndicator.setVisibility(View.GONE);
                ViewParent parent = viewBusyIndicator.getParent();
                ((ViewGroup) parent).removeView(viewBusyIndicator);

                viewBusyIndicator = null;
            }

            swipeRefreshLayout.setRefreshing(false);
        }
    }

    protected abstract LoaderHelper<List<T>> newLoaderHelper();

    private boolean hasView() {
        return messagesView != null;
    }

    private void onMessagesLoaded(List<T> messages) {
        hideBusyIndicator();
        hideNothingHereIndicator();

        // replace previous adapter with new values
        displayMessages(messagesView, messages);

        if (messages.isEmpty())
            showNothingHereIndicator();
    }

    protected abstract void displayMessages(RecyclerView recyclerView, List<T> messages);

    protected InboxType getInboxType() {
        InboxType type = InboxType.ALL;
        Bundle args = getArguments();
        if (args != null) {
            type = InboxType.values()[args.getInt(ARG_INBOX_TYPE, InboxType.ALL.ordinal())];
        }

        return type;
    }

    protected InboxService getInboxService() {
        return inboxService;
    }

    protected final MessageActionListener actionListener = new MessageActionListener() {
        @Override
        public void onAnswerToPrivateMessage(int receiverId, String name) {
            DialogFragment dialog = WritePrivateMessageDialog.newInstance(receiverId, name);
            dialog.show(getFragmentManager(), null);
        }

        @Override
        public void onCommentClicked(long itemId, long commentId) {
            open(Uris.of(getActivity()).post(FeedType.NEW, itemId, commentId));
        }

        private void open(Uri uri) {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri, getActivity(), MainActivity.class);
            startActivity(intent);
        }

        @Override
        public void onAnswerToCommentClicked(long itemId, long commentId, String name) {

            DialogFragment dialog = ReplyCommentDialogFragment.newInstance(itemId, commentId, name);
            dialog.show(getFragmentManager(), null);
        }

        @Override
        public void onUserClicked(int userId, String username) {
            open(Uris.of(getActivity()).uploads(username));
        }
    };
}
