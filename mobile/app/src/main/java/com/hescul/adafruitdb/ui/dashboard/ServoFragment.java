package com.hescul.adafruitdb.ui.dashboard;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.hescul.adafruitdb.MainActivity;
import com.hescul.adafruitdb.R;
import com.hescul.adafruitdb.databinding.FragmentServoBinding;

import static com.hescul.adafruitdb.BR.dashboardViewModel;

import java.util.Objects;

public class ServoFragment extends Fragment {
    private FragmentServoBinding binding;
    private NavController  navController;

    private ServoFragmentListener activityCallback;     // a local reference to the host activity listener

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment using data binding
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_servo, container, false);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        // access navigation controller
        navController = Navigation.findNavController(Objects.requireNonNull((MainActivity)getContext()), R.id.navHostMainActivity);

        // propagate data binding and view model instance back to host activity
        activityCallback.onServoCreate(binding, dashboardViewModel);

        // set views' callbacks
        binding.servoBackButton.setOnClickListener(v -> navController.navigate(R.id.servoToDashboard));
        binding.servoSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                activityCallback.onServoModify(seekBar.getProgress());
            }
        });

        // override back button
        OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navController.navigate(R.id.servoToDashboard);

            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backPressedCallback);

        return binding.getRoot();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {   // obtain host activity listener's reference
            activityCallback = (ServoFragmentListener) context; // store activity listener reference
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement LedFragmentListener");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public interface ServoFragmentListener {
        void onServoCreate(FragmentServoBinding binding, int bindingVariable);
        void onServoModify(int value);
    }
}