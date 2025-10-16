package com.example.elevate;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

public class FAQFragment extends Fragment {

    public FAQFragment() {
        // Required empty constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        // If your file is named fragment_f_a_q.xml, change this to R.layout.fragment_f_a_q
        return inflater.inflate(R.layout.fragment_f_a_q, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        // Back chevron → go back (pop back stack)
        ImageView back = v.findViewById(R.id.backButton);
        if (back != null) {
            back.setOnClickListener(x ->
                    NavHostFragment.findNavController(this).navigateUp()
            );
        }

        // Optional: tap the center message to open your FAQ in a browser
        View faqOpener = v.findViewById(R.id.tv_faq_instruction);
        if (faqOpener != null) {
            faqOpener.setOnClickListener(x -> {
                // TODO: replace with your real FAQ URL
                String url = "https://example.com/faq";
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (Exception ignored) { }
            });
        }
    }
}
