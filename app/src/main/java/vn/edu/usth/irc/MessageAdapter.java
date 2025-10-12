package vn.edu.usth.irc.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import vn.edu.usth.irc.R;
import vn.edu.usth.irc.model.Message;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<Message> data = new ArrayList<>();

    public MessageAdapter(List<Message> init) {
        if (init != null) data.addAll(init);
    }

    @Override
    public int getItemViewType(int position) {
        return data.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());

        if (viewType == Message.LEFT) {
            View v = inf.inflate(R.layout.item_message_left, parent, false);
            return new LeftVH(v);
        } else if (viewType == Message.RIGHT) {
            View v = inf.inflate(R.layout.item_message_right, parent, false);
            return new RightVH(v);
        } else {
            View v = inf.inflate(R.layout.item_message_left, parent, false);
            return new SystemVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message m = data.get(position);
        TextView tv = holder.itemView.findViewById(R.id.tvText);
        tv.setText(m.getText());
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void addRightMessage(String text) {
        data.add(Message.right(text));
        notifyItemInserted(data.size() - 1);
    }

    static class LeftVH extends RecyclerView.ViewHolder { LeftVH(@NonNull View itemView){ super(itemView); } }
    static class RightVH extends RecyclerView.ViewHolder { RightVH(@NonNull View itemView){ super(itemView); } }
    static class SystemVH extends RecyclerView.ViewHolder { SystemVH(@NonNull View itemView){ super(itemView); } }
}
