package vn.edu.usth.irc;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class StoryAdapter extends RecyclerView.Adapter<StoryAdapter.VH> {
    private final List<String> names;
    public StoryAdapter(List<String> names){ this.names = names; }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
        return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_story, p, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int i) { h.name.setText(names.get(i)); }
    @Override public int getItemCount() { return names.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView name;
        VH(@NonNull View v){ super(v); name = v.findViewById(R.id.tvName); }
    }
}
