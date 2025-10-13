package vn.edu.usth.ircui;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import vn.edu.usth.ircui.feature_chat.data.Message;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_OTHER = 0;
    private static final int TYPE_ME    = 1;

    private final List<Message> data;
    private final String me;
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());

    private final int[] namePalette;
    private String currentFontSize = "medium";

    public MessageAdapter(List<Message> data, String currentUsername) {
        this.data = data;
        this.me   = currentUsername;
        namePalette = new int[]{
                R.color.userColor1, R.color.userColor2, R.color.userColor3, R.color.userColor4,
                R.color.userColor5, R.color.userColor6, R.color.userColor7, R.color.userColor8
        };
    }

    @Override
    public int getItemViewType(int position) {
        return data.get(position).isMine() ? TYPE_ME : TYPE_OTHER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_ME) {
            View v = inf.inflate(R.layout.item_message_me, parent, false);
            return new MeHolder(v);
        } else {
            View v = inf.inflate(R.layout.item_message_other, parent, false);
            return new OtherHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) {
        Message m = data.get(pos);
        String time = sdf.format(new Date(m.getTimestamp()));

        applyFontSize(h);

        if (h instanceof MeHolder) {
            MeHolder holder = (MeHolder) h;
            holder.username.setText(m.getUsername() + "  •  " + time);
            tintUsername(holder.username, m.getUsername());
            styleContent(holder.content, m);
        } else {
            OtherHolder holder = (OtherHolder) h;
            holder.username.setText(m.getUsername() + "  •  " + time);
            tintUsername(holder.username, m.getUsername());
            styleContent(holder.content, m);
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    private void tintUsername(TextView tv, String username) {
        int idx = Math.abs(username.hashCode()) % namePalette.length;
        @ColorInt int color = tv.getResources().getColor(namePalette[idx], null);
        tv.setTextColor(color);
    }

    private void styleContent(TextView tv, Message m) {
        if (m.isCodeBlock()) {
            // strip ``` if present, and render monospace bubble
            String t = m.getContent().trim();
            if (t.startsWith("```")) {
                t = t.replaceFirst("^```[a-zA-Z0-9]*\\n?", "")
                        .replaceFirst("\\n?```$", "");
            }
            tv.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
            tv.setBackgroundResource(m.isMine() ? R.drawable.bubble_code_me : R.drawable.bubble_code_other);
            tv.setText(t);
        } else {
            tv.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
            tv.setBackgroundResource(m.isMine() ? R.drawable.bubble_me : R.drawable.bubble_other);
            tv.setText(m.getContent());
        }
    }

    private void applyFontSize(RecyclerView.ViewHolder holder) {
        SharedPreferences prefs = holder.itemView.getContext().getSharedPreferences("app_settings", 0);
        String fontSize = prefs.getString("font_size", "medium");

        float textSizeSP = 14f;

        switch (fontSize) {
            case "small":
                textSizeSP = 12f;
                break;
            case "large":
                textSizeSP = 16f;
                break;
            case "medium":
            default:
                textSizeSP = 14f;
                break;
        }

        if (holder instanceof MeHolder) {
            MeHolder meHolder = (MeHolder) holder;
            meHolder.content.setTextSize(textSizeSP);
            meHolder.username.setTextSize(textSizeSP - 2f);
        } else if (holder instanceof OtherHolder) {
            OtherHolder otherHolder = (OtherHolder) holder;
            otherHolder.content.setTextSize(textSizeSP);
            otherHolder.username.setTextSize(textSizeSP - 2f);
        }
    }

    static class OtherHolder extends RecyclerView.ViewHolder {
        TextView username, content;
        OtherHolder(@NonNull View v) {
            super(v);
            username = v.findViewById(R.id.tvUsername);
            content  = v.findViewById(R.id.tvContent);
        }
    }

    static class MeHolder extends RecyclerView.ViewHolder {
        TextView username, content;
        MeHolder(@NonNull View v) {
            super(v);
            username = v.findViewById(R.id.tvUsername);
            content  = v.findViewById(R.id.tvContent);
        }
    }
}