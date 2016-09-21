package com.android.gary.common.view;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.gary.common.R;
import com.android.gary.common.utils.ClickUtils;


public class TitleBar extends RelativeLayout implements OnClickListener {

    public static final int LEFT = 0;
    public static final int RIGHT = 1;


    LayoutInflater mInflater;

    View titlebar;
    TextView titlebar_title;
    LinearLayout titlebar_left_layout;
    LinearLayout titlebar_right_layout;

    public TitleBar(Context context) {
        super(context);
        initTitleBar(context);
    }

    public TitleBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initTitleBar(context);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TitleBar);
        CharSequence title = a.getString(R.styleable.TitleBar_titleName);
        int titleGravity = a.getInteger(R.styleable.TitleBar_titleGravity, 17);
        if (title != null) {
            setTitle(title);
        }
        if (null != titlebar_title) {
            titlebar_title.setGravity(titleGravity);
        }
        ColorStateList bgColor = a.getColorStateList(R.styleable.TitleBar_titlebar_bg_color);
        if (bgColor != null) {
            titlebar.setBackgroundColor(bgColor.getDefaultColor());
        }
        a.recycle();
    }

    public void initTitleBar(Context context) {
        mInflater = LayoutInflater.from(context);

        titlebar = mInflater.inflate(R.layout.titlebar, null);
        titlebar_left_layout = (LinearLayout) titlebar.findViewById(R.id.titlebar_left_layout);
        titlebar_right_layout = (LinearLayout) titlebar.findViewById(R.id.titlebar_right_layout);
        titlebar_title = (TextView) titlebar.findViewById(R.id.titlebar_title);
        addView(titlebar);
    }

    public void setTitle(int textId) {
        if (textId > 0) {
            setTitle(getResources().getString(textId));
        }
    }

    public void setTitle(CharSequence text) {
        if (!TextUtils.isEmpty(text) && titlebar_title != null) {
            titlebar_title.setText(text);
        }
    }

    public String getText() {
        if (titlebar_title != null) {
            if (titlebar_title.getText() != null) {
                return titlebar_title.getText().toString();
            }
        }
        return "";
    }

    public LinearLayout getRightLayout() {
        return titlebar_right_layout;
    }

    public LinearLayout getLeftLayout() {
        return titlebar_left_layout;
    }

    /**
     * 左侧按钮
     * 
     * @param action
     */
    public View setLeftAction(Action action) {
        titlebar_left_layout.removeAllViews();
        View leftView = inflateAction(action);
        titlebar_left_layout.addView(leftView);
        titlebar_left_layout.setVisibility(View.VISIBLE);
        titlebar_left_layout.setTag(action);
        titlebar_left_layout.setOnClickListener(this);
        // title 文字居左时，文字支持点击返回，居中（center）时不影响
        if (titlebar_title != null && titlebar_title.getGravity() == 19) {
            titlebar_title.setTag(action);
            titlebar_title.setOnClickListener(this);
        }
        return leftView;
    }

    /**
     * 右侧按钮
     * 
     * @param action
     */
    public View setRightAction(Action action) {
        titlebar_right_layout.removeAllViews();
        View rightView = inflateAction(action);
        titlebar_right_layout.addView(rightView);
        titlebar_right_layout.setVisibility(View.VISIBLE);
        titlebar_right_layout.setTag(action);
        titlebar_right_layout.setOnClickListener(this);
        return rightView;
    }

    /**
     * inflate 布局
     * 
     * @param action
     * @return
     */
    private View inflateAction(Action action) {
        // 通过Action getText判断是否有文字
        View view = null;
        if (action.getText() > 0) {
            view = mInflater.inflate(R.layout.titlebar_item_text, null);
            TextView textView = (TextView) view.findViewById(R.id.titlebar_item);
            textView.setText(action.getText());
            if (action.getDrawable() > 0) {
                textView.setBackgroundResource(action.getDrawable());
            }
        } else {
            view = mInflater.inflate(R.layout.titlebar_item_img, null);
            ImageView imgView = (ImageView) view.findViewById(R.id.titlebar_item);
            imgView.setImageResource(action.getDrawable());
        }
        return view;
    }

    @Override
    public void onClick(View view) {
        if (!ClickUtils.isFastClick()) {// 防点击过快
            final Object tag = view.getTag();
            if (tag instanceof Action) {
                final Action action = (Action) tag;
                action.performAction(view);
            }
        }
    }

    /**
     * 事件监听事件
     * 
     * @author yuanhang <729741121@qq.com>
     * 
     */
    public interface Action {

        public int getDrawable();// 图片or背景

        public int getText();// 文字

        public void performAction(View view);

    }

    /**
     * 抽象Action
     * 
     * @author yuanhang <729741121@qq.com>
     * 
     */
    public static abstract class AbsAction implements Action {

        final private int mDrawable;
        final private int mText;

        public AbsAction(int drawable) {
            mDrawable = drawable;
            mText = -1;
        }

        public AbsAction(int drawable, int text) {
            mDrawable = drawable;
            mText = text;
        }

        @Override
        public int getDrawable() {
            return mDrawable;
        }

        public int getText() {
            return mText;
        };
    }

    /**
     * 返回按钮 监听事件
     * 
     * @author yuanhang <729741121@qq.com>
     * 
     */
    public static class BackAction extends AbsAction {

        Activity mActivity;

        public BackAction(Activity activity) {
            super(R.drawable.titlebar_back);
            mActivity = activity;
        }

        public BackAction(Activity activity, int drawable) {
            super(drawable);
            mActivity = activity;
        }

        @Override
        public void performAction(View view) {
            if (mActivity != null) {
                mActivity.onBackPressed();
            }
        }

    }

    /**
     * 跳转 监听事件
     * 
     * @author yuanhang <729741121@qq.com>
     * 
     */
    public static class IntentAction extends AbsAction {
        private Context mContext;
        private Intent mIntent;

        public IntentAction(Context context, Intent intent, int drawable) {
            super(drawable);
            mContext = context;
            mIntent = intent;
        }

        public IntentAction(Context context, Intent intent, int drawable, int text) {
            super(drawable, text);
            mContext = context;
            mIntent = intent;
        }

        @Override
        public void performAction(View view) {
            try {
                mContext.startActivity(mIntent);
            } catch (ActivityNotFoundException e) {}
        }
    }
}
