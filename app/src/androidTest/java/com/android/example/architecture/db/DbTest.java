package com.android.example.architecture.db;


import android.arch.persistence.room.Room;
import android.support.test.InstrumentationRegistry;

import com.android.example.architecture.db.database.GithubDb;

import org.junit.After;
import org.junit.Before;

abstract public class DbTest {
    protected GithubDb db;

    @Before
    public void initDb() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getContext(),
                GithubDb.class).build();
    }

    @After
    public void closeDb() {
        db.close();
    }
}
