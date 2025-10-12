package vn.edu.usth.irc;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.VH> {
    private final List<String> data;
    public MemberAdapter(List<String> data){ this.data=data; }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
        return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_member, p, false));
    }
    @Override public void onBindViewHolder(@NonNull VH h, int i) { h.name.setText(data.get(i)); }
    @Override public int getItemCount() { return data.size(); }
    static class VH extends RecyclerView.ViewHolder {
        TextView name;
        VH(@NonNull View v){ super(v); name = v.findViewById(R.id.tvName); }
    }
}
