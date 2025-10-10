package com.example.elevate;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

public class LoginStreakFragment extends Fragment {

    private NavController navC;
    private TextView streakNumber;
    private TextView[] daysOfWeek = new TextView[7];
    private SharedPreferences prefs;

    private static final String PREFS_NAME = "LoginStreakPrefs";
    private static final String KEY_STREAK = "streak";
    private static final String KEY_LAST_LOGIN = "lastLogin";

    public LoginStreakFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_login_streak, container, false);

        // Initialize views
        streakNumber = view.findViewById(R.id.streakNumber);
        daysOfWeek[0] = view.findViewById(R.id.day_sun);
        daysOfWeek[1] = view.findViewById(R.id.day_mon);
        daysOfWeek[2] = view.findViewById(R.id.day_tue);
        daysOfWeek[3] = view.findViewById(R.id.day_wed);
        daysOfWeek[4] = view.findViewById(R.id.day_thu);
        daysOfWeek[5] = view.findViewById(R.id.day_fri);
        daysOfWeek[6] = view.findViewById(R.id.day_sat);

        // Load SharedPreferences
        prefs = requireContext().getSharedPreferences(PREFS_NAME, 0);

        updateStreak();

        // Navigate via Navigation Component
        navC = androidx.navigation.fragment.NavHostFragment.findNavController(this);
        view.findViewById(R.id.startButton).setOnClickListener(v -> {
            if (navC != null) {
                navC.navigate(R.id.action_loginStreakFragment_to_homePageFragment);
            }
        });

        return view;
    }

    private void updateStreak() {
        int streak = prefs.getInt(KEY_STREAK, 0);
        String lastLoginStr = prefs.getString(KEY_LAST_LOGIN, "");
        Calendar today = Calendar.getInstance();

        // Check last login
        Calendar lastLogin = Calendar.getInstance();
        if (!lastLoginStr.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
                lastLogin.setTime(sdf.parse(lastLoginStr));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Update streak
        if (!lastLoginStr.isEmpty()) {
            long diff = today.getTimeInMillis() - lastLogin.getTimeInMillis();
            long diffDays = diff / (1000 * 60 * 60 * 24);
            if (diffDays > 1) {
                streak = 1; // reset streak
            } else if (diffDays == 1) {
                streak++; // continue streak
            }
        } else {
            streak = 1; // first login
        }

        prefs.edit()
                .putInt(KEY_STREAK, streak)
                .putString(KEY_LAST_LOGIN, new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(today.getTime()))
                .apply();

        streakNumber.setText(String.valueOf(streak));

        highlightDays(streak, today.get(Calendar.DAY_OF_WEEK));
    }

    private void highlightDays(int streak, int todayWeekDay) {
        // Pool of drawable images
        int[] imagePool = {
                R.drawable.circle1,
                R.drawable.circle2,
                R.drawable.circle3,
                R.drawable.circle4,
                R.drawable.circle5,
                R.drawable.circle6,
                R.drawable.circle7,
                R.drawable.circle8,
                R.drawable.circle9,
                R.drawable.circle10
        };

        // Reset all days
        for (TextView day : daysOfWeek) {
            day.setBackgroundResource(R.drawable.day_circle_inactive);
            day.setTextColor(Color.WHITE);
        }

        // Highlight recent streak days
        int daysToHighlight = Math.min(streak, 7);
        int index = todayWeekDay - 1;
        Random random = new Random();

        for (int i = 0; i < daysToHighlight; i++) {
            int dayIndex = (index - i + 7) % 7;
            int randomDrawable = imagePool[random.nextInt(imagePool.length)];
            Drawable circular = makeCircularDrawable(requireContext(), randomDrawable);
            daysOfWeek[dayIndex].setBackground(circular);
        }
    }

    // ✅ Helper method to make any image circular
    private Drawable makeCircularDrawable(Context context, int drawableId) {
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), drawableId);
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(output);
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        float radius = Math.min(bitmap.getWidth(), bitmap.getHeight()) / 2f;

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(bitmap.getWidth() / 2f, bitmap.getHeight() / 2f, radius, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return new BitmapDrawable(context.getResources(), output);
    }
}
