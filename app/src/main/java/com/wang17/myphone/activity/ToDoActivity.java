package com.wang17.myphone.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.wang17.myphone.R;
import com.wang17.myphone.model.DateTime;
import com.wang17.myphone.model.database.BankToDo;
import com.wang17.myphone.model.database.Setting;
import com.wang17.myphone.util.DataContext;
import com.wang17.myphone.util._DialogUtils;
import com.wang17.myphone.callback.MyCallback;
import com.wang17.myphone.util._Utils;
import com.wang17.myphone.widget.MyWidgetProvider;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ToDoActivity extends android.support.v4.app.FragmentActivity {

    private ListView listView;

    private DataContext mDataContext;

    private List<BankToDo> mBankToDoList;
    private TodoListAdapter listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //region fragment启用这段代码
//        setContentView(R.layout.activity_to_do);
//        FragmentManager fm = getSupportFragmentManager();
//        FragmentTransaction ft = fm.beginTransaction();
//        ft.replace(R.id.fragment2, new ToDoFragment());
//        ft.commit();
//        return;
        //endregion

        //region activity启用这段代码
        setContentView(R.layout.fragment_to_do);
        mDataContext = new DataContext(this);
        getData();

        listView = findViewById(R.id.listView_todo);
        listView.setAdapter(listAdapter);

        FloatingActionButton actionButton = findViewById(R.id.floatingActionButtonAdd);
        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //  3/1 001 新建待办事项
                _DialogUtils.addTodoDialog(ToDoActivity.this, new MyCallback() {
                    @Override
                    public void callBack() {
                        getData();
                        listAdapter.notifyDataSetChanged();
                    }
                });
            }
        });
        actionButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                startActivity(new Intent(ToDoActivity.this,SmsActivity.class));
                return true;
            }
        });

//        FloatingActionButton.LayoutParams starParams = new FloatingActionButton.LayoutParams(35, 35);
//        starParams.setMargins(5, 5, 5, 5);
//
//        FloatingActionButton.LayoutParams fabIconStarParams = new FloatingActionButton.LayoutParams(35, 35);
//        fabIconStarParams.setMargins(5, 5, 5, 5);
//
//        final ImageView fabIconNew = new ImageView(this);
//        fabIconNew.setLayoutParams(starParams);
//        fabIconNew.setImageDrawable(getResources().getDrawable(R.drawable.ic_menu_star));
//        final FloatingActionButton rightLowerButton = new FloatingActionButton.Builder(this).setContentView(fabIconNew,fabIconStarParams).build();
//
//
//        ImageView rlIcon1 = new ImageView(this);
//        ImageView rlIcon2 = new ImageView(this);
//        rlIcon1.setImageDrawable(getResources().getDrawable(R.drawable.ic_menu_add));
//        rlIcon2.setImageDrawable(getResources().getDrawable(R.drawable.ic_menu_directions));
//        SubActionButton.Builder rLSubBuilder = new SubActionButton.Builder(this);
//        final FloatingActionMenu floatingActionMenu = new FloatingActionMenu.Builder(this)
//                .addSubActionView(rLSubBuilder.setContentView(rlIcon1).build())
//                .addSubActionView(rLSubBuilder.setContentView(rlIcon2).build()).attachTo(rightLowerButton).build();
//
//        floatingActionMenu.setStateChangeListener(new FloatingActionMenu.MenuStateChangeListener() {
//            @Override
//            public void onMenuOpened(FloatingActionMenu menu) {
//                fabIconNew.setRotation(0);
//                PropertyValuesHolder pvhR = PropertyValuesHolder.ofFloat(View.ROTATION, 45);
//                ObjectAnimator animation = ObjectAnimator.ofPropertyValuesHolder(fabIconNew, pvhR);
//                animation.start();
//            }
//
//            @Override
//            public void onMenuClosed(FloatingActionMenu menu) {
//                fabIconNew.setRotation(45);
//                PropertyValuesHolder pvhR = PropertyValuesHolder.ofFloat(View.ROTATION, 0);
//                ObjectAnimator animation = ObjectAnimator.ofPropertyValuesHolder(fabIconNew, pvhR);
//                animation.start();
//            }
//        });
//        rlIcon1.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
////                //  3/1 001 新建待办事项
//                DialogUtils.addTodoDialog(ToDoActivity.this, new MyCallback() {
//                    @Override
//                    public void callBack() {
//                        mBankToDoList = mDataContext.getBankToDos();
//                        listAdapter.notifyDataSetChanged();
//                    }
//                });
//            }
//        });
//
//        rlIcon2.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                startActivity(new Intent(ToDoActivity.this,SmsActivity.class));
//            }
//        });


        //endregion

    }

    private void getData() {
        mBankToDoList = mDataContext.getBankToDos();
//        for(BankToDo toDo:mBankToDoList){
//            if(toDo.getMoney()==-1){
//                toDo.setDateTime(new DateTime().addDays(1000));
//            }
//        }
        Collections.sort(mBankToDoList, new Comparator<BankToDo>() {
            @Override
            public int compare(BankToDo o1, BankToDo o2) {
                if(o1.getMoney()==-1){
                    return 1;
                }
                if(o2.getMoney()==-1){
                    return -1;
                }

                return (int)(o1.getDateTime().getTimeInMillis()-o2.getDateTime().getTimeInMillis());

//                if(o1.getDateTime().getTimeInMillis()<o2.getDateTime().getTimeInMillis()){
//                    return -1;
//                }else{
//                    return 1;
//                }
            }
        });
        listAdapter = new TodoListAdapter();
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
            convertView = View.inflate(ToDoActivity.this, R.layout.inflate_list_item_todo, null);

            try {
                final BankToDo bankToDo = mBankToDoList.get(position);

                DateTime now = new DateTime();
                int dayOffset = DateTime.dayOffset(now, bankToDo.getDateTime());
                TextView textViewDays = convertView.findViewById(R.id.textView_days);
                TextView textViewName = convertView.findViewById(R.id.textView_name);
                TextView textViewMoney = convertView.findViewById(R.id.textView_money);

                if(bankToDo.getMoney()==0||bankToDo.getMoney()==-1){
                    textViewName.setTextColor(Color.GRAY);
                } else{
                    textViewName.setTextColor(Color.BLACK);
                }


                final ConstraintLayout layoutRoot = convertView.findViewById(R.id.layout_root);
                layoutRoot.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        //  3/1 001 编辑选中项
                        _DialogUtils.editTodoDialog(ToDoActivity.this, bankToDo, null);

                        return true;
                    }
                });
                layoutRoot.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new AlertDialog.Builder(ToDoActivity.this).setItems(new String[]{"编辑","删除", "月+", "月-"}, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0:
                                        //  3/1 001 编辑选中项
                                        _DialogUtils.editTodoDialog(ToDoActivity.this, bankToDo, new MyCallback() {
                                            @Override
                                            public void callBack() {
                                                listAdapter.notifyDataSetChanged();
                                            }
                                        });
                                        break;
                                    case 1:

                                        // 3/1 001 删除选中项
                                        new AlertDialog.Builder(ToDoActivity.this).setMessage("确定要删除此项目及其所有记录吗？").setNegativeButton("确定", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                mDataContext.deleteBankToDo(bankToDo.getId());
                                                mBankToDoList = mDataContext.getBankToDos();
                                                listAdapter.notifyDataSetChanged();

                                                // 发送更新广播
                                                Intent intent = new Intent(MyWidgetProvider.ACTION_UPDATE_LISTVIEW);
                                                ToDoActivity.this.sendBroadcast(intent);
                                            }
                                        }).setPositiveButton("取消", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        }).show();
                                        break;
                                    case 2:

                                        new AlertDialog.Builder(ToDoActivity.this).setMessage("是否将"+bankToDo.getBankName()+"目标日期切换至" + bankToDo.getDateTime().addMonths(1).toShortDateString3() + "？").setPositiveButton("是", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                bankToDo.setDateTime(bankToDo.getDateTime().addMonths(1));
                                                mDataContext.editBankToDo(bankToDo);
                                                mBankToDoList = mDataContext.getBankToDos();
                                                listAdapter.notifyDataSetChanged();
                                                // 发送更新广播
                                                Intent intent = new Intent(MyWidgetProvider.ACTION_UPDATE_LISTVIEW);
                                                ToDoActivity.this.sendBroadcast(intent);
                                            }
                                        }).setNegativeButton("否", null).show();
                                        break;
                                    case 3:
                                        new AlertDialog.Builder(ToDoActivity.this).setMessage("是否将"+bankToDo.getBankName()+"目标日期切换至" + bankToDo.getDateTime().addMonths(-1).toShortDateString3() + "？").setPositiveButton("是", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                bankToDo.setDateTime(bankToDo.getDateTime().addMonths(-1));
                                                mDataContext.editBankToDo(bankToDo);
                                                mBankToDoList = mDataContext.getBankToDos();
                                                listAdapter.notifyDataSetChanged();
                                                // 发送更新广播
                                                Intent intent1 = new Intent(MyWidgetProvider.ACTION_UPDATE_LISTVIEW);
                                                ToDoActivity.this.sendBroadcast(intent1);
                                            }
                                        }).setNegativeButton("否", null).show();
                                        break;
                                }
                            }
                        }).show();
                    }
                });

                DecimalFormat formatter = new DecimalFormat("#,##0");
                if (dayOffset < 0) {
                    textViewDays.setText("+" + (-dayOffset));
                } else {
                    textViewDays.setText(dayOffset + "");
                }


                textViewName.setText(bankToDo.getBankName());
                textViewMoney.setText(formatter.format(bankToDo.getMoney()));
                if(bankToDo.getMoney()==-1){
                    textViewDays.setText("");
                    textViewMoney.setText("");
                }
            } catch (Exception e) {
                _Utils.printException(ToDoActivity.this, e);
                Log.e("wangsc", e.getMessage());
            }


            return convertView;
        }
    }
}
