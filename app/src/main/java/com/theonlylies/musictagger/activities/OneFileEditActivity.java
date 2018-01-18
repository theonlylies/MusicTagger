package com.theonlylies.musictagger.activities;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.RequiresPermission;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.NotificationCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.MediaStoreSignature;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionButtonBehavior;
import com.github.clans.fab.FloatingActionMenu;
import com.github.clans.fab.FloatingActionMenuBehavior;
import com.theonlylies.musictagger.R;
import com.theonlylies.musictagger.utils.GlideApp;
import com.theonlylies.musictagger.utils.MediaStoreUtils;
import com.theonlylies.musictagger.utils.adapters.MusicFile;
import com.theonlylies.musictagger.utils.TagManager;

import java.util.HashMap;
import java.util.Map;

import lib.DataSet;
import lib.source.ID3V2;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;
import static com.theonlylies.musictagger.utils.MediaStoreUtils.GENRES;
import static com.theonlylies.musictagger.utils.MediaStoreUtils.dumpAlbums;
import static com.theonlylies.musictagger.utils.MediaStoreUtils.dumpMedia;

public class OneFileEditActivity extends AppCompatActivity implements View.OnClickListener {

    FloatingActionMenu menu;

    EditText titleEdit,albumEdit,artistEdit,yearEdit,trackNumberEdit;
    AutoCompleteTextView genreEdit;
    ImageView artworkImageView,bestMatchArtworkImageView;
    TextView bestMathAlbumTextView,bestMathArtistTextView;
    MusicFile musicFile;

    CardView cardSearched;
    NestedScrollView nestedScrollView;
    AppBarLayout appBarLayout;
    Context context;

    Uri newArtworkUri;



    public native String fpCalc(String[] args);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onefiledit);
        context=this;

        try {
            System.loadLibrary("fpcalc");
        } catch (UnsatisfiedLinkError e) {
            Log.e("fpCacl","Could not load library libfpcalc.so : " + e);
        }

        titleEdit = findViewById(R.id.titleEdit);
        albumEdit = findViewById(R.id.albumEdit);
        artistEdit = findViewById(R.id.artistEdit);
        yearEdit = findViewById(R.id.yearEdit);
        genreEdit = findViewById(R.id.genreEdit);
        trackNumberEdit = findViewById(R.id.trackNumEdit);
        artworkImageView = findViewById(R.id.artwortImageView);

        bestMatchArtworkImageView = findViewById(R.id.bestMathArtworkImageView);
        bestMathAlbumTextView = findViewById(R.id.bestMathAlbumTextView);
        bestMathArtistTextView = findViewById(R.id.bestMathArtistTextView);

        cardSearched=findViewById(R.id.cardSearched);
        nestedScrollView = findViewById(R.id.nestedScrollView);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        /**
         * This Block for FABs init
         */
        android.support.design.widget.FloatingActionButton fab = findViewById(R.id.fab1);
        fab.setOnClickListener(this);

        final CardView cardView=findViewById(R.id.cardView);

        menu = findViewById(R.id.menu);

        appBarLayout=findViewById(R.id.app_bar);

        FloatingActionMenuBehavior behavior = new FloatingActionMenuBehavior();
        int topInset=getStatusBarHeight(getResources());
        behavior.setTopInset(topInset);

        CoordinatorLayout.LayoutParams params =
                (CoordinatorLayout.LayoutParams) menu.getLayoutParams();
        params.setBehavior(behavior);

        FloatingActionButton button=findViewById(R.id.fab2);
        FloatingActionButtonBehavior behaviorButton = new FloatingActionButtonBehavior();
        behaviorButton.setTopInset(topInset);
        CoordinatorLayout.LayoutParams paramsButton = (CoordinatorLayout.LayoutParams) button.getLayoutParams();
        paramsButton.setBehavior(behaviorButton);

        /*FloatingActionButton aqua = menu.getMenuMainButton();
        CoordinatorLayout.LayoutParams p = (CoordinatorLayout.LayoutParams) aqua.getLayoutParams();
        p.setBehavior(behaviorButton);*/


        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                int offset = appBarLayout.getTotalScrollRange()- Math.abs(verticalOffset);
                float percentage = (offset/(float)appBarLayout.getTotalScrollRange());
                cardView.setAlpha(percentage);
            }
        });

        /**
         * FAB init block END!
         */


        String path = getIntent().getStringExtra("music_file_path");
        //Start
        //musicFile = MediaStoreUtils.getMusicFileByPath(path,this);
        //initTagsInterface(musicFile);
        new ReadFromMediaStore().execute(path);
    }

    void initTagsInterface(MusicFile file){
        titleEdit.setText(file.getTitle());
        albumEdit.setText(file.getAlbum());
        artistEdit.setText(file.getArtist());
        yearEdit.setText(file.getYear());
        genreEdit.setText(file.getGenre());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, GENRES);
        genreEdit.setAdapter(adapter);
        trackNumberEdit.setText(file.getTrackNumber());
        GlideApp.with(this)
                .load(file.getArtworkUri())
                .signature(new MediaStoreSignature("lol",System.currentTimeMillis(),3))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .error(R.drawable.vector_artwork_placeholder)
                .into(artworkImageView);
        //dumpMedia(this);
        //dumpAlbums(this);
    }

    private static int getStatusBarHeight(android.content.res.Resources res) {
        return (int) (24 * res.getDisplayMetrics().density);
    }

    @Override
    public void onBackPressed(){
        if( menu.isOpened() ) {
            menu.close(true);
        }else super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_onefileedit_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        Log.d("id",String.valueOf(id));

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if(id==R.id.action_save_file){
            //saveChanges(musicFile);
            new WriteChanges().execute(musicFile);
        }
        if(id==android.R.id.home){
            //TODO create return intent with msg for update recyclerView !!!
            finish();
        }
        return super.onOptionsItemSelected(item);
    }


    private static final int REQUEST_CODE_GALLERY_PICK=1;

    @Override
    public void onClick(View v) {

        if(v.getId()==R.id.fab1) {
            new DoConnect().execute(musicFile.getRealPath());


            final Rect rect = new Rect(0, 0, cardSearched.getWidth(), cardSearched.getHeight());
            cardSearched.requestRectangleOnScreen(rect, false);

        }
        if (v.getId()==R.id.cardBestSearched){
            Toast.makeText(this,"card best match clicked!",Toast.LENGTH_SHORT).show();
        }


        if(v.getId()==R.id.fabChooseArtworkFromGallery || v.getId()==R.id.artwortImageView){
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_CODE_GALLERY_PICK);
        }

        if(v.getId()==R.id.fabDeleteArtwork){
            artworkWasDeleted=true;
            GlideApp.with(this).load(R.drawable.vector_artwork_placeholder).error(R.drawable.vector_artwork_placeholder).into(artworkImageView);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null && resultCode==AppCompatActivity.RESULT_OK){
            switch (requestCode){
                case REQUEST_CODE_GALLERY_PICK:{
                    newArtworkUri = data.getData();
                    GlideApp.with(this).load(newArtworkUri).centerCrop().error(R.drawable.vector_artwork_placeholder).into(artworkImageView);
                    artWorkWasChanged=true;
                    break;
                }
            }
        }

    }

    /**
     * artWorkWasChanged for check state of artwork change or not fck my eng!
     */

    static boolean artWorkWasChanged=false;
    static boolean artworkWasDeleted=false;

    void updateMusicFile(MusicFile file){
        musicFile = file;
    }

    public MusicFile collectDataFromUI(){
        musicFile.setTitle(titleEdit.getText().toString());
        musicFile.setAlbum(albumEdit.getText().toString());
        musicFile.setArtist(artistEdit.getText().toString());
        musicFile.setYear(yearEdit.getText().toString());
        musicFile.setTrackNumber(trackNumberEdit.getText().toString());
        musicFile.setGenre(genreEdit.getText().toString());
        return musicFile;
    }

    public void saveChanges(MusicFile musicFile){
        TagManager tagManager = new TagManager(musicFile.getRealPath());
        tagManager.setTagsFromMusicFile(
                collectDataFromUI() );
        if(artWorkWasChanged){
            Bitmap bitmap = ((BitmapDrawable)artworkImageView.getDrawable()).getBitmap();

            tagManager.setArtwork(bitmap);
        }else if (artworkWasDeleted){
            tagManager.deleteArtwork();
        }
        /**
         * Section for album art change if it need !
         */

        //TODO change artwork with rights logic !!!!!

        long album_id = musicFile.getAlbum_id();

        dumpMedia(getApplicationContext());
        dumpAlbums(getApplicationContext());

        MediaStoreUtils.updateFileMediaStoreMedia(musicFile, this, (path, uri) -> {
            Log.i("ExternalStorage", "Scanned " + path + ":");
            Log.i("ExternalStorage", "-> uri=" + uri);
            dumpMedia(getApplicationContext());
            dumpAlbums(getApplicationContext());
            updateMusicFile( MediaStoreUtils.getMusicFileByPath(musicFile.getRealPath(),getApplicationContext()) );
            if (musicFile.getAlbum_id()==album_id){
                if(artWorkWasChanged){
                    Bitmap bitmap = ((BitmapDrawable)artworkImageView.getDrawable()).getBitmap();
                    Log.d("setArtwork()",String.valueOf(MediaStoreUtils.setAlbumArt(bitmap,getApplicationContext(),musicFile.getAlbum_id())));
                }else if (artworkWasDeleted){
                    MediaStoreUtils.deleteAlbumArt(getApplicationContext(),musicFile.getAlbum_id());
                }
            }
            /*NotificationCompat.Builder builder = new NotificationCompat.Builder(this,"musictagger")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("All finished")
                    .setContentText("Update tags finshed!")
                    .setAutoCancel(true);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.notify(1,builder.build());*/
            //initTagsInterface(musicFile);
        });


        //All done
    }

    public static final String CONST_FINGERPRINT = "FINGERPRINT=";
    public static final String CONST_DURATION = "DURATION=";

    public Map<String,Object> fpcalc(String in) {

        Log.d("fpCacl","my url: " + in);

        String[] args = {in};// {"-version"};// { in};
        String result = fpCalc(args);
        Map<String ,Object> map=new HashMap<>();
        String[] parts = result.split("\n");
        String f=null;
        Integer d=null;
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].startsWith(CONST_FINGERPRINT))
                f=parts[i].substring(CONST_FINGERPRINT.length());
            if (parts[i].startsWith(CONST_DURATION))
                d=Integer.parseInt(parts[i].substring(CONST_DURATION.length()));
        }
        Log.d("f",f);
        Log.d("d",String.valueOf(d));
        map.put("fingerprint",f);
        map.put("duration",d);
        return map;
    }

    class WriteChanges extends AsyncTask<MusicFile,Void,Void>{

        @Override
        protected Void doInBackground(MusicFile... files) {
            saveChanges(files[0]);
            return null;
        }

    }

    class ReadFromMediaStore extends AsyncTask<String,Void,MusicFile>{

        Context context;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            context=getApplicationContext();
        }

        @Override
        protected MusicFile doInBackground(String... strings) {
            MusicFile file = MediaStoreUtils.getMusicFileByPath(strings[0],context);
            TagManager manager = new TagManager(strings[0]);
            file.setGenre(manager.getGenre());
            return file;
        }

        @Override
        protected void onPostExecute(MusicFile file) {
            super.onPostExecute(file);
            initTagsInterface(file);
            musicFile=file;
        }
    }

    class DoConnect extends AsyncTask<String,Void,ID3V2> {

        Context context;
        String bmp=null;
        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            context=getApplicationContext();
        }

        @Override
        protected ID3V2 doInBackground(String... params){
            Map<String ,Object> map = fpcalc(params[0]);
            if(map==null)return null;
            ID3V2 tag=null;
            try {
                DataSet t = new DataSet((String) map.get("fingerprint"),(Integer)map.get("duration"));
                tag = t.getTags();
                t=null;

                if(tag.getArtlink()!=null && !tag.getArtlink().isEmpty()) {
                     bmp =tag.getArtlink().get(0);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return tag;
        }

        @Override
        protected void onPostExecute( ID3V2 track){
            super.onPostExecute(null);
            if(track!=null){
                Log.d("Title",track.getTitle());
                Log.d("Artist",track.getArtist());
                Log.d("Album",track.getAlbum());
                Log.d("Genre",track.getGenre());

                bestMathAlbumTextView.setText(track.getAlbum(),TextView.BufferType.EDITABLE);
                bestMathArtistTextView.setText(track.getArtist(),TextView.BufferType.EDITABLE);

                GlideApp.with(context).load(bmp).error(R.drawable.vector_artwork_placeholder).transition(withCrossFade()).into(bestMatchArtworkImageView);

            }else Toast.makeText(context,"К сожалению ничего не найдено",Toast.LENGTH_LONG).show();
        }
    }


}