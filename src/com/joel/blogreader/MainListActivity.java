package com.joel.blogreader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class MainListActivity extends ListActivity {

	public static final int NUMBER_OF_POST = 20;
	public static final String TAG = MainListActivity.class.getSimpleName();

	protected JSONObject mBlogData;

	private String msg = "Exception caught";
	
	protected ProgressBar mProgressBar;
	
	public final String KEY_TITLE = "title";
	public final String KEY_AUTHOR = "author";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_list);
		
		mProgressBar = (ProgressBar)findViewById(R.id.progressBar);
		if (isNetworkAvailable()) {
			mProgressBar.setVisibility(View.VISIBLE);
			GetBlogPost getBlogPost = new GetBlogPost();
			getBlogPost.execute();
		} else {
			Toast.makeText(this, "Network is unavailable", Toast.LENGTH_SHORT).show();
		}
		
	}
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
		super.onListItemClick(l, v, position, id);
		try {
			JSONArray jsonPosts = mBlogData.getJSONArray("posts");
			JSONObject jsonPost = jsonPosts.getJSONObject(position);
			String blogUrl = jsonPost.getString("url");
			Intent intent = new Intent(this, BlogPostWebView.class);
			intent.setData(Uri.parse(blogUrl));
			startActivity(intent);
		} catch (JSONException e) {
			Log.e(TAG, msg, e);
		}
						
	}

	private boolean isNetworkAvailable() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();

		boolean isAvailable = false;
		if (ni != null && ni.isConnected()) {
			isAvailable = true;
		}
		return isAvailable;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main_list, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);

	}

	public void handBlogResponse() {
		mProgressBar.setVisibility(View.INVISIBLE);
		if(mBlogData == null){
			updateDisplayForError();
		}else{
			try {
				JSONArray jsonPosts = mBlogData.getJSONArray("posts");
				//mBlogTitles = new String[jsonPosts.length()];
				ArrayList<HashMap<String, String>> blogPosts = new ArrayList<HashMap<String, String>>();
				for(int i = 0; i < jsonPosts.length(); i++){
					JSONObject post = jsonPosts.getJSONObject(i);
					String title = post.getString(KEY_TITLE);
					title = Html.fromHtml(title).toString();
					
					String author = post.getString(KEY_AUTHOR);
					author = Html.fromHtml(author).toString();
					
					HashMap<String, String> blogPost = new HashMap<String, String>();
					blogPost.put(KEY_TITLE, title);
					blogPost.put(KEY_AUTHOR, author);
					
					blogPosts.add(blogPost);
					
				}
				String[] keys = {KEY_TITLE, KEY_AUTHOR};
				int[] ids = {android.R.id.text1, android.R.id.text2};
				SimpleAdapter adapter = new SimpleAdapter(this, blogPosts, android.R.layout.simple_list_item_2, keys, ids);
				setListAdapter(adapter);
				
			} catch (JSONException e) {
				Log.e(TAG, "Exception Caught!", e);
			}
		}
	
	}

	private void updateDisplayForError() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Oops!!!");
		builder.setMessage("There was an error getting data from the blog");
		builder.setPositiveButton(android.R.string.ok, null);
		AlertDialog dialog = builder.create();
		dialog.show();
		
		TextView emptyTextView = (TextView)getListView().getEmptyView();
		emptyTextView.setText("No item Selected");
	}

	private class GetBlogPost extends AsyncTask<Object, Void, JSONObject> {

		@Override
		protected JSONObject doInBackground(Object... params) {
			int responseCode = -1;
			JSONObject jsonResponse = null;
			try {
				URL feedUrl = new URL("http://blog.teamtreehouse.com/api/get_recent_summary/?count="+ NUMBER_OF_POST);
				HttpURLConnection connection = (HttpURLConnection) feedUrl.openConnection();
				connection.connect();

				responseCode = connection.getResponseCode();
				if (responseCode == HttpURLConnection.HTTP_OK) {
					InputStream inputStream = connection.getInputStream();
					Reader reader = new InputStreamReader(inputStream);
					int contentLength = connection.getContentLength();
					char[] charArray = new char[contentLength];
					reader.read(charArray);
					String responseData = new String(charArray);

					jsonResponse = new JSONObject(responseData);
				} else {
					Log.i(TAG, "Unsuccesfull Response Code: " + responseCode);
				}
			} catch (MalformedURLException e) {
				Log.e(TAG, msg, e);
			} catch (IOException e) {
				Log.e(TAG, msg, e);
			} catch (Exception e) {
				Log.e(TAG, msg, e);
			}
			return jsonResponse;
		}

		@Override
		protected void onPostExecute(JSONObject result) {
			mBlogData = result;
			handBlogResponse();
		}
	}
}
