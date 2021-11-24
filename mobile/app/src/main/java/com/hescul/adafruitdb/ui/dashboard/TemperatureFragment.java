package com.hescul.adafruitdb.ui.dashboard;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.hescul.adafruitdb.MainActivity;
import com.hescul.adafruitdb.R;
import com.hescul.adafruitdb.databinding.FragmentTemperatureBinding;

import static com.hescul.adafruitdb.BR.dashboardViewModel;

import java.util.Objects;


public class TemperatureFragment extends Fragment {
    private FragmentTemperatureBinding binding;
    private NavController        navController;

    private TemperatureFragmentListener activityCallback;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_temperature, container, false);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        // access navigation controller
        navController = Navigation.findNavController(Objects.requireNonNull((MainActivity)getContext()), R.id.navHostMainActivity);

        // propagate data binding and view model instance back to host activity
        activityCallback.onTemperatureCreate(binding, dashboardViewModel);

        // set views' callbacks
        binding.temperatureBackButton.setOnClickListener(v -> navController.navigate(R.id.temperatureToDashboard));

        // override back button
        OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() { navController.navigate(R.id.temperatureToDashboard); }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backPressedCallback);

        return binding.getRoot();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            activityCallback = (TemperatureFragmentListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement TemperatureFragmentListener");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public interface TemperatureFragmentListener {
        void onTemperatureCreate(FragmentTemperatureBinding binding, int bindingVariable);
    }
}