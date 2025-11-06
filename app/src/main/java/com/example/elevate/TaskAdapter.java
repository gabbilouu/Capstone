package com.example.elevate;

import android.animation.ValueAnimator;
import android.os.Handler;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Shows tasks with:
 * - Tap-and-hold to complete (progress fill from left to right).
 * - Checkbox on the right for visual confirmation (not directly tappable).
 * - Tap to open editor (even after completed).
 * - No unchecking once completed.
 *
 * Requires item_task.xml with:
 *   - root (FrameLayout)
 *   - holdFill (View)
 *   - ivTaskEmoji (TextView)
 *   - tvTaskName (TextView)
 *   - checkTaskDone (CheckBox)
 */
public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private final List<Task> taskList;
    private final List<String> docIds;

    /* ==== Callbacks ==== */
    public interface OnCompletionToggleListener {
        void onToggle(String docId, Task task, boolean isChecked);
    }
    private OnCompletionToggleListener toggleListener;
    public void setOnCompletionToggleListener(OnCompletionToggleListener l) { this.toggleListener = l; }

    public interface OnItemClickListener {
        void onItemClick(Task task, String taskId);
    }
    private OnItemClickListener itemClickListener;
    public void setOnItemClickListener(OnItemClickListener listener) { this.itemClickListener = listener; }

    public TaskAdapter(List<Task> taskList, List<String> docIds) {
        this.taskList = taskList;
        this.docIds   = docIds;
        setHasStableIds(true);
    }

    @Override public long getItemId(int position) {
        return docIds.get(position).hashCode();
    }

    @NonNull @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        holder.bind(taskList.get(position), docIds.get(position), toggleListener, itemClickListener);
    }

    @Override
    public int getItemCount() { return taskList.size(); }

    @Override
    public void onViewRecycled(@NonNull TaskViewHolder holder) {
        holder.cleanup();
        super.onViewRecycled(holder);
    }

    /* =========================
       ViewHolder
       ========================= */
    static class TaskViewHolder extends RecyclerView.ViewHolder {
        private final View root;
        private final View holdFill;
        private final TextView tvEmoji, tvName;
        private final CheckBox cbDone;

        private Task boundTask;
        private String boundDocId;
        private OnCompletionToggleListener toggleListener;
        private OnItemClickListener itemClickListener;

        private static final long HOLD_MS  = 900; // time to complete
        private static final long CLICK_MS = 180; // quick tap = edit

        private final Handler handler = new Handler();
        private Runnable updater;
        private boolean holding = false;
        private long downAt = 0L;
        private float downX, downY;
        private int rowWidth = 0;
        private final int touchSlop;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            root     = itemView.findViewById(R.id.root);
            holdFill = itemView.findViewById(R.id.holdFill);
            tvEmoji  = itemView.findViewById(R.id.ivTaskEmoji);
            tvName   = itemView.findViewById(R.id.tvTaskName);
            cbDone   = itemView.findViewById(R.id.checkTaskDone);

            touchSlop = ViewConfiguration.get(itemView.getContext()).getScaledTouchSlop();

            // Keep checkbox visible but not interactable
            cbDone.setClickable(false);
            cbDone.setFocusable(false);
            cbDone.setFocusableInTouchMode(false);
        }

        void bind(Task task, String docId,
                  OnCompletionToggleListener toggleListener,
                  OnItemClickListener itemClickListener) {
            this.boundTask = task;
            this.boundDocId = docId;
            this.toggleListener = toggleListener;
            this.itemClickListener = itemClickListener;

            tvEmoji.setText(task.getEmoji());
            tvName.setText(task.getName());

            // Visual state
            cbDone.setChecked(task.isCompleted());
            setFillFraction(task.isCompleted() ? 1f : 0f);

            // Always allow tap-to-edit (completed or not)
            itemView.setOnClickListener(v -> {
                if (this.itemClickListener != null) {
                    this.itemClickListener.onItemClick(boundTask, boundDocId);
                }
            });

            // Reset touch listener; only enable hold-to-complete if not completed
            root.setOnTouchListener(null);
            if (!task.isCompleted()) {
                root.setOnTouchListener(this::onTouch);
            }
        }

        private boolean onTouch(View v, MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    rowWidth = v.getWidth();
                    downAt = SystemClock.uptimeMillis();
                    downX = e.getX();
                    downY = e.getY();
                    holding = true;
                    startUpdater(v);
                    // consume so we own the gesture
                    return true;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (!holding) return true;
                    // cancel if the finger strays or exits bounds
                    if (!pointInView(v, e.getX(), e.getY()) ||
                            Math.abs(e.getX() - downX) > touchSlop ||
                            Math.abs(e.getY() - downY) > touchSlop) {
                        cancelHold(true);
                    }
                    return true;
                }
                case MotionEvent.ACTION_UP: {
                    if (!holding) return true;
                    long elapsed = SystemClock.uptimeMillis() - downAt;
                    // A quick tap (< CLICK_MS) means open editor
                    if (elapsed < CLICK_MS && itemClickListener != null) {
                        itemClickListener.onItemClick(boundTask, boundDocId);
                    }
                    // If hold didn't finish, slide fill back
                    cancelHold(true);
                    return true;
                }
                case MotionEvent.ACTION_CANCEL: {
                    cancelHold(true);
                    return true;
                }
            }
            return false;
        }

        private void startUpdater(View v) {
            if (updater != null) handler.removeCallbacks(updater);
            updater = new Runnable() {
                @Override public void run() {
                    if (!holding) return;
                    float frac = Math.min(1f,
                            (SystemClock.uptimeMillis() - downAt) / (float) HOLD_MS);
                    setFillFraction(frac);
                    if (frac >= 1f) {
                        // Completed!
                        holding = false;
                        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        setFillFraction(1f);

                        cbDone.setChecked(true);
                        // a tiny "pop" for the check
                        cbDone.setScaleX(0.85f); cbDone.setScaleY(0.85f); cbDone.setAlpha(0.8f);
                        cbDone.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(120).start();

                        // Lock hold interaction (still tappable to edit via itemView click)
                        v.setOnTouchListener(null);

                        if (boundTask != null) boundTask.setCompleted(true);
                        if (toggleListener != null && boundDocId != null) {
                            toggleListener.onToggle(boundDocId, boundTask, true);
                        }
                        return;
                    }
                    handler.postDelayed(this, 16); // ~60fps
                }
            };
            handler.post(updater);
        }

        private void cancelHold(boolean animateBack) {
            holding = false;
            if (updater != null) handler.removeCallbacks(updater);
            if (animateBack) animateFillBack();
        }

        private void animateFillBack() {
            int current = holdFill.getLayoutParams().width;
            ValueAnimator va = ValueAnimator.ofInt(current, 0);
            va.setDuration(150);
            va.addUpdateListener(a -> {
                int w = (int) a.getAnimatedValue();
                ViewGroup.LayoutParams lp = holdFill.getLayoutParams();
                lp.width = w;
                holdFill.setLayoutParams(lp);
            });
            va.start();
        }

        private void setFillFraction(float f) {
            f = clamp01(f);
            int baseWidth = (rowWidth > 0) ? rowWidth : itemView.getWidth();
            int w = (int) (baseWidth * f);
            ViewGroup.LayoutParams lp = holdFill.getLayoutParams();
            lp.width = w;
            holdFill.setLayoutParams(lp);
        }

        private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }

        private boolean pointInView(View v, float x, float y) {
            return x >= 0 && y >= 0 && x <= v.getWidth() && y <= v.getHeight();
        }

        void cleanup() {
            holding = false;
            if (updater != null) handler.removeCallbacks(updater);
        }
    }
}
