package com.mobileTicket.hello12306.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mobileTicket.hello12306.R;

public class AddTaskLayout extends LinearLayout {

    private final TextView titleView;
    private EditText editText;
    private View.OnClickListener onClickListener;

    public AddTaskLayout(Context context) {
        this(context, null);
    }

    public AddTaskLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AddTaskLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AddTaskLayout(final Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.AddTaskLayout);
        String title = typedArray.getString(R.styleable.AddTaskLayout_title);
        typedArray.recycle();
        setOrientation(HORIZONTAL);
        titleView = new TextView(context);
        titleView.setText(title);
        titleView.setGravity(Gravity.CENTER_VERTICAL);
        titleView .setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        int padding = 20;
        titleView.setPadding(padding, padding, padding, padding);
        titleView.setTextColor(Color.parseColor("#666666"));
        addView(titleView);
    }

    @Override
    public void setOnClickListener(View.OnClickListener l) {
        super.setOnClickListener(l);
        this.onClickListener = l;
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (child instanceof EditText) {
            setEditText((EditText) child);
        }
        super.addView(child, index, params);
    }

    private void setEditText(EditText editText) {
        if (this.editText != null) {
            throw new IllegalArgumentException("We already have an EditText, can only have one");
        } else {
            this.editText = editText;
            this.editText.setInputType(EditorInfo.TYPE_NULL);
            this.editText.setFocusable(false);
            this.editText.setBackground(null);
            this.editText.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClickListener.onClick(v);
                }
            });
        }
    }
}
