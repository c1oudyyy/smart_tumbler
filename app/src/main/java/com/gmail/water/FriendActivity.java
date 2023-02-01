package com.gmail.water;


import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Bundle;

import java.util.ArrayList;

public class FriendActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private MyRecyclerAdapter mRecyclerAdapter;
    private ArrayList<FriendItem> mFriendItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecyclerView = (RecyclerView) findViewById(R.id.friend_recycler);

        mRecyclerAdapter = new MyRecyclerAdapter();
        mRecyclerView.setAdapter(mRecyclerAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mFriendItem = new ArrayList<>();
        for(int i = 1; i <= 5; i++) {
            if(i == 1)
                mFriendItem.add(new FriendItem(i*10, "김주원"));
            else if (i == 2)
                mFriendItem.add(new FriendItem(i*5, "박준영"));

            else if (i == 3)
                mFriendItem.add(new FriendItem(100, "김시훈"));

            else if(i == 4)
                mFriendItem.add(new FriendItem(40,"노진혁"));

            else
                mFriendItem.add(new FriendItem(77, "박민우"));
        }

        mRecyclerAdapter.setmFriendList(mFriendItem);
    }
}