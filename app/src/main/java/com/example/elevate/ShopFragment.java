package com.example.elevate;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ShopFragment extends Fragment implements ShopItemAdapter.OnShopClick {

    public enum Category { PLANTS, POTS, ROOM }

    // ---- SharedPrefs: match HomePageFragment
    private static final String HOME_PREFS = "home_state";
    private static final String KEY_POINTS = "points_total";
    private static final String KEY_XP     = "xp_total";   // use same XP as Home

    // Shop-local prefs
    private static final String SHOP_PREFS = "shop_prefs";
    private static final String KEY_EQ_PLANT = "eq_plant";
    private static final String KEY_EQ_POT   = "eq_pot";
    private static final String KEY_EQ_ROOM  = "eq_room";

    private RecyclerView rv;
    private AppCompatButton tabPlants, tabPots, tabRoom;
    private View btnBack;
    private TextView tvCoins, tvCountdown;

    private SharedPreferences homePrefs; // where points & xp live
    private SharedPreferences shopPrefs; // owned/equipped

    private ShopItemAdapter adapter;
    private Category current = Category.PLANTS;

    // countdown like TaskListFragment
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_shop, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);

        homePrefs = requireContext().getSharedPreferences(HOME_PREFS, Context.MODE_PRIVATE);
        shopPrefs = requireContext().getSharedPreferences(SHOP_PREFS, Context.MODE_PRIVATE);

        rv = v.findViewById(R.id.rvShop);
        tabPlants = v.findViewById(R.id.tabPlants);
        tabPots = v.findViewById(R.id.tabPots);
        tabRoom = v.findViewById(R.id.tabRoom);
        btnBack = v.findViewById(R.id.btnBack);
        tvCoins = v.findViewById(R.id.tvCoins);
        tvCountdown = v.findViewById(R.id.tvRefreshCountdown);

        rv.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        rv.addItemDecoration(new SpacesItemDecoration(8));
        adapter = new ShopItemAdapter(this);
        rv.setAdapter(adapter);

        // Ensure default equipped selection once
        ensureDefaults();

        // Wiring
        tabPlants.setOnClickListener(vw -> selectTab(Category.PLANTS));
        tabPots.setOnClickListener(vw -> selectTab(Category.POTS));
        tabRoom.setOnClickListener(vw -> selectTab(Category.ROOM));
        btnBack.setOnClickListener(vw -> Navigation.findNavController(v).popBackStack());

        updatePointsLabel();
        selectTab(Category.PLANTS);
        startMidnightCountdown(); // starts ticking + refresh at midnight
    }

    /* =========================
       Data models per category
       ========================= */

    // Uses your real art:
    //  - cactus_stage_1, succulent_stage_1
    //  - pot_classic, pot_rounded
    //  - carpet_green, carpet_blue
    private List<ShopItem> plants() {
        int level = currentLevel();
        String eq = shopPrefs.getString(KEY_EQ_PLANT, "plant_cactus");

        boolean ownSucculent = shopPrefs.getBoolean("owned_plant_succulent", false);

        boolean lockSucc = level < 2;  // unlock at level 2
        boolean lockL5   = level < 5;  // last two open up at level 5 / 10
        boolean lockL10  = level < 10;

        return Arrays.asList(
                // Always-owned cactus
                new ShopItem(
                        "plant_cactus",
                        "Potted Cactus",
                        R.drawable.cactus_stage_1,
                        0,
                        true,                // always owned
                        false,               // never locked
                        eq.equals("plant_cactus"),
                        Category.PLANTS
                ),
                // Succulent: unlocks at level 2, costs 30
                new ShopItem(
                        "plant_succulent",
                        "Succulent",
                        R.drawable.succulent_stage_1,
                        30,
                        ownSucculent,
                        lockSucc,            // locked until level 2
                        eq.equals("plant_succulent"),
                        Category.PLANTS
                ),
                // Placeholder: unlocks at level 5
                new ShopItem(
                        "plant_locked1",
                        "Locked",
                        0,
                        0,
                        false,
                        lockL5,              // locked while level < 5
                        false,
                        Category.PLANTS
                ),
                // Placeholder: unlocks at level 10
                new ShopItem(
                        "plant_locked2",
                        "Locked",
                        0,
                        0,
                        false,
                        lockL10,             // locked while level < 10
                        false,
                        Category.PLANTS
                )
        );
    }

    private List<ShopItem> pots() {
        int level = currentLevel();
        String eq = shopPrefs.getString(KEY_EQ_POT, "pot_classic");

        boolean ownRounded = shopPrefs.getBoolean("owned_pot_rounded", false);

        boolean lockRounded = level < 2;  // unlock at level 2
        boolean lockL5      = level < 5;  // placeholders at 5 / 10
        boolean lockL10     = level < 10;

        return Arrays.asList(
                new ShopItem(
                        "pot_classic",
                        "Classic",
                        R.drawable.pot_classic,
                        0,
                        true,                 // always owned
                        false,
                        eq.equals("pot_classic"),
                        Category.POTS
                ),
                new ShopItem(
                        "pot_rounded",
                        "Rounded",
                        R.drawable.pot_rounded,
                        30,                  // price 30 (changed from 50)
                        ownRounded,
                        lockRounded,         // locked until level 2
                        eq.equals("pot_rounded"),
                        Category.POTS
                ),
                new ShopItem(
                        "pot_locked1",
                        "Locked",
                        0,
                        0,
                        false,
                        lockL5,              // locked while level < 5
                        false,
                        Category.POTS
                ),
                new ShopItem(
                        "pot_locked2",
                        "Locked",
                        0,
                        0,
                        false,
                        lockL10,             // locked while level < 10
                        false,
                        Category.POTS
                )
        );
    }

    private List<ShopItem> room() {
        int level = currentLevel();
        String eq = shopPrefs.getString(KEY_EQ_ROOM, "room_green_carpet");

        boolean ownBlue = shopPrefs.getBoolean("owned_room_blue_carpet", false);

        boolean lockBlue = level < 2;  // unlock at level 2
        boolean lockL5   = level < 5;  // placeholders at 5 / 10
        boolean lockL10  = level < 10;

        return Arrays.asList(
                new ShopItem(
                        "room_green_carpet",
                        "Green Carpet",
                        R.drawable.carpet_green,
                        0,
                        true,                   // always owned
                        false,
                        eq.equals("room_green_carpet"),
                        Category.ROOM
                ),
                new ShopItem(
                        "room_blue_carpet",
                        "Blue Carpet",
                        R.drawable.carpet_blue,
                        30,                    // price 30
                        ownBlue,
                        lockBlue,              // locked until level 2
                        eq.equals("room_blue_carpet"),
                        Category.ROOM
                ),
                new ShopItem(
                        "room_locked1",
                        "Locked",
                        0,
                        0,
                        false,
                        lockL5,                // locked while level < 5
                        false,
                        Category.ROOM
                ),
                new ShopItem(
                        "room_locked2",
                        "Locked",
                        0,
                        0,
                        false,
                        lockL10,               // locked while level < 10
                        false,
                        Category.ROOM
                )
        );
    }

    /* =========================
       Selection, tabs, refresh
       ========================= */
    private void selectTab(Category c) {
        current = c;
        tabPlants.setSelected(c == Category.PLANTS);
        tabPots.setSelected(c == Category.POTS);
        tabRoom.setSelected(c == Category.ROOM);

        switch (c) {
            case PLANTS:
                adapter.submit(plants());
                break;
            case POTS:
                adapter.submit(pots());
                break;
            case ROOM:
                adapter.submit(room());
                break;
        }
    }

    private void ensureDefaults() {
        SharedPreferences.Editor ed = shopPrefs.edit();
        if (!shopPrefs.contains(KEY_EQ_PLANT)) ed.putString(KEY_EQ_PLANT, "plant_cactus");
        if (!shopPrefs.contains(KEY_EQ_POT))   ed.putString(KEY_EQ_POT,   "pot_classic");
        if (!shopPrefs.contains(KEY_EQ_ROOM))  ed.putString(KEY_EQ_ROOM,  "room_green_carpet");
        ed.apply();
    }

    private void updatePointsLabel() {
        tvCoins.setText(String.valueOf(homePrefs.getInt(KEY_POINTS, 0)));
    }

    /* =========================
       Countdown (midnight) + refresh
       ========================= */
    private void startMidnightCountdown() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                Calendar now = Calendar.getInstance();
                Calendar midnight = Calendar.getInstance();
                midnight.add(Calendar.DAY_OF_YEAR, 1);
                midnight.set(Calendar.HOUR_OF_DAY, 0);
                midnight.set(Calendar.MINUTE, 0);
                midnight.set(Calendar.SECOND, 0);
                midnight.set(Calendar.MILLISECOND, 0);

                long diffMillis = midnight.getTimeInMillis() - now.getTimeInMillis();
                long hours = TimeUnit.MILLISECONDS.toHours(diffMillis);
                long minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis) % 60;

                String formatted = String.format(Locale.getDefault(),
                        "⟳ %02d hrs %02d mins", hours, minutes);
                tvCountdown.setText(formatted);

                if (diffMillis <= 1000) {
                    onDailyRefresh();
                }
                timerHandler.postDelayed(this, 60 * 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void onDailyRefresh() {
        // If you later rotate daily offers, do it here.
        // For now, just re-read level/points and rebuild the current list.
        selectTab(current);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (timerRunnable != null) timerHandler.removeCallbacks(timerRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePointsLabel();
        if (timerRunnable != null) timerHandler.post(timerRunnable);
    }

    /* =========================
       Buying / equipping
       ========================= */
    @Override
    public void onClick(ShopItem item) {
        if (item.locked) {
            // Dynamic unlock messages
            String msg;
            if ("plant_succulent".equals(item.id)
                    || "pot_rounded".equals(item.id)
                    || "room_blue_carpet".equals(item.id)) {
                msg = "Unlocks at level 2.";
            } else if (item.id.endsWith("locked1")) {
                msg = "Unlocks at level 5.";
            } else if (item.id.endsWith("locked2")) {
                msg = "Unlocks at level 10.";
            } else {
                msg = "Locked.";
            }
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            return;
        }

        // If not owned -> try to buy with points
        boolean owned = isOwned(item);
        if (!owned) {
            int points = homePrefs.getInt(KEY_POINTS, 0);
            if (item.price > points) {
                Toast.makeText(requireContext(), "Not enough suns.", Toast.LENGTH_SHORT).show();
                return;
            }
            // Deduct points, persist ownership
            homePrefs.edit().putInt(KEY_POINTS, points - item.price).apply();
            setOwned(item, true);
            updatePointsLabel();
            pushPointsToCloud(points - item.price);
            Toast.makeText(requireContext(), "Purchased!", Toast.LENGTH_SHORT).show();
        }

        // Equip
        equip(item);
        selectTab(current); // refresh UI
        Toast.makeText(requireContext(), "Equipped", Toast.LENGTH_SHORT).show();
    }

    private boolean isOwned(ShopItem it) {
        switch (it.category) {
            case PLANTS:
                return it.id.equals("plant_cactus")
                        || shopPrefs.getBoolean("owned_" + it.id, false);
            case POTS:
                return it.id.equals("pot_classic")
                        || shopPrefs.getBoolean("owned_" + it.id, false);
            case ROOM:
                return it.id.equals("room_green_carpet")
                        || shopPrefs.getBoolean("owned_" + it.id, false);
        }
        return false;
    }

    private void setOwned(ShopItem it, boolean owned) {
        shopPrefs.edit().putBoolean("owned_" + it.id, owned).apply();
    }

    private void equip(ShopItem it) {
        SharedPreferences.Editor ed = shopPrefs.edit();
        switch (it.category) {
            case PLANTS:
                ed.putString(KEY_EQ_PLANT, it.id);
                break;
            case POTS:
                ed.putString(KEY_EQ_POT, it.id);
                break;
            case ROOM:
                ed.putString(KEY_EQ_ROOM, it.id);
                break;
        }
        ed.apply();
    }

    /* =========================
       Level math (same curve as Home, using XP)
       ========================= */
    private int currentLevel() {
        // XP is stored in HOME_PREFS by HomePageFragment
        int points = homePrefs.getInt(KEY_POINTS, 0);
        int xp = homePrefs.getInt(KEY_XP, points); // fallback to points for old users

        int n = 0;
        while (true) {
            int need = 5 * ((n + 1) * (n + 2)) / 2;
            if (xp < need) break;
            n++;
        }
        // n is internal 0-based; Home UI shows (n + 1)
        return n + 1;
    }

    /* =========================
       Optional cloud sync of points
       ========================= */
    private void pushPointsToCloud(int newPoints) {
        try {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseFirestore.getInstance()
                    .collection("users").document(uid)
                    .set(Collections.singletonMap("pointsTotal", newPoints), SetOptions.merge());
        } catch (Exception ignore) {
            // no-op if not signed in here; Home will resync later
        }
    }
}
