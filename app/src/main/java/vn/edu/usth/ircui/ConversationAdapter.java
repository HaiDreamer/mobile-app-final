package vn.edu.usth.ircui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

// RecyclerView.Adapter that renders a scrolling list of Conversation items
public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.VH> {
        public interface OnClick { void open(Conversation c); }
    private final List<Conversation> data;
    private final OnClick onClick;

    public ConversationAdapter(List<Conversation> data, OnClick onClick){
        this.data = data; this.onClick = onClick;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position){
        Conversation c = data.get(position);
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
