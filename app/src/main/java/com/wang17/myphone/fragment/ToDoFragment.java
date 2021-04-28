package com.wang17.myphone.fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.wang17.myphone.R;
import com.wang17.myphone.database.BankToDo;
import com.wang17.myphone.model.DateTime;
import com.wang17.myphone.database.DataContext;
import com.wang17.myphone.util._DialogUtils;
import com.wang17.myphone.callback.MyCallback;
import com.wang17.myphone.util._Utils;
import com.wang17.myphone.widget.MyWidgetProvider;

import java.text.DecimalFormat;
import java.util.List;
@Deprecated
public class ToDoFragment extends Fragment {

    private ListView listView;

    private DataContext mDataContext;

    private List<BankToDo> mBankToDoList;
    private TodoListAdapter listAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDataContext = new DataContext(getContext());
        mBankToDoList = mDataContext.getBankToDos();
        listAdapter = new TodoListAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_to_do, container, false);
        listView = view.findViewById(R.id.listView_todo);
        listView.setAdapter(listAdapter);

        return view;
    }


    protected class TodoListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mBankToDoList.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            convertView = View.inflate(getContext(), R.layout.inflate_list_item_todo, null);

            try {
                final BankToDo bankToDo = mBankToDoList.get(position);

                DateTime now = new DateTime();
                int dayOffset = DateTime.dayOffset(now, bankToDo.getDateTime());
                TextView textViewDays = convertView.findViewById(R.id.textView_days);
                TextView textViewName = convertView.findViewById(R.id.tv_name);
                TextView textViewMoney = convertView.findViewById(R.id.textView_money);

                final ConstraintLayout layoutRoot = convertView.findViewById(R.id.layout_root);
                layoutRoot.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {

                        // 3/1 001 删除选中项
                        new AlertDialog.Builder(getContext()).setMessage("确定要删除此项目及其所有记录吗？").setNegativeButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mDataContext.deleteBankToDo(bankToDo.getId());
                                mBankToDoList = mDataContext.getBankToDos();
                                listAdapter.notifyDataSetChanged();
                                // 发送更新广播
                                Intent intent = new Intent(MyWidgetProvider.ACTION_UPDATE_LISTVIEW);
                                getContext().sendBroadcast(intent);
                            }
                        }).setPositiveButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).show();

                        return true;
                    }
                });
                layoutRoot.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new AlertDialog.Builder(getContext()).setItems(new String[]{"新建", "编辑","月+","月-"}, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0:
                                        //  3/1 001 新建待办事项
                                        _DialogUtils.addTodoDialog(getContext(), new MyCallback() {
                                            @Override
                                            public void execute() {
                                                mBankToDoList = mDataContext.getBankToDos();
                                                listAdapter.notifyDataSetChanged();
                                            }
                                        });
                                        break;
                                    case 1:
                                        //  3/1 001 编辑选中项
                                        _DialogUtils.editTodoDialog(getContext(), bankToDo, null);
                                        break;
                                    case 2:
                                        bankToDo.setDateTime(bankToDo.getDateTime().addMonths(1));
                                        mDataContext.editBankToDo(bankToDo);
                                        mBankToDoList = mDataContext.getBankToDos();
                                        listAdapter.notifyDataSetChanged();
                                        // 发送更新广播
                                        Intent intent = new Intent(MyWidgetProvider.ACTION_UPDATE_LISTVIEW);
                                        getContext().sendBroadcast(intent);
                                        break;
                                    case 3:
                                        bankToDo.setDateTime(bankToDo.getDateTime().addMonths(-1));
                                        mDataContext.editBankToDo(bankToDo);
                                        mBankToDoList = mDataContext.getBankToDos();
                                        listAdapter.notifyDataSetChanged();
                                        // 发送更新广播
                                        Intent intent1 = new Intent(MyWidgetProvider.ACTION_UPDATE_LISTVIEW);
                                        getContext().sendBroadcast(intent1);
                                        break;
                                }
                            }
                        }).show();
                    }
                });

                DecimalFormat formatter = new DecimalFormat("#,###.00");
                if (dayOffset < 0) {
                    textViewDays.setText("+" + (-dayOffset));
                } else {
                    textViewDays.setText(dayOffset + "");
                }


                textViewName.setText(bankToDo.getBankName());
                textViewMoney.setText(formatter.format(bankToDo.getMoney()));
            } catch (Exception e) {
                _Utils.printException(getContext(), e);
                Log.e("wangsc", e.getMessage());
            }


            return convertView;
        }
    }
}
