package com.example.elevate;
import android.text.Editable;
import android.text.TextWatcher;

public abstract class SimpleTextWatcher implements TextWatcher {

    public abstract void onTextChanged(String text);

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // no-op
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        onTextChanged(s.toString());
    }

    @Override
    public void afterTextChanged(Editable s) {
        // no-op
    }
}
