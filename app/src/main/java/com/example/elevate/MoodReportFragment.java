package com.example.elevate;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.github.mikephil.charting.charts.RadarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.RadarData;
import com.github.mikephil.charting.data.RadarDataSet;
import com.github.mikephil.charting.data.RadarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

public class MoodReportFragment extends Fragment {

    private RadarChart radarChart;
    private TextView tvSummary;

    public MoodReportFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mood_report, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        ImageButton back = v.findViewById(R.id.btn_back);
        radarChart = v.findViewById(R.id.moodRadarChart);
        tvSummary  = v.findViewById(R.id.tvSummary);

        if (back != null) {
            back.setOnClickListener(btn ->
                    Navigation.findNavController(btn).navigateUp()
            );
        }

        buildFullHistoryReport();
    }

    private void buildFullHistoryReport() {
        SharedPreferences prefs = requireContext().getSharedPreferences("MoodPrefs", 0);

        // These MUST match whatever you store when logging moods
        String[] moodNames  = {"Very Happy", "Happy", "Neutral", "Sad", "Very Sad"};
        String[] moodEmojis = {"😄", "😊", "😐", "☹️", "😞"};
        int[] counts = new int[moodNames.length];

        Map<String, ?> all = prefs.getAll();
        int total = 0;

        // We assume keys are yyyyMMdd; we only care about valid mood strings
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            Object value = entry.getValue();
            if (!(value instanceof String)) continue;
            String mood = (String) value;

            int index = -1;
            for (int i = 0; i < moodNames.length; i++) {
                if (moodNames[i].equals(mood)) {
                    index = i;
                    break;
                }
            }
            if (index == -1) continue;

            counts[index]++;
            total++;
        }

        if (total == 0) {
            tvSummary.setText("No mood check-ins yet.\nStart logging your mood to see your report here.");
            radarChart.clear();
            radarChart.invalidate();
            return;
        }

        // Figure out most common mood
        int maxCount = 0;
        int maxIdx = 0;
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] > maxCount) {
                maxCount = counts[i];
                maxIdx = i;
            }
        }

        // Account creation date (for text only)
        String sinceText = "Unknown";
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getMetadata() != null) {
            long created = user.getMetadata().getCreationTimestamp();
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
            sinceText = sdf.format(created);
        }

        String summary = "Tracking since: " + sinceText +
                "\nTotal mood check-ins: " + total +
                "\nMost common mood: " + moodEmojis[maxIdx] + " " + moodNames[maxIdx];
        tvSummary.setText(summary);

        // Build radar data from ALL-TIME counts
        ArrayList<RadarEntry> entries = new ArrayList<>();
        for (int cVal : counts) {
            entries.add(new RadarEntry(cVal));
        }

        RadarDataSet dataSet = new RadarDataSet(entries, null);
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(180);
        dataSet.setLineWidth(2f);
        dataSet.setColor(requireContext().getResources().getColor(android.R.color.holo_blue_light));
        dataSet.setFillColor(requireContext().getResources().getColor(android.R.color.holo_blue_light));
        dataSet.setDrawValues(false);
        dataSet.setDrawHighlightCircleEnabled(false);

        RadarData data = new RadarData(dataSet);
        radarChart.setData(data);

        // X axis: mood emojis
        XAxis xAxis = radarChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(moodEmojis));
        xAxis.setTextSize(12f);
        xAxis.setXOffset(0f);
        xAxis.setYOffset(0f);

        // Y axis: counts
        YAxis yAxis = radarChart.getYAxis();
        yAxis.setAxisMinimum(0f);
        yAxis.setAxisMaximum(Math.max(5f, maxCount)); // so chart doesn’t squash small counts
        yAxis.setLabelCount(5, true);
        yAxis.setDrawLabels(false);

        radarChart.getLegend().setEnabled(false);
        radarChart.getDescription().setEnabled(false);
        radarChart.setRotationEnabled(false);
        radarChart.setExtraOffsets(0f, 0f, 0f, 0f);
        radarChart.setMinOffset(0f);
        radarChart.setPadding(0, 0, 0, 0);
        radarChart.setWebLineWidth(1f);
        radarChart.setWebColor(requireContext().getResources().getColor(android.R.color.darker_gray));
        radarChart.setWebAlpha(180);

        radarChart.invalidate();
    }
}
