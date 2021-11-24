package com.hescul.adafruitdb.ui.dashboard;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hescul.adafruitdb.R;
import com.hescul.adafruitdb.databinding.FragmentDashboardBinding;

import static com.hescul.adafruitdb.BR.dashboardViewModel;


public class DashboardFragment extends Fragment {
    private DashboardFragmentListener activityCallback;     // a local reference to the host activity
    private FragmentDashboardBinding binding;               // data binding object

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // inflate using data binding library
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_dashboard, container, false);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        // set views' callbacks
        binding.ledButton.setOnClickListener(view -> Navigation.findNavController(view).navigate(R.id.dashboardToLed));
        binding.servoButton.setOnClickListener(view -> Navigation.findNavController(view).navigate(R.id.dashboardToServo));
        binding.temperatureButton.setOnClickListener(view -> Navigation.findNavController(view).navigate(R.id.dashboardToTemperature));
        binding.refreshButton.setOnClickListener(v -> activityCallback.onDashboardRefresh());

        // propagate data binding and view model instance back to host activity
        activityCallback.onDashboardCreate(binding, dashboardViewModel);

        return binding.getRoot();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {   // obtain host activity listener's reference
            activityCallback = (DashboardFragmentListener)context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement DashboardListener");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public interface DashboardFragmentListener {
        void onDashboardCreate(FragmentDashboardBinding binding, int bindingVariable);
        void onDashboardRefresh();
    }
}
