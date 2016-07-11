package com.nplusnapps.todolist;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

/**
 * The recycler adapter manages the data stored in a list.
 */
public class RecyclerListAdapter extends RecyclerView.Adapter<RecyclerListAdapter.ItemViewHolder> implements ItemTouchAdapter {

    private Context mContext;
    private List<Map<String, String>> mItemsList;
    private TextWatcher mTextWatcher;
    private OnTaskChangedListener mTaskListener;
    private View.OnFocusChangeListener mFocusListener;
    private int mSelectedId, mTargetId, mStartPosition, mEndPosition, mMoveDirection, mFocusedViewTag;

    /**
     * Constructs a new instance of the adapter.
     * If the data is not available yet, an empty list must be provided.
     *
     * @param context The context
     * @param list The list
     */
    public RecyclerListAdapter(Context context, List<Map<String, String>> list) {
        mContext = context;
        mItemsList = list;

        // Notifies the provided task listener when the text is changed.
        mTextWatcher = new TextWatcher() {
            private String textBefore, textAfter;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                textBefore = s.toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                textAfter = s.toString();

                // Invokes the task listener only if the input is active and the text has been changed.
                if (mFocusedViewTag != 0 && !TextUtils.equals(textBefore, textAfter)) {
                    mTaskListener.onTaskEdited(mFocusedViewTag, s.toString());
                }
            }
        };

        // Updates the focused view tag.
        mFocusListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                mFocusedViewTag = hasFocus ? (int) v.getTag() : 0;
            }
        };
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
        return new ItemViewHolder(view, mTextWatcher, mFocusListener);
    }

    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {
        Map<String, String> item = mItemsList.get(position);

        holder.mPosTextView.setText(String.valueOf(++position));

        holder.mTaskEditText.setHint(mContext.getString(R.string.hint_task, position));

        String task = item.get(MainActivity.EXTRA_TASK);
        if (task != null) {
            holder.mTaskEditText.setText(task);
            holder.mTaskEditText.setSelection(task.length());
        } else {
            holder.mTaskEditText.setText("");
        }

        int id = Integer.valueOf(item.get(MainActivity.EXTRA_TASK_ID));

        holder.setTag(id);
    }

    @Override
    public void onItemDismiss(RecyclerView.ViewHolder viewHolder) {
        int position = viewHolder.getAdapterPosition();

        mItemsList.remove(position);

        if (mTaskListener != null) {
            int id = ((ItemViewHolder) viewHolder).getTag();
            mTaskListener.onTaskDeleted(id);
        }

        notifyItemRemoved(position);
    }

    @Override
    public void onItemMove(RecyclerView.ViewHolder sourceHolder, RecyclerView.ViewHolder targetHolder) {
        int fromPosition = sourceHolder.getAdapterPosition();
        int toPosition = targetHolder.getAdapterPosition();

        if (mSelectedId == 0) {
            mSelectedId = ((ItemViewHolder) sourceHolder).getTag();
            mStartPosition = fromPosition;
        }

        mEndPosition = toPosition;
        mTargetId = ((ItemViewHolder) targetHolder).getTag();
        mMoveDirection = mStartPosition > toPosition ? ItemTouchHelper.UP : ItemTouchHelper.DOWN;

        Map<String, String> selectedItem = mItemsList.remove(fromPosition);
        mItemsList.add(toPosition > fromPosition ? toPosition - 1 : toPosition, selectedItem);

        notifyItemMoved(fromPosition, toPosition);
    }

    @Override
    public void onItemDrop(RecyclerView.ViewHolder viewHolder) {
        // Invokes the task listener only if there's a view selected and if it has actually changed its position.
        if (mTaskListener != null && mSelectedId != 0 && mStartPosition != mEndPosition) {
            mTaskListener.onTaskMoved(mSelectedId, mTargetId, mMoveDirection);
        }
        mSelectedId = 0;
    }

    @Override
    public int getItemCount() {
        return mItemsList.size();
    }

    /**
     * Sets the task listener to listen for tasks being edited, deleted or moved.
     *
     * @param listener The listener
     */
    public void setOnTaskChangedListener(OnTaskChangedListener listener) {
        mTaskListener = listener;
    }

    /**
     * Adds the provided data to the adapter. Optionally clears the old data.
     *
     * @param newList The list containing new data
     * @param clearPrevious True to clear the old data first
     */
    public void addItems(List<Map<String, String>> newList, boolean clearPrevious) {
        if (clearPrevious) {
            mItemsList.clear();
        }

        if (newList != null) {
            for (Map<String, String> item : newList) {
                mItemsList.add(item);
            }
        }

        notifyDataSetChanged();

        if (mTaskListener != null) {
            mTaskListener.onTaskCountChanged(getItemCount());
        }
    }

    /**
     * This holder stores the links to the views.
     */
    public static class ItemViewHolder extends RecyclerView.ViewHolder implements ItemTouchViewHolder {

        public final TextView mPosTextView;
        public final EditText mTaskEditText;

        public ItemViewHolder(View view, TextWatcher textWatcher, View.OnFocusChangeListener focusListener) {
            super(view);

            mPosTextView = (TextView) view.findViewById(R.id.text_pos);

            mTaskEditText = (EditText) view.findViewById(R.id.text_task);
            mTaskEditText.addTextChangedListener(textWatcher);
            mTaskEditText.setOnFocusChangeListener(focusListener);
        }

        @Override
        public void onItemSelected() {
            mTaskEditText.setEnabled(false);
        }

        @Override
        public void onItemClear() {
            mTaskEditText.setEnabled(true);
            mTaskEditText.requestFocus();
        }

        @Override
        public void setTag(int tag) {
            mTaskEditText.setTag(tag);
        }

        @Override
        public int getTag() {
            return (int) mTaskEditText.getTag();
        }
    }
}
