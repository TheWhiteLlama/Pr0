package com.pr0gramm.app.ui;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.pr0gramm.app.AndroidUtility;
import com.pr0gramm.app.R;
import com.pr0gramm.app.api.pr0gramm.response.Message;
import com.pr0gramm.app.ui.views.SenderInfoView;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import roboguice.RoboGuice;

/**
 */
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    protected final List<Message> messages;
    private final Context context;
    private final Picasso picasso;
    private final MessageActionListener actionListener;
    private final int itemLayout;

    private final TextDrawable.IShapeBuilder textShapeBuilder;

    public MessageAdapter(Context context, List<Message> messages, MessageActionListener actionListener, int layout) {
        this.context = context;
        this.actionListener = actionListener;
        this.messages = new ArrayList<>(messages);
        this.picasso = RoboGuice.getInjector(context).getInstance(Picasso.class);
        this.itemLayout = layout;

        this.textShapeBuilder = TextDrawable.builder().beginConfig()
                .textColor(context.getResources().getColor(R.color.feed_background))
                .fontSize(AndroidUtility.dp(context, 24))
                .bold()
                .endConfig();

        setHasStableIds(true);
    }

    /**
     * Replace all the messages with the new messages from the given iterable.
     */
    public void setMessages(Iterable<Message> messages) {
        this.messages.clear();
        Iterables.addAll(this.messages, messages);
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        return messages.get(position).getId();
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(itemLayout, parent, false);
        return new MessageViewHolder(view);
    }

    @SuppressWarnings("CodeBlock2Expr")
    @Override
    public void onBindViewHolder(MessageViewHolder view, int position) {
        Message message = messages.get(position);

        // set the type. if we have an item, we  have a comment
        boolean isComment = message.getItemId() != 0;
        if (view.type != null) {
            view.type.setText(isComment ? context.getString(R.string.inbox_message_comment) : context.getString(R.string.inbox_message_private));
        }

        // the text of the message
        view.text.setText(message.getMessage());
        Linkify.addLinks(view.text, Linkify.WEB_URLS);

        // draw the image for this post
        if (isComment) {
            String url = "http://thumb.pr0gramm.com/" + message.getThumb();
            picasso.load(url).into(view.image);
        } else {
            picasso.cancelRequest(view.image);

            // set a colored drawable with the first two letters of the user
            view.image.setImageDrawable(makeSenderDrawable(message));
        }

        // sender info
        view.sender.setSenderName(message.getName(), message.getMark());
        view.sender.setPointsVisible(isComment);
        view.sender.setPoints(message.getScore());
        view.sender.setDate(message.getCreated());

        if (actionListener != null) {
            view.sender.setOnSenderClickedListener(v -> {
                actionListener.onUserClicked(message.getSenderId(), message.getName());
            });

            if (isComment) {
                view.sender.setAnswerClickedListener(v -> {
                    actionListener.onAnswerToCommentClicked(message.getItemId(), message.getId(), message.getName());
                });

                view.image.setOnClickListener(v -> {
                    actionListener.onCommentClicked(message.getItemId(), message.getId());
                });
            } else {
                view.sender.setAnswerClickedListener(v -> {
                    actionListener.onAnswerToPrivateMessage(message.getSenderId(), message.getName());
                });
                view.image.setOnClickListener(null);
            }
        }
    }

    private TextDrawable makeSenderDrawable(Message message) {
        String name = message.getName();

        StringBuilder text = new StringBuilder();
        text.append(Character.toUpperCase(name.charAt(0)));
        if(name.length() > 1) {
            text.append(Character.toLowerCase(name.charAt(1)));
        }

        int color = ColorGenerator.MATERIAL.getColor(message.getSenderId());
        return textShapeBuilder.buildRect(text.toString(), color);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    protected static class MessageViewHolder extends RecyclerView.ViewHolder {
        final TextView text;
        final TextView type;
        final ImageView image;
        final SenderInfoView sender;

        public MessageViewHolder(View itemView) {
            super(itemView);

            text = (TextView) itemView.findViewById(R.id.message_text);
            type = (TextView) itemView.findViewById(R.id.message_type);
            image = (ImageView) itemView.findViewById(R.id.message_image);
            sender = (SenderInfoView) itemView.findViewById(R.id.sender_info);
        }
    }
}
