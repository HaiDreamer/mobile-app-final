package vn.edu.usth.ircui.feature_chat.ui;

import android.content.Intent;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import vn.edu.usth.ircui.R;
import vn.edu.usth.ircui.feature_chat.data.Attachment;

public class DirectMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int T_OTHER_TEXT = 0;
    private static final int T_ME_TEXT    = 1;
    private static final int T_OTHER_IMG  = 2;
    private static final int T_ME_IMG     = 3;
    private static final int T_OTHER_FILE = 4;
    private static final int T_ME_FILE    = 5;

    static class Row {
        int type;
        boolean mine;
        String username;
        String text;
        Attachment attachment;
        Row(int type, boolean mine, String username, String text, Attachment a) {
            this.type = type; this.mine = mine; this.username = username; this.text = text; this.attachment = a;
        }
    }

    private final List<Row> rows = new ArrayList<>();
    private final String me;

    public DirectMessageAdapter(String me) { this.me = me; }

    public void addText(boolean mine, String username, String text) {
        rows.add(new Row(mine ? T_ME_TEXT : T_OTHER_TEXT, mine, username, text, null));
        notifyItemInserted(rows.size() - 1);
    }

    public void addAttachment(Attachment.Type type, boolean mine, String username,
                              android.net.Uri uri, String name, long size) {
        Attachment a = new Attachment(type, uri, name, size);
        int t = mine
                ? (type == Attachment.Type.IMAGE ? T_ME_IMG : T_ME_FILE)
                : (type == Attachment.Type.IMAGE ? T_OTHER_IMG : T_OTHER_FILE);
        rows.add(new Row(t, mine, username, null, a));
        notifyItemInserted(rows.size() - 1);
    }

    @Override public int getItemCount() { return rows.size(); }
    @Override public int getItemViewType(int position) { return rows.get(position).type; }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case T_ME_TEXT:    return new TextHolder(inf.inflate(R.layout.item_dm_text_me, parent, false));
            case T_OTHER_TEXT: return new TextHolder(inf.inflate(R.layout.item_dm_text_other, parent, false));
            case T_ME_IMG:     return new ImageHolder(inf.inflate(R.layout.item_dm_image_me, parent, false));
            case T_OTHER_IMG:  return new ImageHolder(inf.inflate(R.layout.item_dm_image_other, parent, false));
            case T_ME_FILE:    return new FileHolder(inf.inflate(R.layout.item_dm_file_me, parent, false));
            default:           return new FileHolder(inf.inflate(R.layout.item_dm_file_other, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row r = rows.get(position);

        if (holder instanceof TextHolder) {
            TextHolder th = (TextHolder) holder;
            th.username.setText(r.mine ? me : r.username);
            th.content.setText(r.text);
        } else if (holder instanceof ImageHolder) {
            ImageHolder ih = (ImageHolder) holder;
            ih.username.setText(r.mine ? me : r.username);
            ih.image.setImageURI(r.attachment.getUri());
            ih.image.setOnClickListener(v -> {
                Intent viewIntent = new Intent(Intent.ACTION_VIEW)
                        .setDataAndType(r.attachment.getUri(), "image/*")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                v.getContext().startActivity(viewIntent);
            });
        } else if (holder instanceof FileHolder) {
            FileHolder fh = (FileHolder) holder;
            fh.username.setText(r.mine ? me : r.username);
            fh.name.setText(r.attachment.getDisplayName());
            String sizeTxt = r.attachment.getSizeBytes() >= 0
                    ? Formatter.formatShortFileSize(fh.itemView.getContext(), r.attachment.getSizeBytes())
                    : "Unknown size";
            fh.size.setText(sizeTxt);
            fh.icon.setOnClickListener(v -> {
                Intent viewIntent = new Intent(Intent.ACTION_VIEW)
                        .setData(r.attachment.getUri())
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                v.getContext().startActivity(viewIntent);
            });
        }
    }

    static class TextHolder extends RecyclerView.ViewHolder {
        TextView username, content;
        TextHolder(@NonNull View v) {
            super(v);
            username = v.findViewById(R.id.dmUser);
            content  = v.findViewById(R.id.dmText);
        }
    }

    static class ImageHolder extends RecyclerView.ViewHolder {
        TextView username;
        ImageView image;
        ImageHolder(@NonNull View v) {
            super(v);
            username = v.findViewById(R.id.dmUser);
            image    = v.findViewById(R.id.dmImage);
        }
    }

    static class FileHolder extends RecyclerView.ViewHolder {
        TextView username, name, size;
        ImageView icon;
        FileHolder(@NonNull View v) {
            super(v);
            username = v.findViewById(R.id.dmUser);
            name     = v.findViewById(R.id.dmFileName);
            size     = v.findViewById(R.id.dmFileSize);
            icon     = v.findViewById(R.id.dmFileIcon);
        }
    }
}
