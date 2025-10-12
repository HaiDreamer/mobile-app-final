package vn.edu.usth.irc;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import vn.edu.usth.irc.databinding.FragmentPlaceholderHomeBinding;

public class PlaceholderHomeFragment extends Fragment {

    public static PlaceholderHomeFragment newInstance(){ return new PlaceholderHomeFragment(); }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentPlaceholderHomeBinding b = FragmentPlaceholderHomeBinding.inflate(inflater, container, false);
        return b.getRoot();
    }
}
