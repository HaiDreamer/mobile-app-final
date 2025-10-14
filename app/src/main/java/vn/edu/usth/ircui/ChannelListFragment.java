package vn.edu.usth.ircui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Fragment hiển thị danh sách kênh IRC ở ngăn kéo trái.
 * MainActivity nên implement {@link OnChannelSelected} để nhận callback khi chọn kênh.
 *
 * Yêu cầu layout: res/layout/fragment_channel_list.xml
 *  - chứa RecyclerView id: @id/rvChannels
 */
public class ChannelListFragment extends Fragment {

    /** Callback chọn kênh */
    public interface OnChannelSelected {
        void onChannelSelected(String channelName);
    }

    private OnChannelSelected callback;
    private RecyclerView rv;
    private ChannelsAdapter adapter;

    /** Nguồn dữ liệu kênh (tạm thời mock). Có thể set từ ngoài qua {@link #setChannels(List)} */
    private final List<String> channels = new ArrayList<>(
            Arrays.asList("#general", "#chat", "#random")
    );

    public ChannelListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnChannelSelected) {
            callback = (OnChannelSelected) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_channel_list, container, false);

        rv = v.findViewById(R.id.rvChannels);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

        adapter = new ChannelsAdapter(channels, ch -> {
            if (callback != null) callback.onChannelSelected(ch);
        });
        rv.setAdapter(adapter);

        return v;
    }

    /** Cho phép activity/fragment khác cập nhật danh sách kênh động. */
    public void setChannels(@NonNull List<String> newChannels) {
        channels.clear();
        channels.addAll(newChannels);
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    /* ===================== Adapter nội bộ ===================== */

    private static class ChannelsAdapter extends RecyclerView.Adapter<ChannelsAdapter.VH> {
        interface Click { void onClick(String channel); }

        private final List<String> data;
        private final Click click;

        ChannelsAdapter(List<String> data, Click click) {
            this.data = data;
            this.click = click;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Tạo TextView đơn giản cho từng item (có thể đổi sang layout riêng nếu muốn)
            TextView tv = new TextView(parent.getContext());
            tv.setTextSize(16f);
            int padH = (int) (16 * parent.getResources().getDisplayMetrics().density);
            int padV = (int) (12 * parent.getResources().getDisplayMetrics().density);
            tv.setPadding(padH, padV, padH, padV);
            tv.setClickable(true);
            tv.setFocusable(true);
            return new VH(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            String ch = data.get(position);
            holder.tv.setText(ch);
            holder.tv.setOnClickListener(v -> {
                if (click != null) click.onClick(ch);
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            final TextView tv;
            VH(@NonNull View itemView) {
                super(itemView);
                tv = (TextView) itemView;
            }
        }
    }
}
