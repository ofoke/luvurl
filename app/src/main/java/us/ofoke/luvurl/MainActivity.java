package us.ofoke.luvurl;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.CoordinatorLayout;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

//import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toolbar;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

//public class MainActivity extends AppCompatActivity {
public class MainActivity extends Activity {

    public static final String TAG = "Lurl";
    private DatabaseReference mRef;
    private DatabaseReference lRef;
    private DatabaseReference nRef;
    private RecyclerView mLinks;
    private LinearLayoutManager mManager;
    private FirebaseRecyclerAdapter<Lurl, LinkHolder> mRecyclerViewAdapter;

    private WebView myWebView;

    private BottomSheetBehavior<View> behavior;
    private BottomSheetDialog dialog;
    private CoordinatorLayout coordinatorLayout;

    private ImageButton luvRaterBtn;
    private ImageButton noLuvRaterBtn;
    private TextView luvRateTV;
    private TextView noLuvRateTV;

    private int luvrater;
    private int noluvrater;
    private Long luvRatingCum;
    private Long noLuvRatingCum;
    private Map<String, Long> ratingsCum;
    private String key;

    private MutableData luvRatingCumMd;
    private MutableData noLuvRatingCumMd;

    private String url;
    private ChildEventListener ceListen;
    private ValueEventListener veListen;
    private ValueEventListener uiListen;

    private Lurl lurl = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar mToolbar = (android.widget.Toolbar) findViewById(R.id.toolbar);
        setActionBar(mToolbar);

        luvRaterBtn = (ImageButton) findViewById(R.id.bsheet_luv);
        noLuvRaterBtn = (ImageButton) findViewById(R.id.bsheet_noluv);

        luvRateTV = (TextView) findViewById(R.id.textview_luv);
        noLuvRateTV = (TextView) findViewById(R.id.textview_noluv);

        mRef = FirebaseDatabase.getInstance().getReference();

        myWebView = (WebView) findViewById(R.id.webview);

        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        myWebView.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                urlInFB(url);
            }
        });

        myWebView.loadUrl("https://www.google.com");



        mLinks = (RecyclerView) findViewById(R.id.rview);
        mLinks.setHasFixedSize(true);

        mManager = new LinearLayoutManager(this);
        mManager.setReverseLayout(false);

        mLinks.setLayoutManager(mManager);

        // THIS IS TEMP
//        mRecyclerViewAdapter = new FirebaseRecyclerAdapter<Lurl, LinkHolder>(Lurl.class, R.layout.link, LinkHolder.class, mRef) {
//            @Override
//            protected void populateViewHolder(LinkHolder viewHolder, Lurl model, final int position) {
//                viewHolder.setUrl(model.getUrl());
//
//                viewHolder.mView.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        mRecyclerViewAdapter.getRef(position);
//                        Log.w(TAG, "You clicked on "+position);
//                    }
//                });
//            }
//            //android.R.layout.two_line_list_item
//        };
//
//        mLinks.setAdapter(mRecyclerViewAdapter);

        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);

        initBottomSheet();
    }


    private void initBottomSheet() {
        View bottomSheet = coordinatorLayout.findViewById(R.id.bottom_sheet);
        behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                // React to state change
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // React to dragging events
            }
        });
    }

    public void urlInFB(String rawUrl){

        if (null != rawUrl && rawUrl.length() > 0) {
            if (rawUrl.contains("?")) {
                //url cleanup - remove everything after ?
                StringBuilder str = new StringBuilder(rawUrl);
                int qMarkStart = str.indexOf("?");
                int qMarkEnd = str.length();
                url = str.delete(qMarkStart, qMarkEnd).toString();
            } else {
                url = rawUrl;
            }

            //check to see if url exists in FB
            veListen = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    boolean urlExists = dataSnapshot.exists();

                    if (urlExists) {
                        Map<String, Object> rawkey = (Map<String, Object>) dataSnapshot.getValue();
                        key = (String) rawkey.keySet().toArray()[0];

                        lRef = mRef.child("lurls").child(key).child("luvRating");
                        nRef = mRef.child("lurls").child(key).child("noLuvRating");
                        uiRatingCounter(lRef, nRef);
                    } else {
                        luvRateTV.setText("0");
                        noLuvRateTV.setText("0");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            };
            mRef.child("lurls").orderByChild("url").equalTo(url).addListenerForSingleValueEvent(veListen);
        }
    }

    public void uiRatingCounter(DatabaseReference lRef, DatabaseReference nRef){
        lRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Long luV = (Long) dataSnapshot.getValue();
                luvRateTV.setText(luV.toString());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        nRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Long noLuV = (Long) dataSnapshot.getValue();
                noLuvRateTV.setText(noLuV.toString());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void rater_luv(View view) {
        luvrater = 1;
        noluvrater = 0;
        rater(luvrater, noluvrater);
    }

    public void rater_noluv(View view) {
        luvrater = 0;
        noluvrater = 1;
        rater(luvrater, noluvrater);
    }

    public void rater(final int luvrater, int noluvrater) {
        lurl = null;

        String rawUrl = myWebView.getUrl();

        if (null != rawUrl && rawUrl.length() > 0) {
            if (rawUrl.contains("?")) {
                //url cleanup - remove everything after ?
                StringBuilder str = new StringBuilder(rawUrl);
                int qMarkStart = str.indexOf("?");
                int qMarkEnd = str.length();
                url = str.delete(qMarkStart, qMarkEnd).toString();
            } else {
                url = rawUrl;
            }

            final long ts = System.currentTimeMillis();
            lurl = new Lurl(luvrater, noluvrater, ts, url);

            //check to see if url has been rated
            veListen = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    boolean prevRated = dataSnapshot.exists();

                    if (prevRated) {
                        //get key cuz getKey() doesn't work here
                        Map<String, Object> rawkey = (Map<String, Object>) dataSnapshot.getValue();
                        key = (String) rawkey.keySet().toArray()[0];
                       // Log.v("thekey", key);

                        //get luvrating and noluvrating
                        ratingsCum = (Map<String, Long>) rawkey.values().toArray()[0];

                        if (luvrater == 1) {
                            luvRatingCum = ratingsCum.get("luvRating");
                            lRef = mRef.child("lurls").child(key).child("luvRating");
                            transIncrement(lRef, luvRatingCum);
                        } else {
                            noLuvRatingCum = ratingsCum.get("noLuvRating");
                            nRef = mRef.child("lurls").child(key).child("noLuvRating");
                            transIncrement(nRef, noLuvRatingCum);
                        }

                    } else {
                       // mRef.child("lurls").push().setValue(lurl);
                        //create it
                        String key = mRef.child("lurls").push().getKey();
                        mRef.child("lurls").child(key).setValue(lurl);

                        //bind to ui
                        lRef = mRef.child("lurls").child(key).child("luvRating");
                        nRef = mRef.child("lurls").child(key).child("noLuvRating");
                        uiRatingCounter(lRef, nRef);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            };

            mRef.child("lurls").orderByChild("url").equalTo(url).addListenerForSingleValueEvent(veListen);
        }

    }


    public void transIncrement(DatabaseReference oRef, Long mData) {
        //write it via transaction
        oRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mData) {

                if (mData.getValue() != null) {
                    Long theValue = (Long) mData.getValue();
                    theValue++;
                    mData.setValue(theValue);
                }
                return Transaction.success(mData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                //Log.v("md", dataSnapshot.toString());
                //Log.v("mderror", databaseError.toString());
            }
        });
    }

    public void oh(View view) {
        String bob = myWebView.getUrl();
        Log.v("bob", bob);

        behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }


    public WebView getMyWebView() {
        return myWebView;
    }

    @Override
    protected void onStart() {
        super.onStart();


    }

    @Override
    public void onStop() {
        super.onStop();

        if (mRecyclerViewAdapter != null) {
            mRecyclerViewAdapter.cleanup();
        }
    }

/*    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.go_back:
                // openCustomTab();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


/*    public void onTimeRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radio_time_hour:
                if (checked)
                    // Pirates are the best
                    break;
            case R.id.radio_time_day:
                if (checked)
                    // Ninjas rule
                    break;
            case R.id.radio_time_week:
                if (checked)
                    // Pirates are the best
                    break;
            case R.id.radio_time_4ever:
                if (checked)
                    // Ninjas rule
                    break;
        }
    }*/

/*    public void onLuvRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radio_luv_yes:
                if (checked)
                    // Pirates are the best
                    break;
            case R.id.radio_luv_no:
                if (checked)
                    // Ninjas rule
                    break;
        }
    }*/


    public static class LinkHolder extends RecyclerView.ViewHolder {
        View mView;

        public LinkHolder(View itemView) {
            super(itemView);
            mView = itemView;


        }

//        public void setName(String name) {
//            TextView field = (TextView) mView.findViewById(R.id.name_text);
//            field.setText(name);
//        }

        public void setUrl(String text) {
            TextView field = (TextView) mView.findViewById(R.id.link);
            field.setText(text);
        }
    }
}
