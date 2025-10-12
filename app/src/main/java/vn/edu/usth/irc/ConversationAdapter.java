package vn.edu.usth.irc;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.VH> {
    public interface OnClick { void open(Conversation c); }
    private final List<Conversation> data;
    private final OnClick onClick;

    public ConversationAdapter(List<Conversation> data, OnClick onClick){
        this.data = data; this.onClick = onClick;
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int v){
        View view = LayoutInflater.from(p.getContext()).inflate(R.layout.item_conversation, p, false);
        return new VH(view);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int i){
        Conversation c = data.get(i);
        h.title.setText(c.title);
        h.subtitle.setText(c.last);
        h.time.setText(c.time);
        h.itemView.setOnClickListener(v -> onClick.open(c));
    }

    @Override public int getItemCount(){ return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, subtitle, time;
        VH(@NonNull View v){
            super(v);
            title = v.findViewById(R.id.tvTitle);
            subtitle = v.findViewById(R.id.tvSubtitle);
            time = v.findViewById(R.id.tvTime);
        }
    }
}
