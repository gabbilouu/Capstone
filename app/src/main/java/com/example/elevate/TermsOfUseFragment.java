package com.example.elevate;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.github.barteksc.pdfviewer.PDFView;

public class TermsOfUseFragment extends Fragment {

    public TermsOfUseFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_terms_of_use, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        ImageView back = v.findViewById(R.id.backButton);
        if (back != null) {
            back.setOnClickListener(view ->
                    NavHostFragment.findNavController(this).navigateUp()
            );
        }

        PDFView pdfView = v.findViewById(R.id.pdfView);
        // Make sure app/src/main/assets/Terms_Conditions.pdf exists
        pdfView.fromAsset("Terms_Conditions.pdf")
                .enableSwipe(true)
                .swipeHorizontal(false)
                .enableDoubletap(true)
                .spacing(8)
                .load();
    }
}
