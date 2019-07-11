package com.sutdy2change.sqlitemanager;

import android.content.Context;
import android.media.Image;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.study2change.dbManager.Entity;
import com.study2change.dbManager.JustDBManager;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ListView listView;

    private JustDBManager dbManager;

    private String[] data = {
            "Apple",
            "Banana","Orange","Watermelon",
            "Pear","Grape","Pineapple",
            "Strawberry","Cherry","Mango"
    };

    private List<Entity> entityList;

    private List<Entity> dataList;

    private EntityAdapter entityAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbManager = JustDBManager.getInstance(MainActivity.this);

        listView = (ListView)findViewById(R.id.list_view);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String str = data[position];
                Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT).show();
            }
        });

        //初始化数据
        initData();

        dbManager.insertEntity(entityList);

        dataList = dbManager.getAllEntity(new DemoEntity());

        entityAdapter = new EntityAdapter(MainActivity.this,R.layout.demo_item, dataList);
//        ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(MainActivity.this,   // Context上下文
//                android.R.layout.simple_list_item_1,  // 子项布局id
//                data);
        listView.setAdapter(entityAdapter);

    }


    private void initData(){
        int indedx = 1;
        List<Entity> list = new ArrayList<Entity>();
        for (int ii = 0; ii < 100; ii++) {
            DemoEntity en = new DemoEntity();
            en.flags = indedx;
            en.age = indedx;
            en.name = "名字:" + indedx;
            en.uin = "qq" + indedx;
            indedx++;
            list.add(en);
        }
        entityList = list;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //加载布局，使用菜单特有方法getMenInfater，或Inflate对象
        //参数：1.菜单显示的布局  2.固定menu
        getMenuInflater().inflate(R.menu.menu_mian, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_list_normal) {
            //标准显示
            Toast.makeText(getApplicationContext(), item.getTitle(), Toast.LENGTH_SHORT).show();
            dbManager.removeEntity(dataList);
            entityAdapter.clear();
            entityAdapter.notifyDataSetChanged();
            return true;
        } else if (itemId == R.id.action_list_vertical_reverse) {
            Toast.makeText(getApplicationContext(), item.getTitle(), Toast.LENGTH_SHORT).show();
            entityList.clear();
            initData();
            dbManager.insertEntity(entityList);
            dataList = dbManager.getAllEntity(new DemoEntity());
            return true;
            //垂直反向显示
        } else if (itemId == R.id.action_list_horizontal) {
            Toast.makeText(getApplicationContext(), item.getTitle(), Toast.LENGTH_SHORT).show();
            return true;
            //水平显示
        } else if (itemId == R.id.action_list_horizontal_reverse) {
            //水平反向显示

        }
        return super.onOptionsItemSelected(item);
    }

    public class EntityAdapter extends ArrayAdapter<Entity>
    {
        private int resourid;

        public EntityAdapter(Context context,int resourid, List<Entity> objects){
            super(context,resourid,objects);

            this.resourid = resourid;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

            Entity entity = getItem(position);
            View view = LayoutInflater.from(getContext()).inflate(resourid,parent,false);
            ImageView imageView = (ImageView)view.findViewById(R.id.image_item);
            TextView textView   = (TextView)view.findViewById(R.id.text_item);
            textView.setText("" + entity.getId());

            return view;
        }
    }
}
