package com.hescul.adafruitdb.ui.dashboard;

import android.content.Context;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hescul.adafruitdb.MainActivity;
import com.hescul.adafruitdb.R;
import com.hescul.adafruitdb.databinding.FragmentLedBinding;

import static com.hescul.adafruitdb.BR.dashboardViewModel;

import java.util.Objects;

public class LedFragment extends Fragment {
    private FragmentLedBinding  binding;
    private NavController navController;

    private LedFragmentListener activityCallback;   // a local reference to the host activity listener

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment using data binding
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_led, container, false);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        // access navigation controller
        navController = Navigation.findNavController(Objects.requireNonNull((MainActivity)getContext()), R.id.navHostMainActivity);

        // propagate data binding and view model instance back to host activity
        activityCallback.onLedCreate(binding, dashboardViewModel);

        // set views' callbacks
        binding.ledBackButton.setOnClickListener(view -> navController.navigate(R.id.ledToDashboard));
        binding.ledTurnOnButton.setOnClickListener(v -> activityCallback.onLedModify(new DashboardViewModel().ledStatusOn));
        binding.ledTurnOffButton.setOnClickListener(v -> activityCallback.onLedModify(new DashboardViewModel().ledStatusOff));

        // override back button
        OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navController.navigate(R.id.ledToDashboard);
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backPressedCallback);

        return binding.getRoot();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try { // obtain host activity listener's reference
            activityCallback = (LedFragmentListener) context;   // store activity listener reference
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement LedFragmentListener");
        }
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public interface LedFragmentListener {
        void onLedCreate(FragmentLedBinding binding, int bindingVariable);
        void onLedModify(String msg);
    }
}